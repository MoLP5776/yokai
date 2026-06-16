package com.yokai

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.yokai.ui.App

fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Yokai",
        state = windowState,
    ) {
        App()
    }
}
