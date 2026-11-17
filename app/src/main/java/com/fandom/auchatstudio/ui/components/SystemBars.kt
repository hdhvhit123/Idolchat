package com.fandom.auchatstudio.ui.components

import android.app.Activity
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import java.util.WeakHashMap

/**
 * Makes status/navigation bar icons readable on a full-screen themed chat, restoring on exit.
 *
 * During a screen transition two themed screens are composed at once, and the one being left is
 * disposed *after* the new one has started. Each screen therefore registers a request, and the
 * most recent request still alive decides the icon colour; the appearance from before the first
 * request comes back once the last one is gone.
 */
@Composable
fun SystemBarsForBackground(darkBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(darkBackground) {
        val window = (view.context as? Activity)?.window
        val request = SystemBarRequests.Request(darkBackground)
        if (window != null) SystemBarRequests.add(window, request)
        onDispose {
            if (window != null) SystemBarRequests.remove(window, request)
        }
    }
}

/** Per-window stack of icon-colour requests. Only touched from the main thread. */
internal object SystemBarRequests {
    class Request(val darkBackground: Boolean)

    private class State(val lightStatus: Boolean, val lightNavigation: Boolean) {
        val requests = mutableListOf<Request>()
    }

    private val states = WeakHashMap<Window, State>()

    fun add(window: Window, request: Request) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val state = states.getOrPut(window) {
            State(controller.isAppearanceLightStatusBars, controller.isAppearanceLightNavigationBars)
        }
        state.requests += request
        apply(window, state)
    }

    fun remove(window: Window, request: Request) {
        val state = states[window] ?: return
        state.requests -= request
        apply(window, state)
        if (state.requests.isEmpty()) states.remove(window)
    }

    private fun apply(window: Window, state: State) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val top = state.requests.lastOrNull()
        controller.isAppearanceLightStatusBars = top?.let { !it.darkBackground } ?: state.lightStatus
        controller.isAppearanceLightNavigationBars = top?.let { !it.darkBackground } ?: state.lightNavigation
    }
}
