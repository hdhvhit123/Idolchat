package com.fandom.auchatstudio.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TapToChatReducerTest {
    private val alwaysType: (Int) -> Boolean = { true }
    private val neverType: (Int) -> Boolean = { false }

    private fun TapToChatState.on(event: TapToChatEvent, needsTyping: (Int) -> Boolean = alwaysType) =
        TapToChatReducer.reduce(this, event, needsTyping)

    @Test
    fun `tap starts typing then timer reveals message`() {
        val typing = TapToChatState(total = 3).on(TapToChatEvent.Tap)
        assertThat(typing.typingIndex).isEqualTo(0)
        assertThat(typing.revealed).isEqualTo(0)

        val shown = typing.on(TapToChatEvent.TypingFinished(0))
        assertThat(shown.revealed).isEqualTo(1)
        assertThat(shown.isTyping).isFalse()
    }

    @Test
    fun `tap while typing skips the wait`() {
        val state = TapToChatState(total = 3, revealed = 1, typingIndex = 1).on(TapToChatEvent.Tap)
        assertThat(state.revealed).isEqualTo(2)
        assertThat(state.typingIndex).isNull()
    }

    @Test
    fun `stale timer is ignored`() {
        val state = TapToChatState(total = 3, revealed = 2, typingIndex = 2)
        assertThat(state.on(TapToChatEvent.TypingFinished(1))).isEqualTo(state)
    }

    @Test
    fun `messages without typing appear immediately`() {
        val state = TapToChatState(total = 2).on(TapToChatEvent.Tap, neverType)
        assertThat(state.revealed).isEqualTo(1)
        assertThat(state.typingIndex).isNull()
    }

    @Test
    fun `tap at the end does nothing and reports finished`() {
        val end = TapToChatState(total = 2, revealed = 2)
        assertThat(end.isFinished).isTrue()
        assertThat(end.on(TapToChatEvent.Tap)).isEqualTo(end)
    }

    @Test
    fun `restart clears progress`() {
        val state = TapToChatState(total = 4, revealed = 3).on(TapToChatEvent.Restart)
        assertThat(state).isEqualTo(TapToChatState(total = 4))
    }

    @Test
    fun `shrinking the script clamps revealed and drops a typing index past the end`() {
        val state = TapToChatState(total = 5, revealed = 4, typingIndex = 4).on(TapToChatEvent.TotalChanged(3))
        assertThat(state.total).isEqualTo(3)
        assertThat(state.revealed).isEqualTo(3)
        assertThat(state.typingIndex).isNull()
        assertThat(state.isFinished).isTrue()
    }

    @Test
    fun `growing the script keeps progress`() {
        val state = TapToChatState(total = 2, revealed = 2).on(TapToChatEvent.TotalChanged(4))
        assertThat(state.revealed).isEqualTo(2)
        assertThat(state.isFinished).isFalse()
    }

    @Test
    fun `empty script is finished with full progress`() {
        val state = TapToChatState(total = 0)
        assertThat(state.isFinished).isTrue()
        assertThat(state.progress).isEqualTo(1f)
    }

    @Test
    fun `full playback through every message`() {
        var state = TapToChatState(total = 3)
        repeat(3) { i ->
            state = state.on(TapToChatEvent.Tap)
            state = state.on(TapToChatEvent.TypingFinished(i))
        }
        assertThat(state.isFinished).isTrue()
        assertThat(state.progress).isEqualTo(1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `revealed beyond total is rejected`() {
        TapToChatState(total = 1, revealed = 2)
    }

    @Test
    fun `typing duration grows with length and is clamped`() {
        val short = TypingTiming.durationFor(MessageType.TEXT, 1)
        val medium = TypingTiming.durationFor(MessageType.TEXT, 20)
        val huge = TypingTiming.durationFor(MessageType.TEXT, 10_000)
        assertThat(short).isEqualTo(TypingTiming.MIN_TEXT_MS)
        assertThat(medium).isGreaterThan(short)
        assertThat(huge).isEqualTo(TypingTiming.MAX_TEXT_MS)
        assertThat(TypingTiming.durationFor(MessageType.NARRATION, 50)).isEqualTo(0L)
        assertThat(TypingTiming.durationFor(MessageType.VOICE, 0)).isEqualTo(TypingTiming.VOICE_MS)
        assertThat(TypingTiming.durationFor(MessageType.IMAGE, 0)).isEqualTo(TypingTiming.IMAGE_MS)
    }
}
