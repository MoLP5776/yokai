package com.yokai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yokai.metadata.KeyBindings
import com.yokai.metadata.KeyBindingsRepository
import com.yokai.ui.AppState
import com.yokai.ui.Screen
import com.yokai.ui.components.Sidebar

@Composable
fun KeyBindingsScreen(state: AppState) {
    var bindings by remember { mutableStateOf(state.keybindings) }

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
                IconButton(onClick = { state.currentScreen = Screen.Settings }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                }
                Text(
                    "Key Bindings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text("Page Navigation", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    KeyBindingRow("Next Page", bindings.nextPageKey) { bindings = bindings.copy(nextPageKey = it) }
                    KeyBindingRow("Previous Page", bindings.prevPageKey) { bindings = bindings.copy(prevPageKey = it) }
                }

                item {
                    HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    Text("Chapter Navigation", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    KeyBindingRow("Next Chapter", bindings.nextChapterKey) {
                        bindings = bindings.copy(nextChapterKey = it)
                    }
                    KeyBindingRow("Previous Chapter", bindings.prevChapterKey) {
                        bindings = bindings.copy(prevChapterKey = it)
                    }
                    KeyBindingRow("Next Chapter (Alt)", bindings.nextChapterAltKey) {
                        bindings = bindings.copy(nextChapterAltKey = it)
                    }
                    KeyBindingRow("Previous Chapter (Alt)", bindings.prevChapterAltKey) {
                        bindings = bindings.copy(prevChapterAltKey = it)
                    }
                }

                item {
                    HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    Text("Reader Controls", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    KeyBindingRow("Exit Reader", bindings.exitReaderKey) {
                        bindings = bindings.copy(exitReaderKey = it)
                    }
                    KeyBindingRow("Close", bindings.closeKey) { bindings = bindings.copy(closeKey = it) }
                    KeyBindingRow("Toggle Reading Direction", bindings.toggleReadingDirKey) {
                        bindings = bindings.copy(toggleReadingDirKey = it)
                    }
                    KeyBindingRow("Toggle Page Style", bindings.togglePageStyleKey) {
                        bindings = bindings.copy(togglePageStyleKey = it)
                    }
                    KeyBindingRow("Toggle Double Page Offset", bindings.toggleDoublePageOffsetKey) {
                        bindings = bindings.copy(toggleDoublePageOffsetKey = it)
                    }
                    KeyBindingRow("Toggle Fullscreen", bindings.toggleFullscreenKey) {
                        bindings = bindings.copy(toggleFullscreenKey = it)
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = { bindings = KeyBindings() }) {
                            Text("Reset to Defaults")
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { state.currentScreen = Screen.Settings }) {
                            Text("Cancel")
                        }
                        Button(onClick = {
                            KeyBindingsRepository.save(bindings)
                            state.keybindings = bindings
                            state.currentScreen = Screen.Settings
                        }) {
                            Text("Save")
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun KeyBindingRow(label: String, value: String, onValueChange: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf(value) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(label, fontSize = 12.sp, modifier = Modifier.weight(1f))
        if (isEditing) {
            TextField(
                value = editValue,
                onValueChange = { editValue = it },
                modifier = Modifier.width(120.dp),
                singleLine = true,
            )
            TextButton(onClick = {
                onValueChange(editValue)
                isEditing = false
            }) {
                Text("OK")
            }
        } else {
            Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            TextButton(onClick = { isEditing = true }) {
                Text("Edit")
            }
        }
    }
}
