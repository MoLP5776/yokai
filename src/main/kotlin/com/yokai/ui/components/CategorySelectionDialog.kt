package com.yokai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                    Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                }

                Spacer(Modifier.height(12.dp))
                Text("Add new category:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                ) {
                    BasicTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (newCategoryText.isEmpty()) {
                                Text("Type category name...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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