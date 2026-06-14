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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yokai.ui.AppState
import com.yokai.ui.components.Sidebar

@Composable
fun SettingsScreen(state: AppState) {
    var libraryPath by remember(state.prefs.libraryRootPath) { mutableStateOf(state.prefs.libraryRootPath) }
    var aniListToken by remember(state.prefs.aniListAccessToken) { mutableStateOf(state.prefs.aniListAccessToken) }

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
            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)

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
            }
        }
    }
}
