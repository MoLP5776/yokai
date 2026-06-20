package com.yokai

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.yokai.ui.App
import com.yokai.yokai.generated.resources.Res
import com.yokai.yokai.generated.resources.icon
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 800.dp,
        position = WindowPosition.Aligned(Alignment.Center),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Yokai",
        state = windowState,
        icon = painterResource(Res.drawable.icon),
    ) {
        App()
    }
}
