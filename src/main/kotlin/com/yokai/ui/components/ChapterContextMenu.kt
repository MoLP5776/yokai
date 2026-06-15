package com.yokai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
fun ChapterContextMenu(
    isVisible: Boolean,
    offset: DpOffset,
    onDismiss: () -> Unit,
    onViewChapter: () -> Unit,
    onToggleRead: () -> Unit,
    isChapterRead: Boolean,
    onSelectPrevious: () -> Unit,
) {
    if (!isVisible) return

    Popup(
        offset = IntOffset(offset.x.value.toInt(), offset.y.value.toInt()),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .width(180.dp),
        ) {
            ContextMenuItem(
                icon = Icons.Outlined.Visibility,
                label = "View chapter",
                onClick = {
                    onViewChapter()
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = if (isChapterRead) Icons.Outlined.RadioButtonUnchecked else Icons.Outlined.CheckCircle,
                label = if (isChapterRead) "Mark unread" else "Mark read",
                onClick = {
                    onToggleRead()
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Outlined.DoneAll,
                label = "Select previous",
                onClick = {
                    onSelectPrevious()
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
            .fillMaxWidth()
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