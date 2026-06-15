package com.yokai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
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

enum class PageStyle { SINGLE, DOUBLE, VERTICAL }

@Composable
fun ReaderScreen(state: AppState) {
    val chapter = state.readerChapter ?: return
    var pageNames by remember(chapter.filePath) { mutableStateOf<List<String>>(emptyList()) }
    var pageIndex by remember(chapter.filePath) { mutableIntStateOf(0) }
    var pageImage by remember(chapter.filePath, pageIndex) { mutableStateOf<ImageBitmap?>(null) }

    var showChapterDropdown by remember { mutableStateOf(false) }
    var showPageDropdown by remember { mutableStateOf(false) }

    // Page Style and Fit states
    var pageStyle by remember { mutableStateOf(PageStyle.SINGLE) }
    var containToWidth by remember { mutableStateOf(true) }
    var containToHeight by remember { mutableStateOf(false) }
    var stretchSmallPages by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(chapter.filePath) {
        pageNames = withContext(Dispatchers.IO) { CbzReader.listPages(chapter.filePath) }
        pageIndex = 0
        focusRequester.requestFocus()
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
            // Mark current chapter as read before moving to the next
            val currentSeriesDir = state.selectedSeries
            val currentChapter = state.readerChapter
            if (currentSeriesDir != null && currentChapter != null) {
                state.markChapterRead(currentSeriesDir, currentChapter, true)
            }
            state.nextChapter()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionRight -> {
                            if (keyEvent.isCtrlPressed) pageIndex = pageNames.lastIndex else nextPage()
                            true
                        }
                        Key.DirectionLeft -> {
                            if (keyEvent.isCtrlPressed) pageIndex = 0 else previousPage()
                            true
                        }
                        Key.RightBracket -> { state.nextChapter(); true }
                        Key.LeftBracket -> { state.previousChapter(); true }
                        Key.Backspace, Key.Escape -> { state.closeReader(); true }
                        else -> false
                    }
                } else {
                    false
                }
            },
    ) {
        // Side panel
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(250.dp) // Fixed width for the side panel
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { state.closeReader() }) {
                    Icon(Icons.Outlined.Close, "Close reader")
                }
                Text(
                    text = state.selectedSeriesMetadata?.title ?: "Series Name",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.size(16.dp))

            // Chapter Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { state.previousChapter() }) {
                    Text("<", fontSize = 24.sp)
                }
                Box {
                    Text(
                        text = "Chapter ${chapter.chapterNumber.removePrefix("ch. 0").removePrefix("ch. ")}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showChapterDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showChapterDropdown,
                        onDismissRequest = { showChapterDropdown = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        state.selectedSeriesChapters.forEach { ch ->
                            DropdownMenuItem(
                                text = { Text("Chapter ${ch.chapterNumber.removePrefix("ch. 0").removePrefix("ch. ")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    state.openReader(ch)
                                    showChapterDropdown = false
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = { state.nextChapter() }) {
                    Text(">", fontSize = 24.sp)
                }
            }

            Spacer(Modifier.size(8.dp))

            // Page Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { pageIndex = 0 }) { // Beginning of chapter
                    Text("|<", fontSize = 20.sp)
                }
                IconButton(onClick = { previousPage() }) { // Back one page
                    Text("<", fontSize = 20.sp)
                }
                Box {
                    Text(
                        text = "${pageIndex + 1} / ${pageNames.size}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable { showPageDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showPageDropdown,
                        onDismissRequest = { showPageDropdown = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        (0 until pageNames.size).forEach { index ->
                            DropdownMenuItem(
                                text = { Text("Page ${index + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    pageIndex = index
                                    showPageDropdown = false
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = { nextPage() }) { // Forward one page
                    Text(">", fontSize = 20.sp)
                }
                IconButton(onClick = { pageIndex = pageNames.lastIndex }) { // End of chapter
                    Text(">|", fontSize = 20.sp)
                }
            }

            Spacer(Modifier.size(16.dp))

            // Collapsible Page Style
            var pageStyleExpanded by remember { mutableStateOf(false) }
            Column {
                Text(
                    text = "Page Style",
                    modifier = Modifier.clickable { pageStyleExpanded = !pageStyleExpanded },
                    fontWeight = FontWeight.Bold
                )
                if (pageStyleExpanded) {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = pageStyle == PageStyle.SINGLE, onClick = { pageStyle = PageStyle.SINGLE })
                            Text("Single")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = pageStyle == PageStyle.DOUBLE, onClick = { pageStyle = PageStyle.DOUBLE })
                            Text("Double")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = pageStyle == PageStyle.VERTICAL, onClick = { pageStyle = PageStyle.VERTICAL })
                            Text("Vertical")
                        }
                    }
                }
            }

            Spacer(Modifier.size(8.dp))

            // Collapsible Page Fit
            var pageFitExpanded by remember { mutableStateOf(false) }
            Column {
                Text(
                    text = "Page Fit",
                    modifier = Modifier.clickable { pageFitExpanded = !pageFitExpanded },
                    fontWeight = FontWeight.Bold
                )
                if (pageFitExpanded) {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = containToWidth, onCheckedChange = { containToWidth = it })
                            Text("Contain to width")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = containToHeight, onCheckedChange = { containToHeight = it })
                            Text("Contain to height")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = stretchSmallPages, onCheckedChange = { stretchSmallPages = it })
                            Text("Stretch small pages")
                        }
                    }
                }
            }
        }

        // Main content area for the manga page
        Box(
            modifier = Modifier
                .weight(1f) // Fills remaining space
                .fillMaxHeight()
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
                else -> {
                    // TODO: Implement actual rendering logic based on pageStyle and pageFit states
                    // For now, it displays a single page with ContentScale.Fit
                    Image(
                        bitmap = image,
                        contentDescription = pageNames.getOrNull(pageIndex),
                        contentScale = ContentScale.Fit, // This will need to change based on pageFit
                        modifier = Modifier
                            .fillMaxSize(0.95f)
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}