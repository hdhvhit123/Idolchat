package com.fandom.auchatstudio.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fandom.auchatstudio.FakeAppPrefs
import com.fandom.auchatstudio.FakeImageStore
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.StoryTemplates
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.data.AppDatabase
import com.fandom.auchatstudio.data.StudioRepository
import com.fandom.auchatstudio.ui.home.HomeViewModel
import com.fandom.auchatstudio.waitFor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repo: StudioRepository
    private val prefs = FakeAppPrefs()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repo = StudioRepository(db, FakeImageStore())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun TestScope.subscribed(messages: MutableList<String>? = null): HomeViewModel {
        val vm = HomeViewModel(repo, prefs)
        backgroundScope.launch { vm.state.collect {} }
        if (messages != null) backgroundScope.launch { vm.messageFlow.collect { messages += it } }
        return vm
    }

    @Test
    fun staysLoadingUntilStartUpWorkIsDoneSoNoFalseEmptyState() = runTest(dispatcher) {
        val vm = subscribed()
        runCurrent()
        assertThat(vm.state.value.loading).isTrue()

        runBlocking { repo.initialize(seedSample = true) }
        waitFor(step = { runCurrent() }) { !vm.state.value.loading && vm.state.value.stories.size == 1 }
        assertThat(vm.state.value.latest?.story?.title).isEqualTo("Tin nhắn lúc nửa đêm")
        assertThat(vm.state.value.characters).hasSize(3)
    }

    @Test
    fun guideIsShownUntilDismissed() = runTest(dispatcher) {
        runBlocking { repo.initialize(seedSample = false) }
        val vm = subscribed()
        waitFor(step = { runCurrent() }) { !vm.state.value.loading }
        assertThat(vm.state.value.showGuide).isTrue()

        vm.dismissGuide()
        waitFor(step = { runCurrent() }) { !vm.state.value.showGuide }
        assertThat(prefs.showGuide.value).isFalse()
    }

    @Test
    fun templateCreatesAStoryAndReportsItsIdForNavigation() = runTest(dispatcher) {
        runBlocking { repo.initialize(seedSample = false) }
        val vm = subscribed()
        var opened = -1L

        vm.createFromTemplate(StoryTemplates.all.first()) { opened = it }
        waitFor(step = { runCurrent() }) { opened > 0 }

        val template = StoryTemplates.all.first()
        assertThat(repo.story(opened).first()?.title).isEqualTo(template.title)
        assertThat(repo.messages(opened).first()).hasSize(template.lines.size)
    }

    @Test
    fun blankTitleFallsBackToADefaultNameInsteadOfFailing() = runTest(dispatcher) {
        runBlocking { repo.initialize(seedSample = false) }
        val vm = subscribed()
        var opened = -1L

        vm.createStory("   ", ThemePreset.INSTAGRAM_DM) { opened = it }
        waitFor(step = { runCurrent() }) { opened > 0 }

        val story = repo.story(opened).first()!!
        assertThat(story.title).isEqualTo(HomeViewModel.DEFAULT_TITLE)
        assertThat(story.theme).isEqualTo(ThemePreset.INSTAGRAM_DM)
    }

    @Test
    fun duplicateAndDeleteConfirmWithAMessage() = runTest(dispatcher) {
        runBlocking { repo.initialize(seedSample = false) }
        val id = runBlocking {
            val a = repo.saveCharacter(null, "A", 0xFF000000, false, null)
            repo.createStory("Gốc", ThemePreset.LYSN_BUBBLE).also { repo.addMessage(it, a, MessageType.TEXT, "một") }
        }
        val messages = mutableListOf<String>()
        val vm = subscribed(messages)
        waitFor(step = { runCurrent() }) { vm.state.value.stories.size == 1 }

        vm.duplicateStory(id)
        waitFor(step = { runCurrent() }) { vm.state.value.stories.size == 2 && messages.size == 1 }
        assertThat(messages.last()).isEqualTo("Đã tạo bản sao")
        assertThat(vm.state.value.stories.map { it.story.title }).containsExactly("Gốc (bản sao)", "Gốc")

        vm.deleteStory(id)
        waitFor(step = { runCurrent() }) { vm.state.value.stories.size == 1 && messages.size == 2 }
        assertThat(messages.last()).isEqualTo("Đã xóa kịch bản")
        assertThat(vm.state.value.stories.single().story.title).isEqualTo("Gốc (bản sao)")
    }
}
