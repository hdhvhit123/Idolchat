package com.fandom.auchatstudio.ui.studio

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.NameRules
import com.fandom.auchatstudio.core.TimeLabel
import com.fandom.auchatstudio.core.VoiceFormat
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.MessageEntity
import com.fandom.auchatstudio.data.StoryEntity
import com.fandom.auchatstudio.data.StudioRepository
import com.fandom.auchatstudio.export.ExportService
import com.fandom.auchatstudio.ui.chat.ChatItem
import com.fandom.auchatstudio.ui.chat.ChatLayout
import com.fandom.auchatstudio.ui.chat.ChatStyle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Who the next message is sent as ("Director Switcher"). */
sealed interface Speaker {
    data object Narrator : Speaker
    data class Character(val id: Long) : Speaker
}

data class StudioUiState(
    val loading: Boolean = true,
    val story: StoryEntity? = null,
    val characters: List<CharacterEntity> = emptyList(),
    val items: List<ChatItem> = emptyList(),
    val isGroup: Boolean = false,
    val headerTitle: String = "",
    val partners: List<CharacterEntity> = emptyList(),
    val speaker: Speaker? = null,
    val exporting: Boolean = false,
) {
    val missing: Boolean get() = !loading && story == null
    val style: ChatStyle get() = ChatStyle.of(story?.theme ?: com.fandom.auchatstudio.core.ThemePreset.Default)
    val speakerCharacter: CharacterEntity?
        get() = (speaker as? Speaker.Character)?.let { s -> characters.firstOrNull { it.id == s.id } }
}

sealed interface StudioEvent {
    data class Toast(val message: String) : StudioEvent
    data class Exported(val uris: List<Uri>) : StudioEvent
}

class StudioViewModel(
    private val storyId: Long,
    private val repository: StudioRepository,
    private val exporter: ExportService?,
) : ViewModel() {

    private val chosenSpeaker = MutableStateFlow<Speaker?>(null)
    private val exporting = MutableStateFlow(false)
    private val events = Channel<StudioEvent>(Channel.BUFFERED)
    val eventFlow: Flow<StudioEvent> = events.receiveAsFlow()

    val state: StateFlow<StudioUiState> = combine(
        repository.story(storyId),
        repository.characters,
        repository.messages(storyId),
        chosenSpeaker,
        exporting,
    ) { story, characters, messages, chosen, isExporting ->
        StudioUiState(
            loading = false,
            story = story,
            characters = characters,
            items = ChatLayout.build(messages, characters),
            isGroup = ChatLayout.isGroup(messages, characters),
            headerTitle = ChatLayout.headerTitle(story?.chatTitle, messages, characters),
            partners = ChatLayout.partners(messages, characters).ifEmpty { characters.filterNot { it.isUser }.take(1) },
            speaker = resolveSpeaker(chosen, characters),
            exporting = isExporting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudioUiState())

    /** Keeps the choice valid if its character was deleted; defaults to the first idol. */
    private fun resolveSpeaker(chosen: Speaker?, characters: List<CharacterEntity>): Speaker? = when {
        chosen == Speaker.Narrator -> chosen
        chosen is Speaker.Character && characters.any { it.id == chosen.id } -> chosen
        else -> (characters.firstOrNull { !it.isUser } ?: characters.firstOrNull())?.let { Speaker.Character(it.id) }
    }

    fun selectSpeaker(speaker: Speaker) {
        chosenSpeaker.value = speaker
    }

    private fun currentSpeaker(): Speaker? = state.value.speaker ?: resolveSpeaker(chosenSpeaker.value, state.value.characters)

    /** Sends a text (or narration) line as the current speaker. Returns false if nothing was sent. */
    fun sendText(text: String): Boolean {
        val body = text.trim()
        if (body.isEmpty()) return false
        val speaker = currentSpeaker() ?: run {
            toast("Hãy thêm nhân vật trước khi nhắn")
            return false
        }
        viewModelScope.launch {
            when (speaker) {
                Speaker.Narrator -> repository.addMessage(storyId, null, MessageType.NARRATION, body)
                is Speaker.Character -> repository.addMessage(storyId, speaker.id, MessageType.TEXT, body)
            }
        }
        return true
    }

    fun sendVoice(seconds: Int) {
        val speaker = currentSpeaker() as? Speaker.Character ?: run {
            toast("Chọn một nhân vật để gửi tin nhắn thoại")
            return
        }
        viewModelScope.launch {
            repository.addMessage(storyId, speaker.id, MessageType.VOICE, voiceSeconds = VoiceFormat.clampSeconds(seconds))
        }
    }

    fun sendImage(uri: Uri) {
        val speaker = currentSpeaker() as? Speaker.Character ?: run {
            toast("Chọn một nhân vật để gửi ảnh")
            return
        }
        viewModelScope.launch {
            val path = repository.importImage(uri, "msg_$storyId")
            if (path == null) {
                toast("Không đọc được ảnh đã chọn")
            } else {
                repository.addMessage(storyId, speaker.id, MessageType.IMAGE, imagePath = path)
            }
        }
    }

    /** Saves edits from the message editor. A blank or invalid time label clears it. */
    fun updateMessage(message: MessageEntity, text: String, timeLabel: String, voiceSeconds: Int) {
        val needsText = message.type == MessageType.TEXT || message.type == MessageType.NARRATION
        if (needsText && text.isBlank()) {
            toast("Nội dung không được để trống")
            return
        }
        val label = timeLabel.takeIf { it.isNotBlank() }?.let { TimeLabel.normalize(it) }
        if (timeLabel.isNotBlank() && label == null) toast("Giờ không hợp lệ, dùng dạng 23:41")
        viewModelScope.launch {
            repository.updateMessage(
                message.copy(
                    text = if (needsText) text.trim() else message.text,
                    timeLabel = label,
                    voiceSeconds = if (message.type == MessageType.VOICE) VoiceFormat.clampSeconds(voiceSeconds) else message.voiceSeconds,
                ),
            )
        }
    }

    fun changeSpeaker(message: MessageEntity, characterId: Long) {
        if (message.type == MessageType.NARRATION) return
        viewModelScope.launch { repository.updateMessage(message.copy(characterId = characterId)) }
    }

    fun move(message: MessageEntity, delta: Int) {
        val index = state.value.items.indexOfFirst { it.id == message.id }
        if (index < 0) return
        viewModelScope.launch { repository.moveMessage(storyId, index, index + delta) }
    }

    fun delete(message: MessageEntity) {
        viewModelScope.launch { repository.deleteMessage(message) }
    }

    fun cycleTheme() {
        val story = state.value.story ?: return
        val next = story.theme.next()
        viewModelScope.launch { repository.setTheme(storyId, next) }
        toast("Phong cách: ${next.displayName}")
    }

    fun rename(title: String) {
        val clean = NameRules.normalizeName(title, NameRules.MAX_TITLE_LENGTH) ?: return
        viewModelScope.launch { repository.renameStory(storyId, clean) }
    }

    fun setChatTitle(title: String) {
        val clean = NameRules.normalizeName(title)
        viewModelScope.launch { repository.setChatTitle(storyId, clean) }
    }

    fun export() {
        val service = exporter ?: return
        val s = state.value
        val story = s.story ?: return
        if (s.items.isEmpty()) {
            toast("Kịch bản chưa có tin nhắn để xuất")
            return
        }
        if (exporting.value) return
        exporting.value = true
        viewModelScope.launch {
            try {
                val uris = service.export(
                    title = story.title,
                    headerTitle = s.headerTitle,
                    partners = s.partners,
                    items = s.items,
                    showNames = s.isGroup,
                    style = s.style,
                )
                if (uris.isEmpty()) toast("Xuất ảnh thất bại") else events.send(StudioEvent.Exported(uris))
            } catch (e: Exception) {
                toast("Xuất ảnh thất bại: ${e.message ?: "lỗi không rõ"}")
            } finally {
                exporting.value = false
            }
        }
    }

    private fun toast(message: String) {
        events.trySend(StudioEvent.Toast(message))
    }
}
