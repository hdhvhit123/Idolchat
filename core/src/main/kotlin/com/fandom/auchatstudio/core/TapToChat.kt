package com.fandom.auchatstudio.core

/** Timing rules for the "Tap-to-Chat" reader. Everything is in milliseconds. */
object TypingTiming {
    const val MIN_TEXT_MS = 700L
    const val MAX_TEXT_MS = 2_400L
    const val VOICE_MS = 1_400L
    const val IMAGE_MS = 1_000L

    /**
     * How long the "..." typing indicator stays up before a message appears. Longer messages take
     * longer to type, which is what makes the playback feel real; narration appears instantly.
     */
    fun durationFor(type: MessageType, textLength: Int): Long = when (type) {
        MessageType.TEXT -> (450L + 45L * textLength.coerceAtLeast(0)).coerceIn(MIN_TEXT_MS, MAX_TEXT_MS)
        MessageType.VOICE -> VOICE_MS
        MessageType.IMAGE -> IMAGE_MS
        MessageType.NARRATION -> 0L
    }
}

/**
 * Immutable state of a Tap-to-Chat playback over [total] messages.
 *
 * [revealed] messages are visible. When [typingIndex] is non-null the message at that index is
 * "being typed" and will be revealed by [TapToChatEvent.TypingFinished] or by another tap.
 */
data class TapToChatState(
    val total: Int,
    val revealed: Int = 0,
    val typingIndex: Int? = null,
) {
    init {
        require(total >= 0) { "total must be >= 0" }
        require(revealed in 0..total) { "revealed must be within 0..total" }
    }

    val isTyping: Boolean get() = typingIndex != null
    val isFinished: Boolean get() = revealed >= total && typingIndex == null
    val progress: Float get() = if (total == 0) 1f else revealed.toFloat() / total
}

sealed interface TapToChatEvent {
    /** The reader tapped the screen. */
    data object Tap : TapToChatEvent

    /** The typing timer for [index] ran out. Stale timers (wrong index) are ignored. */
    data class TypingFinished(val index: Int) : TapToChatEvent

    data object Restart : TapToChatEvent

    /** The script was edited while playing; clamp to the new size. */
    data class TotalChanged(val total: Int) : TapToChatEvent
}

object TapToChatReducer {

    /**
     * @param needsTyping whether the message at an index shows a typing indicator first
     *   (narration lines and zero-delay messages don't).
     */
    fun reduce(
        state: TapToChatState,
        event: TapToChatEvent,
        needsTyping: (Int) -> Boolean,
    ): TapToChatState = when (event) {
        TapToChatEvent.Tap -> when {
            // A tap while typing skips the wait and shows the message right away.
            state.typingIndex != null -> state.copy(revealed = state.typingIndex + 1, typingIndex = null)
            state.revealed >= state.total -> state
            needsTyping(state.revealed) -> state.copy(typingIndex = state.revealed)
            else -> state.copy(revealed = state.revealed + 1)
        }

        is TapToChatEvent.TypingFinished ->
            if (state.typingIndex == event.index) {
                state.copy(revealed = event.index + 1, typingIndex = null)
            } else {
                state
            }

        TapToChatEvent.Restart -> TapToChatState(total = state.total)

        is TapToChatEvent.TotalChanged -> {
            val total = event.total.coerceAtLeast(0)
            val typing = state.typingIndex?.takeIf { it < total }
            TapToChatState(total = total, revealed = state.revealed.coerceAtMost(total), typingIndex = typing)
        }
    }
}
