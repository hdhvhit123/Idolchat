package com.fandom.auchatstudio.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fandom.auchatstudio.core.TapToChatEvent
import com.fandom.auchatstudio.core.TapToChatReducer
import com.fandom.auchatstudio.core.TapToChatState
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.core.TypingTiming
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.MessageEntity
import com.fandom.auchatstudio.data.StoryEntity
import com.fandom.auchatstudio.data.StudioRepository
import com.fandom.auchatstudio.ui.chat.ChatItem
import com.fandom.auchatstudio.ui.chat.ChatLayout
import com.fandom.auchatstudio.ui.chat.ChatStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val loading: Boolean = true,
    val story: StoryEntity? = null,
    val headerTitle: String = "",
    val partners: List<CharacterEntity> = emptyList(),
    val isGroup: Boolean = false,
    val visible: List<ChatItem> = emptyList(),
    /** Who is "typing" right now, if anyone. */
    val typingSpeaker: CharacterEntity? = null,
    val typingOutgoing: Boolean = false,
    val playback: TapToChatState = TapToChatState(total = 0),
    val autoPlay: Boolean = false,
) {
    val style: ChatStyle get() = ChatStyle.of(story?.theme ?: ThemePreset.Default)
    val isTyping: Boolean get() = playback.isTyping
}

/**
 * Drives "Tap-to-Chat": each tap shows a typing indicator for a realistic time and then pops
 * the next message in. Tapping during typing skips the wait. Optional auto-play keeps going on
 * its own, which is handy for screen recording.
 */
class PlayerViewModel(
    private val storyId: Long,
    private val repository: StudioRepository,
) : ViewModel() {

    private data class Script(
        val story: StoryEntity?,
        val characters: List<CharacterEntity>,
        val messages: List<MessageEntity>,
    )

    private var script = Script(null, emptyList(), emptyList())
    private var typingJob: Job? = null
    private var autoJob: Job? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.story(storyId), repository.characters, repository.messages(storyId)) { s, c, m -> Script(s, c, m) }
                .collect { next ->
                    script = next
                    val current = _state.value.playback
                    val playback = if (current.total == next.messages.size) {
                        current
                    } else {
                        TapToChatReducer.reduce(current, TapToChatEvent.TotalChanged(next.messages.size), ::needsTyping)
                    }
                    publish(playback, loading = false)
                }
        }
    }

    fun tap() {
        autoJob?.cancel()
        dispatch(TapToChatEvent.Tap)
    }

    fun restart() {
        typingJob?.cancel()
        autoJob?.cancel()
        dispatch(TapToChatEvent.Restart)
        if (_state.value.autoPlay) scheduleAuto()
    }

    fun setAutoPlay(enabled: Boolean) {
        _state.update { it.copy(autoPlay = enabled) }
        autoJob?.cancel()
        if (enabled && !_state.value.isTyping) scheduleAuto(immediate = true)
    }

    private fun needsTyping(index: Int): Boolean {
        val m = script.messages.getOrNull(index) ?: return false
        return TypingTiming.durationFor(m.type, m.text.length) > 0
    }

    private fun dispatch(event: TapToChatEvent) {
        val before = _state.value.playback
        val after = TapToChatReducer.reduce(before, event, ::needsTyping)
        if (after == before) return
        publish(after)
        val typing = after.typingIndex
        if (typing != null && typing != before.typingIndex) {
            startTypingTimer(typing)
        } else if (typing == null) {
            typingJob?.cancel()
            if (after.revealed > before.revealed && _state.value.autoPlay) scheduleAuto()
        }
    }

    private fun startTypingTimer(index: Int) {
        typingJob?.cancel()
        val message = script.messages.getOrNull(index) ?: return
        val duration = TypingTiming.durationFor(message.type, message.text.length)
        typingJob = viewModelScope.launch {
            delay(duration)
            dispatch(TapToChatEvent.TypingFinished(index))
        }
    }

    private fun scheduleAuto(immediate: Boolean = false) {
        autoJob?.cancel()
        if (_state.value.playback.isFinished) return
        autoJob = viewModelScope.launch {
            delay(if (immediate) 250L else AUTO_GAP_MS)
            dispatch(TapToChatEvent.Tap)
        }
    }

    private fun publish(playback: TapToChatState, loading: Boolean = _state.value.loading) {
        val (story, characters, messages) = script
        val shown = messages.take(playback.revealed)
        val typingMessage = playback.typingIndex?.let(messages::getOrNull)
        val typingSpeaker = typingMessage?.characterId?.let { id -> characters.firstOrNull { it.id == id } }
        _state.update {
            it.copy(
                loading = loading,
                story = story,
                headerTitle = ChatLayout.headerTitle(story?.chatTitle, messages, characters),
                partners = ChatLayout.partners(messages, characters),
                isGroup = ChatLayout.isGroup(messages, characters),
                visible = ChatLayout.build(shown, characters),
                typingSpeaker = typingSpeaker,
                typingOutgoing = typingSpeaker?.isUser == true,
                playback = playback,
            )
        }
    }

    private companion object {
        const val AUTO_GAP_MS = 900L
    }
}
