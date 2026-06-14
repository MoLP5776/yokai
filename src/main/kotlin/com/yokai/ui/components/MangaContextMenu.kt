package com.yokai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup

@Composable
fun MangaContextMenu(
    isVisible: Boolean,
    offset: DpOffset,
    onDismiss: () -> Unit,
    onViewSeries: () -> Unit,
    onMarkAllRead: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onManageCategories: () -> Unit,
) {
    if (!isVisible) return

    Popup(
        offset = IntOffset(offset.x.value.toInt(), offset.y.value.toInt()),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .width(200.dp),
        ) {
            ContextMenuItem(
                icon = Icons.Outlined.Visibility,
                label = "View series",
                onClick = {
                    onViewSeries()
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Outlined.DoneAll,
                label = "Mark all read",
                onClick = {
                    onMarkAllRead()
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Outlined.PlaylistAddCheck,
                label = "Multi-select",
                onClick = {
                    onToggleMultiSelect()
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Outlined.Category,
                label = "Categories",
                onClick = {
                    onManageCategories()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val textColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.width(20.dp),
        )
        Text(
            label,
            fontSize = 13.sp,
            color = textColor,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
