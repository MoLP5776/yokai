package com.yokai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.yokai.reader.CbzReader
import com.yokai.ui.AppState
import kotlinx.coroutines.*

enum class PageStyle { SINGLE, DOUBLE, VERTICAL }

@Composable
fun ReaderScreen(state: AppState) {
    val chapter = state.readerChapter ?: return
    var pageNames by remember(chapter.filePath) { mutableStateOf<List<String>>(emptyList()) }
    var pageIndex by remember(chapter.filePath) { mutableIntStateOf(0) }

    var pageImage by remember(chapter.filePath, pageIndex) { mutableStateOf<ImageBitmap?>(null) }
    var pageImageRight by remember(chapter.filePath, pageIndex) { mutableStateOf<ImageBitmap?>(null) }

    var allPages by remember(chapter.filePath) { mutableStateOf<List<ImageBitmap>>(emptyList()) }

    var showChapterDropdown by remember { mutableStateOf(false) }
    var showPageDropdown by remember { mutableStateOf(false) }

    var pageStyle by remember { mutableStateOf(PageStyle.SINGLE) }
    var containToWidth by remember { mutableStateOf(false) }
    var containToHeight by remember { mutableStateOf(true) }
    var stretchSmallPages by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(chapter.filePath) {
        pageNames = withContext(Dispatchers.IO) { CbzReader.listPages(chapter.filePath) }
        pageIndex = if (state.openAtLastPage) {
            state.consumeOpenAtLastPage()
            pageNames.lastIndex.coerceAtLeast(0)
        } else {
            0
        }
        if (pageStyle == PageStyle.VERTICAL) {
            allPages = withContext(Dispatchers.IO) { CbzReader.loadAllPages(chapter.filePath) }
        }
        focusRequester.requestFocus()
    }

    LaunchedEffect(chapter.filePath, pageIndex, pageStyle) {
        when (pageStyle) {
            PageStyle.SINGLE -> {
                pageImage = null
                pageImage = withContext(Dispatchers.IO) { CbzReader.loadPage(chapter.filePath, pageIndex) }
                pageImageRight = null
            }

            PageStyle.DOUBLE -> {
                pageImage = null
                pageImageRight = null
                val (left, right) = withContext(Dispatchers.IO) {
                    CbzReader.loadTwoPages(chapter.filePath, pageIndex)
                }
                pageImage = left
                pageImageRight = right
            }

            PageStyle.VERTICAL -> {
                if (allPages.isEmpty()) {
                    allPages = withContext(Dispatchers.IO) { CbzReader.loadAllPages(chapter.filePath) }
                }
            }
        }
    }

    LaunchedEffect(pageStyle) {
        if (pageStyle == PageStyle.VERTICAL && allPages.isEmpty() && pageNames.isNotEmpty()) {
            allPages = withContext(Dispatchers.IO) { CbzReader.loadAllPages(chapter.filePath) }
        }
    }

    fun previousPage() {
        when {
            state.noNextChapterAvailable -> {
                state.clearNoNextChapter()
                pageIndex = pageNames.lastIndex.coerceAtLeast(0)
            }

            pageIndex == 0 -> state.previousChapterAtLastPage()
            pageStyle == PageStyle.DOUBLE -> pageIndex = (pageIndex - 2).coerceAtLeast(0)
            else -> pageIndex -= 1
        }
    }

    fun nextPage() {
        if (state.noNextChapterAvailable) {
            state.closeReader()
            return
        }
        val step = if (pageStyle == PageStyle.DOUBLE) 2 else 1
        if (pageIndex + step <= pageNames.lastIndex) {
            pageIndex += step
        } else {
            val currentSeriesDir = state.selectedSeries
            val currentChapter = state.readerChapter
            if (currentSeriesDir != null && currentChapter != null) {
                state.completeChapter(currentSeriesDir, currentChapter)
            }
            state.nextChapter()
        }
    }

    val contentScale = when {
        containToWidth && containToHeight -> ContentScale.Fit
        containToWidth -> ContentScale.FillWidth
        containToHeight -> ContentScale.FillHeight
        stretchSmallPages -> ContentScale.FillBounds
        else -> ContentScale.Fit
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

                        Key.RightBracket -> {
                            state.nextChapter(); true
                        }

                        Key.LeftBracket -> {
                            state.previousChapter(); true
                        }

                        Key.Backspace, Key.Escape -> {
                            state.closeReader(); true
                        }

                        else -> false
                    }
                } else false
            },
    ) {

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(260.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { state.closeReader() }) {
                    Icon(Icons.Outlined.Close, "Close reader")
                }
                Text(
                    text = state.selectedSeriesMetadata?.effectiveTitle ?: "Reader",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Chapter",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavButton(icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft, onClick = { state.previousChapter() }, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Ch. ${chapter.chapterNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { showChapterDropdown = true },
                    )
                    DropdownMenu(
                        expanded = showChapterDropdown,
                        onDismissRequest = { showChapterDropdown = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        state.selectedSeriesChapters.forEach { ch ->
                            DropdownMenuItem(
                                text = { Text("Ch. ${ch.chapterNumber}", fontSize = 13.sp) },
                                onClick = { state.openReader(ch); showChapterDropdown = false },
                            )
                        }
                    }
                }
                NavButton(icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight, onClick = { state.nextChapter() }, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Page",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavButton(icon = Icons.Outlined.KeyboardDoubleArrowLeft, onClick = { pageIndex = 0 }, modifier = Modifier.weight(1f))
                NavButton(icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft, onClick = { previousPage() }, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    val displayPage = if (pageStyle == PageStyle.DOUBLE)
                        "${pageIndex + 1}-${(pageIndex + 2).coerceAtMost(pageNames.size)}"
                    else
                        "${pageIndex + 1}"
                    Text(
                        text = "$displayPage / ${pageNames.size}",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { showPageDropdown = true },
                    )
                    DropdownMenu(
                        expanded = showPageDropdown,
                        onDismissRequest = { showPageDropdown = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        pageNames.indices.forEach { index ->
                            DropdownMenuItem(
                                text = { Text("Page ${index + 1}", fontSize = 12.sp, lineHeight = 12.sp) },
                                onClick = { pageIndex = index; showPageDropdown = false },
                            )
                        }
                    }
                }
                NavButton(icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight, onClick = { nextPage() }, modifier = Modifier.weight(1f))
                NavButton(
                    icon = Icons.Outlined.KeyboardDoubleArrowRight,
                    onClick = { pageIndex = pageNames.lastIndex.coerceAtLeast(0) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))

            Text(
                "Page Style",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PageStyleButton(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    label = "Single",
                    selected = pageStyle == PageStyle.SINGLE,
                    modifier = Modifier.weight(1f),
                    onClick = { pageStyle = PageStyle.SINGLE },
                )
                PageStyleButton(
                    icon = Icons.Outlined.AutoStories,
                    label = "Double",
                    selected = pageStyle == PageStyle.DOUBLE,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (pageIndex % 2 != 0) pageIndex = (pageIndex - 1).coerceAtLeast(0)
                        pageStyle = PageStyle.DOUBLE
                    },
                )
                PageStyleButton(
                    icon = Icons.Outlined.SwipeDown,
                    label = "Vertical",
                    selected = pageStyle == PageStyle.VERTICAL,
                    modifier = Modifier.weight(1f),
                    onClick = { pageStyle = PageStyle.VERTICAL },
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))

            Text(
                "Page Fit",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(4.dp))
            FitCheckRow("Contain to width", containToWidth) { containToWidth = it }
            FitCheckRow("Contain to height", containToHeight) { containToHeight = it }
            FitCheckRow("Stretch small pages", stretchSmallPages) { stretchSmallPages = it }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(pageNames, pageIndex, pageStyle) {
                    if (pageStyle != PageStyle.VERTICAL) {
                        detectTapGestures { offset ->
                            if (offset.x < size.width / 2) previousPage() else nextPage()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.noNextChapterAvailable -> {
                    Text(
                        "No next chapter available",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }

                pageNames.isEmpty() -> {
                    Text(
                        "No image pages in this archive",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }

                pageStyle == PageStyle.VERTICAL -> {
                    if (allPages.isEmpty()) {
                        Text("Loading...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            allPages.forEachIndexed { index, bitmap ->
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Page ${index + 1}",
                                    contentScale = if (containToWidth) ContentScale.FillWidth else ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            // Tap at bottom of vertical strip → next chapter
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clickable {
                                        val dir = state.selectedSeries
                                        val ch = state.readerChapter
                                        if (dir != null && ch != null) state.completeChapter(dir, ch)
                                        state.nextChapter()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "▼  Next chapter",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }

                pageStyle == PageStyle.DOUBLE -> {
                    val left = pageImage
                    val right = pageImageRight
                    when {
                        left == null -> Text(
                            "Loading...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        else -> Row(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                bitmap = left,
                                contentDescription = "Page ${pageIndex + 1}",
                                contentScale = contentScale,
                                modifier = Modifier.fillMaxHeight(),
                            )
                            if (right != null) {
                                Image(
                                    bitmap = right,
                                    contentDescription = "Page ${pageIndex + 2}",
                                    contentScale = contentScale,
                                    modifier = Modifier.fillMaxHeight(),
                                )
                            }
                        }
                    }
                }

                else -> {
                    val image = pageImage
                    if (image == null) {
                        Text("Loading...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else if (containToWidth && !containToHeight) {
                        // Page may be taller than the viewport; allow scrolling within just this
                        // page instead of clipping it, without crossing into other pages.
                        val pageScrollState = remember(chapter.filePath, pageIndex) { ScrollState(0) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(pageScrollState),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Image(
                                bitmap = image,
                                contentDescription = pageNames.getOrNull(pageIndex),
                                contentScale = contentScale,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        Image(
                            bitmap = image,
                            contentDescription = pageNames.getOrNull(pageIndex),
                            contentScale = contentScale,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageStyleButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val bgColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
        )
    }
}


@Composable
private fun FitCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
    }
}