package com.yokai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.yokai.metadata.MetadataRepository
import com.yokai.metadata.SeriesMetadata
import com.yokai.metadata.SeriesStatus
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

@Composable
fun MetadataEditorDialog(
    seriesDir: File,
    metadata: SeriesMetadata,
    onDismiss: () -> Unit,
    onSave: (SeriesMetadata) -> Unit,
) {
    var title by remember(metadata) { mutableStateOf(metadata.title) }
    var displayName by remember(metadata) { mutableStateOf(metadata.displayName) }
    var description by remember(metadata) { mutableStateOf(metadata.description) }
    var author by remember(metadata) { mutableStateOf(metadata.author) }
    var artist by remember(metadata) { mutableStateOf(metadata.artist) }
    var tags by remember(metadata) { mutableStateOf(metadata.tags.joinToString(", ")) } // Changed to tags
    var status by remember(metadata) { mutableStateOf(metadata.status) }
    var selectedCover by remember(metadata) { mutableStateOf<File?>(null) }

    DialogWindow(
        onCloseRequest = onDismiss,
        title = "Edit metadata",
        resizable = false,
        state = rememberDialogState(width = 900.dp, height = 760.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Edit metadata",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, "Close")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 12.dp),
                ) {
                    Column(modifier = Modifier.width(192.dp)) {
                        CoverArt(
                            seriesDir = seriesDir,
                            metadata = metadata.copy(title = title),
                            coverOverride = selectedCover,
                            placeholderFontSize = 48.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { selectedCover = chooseCoverFile() ?: selectedCover },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Select cover")
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            supportingText = { Text("Folder name — not shown in the UI") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display name") },
                            supportingText = { Text("Shown in the library and used for sorting. Leave blank to use the folder name.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = author,
                            onValueChange = { author = it },
                            label = { Text("Author") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = artist,
                            onValueChange = { artist = it },
                            label = { Text("Artist") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            label = { Text("Tags") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        StatusSelector(status = status, onStatusChange = { status = it })
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val copiedCoverPath =
                                selectedCover?.let { MetadataRepository.copyCoverImage(seriesDir, it) }
                            onSave(
                                metadata.copy(
                                    title = title.ifBlank { seriesDir.name },
                                    displayName = displayName.trim(),
                                    description = description.trim(),
                                    author = author.trim(),
                                    artist = artist.trim(),
                                    status = status,
                                    coverImagePath = copiedCoverPath ?: metadata.coverImagePath,
                                    tags = tags
                                        .split(',')
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() },
                                ),
                            )
                        },
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusSelector(status: SeriesStatus, onStatusChange: (SeriesStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Status", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = status.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(260.dp),
            ) {
                SeriesStatus.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onStatusChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private val SeriesStatus.label: String
    get() = when (this) {
        SeriesStatus.ONGOING -> "Currently airing"
        SeriesStatus.COMPLETED -> "Completed"
        SeriesStatus.HIATUS -> "On hiatus"
        SeriesStatus.CANCELLED -> "Cancelled"
        SeriesStatus.UNKNOWN -> "Unknown"
    }

private fun chooseCoverFile(): File? {
    val dialog = FileDialog(null as Frame?, "Select cover", FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, name ->
        name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp")
    }
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(directory, file)
}