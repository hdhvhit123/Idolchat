package com.fandom.auchatstudio.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small on-device flags that don't belong in the database. */
interface AppPrefs {
    /** True once the sample story has been offered; it is never re-created after that. */
    var sampleSeeded: Boolean

    /** Whether the "how it works" card on the home screen is still shown. */
    val showGuide: StateFlow<Boolean>

    fun dismissGuide()
}

class SharedAppPrefs(context: Context) : AppPrefs {
    private val prefs = context.applicationContext.getSharedPreferences("au_studio_prefs", Context.MODE_PRIVATE)

    override var sampleSeeded: Boolean
        get() = prefs.getBoolean(KEY_SEEDED, false)
        set(value) = prefs.edit { putBoolean(KEY_SEEDED, value) }

    private val _showGuide = MutableStateFlow(!prefs.getBoolean(KEY_GUIDE_DISMISSED, false))
    override val showGuide: StateFlow<Boolean> = _showGuide.asStateFlow()

    override fun dismissGuide() {
        prefs.edit { putBoolean(KEY_GUIDE_DISMISSED, true) }
        _showGuide.value = false
    }

    private companion object {
        const val KEY_SEEDED = "sample_seeded"
        const val KEY_GUIDE_DISMISSED = "guide_dismissed"
    }
}
