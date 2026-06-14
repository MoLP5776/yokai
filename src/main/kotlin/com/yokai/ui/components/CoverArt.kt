package com.yokai.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.yokai.metadata.MetadataRepository
import com.yokai.metadata.SeriesMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.io.File

@Composable
fun CoverArt(
    seriesDir: File?,
    metadata: SeriesMetadata,
    modifier: Modifier = Modifier,
    placeholderFontSize: TextUnit = 40.sp,
    coverOverride: File? = null,
) {
    val coverFile = remember(seriesDir, metadata.coverImagePath, coverOverride?.absolutePath) {
        coverOverride ?: seriesDir?.let { MetadataRepository.resolveCoverFile(it, metadata) }
    }
    var bitmap by remember(coverFile?.absolutePath, coverFile?.lastModified()) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(coverFile?.absolutePath, coverFile?.lastModified()) {
        bitmap = coverFile?.let { file ->
            withContext(Dispatchers.IO) {
                runCatching {
                    SkiaImage.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "${metadata.title} cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = metadata.title.firstOrNull()?.uppercase() ?: "?",
                fontSize = placeholderFontSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (metadata.title.isBlank()) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
