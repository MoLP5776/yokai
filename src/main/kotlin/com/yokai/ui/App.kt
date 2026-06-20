package com.yokai.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.yokai.ui.screens.*
import com.yokai.ui.theme.colorScheme

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val state = remember { AppState(scope) }
    val colorScheme = state.prefs.appTheme.colorScheme(state.prefs.themeMode, state.prefs.pureBlackDarkMode)

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            when (state.currentScreen) {
                Screen.Library -> LibraryScreen(state)
                Screen.SeriesDetail -> SeriesDetailScreen(state)
                Screen.Reader -> ReaderScreen(state)
                Screen.Settings -> SettingsScreen(state)
                Screen.KeyBindings -> KeyBindingsScreen(state)
            }
        }
    }
}