package com.yokai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.yokai.ui.AppState
import com.yokai.ui.components.Sidebar

@Composable
fun SettingsScreen(state: AppState) {
    var libraryPath by remember(state.prefs.libraryRootPath) { mutableStateOf(state.prefs.libraryRootPath) }
    var aniListToken by remember(state.prefs.aniListAccessToken) { mutableStateOf(state.prefs.aniListAccessToken) }
    var defaultCategory by remember(state.prefs.defaultCategory) { mutableStateOf(state.prefs.defaultCategory) }
    var showDefaultCategoryDropdown by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxSize()) {
        Sidebar(state)

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            }
            HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column {
                    Text("Library folder", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = libraryPath,
                            onValueChange = { libraryPath = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("/home/user/Manga") },
                        )
                        Button(onClick = { state.updateLibraryPath(libraryPath) }) {
                            Text("Save")
                        }
                        Button(onClick = { state.refreshLibrary() }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Text("Refresh")
                        }
                    }
                    state.libraryError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }

                Column {
                    Text("AniList access token", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = aniListToken,
                            onValueChange = { aniListToken = it },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = { state.saveAniListToken(aniListToken) }) {
                            Text("Save")
                        }
                    }
                }

                Column {
                    Text("Default Category", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedButton(
                            onClick = { showDefaultCategoryDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = defaultCategory ?: "None",
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showDefaultCategoryDropdown,
                            onDismissRequest = { showDefaultCategoryDropdown = false },
                            modifier = Modifier.width(280.dp), // Adjust width as needed
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    defaultCategory = null
                                    state.updateDefaultCategory(null)
                                    showDefaultCategoryDropdown = false
                                }
                            )
                            state.allCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        defaultCategory = category
                                        state.updateDefaultCategory(category)
                                        showDefaultCategoryDropdown = false
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