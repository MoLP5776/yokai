package com.yokai.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.yokai.metadata.SeriesMetadata

@Composable
fun CategorySelectionDialog(
    metadata: SeriesMetadata,
    allCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var selectedCategories by remember { mutableStateOf(metadata.categories) }
    var newCategoryText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Manage Categories",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, "Close")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Select categories:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))

                val categoriesToDisplay = remember(allCategories, selectedCategories) {
                    (allCategories + selectedCategories).distinct().sorted()
                }

                LazyColumn(Modifier.fillMaxWidth()) {
                    items(categoriesToDisplay) { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategories = if (selectedCategories.contains(category)) {
                                        selectedCategories - category
                                    } else {
                                        selectedCategories + category
                                    }
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            Checkbox(
                                checked = selectedCategories.contains(category),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + category
                                    } else {
                                        selectedCategories - category
                                    }
                                },
                            )
                            Text(category, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (categoriesToDisplay.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                }

                Spacer(Modifier.height(12.dp))
                Text("Add new category:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp),
                ) {
                    BasicTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (newCategoryText.isEmpty()) {
                                Text(
                                    "Type category name...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        },
                    )
                    IconButton(
                        onClick = {
                            if (newCategoryText.isNotBlank() && !selectedCategories.contains(newCategoryText)) {
                                selectedCategories = selectedCategories + newCategoryText
                                newCategoryText = ""
                            }
                        },
                        modifier = Modifier.width(40.dp),
                    ) {
                        Icon(Icons.Outlined.Add, "Add category", modifier = Modifier.width(20.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(selectedCategories) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}