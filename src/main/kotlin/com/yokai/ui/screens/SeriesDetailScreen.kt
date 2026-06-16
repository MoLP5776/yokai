package com.yokai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yokai.metadata.ChapterInfo
import com.yokai.metadata.LANGUAGE_FLAGS
import com.yokai.metadata.LANGUAGE_NAMES
import com.yokai.metadata.SeriesMetadata
import com.yokai.metadata.SeriesStatus
import com.yokai.ui.AppState
import com.yokai.ui.Screen
import com.yokai.ui.components.ChapterContextMenu
import com.yokai.ui.components.CoverArt
import com.yokai.ui.components.MetadataEditorDialog
import com.yokai.ui.components.Sidebar
import java.io.File
import kotlin.math.ceil

@Composable
fun SeriesDetailScreen(state: AppState) {
    val metadata = state.selectedSeriesMetadata ?: return
    val chapters = state.selectedSeriesChapters
    val readState = metadata.chapterReadState
    val seriesDir = state.selectedSeries ?: return
    var sortDescending by remember { mutableStateOf(state.prefs.defaultSortDescending) }
    var showMetadataEditor by remember { mutableStateOf(false) }
    val sortedChapters =
        if (sortDescending) chapters.sortedByDescending { it.chapterFloat } else chapters.sortedBy { it.chapterFloat }

    var currentPage by remember { mutableStateOf(1) }
    var chaptersPerPage by remember { mutableStateOf(10) }
    var showChaptersPerPageDropdown by remember { mutableStateOf(false) }

    var showChapterContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var contextMenuChapter by remember { mutableStateOf<ChapterInfo?>(null) }

    val totalPages = ceil(sortedChapters.size.toDouble() / chaptersPerPage).toInt().coerceAtLeast(1)
    val startIndex = (currentPage - 1) * chaptersPerPage
    val endIndex = (startIndex + chaptersPerPage).coerceAtMost(sortedChapters.size)
    val paginatedChapters = sortedChapters.subList(startIndex, endIndex)

    if (showMetadataEditor) {
        MetadataEditorDialog(
            seriesDir = seriesDir,
            metadata = metadata,
            onDismiss = { showMetadataEditor = false },
            onSave = { updated ->
                state.saveMetadata(updated)
                showMetadataEditor = false
            },
        )
    }

    Row(Modifier.fillMaxSize()) {
        Sidebar(state)

        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                IconButton(onClick = {
                    state.clearChapterSelection()
                    state.currentScreen = Screen.Library
                }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                }
                Text(
                    metadata.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showMetadataEditor = true }) {
                    Icon(Icons.Outlined.Edit, "Edit metadata")
                }
            }

            HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            LazyColumn(Modifier.weight(1f)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        CoverArt(
                            seriesDir = seriesDir,
                            metadata = metadata,
                            modifier = Modifier
                                .width(100.dp)
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            placeholderFontSize = 40.sp,
                        )

                        Column(Modifier.weight(1f)) {
                            Text(
                                metadata.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            if (metadata.author.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    metadata.author,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                                )
                            }
                            if (metadata.artist.isNotBlank() && metadata.artist != metadata.author) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Art: ${metadata.artist}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusChip(metadata.status)
                                if (metadata.aniListId != null) AniListChip()
                            }

                            if (metadata.tags.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    metadata.tags.forEach { tag ->
                                        SuggestionChip(onClick = {}, label = { Text(tag, fontSize = 11.sp) })
                                    }
                                }
                            }

                            if (metadata.description.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    metadata.description,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            val readCount = readState.values.count { it }
                            val total = chapters.size
                            if (total > 0) {
                                Text(
                                    "$readCount / $total chapters read",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { readCount.toFloat() / total },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "${chapters.size} chapters",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.selectedChapters.isNotEmpty()) {
                            TextButton(onClick = { state.markSelectedChaptersRead(seriesDir, true) }) {
                                Icon(
                                    Icons.Outlined.DoneAll,
                                    contentDescription = "Mark selected read",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Mark read (${state.selectedChapters.size})", fontSize = 12.sp)
                            }
                            TextButton(onClick = { state.markSelectedChaptersRead(seriesDir, false) }) {
                                Icon(
                                    Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = "Mark selected unread",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Mark unread (${state.selectedChapters.size})", fontSize = 12.sp)
                            }
                            TextButton(onClick = { state.clearChapterSelection() }) {
                                Text("Clear", fontSize = 12.sp)
                            }
                        } else {
                            TextButton(onClick = { sortDescending = !sortDescending }) {
                                Icon(
                                    if (sortDescending) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                                    contentDescription = "Sort",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (sortDescending) "Newest first" else "Oldest first", fontSize = 13.sp)
                            }
                            IconButton(onClick = { state.markAllChaptersRead(seriesDir, true) }) {
                                Icon(Icons.Outlined.DoneAll, "Mark all read", modifier = Modifier.size(20.dp))
                            }

                            val nextChapterToRead = remember(chapters, readState) {
                                if (chapters.isEmpty()) {
                                    null
                                } else {
                                    val firstUnread = chapters.sortedBy { it.chapterFloat }.firstOrNull { !readState.getOrDefault(it.filename, false) }
                                    firstUnread
                                }
                            }

                            if (nextChapterToRead != null) {
                                TextButton(onClick = { state.openReader(nextChapterToRead) }) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = "Continue",
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Continue", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    ChapterColumnHeader(
                        selectAll = state.selectedChapters.size == chapters.size && chapters.isNotEmpty(),
                        onSelectAll = { state.selectAllChapters() },
                        onClearAll = { state.clearChapterSelection() }
                    )
                }

                items(paginatedChapters) { chapter ->
                    val isRead = readState[chapter.filename] == true
                    val isSelected = state.selectedChapters.contains(chapter.filename)
                    ChapterRow(
                        chapter = chapter,
                        isRead = isRead,
                        isSelected = isSelected,
                        onOpen = { state.openReader(chapter) },
                        onToggleRead = { state.markChapterRead(seriesDir, chapter, !isRead) },
                        onToggleSelect = { state.toggleChapterSelection(chapter.filename) },
                        onRightClick = { offset ->
                            contextMenuOffset = offset
                            contextMenuChapter = chapter
                            showChapterContextMenu = true
                        }
                    )
                    HorizontalDivider(
                        Modifier,
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (sortedChapters.isNotEmpty()) {
                HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Page $currentPage of $totalPages",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { currentPage = 1 },
                            enabled = currentPage > 1
                        ) {
                            Icon(Icons.Outlined.KeyboardDoubleArrowLeft, "First page")
                        }
                        IconButton(
                            onClick = { currentPage = (currentPage - 1).coerceAtLeast(1) },
                            enabled = currentPage > 1
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous page")
                        }
                        IconButton(
                            onClick = { currentPage = (currentPage + 1).coerceAtMost(totalPages) },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next page")
                        }
                        IconButton(
                            onClick = { currentPage = totalPages },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(Icons.Outlined.KeyboardDoubleArrowRight, "Last page")
                        }
                        Spacer(Modifier.width(16.dp))
                        Box {
                            TextButton(onClick = { showChaptersPerPageDropdown = true }) {
                                Text("$chaptersPerPage / page")
                                Icon(Icons.Outlined.KeyboardArrowDown, "Chapters per page")
                            }
                            DropdownMenu(
                                expanded = showChaptersPerPageDropdown,
                                onDismissRequest = { showChaptersPerPageDropdown = false }
                            ) {
                                listOf(10, 20, 50, 100).forEach { size ->
                                    DropdownMenuItem(
                                        text = { Text("$size / page") },
                                        onClick = {
                                            chaptersPerPage = size
                                            currentPage = 1
                                            showChaptersPerPageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    contextMenuChapter?.let { chapter ->
        ChapterContextMenu(
            isVisible = showChapterContextMenu,
            offset = contextMenuOffset,
            onDismiss = { showChapterContextMenu = false },
            onViewChapter = { state.openReader(chapter) },
            onToggleRead = { state.markChapterRead(seriesDir, chapter, !readState[chapter.filename]!!) },
            isChapterRead = readState[chapter.filename]!!,
            onSelectPrevious = { state.selectPrevious(chapter) },
        )
    }
}

@Composable
private fun ChapterColumnHeader(
    selectAll: Boolean = false,
    onSelectAll: () -> Unit = {},
    onClearAll: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selectAll,
            onCheckedChange = { if (it) onSelectAll() else onClearAll() },
            modifier = Modifier.width(56.dp),
        )
        Text(
            "CHAPTER",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.weight(1f)
        )
        Text(
            "LANGUAGE",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.width(90.dp)
        )
        Text(
            "TRANSLATOR",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.width(140.dp)
        )
        Text(
            "STATUS",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.width(70.dp)
        )
    }
    HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun ChapterRow(
    chapter: ChapterInfo,
    isRead: Boolean,
    isSelected: Boolean,
    onOpen: () -> Unit,
    onToggleRead: () -> Unit,
    onToggleSelect: () -> Unit,
    onRightClick: (DpOffset) -> Unit,
) {
    val flag = LANGUAGE_FLAGS[chapter.languageCode] ?: chapter.languageCode.uppercase()
    val langName = LANGUAGE_NAMES[chapter.languageCode] ?: chapter.languageCode.uppercase()

    var rowOffset by remember { mutableStateOf(Offset.Zero) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                rowOffset = coordinates.positionInWindow()
            }
            .clickable(onClick = onOpen)
            .alpha(if (isRead) 0.52f else 1f)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val position = event.changes.first().position
                            // Calculate global click position
                            val globalClickOffset = rowOffset + position
                            onRightClick(DpOffset(globalClickOffset.x.toDp(), globalClickOffset.y.toDp()))
                        }
                    }
                }
            },
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelect() },
            modifier = Modifier.width(56.dp),
        )

        Column(Modifier.weight(1f)) {
            Text(
                text = chapter.filename,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Chapter ${chapter.chapterNumber}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(Modifier.width(90.dp)) {
            Text(flag, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Text(langName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }

        Text(
            chapter.translator,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(140.dp),
        )

        IconButton(onClick = onToggleRead, modifier = Modifier.width(70.dp)) {
            if (isRead) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    "Mark unread",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Outlined.RadioButtonUnchecked,
                    "Mark read",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: SeriesStatus) {
    val (color, label) = when (status) {
        SeriesStatus.ONGOING -> MaterialTheme.colorScheme.tertiary to "Ongoing"
        SeriesStatus.COMPLETED -> MaterialTheme.colorScheme.secondary to "Completed"
        SeriesStatus.HIATUS -> MaterialTheme.colorScheme.error to "Hiatus"
        SeriesStatus.CANCELLED -> MaterialTheme.colorScheme.error to "Cancelled"
        SeriesStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) to "Unknown"
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AniListChip() {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            "AniList linked",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Medium
        )
    }
}