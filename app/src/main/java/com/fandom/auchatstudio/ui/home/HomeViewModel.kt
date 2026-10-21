package com.fandom.auchatstudio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fandom.auchatstudio.core.NameRules
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.StorySummary
import com.fandom.auchatstudio.data.StudioRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val stories: List<StorySummary> = emptyList(),
    val characters: List<CharacterEntity> = emptyList(),
)

class HomeViewModel(private val repository: StudioRepository) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        combine(repository.stories, repository.characters) { stories, characters ->
            HomeUiState(loading = false, stories = stories, characters = characters)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Creates a story and hands its id to [onCreated] (used to open the editor). */
    fun createStory(title: String, theme: ThemePreset, onCreated: (Long) -> Unit) {
        val clean = NameRules.normalizeName(title, NameRules.MAX_TITLE_LENGTH) ?: DEFAULT_TITLE
        viewModelScope.launch { onCreated(repository.createStory(clean, theme)) }
    }

    fun renameStory(id: Long, title: String) {
        val clean = NameRules.normalizeName(title, NameRules.MAX_TITLE_LENGTH) ?: return
        viewModelScope.launch { repository.renameStory(id, clean) }
    }

    fun deleteStory(id: Long) {
        viewModelScope.launch { repository.deleteStory(id) }
    }

    companion object {
        const val DEFAULT_TITLE = "Kịch bản chưa đặt tên"
    }
}
