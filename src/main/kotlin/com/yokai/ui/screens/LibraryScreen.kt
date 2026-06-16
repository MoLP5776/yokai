package com.yokai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yokai.metadata.MetadataRepository
import com.yokai.metadata.SeriesMetadata
import com.yokai.metadata.SeriesStatus
import com.yokai.ui.AppState
import com.yokai.ui.Screen
import com.yokai.ui.components.CoverArt
import com.yokai.ui.components.Sidebar
import com.yokai.ui.components.MangaContextMenu
import com.yokai.ui.components.CategorySelectionDialog
import java.io.File

@Composable
fun LibraryScreen(state: AppState) {
    var contextMenuVisible by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
    var contextMenuSeriesDir by remember { mutableStateOf<File?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryDialogSeriesDir by remember { mutableStateOf<File?>(null) }
    var categoryDialogMetadata by remember { mutableStateOf(state.selectedSeriesMetadata) } // Reverted to original initialization

    var showMultiSelectCategoryDialog by remember { mutableStateOf(false) }
    var multiSelectCategorySeriesList by remember { mutableStateOf<Set<File>>(emptySet()) }

    if (showCategoryDialog && categoryDialogSeriesDir != null && categoryDialogMetadata != null) {
        CategorySelectionDialog(
            metadata = categoryDialogMetadata!!,
            allCategories = state.allCategories,
            onDismiss = { showCategoryDialog = false },
            onSave = { categories ->
                state.updateSeriesCategories(categoryDialogSeriesDir!!, categories)
                showCategoryDialog = false
            },
        )
    }

    if (showMultiSelectCategoryDialog && multiSelectCategorySeriesList.isNotEmpty()) {
        val initialCategories = remember(multiSelectCategorySeriesList) {
            multiSelectCategorySeriesList
                .mapNotNull { state.seriesMetadataMap[it]?.categories?.toSet() }
                .reduceOrNull { acc, categories -> acc.intersect(categories) }
                ?.toList() ?: emptyList()
        }
        val syntheticMetadata = remember(initialCategories) {
            SeriesMetadata(title = "Multiple Series", categories = initialCategories)
        }

        CategorySelectionDialog(
            metadata = syntheticMetadata,
            allCategories = state.allCategories,
            onDismiss = { showMultiSelectCategoryDialog = false },
            onSave = { categories ->
                multiSelectCategorySeriesList.forEach { seriesDir ->
                    state.updateSeriesCategories(seriesDir, categories)
                }
                showMultiSelectCategoryDialog = false
                state.toggleMultiSelectMode()
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
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    text = state.selectedCategory ?: "Library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${state.filteredSeriesList.size} series",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }

            HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            val error = state.libraryError
            if (state.filteredSeriesList.isEmpty()) {
                EmptyLibrary(
                    message = error ?: "No series found",
                    onSetupClick = { state.currentScreen = Screen.Settings },
                )
            } else {
                if (state.isMultiSelectMode) {
                    MultiSelectToolbar(state,
                        onManageCategories = {
                            multiSelectCategorySeriesList = state.selectedSeriesList
                            showMultiSelectCategoryDialog = true
                        }
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.filteredSeriesList) { seriesDir ->
                        SeriesCard(
                            seriesDir = seriesDir,
                            state = state,
                            onClick = {
                                if (state.isMultiSelectMode) {
                                    state.toggleSeriesSelection(seriesDir)
                                } else {
                                    state.selectSeries(seriesDir)
                                }
                            },
                            onContextMenu = { offset ->
                                contextMenuSeriesDir = seriesDir
                                contextMenuOffset = offset
                                contextMenuVisible = true
                            },
                            isMultiSelectMode = state.isMultiSelectMode,
                            isSelected = state.selectedSeriesList.contains(seriesDir)
                        )
                    }
                }
            }
        }
    }

    if (contextMenuSeriesDir != null) {
        val metadata = state.seriesMetadataMap[contextMenuSeriesDir!!] ?: MetadataRepository.load(contextMenuSeriesDir!!)
        MangaContextMenu(
            isVisible = contextMenuVisible,
            offset = contextMenuOffset,
            onDismiss = { contextMenuVisible = false },
            onViewSeries = {
                state.selectSeries(contextMenuSeriesDir!!)
            },
            onMarkAllRead = {
                state.markAllChaptersRead(contextMenuSeriesDir!!, true)
            },
            onToggleMultiSelect = {
                state.toggleMultiSelectMode()
                state.toggleSeriesSelection(contextMenuSeriesDir!!)
                contextMenuVisible = false
            },
            onManageCategories = {
                categoryDialogSeriesDir = contextMenuSeriesDir
                categoryDialogMetadata = metadata // Directly assign the already loaded and non-null metadata
                showCategoryDialog = true
            },
        )
    }
}

@Composable
private fun SeriesCard(
    seriesDir: File,
    state: AppState,
    onClick: () -> Unit,
    onContextMenu: (DpOffset) -> Unit = {},
    isMultiSelectMode: Boolean,
    isSelected: Boolean
) {
    val metadata = state.seriesMetadataMap[seriesDir] ?: MetadataRepository.load(seriesDir)
    val chapters = state.seriesChapterCounts[seriesDir] ?: 0
    val readCount = state.readChapterCount(seriesDir)
    val unread = chapters - readCount

    var cardLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                cardLayoutCoordinates = coordinates
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.type == PointerEventType.Press) {
                            if (event.buttons.isPrimaryPressed) {
                                onClick()
                            } else if (event.buttons.isSecondaryPressed) {
                                cardLayoutCoordinates?.let {
                                    val position = event.changes.first().position
                                    val windowPosition = it.localToWindow(position)
                                    onContextMenu(DpOffset(windowPosition.x.dp, windowPosition.y.dp))
                                }
                            }
                        }
                    }
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            ) {
                CoverArt(
                    seriesDir = seriesDir,
                    metadata = metadata,
                    modifier = Modifier.fillMaxSize(),
                    placeholderFontSize = 48.sp,
                )

                if (unread > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "$unread",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 13.sp,
                        )
                    }
                }
                if (isMultiSelectMode && isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.SelectAll,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Column(Modifier.padding(10.dp)) {
                Text(
                    text = metadata.effectiveTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun MultiSelectToolbar(state: AppState, onManageCategories: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${state.selectedSeriesList.size} selected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row {
            TextButton(onClick = {
                state.selectedSeriesList.forEach { seriesDir ->
                    state.markAllChaptersRead(seriesDir, true)
                }
                state.toggleMultiSelectMode()
            }) {
                Text("Mark Read")
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                state.selectedSeriesList.forEach { seriesDir ->
                    state.markAllChaptersRead(seriesDir, false)
                }
                state.toggleMultiSelectMode()
            }) {
                Text("Mark Unread")
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onManageCategories) {
                Text("Manage Categories")
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { state.toggleMultiSelectMode() }) {
                Icon(Icons.Outlined.Close, "Exit multi-select")
            }
        }
    }
}

@Composable
private fun EmptyLibrary(message: String, onSetupClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
            )
            Spacer(Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onSetupClick) {
                Text("Set library folder")
            }
        }
    }
}