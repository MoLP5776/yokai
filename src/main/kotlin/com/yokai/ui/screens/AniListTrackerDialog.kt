package com.yokai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yokai.anilist.AniListLibraryEntry
import com.yokai.anilist.AniListManga
import com.yokai.metadata.SeriesMetadata
import com.yokai.ui.AppState
import com.yokai.ui.openInBrowser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import org.jetbrains.skia.Image as SkiaImage

private val coverImageClient = HttpClient(CIO)

private val STATUS_OPTIONS = listOf(
    "CURRENT" to "Reading",
    "PLANNING" to "Planning",
    "COMPLETED" to "Completed",
    "DROPPED" to "Dropped",
    "PAUSED" to "Paused",
)

@Composable
fun AniListTrackerDialog(state: AppState, metadata: SeriesMetadata, onDismiss: () -> Unit) {
    val client = state.aniListClient
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf(metadata.effectiveTitle) }
    var results by remember { mutableStateOf<List<AniListManga>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var linkedEntry by remember { mutableStateOf<AniListLibraryEntry?>(null) }
    var isLoadingEntry by remember { mutableStateOf(false) }
    var isSavingEntry by remember { mutableStateOf(false) }

    fun performSearch() {
        val query = searchQuery
        if (client == null || query.isBlank()) return
        scope.launch {
            isSearching = true
            searchError = null
            val found = client.searchManga(query)
            results = found
            searchError = if (found.isEmpty()) "No results found." else null
            isSearching = false
        }
    }

    LaunchedEffect(metadata.aniListId) {
        val id = metadata.aniListId
        if (id != null && client != null) {
            isLoadingEntry = true
            linkedEntry = client.getLibraryEntry(id)
            isLoadingEntry = false
        } else {
            linkedEntry = null
            if (id == null) performSearch()
        }
    }

    fun updateStatus(newStatus: String) {
        val id = metadata.aniListId
        if (id == null || client == null) return
        scope.launch {
            isSavingEntry = true
            client.updateLibraryEntry(
                mediaId = id,
                progress = linkedEntry?.progress ?: 0,
                score = linkedEntry?.score ?: 0f,
                status = newStatus,
            )
            linkedEntry = client.getLibraryEntry(id)
            isSavingEntry = false
        }
    }

    fun updateScore(newScore: Float) {
        val id = metadata.aniListId
        if (id == null || client == null) return
        scope.launch {
            isSavingEntry = true
            client.updateLibraryEntry(
                mediaId = id,
                progress = linkedEntry?.progress ?: 0,
                score = newScore,
                status = linkedEntry?.status ?: "PLANNING",
            )
            linkedEntry = client.getLibraryEntry(id)
            isSavingEntry = false
        }
    }

    fun updateProgress(newProgress: Int) {
        val id = metadata.aniListId
        if (id == null || client == null) return
        scope.launch {
            isSavingEntry = true
            client.updateLibraryEntry(
                mediaId = id,
                progress = newProgress,
                score = linkedEntry?.score ?: 0f,
                status = linkedEntry?.status ?: "PLANNING",
            )
            linkedEntry = client.getLibraryEntry(id)
            isSavingEntry = false
        }
    }

    Card(
        modifier = Modifier.width(520.dp).heightIn(max = 620.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    "Tracker · ${metadata.effectiveTitle}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, "Close")
                }
            }

            HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            when {
                client == null -> {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Connect an AniList account in Settings to use the tracker.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                metadata.aniListId != null -> {
                    LinkedEntryView(
                        mediaId = metadata.aniListId,
                        entry = linkedEntry,
                        isLoading = isLoadingEntry,
                        isSaving = isSavingEntry,
                        onUnlink = { state.unlinkAniListSeries() },
                        onStatusChange = { updateStatus(it) },
                        onProgressChange = { updateProgress(it) },
                        onScoreChange = { updateScore(it) },
                    )
                }

                else -> {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                placeholder = { Text("Search AniList...") },
                                modifier = Modifier.weight(1f),
                            )
                            Button(onClick = { performSearch() }) {
                                Icon(Icons.Outlined.Search, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Search")
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        when {
                            isSearching -> Box(
                                Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                            searchError != null -> Box(
                                Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(searchError!!, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            else -> LazyColumn(
                                modifier = Modifier.heightIn(max = 420.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(results) { manga ->
                                    SearchResultRow(
                                        manga = manga,
                                        onLink = { state.linkAniListSeries(manga.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    manga: AniListManga,
    onLink: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        RemoteCoverImage(
            url = manga.coverImage?.medium ?: manga.coverImage?.large,
            modifier = Modifier
                .width(50.dp)
                .height(75.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Column(Modifier.weight(1f)) {
            Text(
                manga.title.english ?: manga.title.romaji ?: "Unknown title",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                manga.description?.replace(Regex("<[^>]*>"), "")?.trim().orEmpty()
                    .ifBlank { "No description available." },
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }

        Button(onClick = onLink) { Text("Link") }
    }
}

@Composable
private fun LinkedEntryView(
    mediaId: Int,
    entry: AniListLibraryEntry?,
    isLoading: Boolean,
    isSaving: Boolean,
    onUnlink: () -> Unit,
    onStatusChange: (String) -> Unit,
    onProgressChange: (Int) -> Unit,
    onScoreChange: (Float) -> Unit,
) {
    if (isLoading) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            entry?.title ?: "Unknown title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Status", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            var showStatusMenu by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { showStatusMenu = true }, enabled = !isSaving) {
                    Text(statusLabel(entry?.status))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                    STATUS_OPTIONS.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                showStatusMenu = false
                                onStatusChange(value)
                            },
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Progress", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            val progress = entry?.progress ?: 0
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { onProgressChange((progress - 1).coerceIn(0, 9999)) },
                    enabled = !isSaving && progress > 0,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease progress", modifier = Modifier.size(16.dp))
                }
                Text(
                    "$progress chapters",
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 84.dp),
                )
                IconButton(
                    onClick = { onProgressChange((progress + 1).coerceIn(0, 9999)) },
                    enabled = !isSaving && progress < 9999,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Increase progress", modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Score", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            var scoreInput by remember(entry?.score) {
                mutableStateOf(entry?.score?.takeIf { it > 0f }?.toString() ?: "")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = scoreInput,
                    onValueChange = { scoreInput = it },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.width(80.dp),
                )
                TextButton(
                    onClick = { onScoreChange(scoreInput.toFloatOrNull() ?: 0f) },
                    enabled = !isSaving,
                ) {
                    Text("Save")
                }
            }
        }

        TextButton(onClick = { openInBrowser("https://anilist.co/manga/$mediaId") }) {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Open on AniList")
        }

        Button(
            onClick = onUnlink,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Unlink")
        }
    }
}

private fun statusLabel(status: String?): String = when (status) {
    "CURRENT" -> "Reading"
    "PLANNING" -> "Planning"
    "COMPLETED" -> "Completed"
    "DROPPED" -> "Dropped"
    "PAUSED" -> "Paused"
    "REREADING" -> "Rereading"
    else -> "Not on list"
}

@Composable
private fun RemoteCoverImage(url: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        bitmap = url?.let {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bytes: ByteArray = coverImageClient.get(it).body()
                    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                }.getOrNull()
            }
        }
    }

    Box(modifier) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
