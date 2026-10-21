package com.fandom.auchatstudio.ui.characters

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fandom.auchatstudio.core.NameRules
import com.fandom.auchatstudio.core.RolePalette
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.ImageStore
import com.fandom.auchatstudio.data.StudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the edit dialog wants done with the avatar. */
sealed interface AvatarChange {
    data object Keep : AvatarChange
    data object Remove : AvatarChange
    data class Picked(val uri: Uri) : AvatarChange
}

data class CharacterDraft(
    val name: String,
    val roleColorHex: Long,
    val isUser: Boolean,
    val avatar: AvatarChange,
)

class CharactersViewModel(private val repository: StudioRepository) : ViewModel() {

    val characters: StateFlow<List<CharacterEntity>?> = repository.characters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun suggestedColor(): Long = RolePalette.nextFree(characters.value.orEmpty().map { it.roleColorHex })

    /** Saves a new ([existing] == null) or edited character. Returns false if the name is blank. */
    fun save(existing: CharacterEntity?, draft: CharacterDraft, onDone: () -> Unit = {}): Boolean {
        val name = NameRules.normalizeName(draft.name) ?: return false
        viewModelScope.launch {
            _saving.value = true
            try {
                val avatarPath = when (val change = draft.avatar) {
                    AvatarChange.Keep -> existing?.avatarPath
                    AvatarChange.Remove -> null
                    is AvatarChange.Picked -> {
                        val prefix = "avatar_${existing?.id ?: "new"}"
                        repository.importImage(change.uri, prefix, ImageStore.AVATAR_MAX_SIZE)
                            ?: run {
                                _error.value = "Không đọc được ảnh đã chọn, giữ nguyên ảnh cũ."
                                existing?.avatarPath
                            }
                    }
                }
                repository.saveCharacter(existing, name, draft.roleColorHex, draft.isUser, avatarPath)
                onDone()
            } finally {
                _saving.value = false
            }
        }
        return true
    }

    fun delete(character: CharacterEntity) {
        viewModelScope.launch { repository.deleteCharacter(character) }
    }

    fun clearError() {
        _error.value = null
    }
}
