package com.yokai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yokai.metadata.ChapterInfo
import com.yokai.reader.CbzReader
import com.yokai.ui.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.clickable // Added this import to resolve 'clickable'

@Composable
fun ReaderScreen(state: AppState) {
    val chapter = state.readerChapter ?: return
    var pageNames by remember(chapter.filePath) { mutableStateOf<List<String>>(emptyList()) }
    var pageIndex by remember(chapter.filePath) { mutableIntStateOf(0) }
    var pageImage by remember(chapter.filePath, pageIndex) { mutableStateOf<ImageBitmap?>(null) }
    var showChapterDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(chapter.filePath) {
        pageNames = withContext(Dispatchers.IO) { CbzReader.listPages(chapter.filePath) }
        pageIndex = 0
    }

    LaunchedEffect(chapter.filePath, pageIndex) {
        pageImage = null
        pageImage = withContext(Dispatchers.IO) { CbzReader.loadPage(chapter.filePath, pageIndex) }
    }

    fun previousPage() {
        if (pageIndex > 0) pageIndex -= 1
    }

    fun nextPage() {
        if (pageIndex < pageNames.lastIndex) {
            pageIndex += 1
        } else {
            state.nextChapter()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val keyName = keyEvent.key.toString()
                    val action = state.keybindings.matches(
                        keyName,
                        keyEvent.isCtrlPressed,
                        keyEvent.isShiftPressed,
                        keyEvent.isAltPressed
                    )
                    when (action) {
                        "nextPage" -> { nextPage(); true }
                        "prevPage" -> { previousPage(); true }
                        "nextChapter" -> { state.nextChapter(); true }
                        "prevChapter" -> {
                            val current = state.readerChapter
                            val chapters = state.selectedSeriesChapters
                            val currentIndex = chapters.indexOfFirst { it.filePath == current?.filePath }
                            if (currentIndex > 0) state.openReader(chapters[currentIndex - 1])
                            true
                        }
                        "exitReader" -> { state.closeReader(); true }
                        "closeReader" -> { state.closeReader(); true }
                        else -> false
                    }
                } else {
                    false
                }
            },
    ) {
        // Main content area for the manga page
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pageNames, pageIndex) {
                    detectTapGestures { offset ->
                        if (offset.x < size.width / 2) previousPage() else nextPage()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val image = pageImage
            when {
                state.noNextChapterAvailable -> Text("No next chapter available", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                pageNames.isEmpty() -> Text("No image pages in this archive", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                image == null -> Text("Loading page...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                else -> Image(
                    bitmap = image,
                    contentDescription = pageNames.getOrNull(pageIndex),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize(0.95f)
                        .padding(12.dp),
                )
            }
        }

        // Top overlay bar for controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.5f)) // Semi-transparent background
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { state.closeReader() }) {
                Icon(Icons.Outlined.Close, "Close reader", tint = Color.White)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Chapter",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Box {
                    Text(
                        text = chapter.chapterNumber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable { showChapterDropdown = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    DropdownMenu(
                        expanded = showChapterDropdown,
                        onDismissRequest = { showChapterDropdown = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        state.selectedSeriesChapters.forEach { ch ->
                            DropdownMenuItem(
                                text = { Text(ch.chapterNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    state.openReader(ch)
                                    showChapterDropdown = false
                                },
                            )
                        }
                    }
                }
                Text(
                    text = "Page ${if (pageNames.isEmpty()) "0 / 0" else "${pageIndex + 1} / ${pageNames.size}"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            IconButton(onClick = { /* TODO: Implement settings */ }) {
                Icon(Icons.Outlined.Settings, "Settings", tint = Color.White)
            }
        }
    }
}