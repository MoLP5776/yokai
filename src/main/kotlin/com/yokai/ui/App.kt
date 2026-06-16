package com.yokai.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.yokai.ui.screens.*

private val YokaiColorScheme = darkColorScheme(
    primary = Color(0xFFE85D4F),
    secondary = Color(0xFF70C1B3),
    tertiary = Color(0xFFF5C542),
    background = Color(0xFF181716),
    surface = Color(0xFF242220),
    surfaceVariant = Color(0xFF36322F),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFF1EFEB),
    onSurface = Color(0xFFF1EFEB),
    onSurfaceVariant = Color(0xFFD5D0C8),
)

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val state = remember { AppState(scope) }

    MaterialTheme(colorScheme = YokaiColorScheme) {
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