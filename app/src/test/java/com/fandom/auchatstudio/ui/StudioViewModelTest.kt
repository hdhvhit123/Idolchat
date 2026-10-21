package com.fandom.auchatstudio.ui

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fandom.auchatstudio.FakeImageStore
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.data.AppDatabase
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.StudioRepository
import com.fandom.auchatstudio.export.ExportService
import com.fandom.auchatstudio.ui.chat.ChatItem
import com.fandom.auchatstudio.ui.chat.ChatStyle
import com.fandom.auchatstudio.ui.studio.Speaker
import com.fandom.auchatstudio.ui.studio.StudioEvent
import com.fandom.auchatstudio.ui.studio.StudioViewModel
import com.fandom.auchatstudio.waitFor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class StudioViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repo: StudioRepository
    private val images = FakeImageStore()
    private var storyId = 0L
    private var idolId = 0L
    private var meId = 0L

    private class FakeExporter : ExportService {
        var lastItems: List<ChatItem> = emptyList()
        var lastStyle: ChatStyle? = null
        override suspend fun export(
            title: String,
            headerTitle: String,
            partners: List<CharacterEntity>,
            items: List<ChatItem>,
            showNames: Boolean,
            style: ChatStyle,
        ): List<Uri> {
            lastItems = items
            lastStyle = style
            return listOf(Uri.parse("content://media/external/images/media/1"))
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repo = StudioRepository(db, images)
        runBlocking {
            meId = repo.saveCharacter(null, "Y/N", 0xFF43D8C9, true, null)
            idolId = repo.saveCharacter(null, "Minho", 0xFFFF6584, false, null)
            storyId = repo.createStory("Test", ThemePreset.LYSN_BUBBLE)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun TestScope.subscribed(exporter: ExportService? = null, events: MutableList<StudioEvent>? = null): StudioViewModel {
        val vm = StudioViewModel(storyId, repo, exporter)
        backgroundScope.launch { vm.state.collect {} }
        if (events != null) backgroundScope.launch { vm.eventFlow.collect { events += it } }
        waitFor(step = { runCurrent() }) { vm.state.value.story != null && vm.state.value.characters.size == 2 }
        return vm
    }

    private fun TestScope.awaitItems(vm: StudioViewModel, n: Int) =
        waitFor(step = { runCurrent() }) { vm.state.value.items.size == n }

    @Test
    fun defaultSpeakerIsFirstNonUserCharacter() = runTest(dispatcher) {
        val vm = subscribed()
        assertThat(vm.state.value.speaker).isEqualTo(Speaker.Character(idolId))
        assertThat(vm.state.value.speakerCharacter?.name).isEqualTo("Minho")
    }

    @Test
    fun sendTextUsesSelectedSpeakerAndTrims() = runTest(dispatcher) {
        val vm = subscribed()
        assertThat(vm.sendText("   ")).isFalse()
        assertThat(vm.sendText("  Còn thức không?  ")).isTrue()
        awaitItems(vm, 1)
        vm.selectSpeaker(Speaker.Character(meId))
        runCurrent()
        vm.sendText("Dạ còn")
        awaitItems(vm, 2)
        val items = vm.state.value.items
        assertThat(items[0].message.text).isEqualTo("Còn thức không?")
        assertThat(items[0].outgoing).isFalse()
        assertThat(items[1].outgoing).isTrue()
        assertThat(vm.state.value.headerTitle).isEqualTo("Minho")
    }

    @Test
    fun narratorSendsNarration() = runTest(dispatcher) {
        val vm = subscribed()
        vm.selectSpeaker(Speaker.Narrator)
        runCurrent()
        vm.sendText("Ba ngày sau...")
        awaitItems(vm, 1)
        val m = vm.state.value.items.single().message
        assertThat(m.type).isEqualTo(MessageType.NARRATION)
        assertThat(m.characterId).isNull()
    }

    @Test
    fun voiceAndImageMessages() = runTest(dispatcher) {
        val events = mutableListOf<StudioEvent>()
        val vm = subscribed(events = events)
        vm.sendVoice(999)
        awaitItems(vm, 1)
        assertThat(vm.state.value.items[0].message.voiceSeconds).isEqualTo(300)

        vm.sendImage(Uri.parse("content://picker/1"))
        awaitItems(vm, 2)
        assertThat(vm.state.value.items[1].message.imagePath).isEqualTo(images.nextImport)

        images.nextImport = null
        vm.sendImage(Uri.parse("content://picker/2"))
        waitFor(step = { runCurrent() }) { events.isNotEmpty() }
        assertThat((events.last() as StudioEvent.Toast).message).contains("Không đọc được ảnh")
        assertThat(vm.state.value.items).hasSize(2)
    }

    @Test
    fun cycleThemeRotatesPresets() = runTest(dispatcher) {
        val vm = subscribed()
        vm.cycleTheme()
        waitFor(step = { runCurrent() }) { vm.state.value.style.preset == ThemePreset.WEVERSE_DM }
        vm.cycleTheme()
        waitFor(step = { runCurrent() }) { vm.state.value.style.preset == ThemePreset.INSTAGRAM_DM }
    }

    @Test
    fun updateMessageNormalisesTimeAndRejectsBadInput() = runTest(dispatcher) {
        val events = mutableListOf<StudioEvent>()
        val vm = subscribed(events = events)
        vm.sendText("một")
        awaitItems(vm, 1)
        val msg = vm.state.value.items.single().message

        vm.updateMessage(msg, "hai", "9h05", 0)
        waitFor(step = { runCurrent() }) { vm.state.value.items.single().message.text == "hai" }
        assertThat(vm.state.value.items.single().message.timeLabel).isEqualTo("09:05")

        vm.updateMessage(msg, "  ", "", 0)
        runCurrent()
        assertThat(events.filterIsInstance<StudioEvent.Toast>().last().message).contains("không được để trống")

        vm.updateMessage(vm.state.value.items.single().message, "ba", "99:99", 0)
        waitFor(step = { runCurrent() }) { vm.state.value.items.single().message.text == "ba" }
        assertThat(vm.state.value.items.single().message.timeLabel).isNull()
    }

    @Test
    fun moveAndDelete() = runTest(dispatcher) {
        val vm = subscribed()
        vm.sendText("1"); awaitItems(vm, 1)
        vm.sendText("2"); awaitItems(vm, 2)
        vm.sendText("3"); awaitItems(vm, 3)
        val last = vm.state.value.items[2].message
        vm.move(last, -1)
        waitFor(step = { runCurrent() }) { vm.state.value.items.map { it.message.text } == listOf("1", "3", "2") }
        vm.delete(vm.state.value.items[0].message)
        awaitItems(vm, 2)
        assertThat(vm.state.value.items.map { it.message.text }).containsExactly("3", "2").inOrder()
    }

    @Test
    fun exportHandsCurrentChatToExporter() = runTest(dispatcher) {
        val exporter = FakeExporter()
        val events = mutableListOf<StudioEvent>()
        val vm = subscribed(exporter, events)
        vm.export()
        runCurrent()
        assertThat(events.filterIsInstance<StudioEvent.Toast>().single().message).contains("chưa có tin nhắn")

        vm.sendText("xin chào")
        awaitItems(vm, 1)
        vm.export()
        waitFor(step = { runCurrent() }) { events.any { it is StudioEvent.Exported } }
        assertThat(exporter.lastItems.single().message.text).isEqualTo("xin chào")
        assertThat(exporter.lastStyle?.preset).isEqualTo(ThemePreset.LYSN_BUBBLE)
        assertThat(vm.state.value.exporting).isFalse()
    }

    @Test
    fun deletedSpeakerFallsBackToAnotherCharacter() = runTest(dispatcher) {
        val vm = subscribed()
        runBlocking { repo.deleteCharacter(db.characterDao().getById(idolId)!!) }
        waitFor(step = { runCurrent() }) { vm.state.value.characters.size == 1 }
        assertThat(vm.state.value.speaker).isEqualTo(Speaker.Character(meId))
    }
}
