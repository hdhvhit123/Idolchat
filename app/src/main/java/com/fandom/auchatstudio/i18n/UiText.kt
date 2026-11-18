package com.fandom.auchatstudio.i18n

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * A message a ViewModel wants shown, kept as a string resource rather than finished text so it
 * is put into words in the language the screen is showing at the time it is shown.
 */
data class UiText(@StringRes val id: Int, val args: List<Any> = emptyList()) {
    constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())

    fun resolve(context: Context): String = context.getString(id, *args.toTypedArray())
}

@Composable
fun UiText.resolve(): String = resolve(LocalContext.current)
