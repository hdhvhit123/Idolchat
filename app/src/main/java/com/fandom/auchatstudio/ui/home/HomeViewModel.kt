package com.fandom.auchatstudio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fandom.auchatstudio.core.NameRules
import com.fandom.auchatstudio.core.StoryTemplate
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.data.AppPrefs
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.StorySummary
import com.fandom.auchatstudio.data.StudioRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val stories: List<StorySummary> = emptyList(),
    val characters: List<CharacterEntity> = emptyList(),
    /** First-run "how it works" card. */
    val showGuide: Boolean = false,
) {
    /** The story the user most recently worked on (the list is sorted newest first). */
    val latest: StorySummary? get() = stories.firstOrNull()
}

class HomeViewModel(
    private val repository: StudioRepository,
    private val prefs: AppPrefs,
) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        combine(repository.ready, repository.stories, repository.characters, prefs.showGuide) { ready, stories, characters, guide ->
            HomeUiState(loading = !ready, stories = stories, characters = characters, showGuide = guide)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val messages = Channel<String>(Channel.BUFFERED)

    /** One-off confirmations for a snackbar. */
    val messageFlow: Flow<String> = messages.receiveAsFlow()

    /** Creates a story and hands its id to [onCreated] (used to open the editor). */
    fun createStory(title: String, theme: ThemePreset, onCreated: (Long) -> Unit) {
        val clean = NameRules.normalizeName(title, NameRules.MAX_TITLE_LENGTH) ?: DEFAULT_TITLE
        viewModelScope.launch { onCreated(repository.createStory(clean, theme)) }
    }

    fun createFromTemplate(template: StoryTemplate, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repository.createStoryFromTemplate(template)) }
    }

    fun renameStory(id: Long, title: String) {
        val clean = NameRules.normalizeName(title, NameRules.MAX_TITLE_LENGTH) ?: return
        viewModelScope.launch { repository.renameStory(id, clean) }
    }

    fun duplicateStory(id: Long) {
        viewModelScope.launch {
            val copy = repository.duplicateStory(id)
            messages.trySend(if (copy != null) "Đã tạo bản sao" else "Không tạo được bản sao")
        }
    }

    fun deleteStory(id: Long) {
        viewModelScope.launch {
            repository.deleteStory(id)
            messages.trySend("Đã xóa kịch bản")
        }
    }

    fun dismissGuide() = prefs.dismissGuide()

    companion object {
        const val DEFAULT_TITLE = "Kịch bản chưa đặt tên"
    }
}
