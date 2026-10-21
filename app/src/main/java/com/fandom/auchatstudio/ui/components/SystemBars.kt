package com.fandom.auchatstudio.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Makes status/navigation bar icons readable on a full-screen themed chat, restoring on exit. */
@Composable
fun SystemBarsForBackground(darkBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(darkBackground) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = !darkBackground
        controller?.isAppearanceLightNavigationBars = !darkBackground
        onDispose {
            if (controller != null && previousStatus != null && previousNav != null) {
                controller.isAppearanceLightStatusBars = previousStatus
                controller.isAppearanceLightNavigationBars = previousNav
            }
        }
    }
}
