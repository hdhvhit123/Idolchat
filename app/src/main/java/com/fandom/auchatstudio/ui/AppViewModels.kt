package com.fandom.auchatstudio.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.fandom.auchatstudio.AppContainer
import com.fandom.auchatstudio.AuChatApp

/** The app-wide dependency container, reachable from any composable. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as AuChatApp).container
