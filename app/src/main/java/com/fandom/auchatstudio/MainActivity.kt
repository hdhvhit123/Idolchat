package com.fandom.auchatstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fandom.auchatstudio.navigation.AppNavHost
import com.fandom.auchatstudio.ui.theme.AUChatStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AUChatStudioTheme {
                AppNavHost()
            }
        }
    }
}
