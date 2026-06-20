package com.yokai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.yokai.anilist.AniListAuth
import com.yokai.metadata.AppTheme
import com.yokai.metadata.ThemeMode
import com.yokai.ui.AppState
import com.yokai.ui.components.Sidebar
import com.yokai.ui.openInBrowser
import com.yokai.ui.theme.colorScheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(state: AppState) {
    var libraryPath by remember(state.prefs.libraryRootPath) { mutableStateOf(state.prefs.libraryRootPath) }
    var defaultCategory by remember(state.prefs.defaultCategory) { mutableStateOf(state.prefs.defaultCategory) }
    var showDefaultCategoryDropdown by remember { mutableStateOf(false) }

    var aniListCode by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                    Text("AniList account", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))

                    if (state.prefs.aniListAccessToken.isNotBlank()) {
                        var username by remember(state.prefs.aniListAccessToken) { mutableStateOf<String?>(null) }
                        LaunchedEffect(state.prefs.aniListAccessToken) {
                            username = state.aniListClient?.getUsername()
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                username?.let { "Logged in as $it" } ?: "Connected to AniList",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            )
                            TextButton(onClick = { state.saveAniListToken("") }) {
                                Text("Disconnect")
                            }
                        }
                    } else {
                        Text(
                            "1. Authenticate on AniList to get an access code.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { openInBrowser(AniListAuth.buildAuthUrl()) }) {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Authenticate on AniList")
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            "2. Paste the access code below.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = aniListCode,
                                onValueChange = { aniListCode = it },
                                singleLine = true,
                                placeholder = { Text("Access code") },
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    val code = aniListCode.trim()
                                    scope.launch {
                                        isAuthenticating = true
                                        authError = null
                                        val token = AniListAuth.exchangeCodeForToken(code)
                                        if (token != null) {
                                            state.saveAniListToken(token)
                                            aniListCode = ""
                                        } else {
                                            authError = "Failed to authenticate. Check the code and try again."
                                        }
                                        isAuthenticating = false
                                    }
                                },
                                enabled = aniListCode.isNotBlank() && !isAuthenticating,
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                } else {
                                    Text("Submit")
                                }
                            }
                        }
                        authError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
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
                Column {
                    Text("Appearance", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.widthIn(max = 240.dp)) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = state.prefs.themeMode == mode,
                                onClick = { state.updateThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                            ) {
                                Text(if (mode == ThemeMode.LIGHT) "Light" else "Dark")
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(AppTheme.entries) { theme ->
                            ThemeSwatchCard(
                                theme = theme,
                                mode = state.prefs.themeMode,
                                selected = state.prefs.appTheme == theme,
                                onClick = { state.updateAppTheme(theme) },
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Pure black dark mode", fontSize = 14.sp)
                        Switch(
                            checked = state.prefs.pureBlackDarkMode,
                            onCheckedChange = { state.updatePureBlackDarkMode(it) },
                            enabled = state.prefs.themeMode == ThemeMode.DARK,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatchCard(
    theme: AppTheme,
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = remember(theme, mode) { theme.colorScheme(mode) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Column(
            modifier = Modifier
                .size(width = 96.dp, height = 150.dp)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) scheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(scheme.background)
                .clickable(onClick = onClick)
                .padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(scheme.onBackground),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(width = 36.dp, height = 16.dp)
                        .clip(RoundedCornerShape(50)),
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(scheme.primary))
                    Box(Modifier.weight(1f).fillMaxHeight().background(scheme.secondary))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(scheme.surfaceVariant),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(theme.displayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}