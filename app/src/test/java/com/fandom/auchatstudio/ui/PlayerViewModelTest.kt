package com.fandom.auchatstudio.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fandom.auchatstudio.FakeImageStore
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.core.TypingTiming
import com.fandom.auchatstudio.data.AppDatabase
import com.fandom.auchatstudio.data.StudioRepository
import com.fandom.auchatstudio.ui.player.PlayerViewModel
import com.fandom.auchatstudio.waitFor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
class PlayerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repo: StudioRepository
    private var storyId = 0L
    private var idolId = 0L
    private var meId = 0L

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repo = StudioRepository(db, FakeImageStore())
        runBlocking {
            idolId = repo.saveCharacter(null, "Minho", 0xFFFF6584, false, null)
            meId = repo.saveCharacter(null, "Y/N", 0xFF43D8C9, true, null)
            storyId = repo.createStory("Test", ThemePreset.WEVERSE_DM)
            repo.addMessage(storyId, idolId, MessageType.TEXT, "Xin chào")
            repo.addMessage(storyId, null, MessageType.NARRATION, "Một lúc sau")
            repo.addMessage(storyId, meId, MessageType.TEXT, "Chào anh")
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun TestScope.loaded(): PlayerViewModel {
        val vm = PlayerViewModel(storyId, repo)
        waitFor(step = { runCurrent() }) { vm.state.value.playback.total == 3 }
        return vm
    }

    @Test
    fun tapShowsTypingThenRevealsAfterRealisticDelay() = runTest(dispatcher) {
        val vm = loaded()
        assertThat(vm.state.value.style.preset).isEqualTo(ThemePreset.WEVERSE_DM)
        assertThat(vm.state.value.visible).isEmpty()

        vm.tap()
        runCurrent()
        assertThat(vm.state.value.isTyping).isTrue()
        assertThat(vm.state.value.typingSpeaker?.id).isEqualTo(idolId)
        assertThat(vm.state.value.typingOutgoing).isFalse()

        val delay = TypingTiming.durationFor(MessageType.TEXT, "Xin chào".length)
        advanceTimeBy(delay - 50)
        runCurrent()
        assertThat(vm.state.value.visible).isEmpty()

        advanceTimeBy(100)
        runCurrent()
        assertThat(vm.state.value.visible.map { it.message.text }).containsExactly("Xin chào")
        assertThat(vm.state.value.isTyping).isFalse()
    }

    @Test
    fun narrationAppearsInstantlyAndTapSkipsTyping() = runTest(dispatcher) {
        val vm = loaded()
        vm.tap() // typing #0
        vm.tap() // skip -> revealed 1
        assertThat(vm.state.value.playback.revealed).isEqualTo(1)
        vm.tap() // narration -> no typing
        assertThat(vm.state.value.playback.revealed).isEqualTo(2)
        assertThat(vm.state.value.isTyping).isFalse()
        vm.tap() // typing #2 (outgoing)
        assertThat(vm.state.value.typingOutgoing).isTrue()
        vm.tap()
        assertThat(vm.state.value.playback.isFinished).isTrue()
        assertThat(vm.state.value.visible.last().outgoing).isTrue()

        vm.restart()
        assertThat(vm.state.value.playback.revealed).isEqualTo(0)
        assertThat(vm.state.value.visible).isEmpty()
    }

    @Test
    fun autoPlayRunsToTheEnd() = runTest(dispatcher) {
        val vm = loaded()
        vm.setAutoPlay(true)
        advanceUntilIdle()
        assertThat(vm.state.value.playback.isFinished).isTrue()
        assertThat(vm.state.value.visible).hasSize(3)
    }

    @Test
    fun staleTimerAfterSkipDoesNotDoubleReveal() = runTest(dispatcher) {
        val vm = loaded()
        vm.tap() // typing #0, timer scheduled
        vm.tap() // skip
        advanceTimeBy(TypingTiming.MAX_TEXT_MS + 10)
        runCurrent()
        assertThat(vm.state.value.playback.revealed).isEqualTo(1)
    }

    @Test
    fun editingTheScriptWhilePlayingUpdatesTotal() = runTest(dispatcher) {
        val vm = loaded()
        runBlocking { repo.addMessage(storyId, idolId, MessageType.VOICE, voiceSeconds = 4) }
        waitFor(step = { runCurrent() }) { vm.state.value.playback.total == 4 }
        assertThat(vm.state.value.playback.revealed).isEqualTo(0)
    }
}
