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
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Image as SkiaImage
import java.io.File

private const val COVER_CACHE_MAX_SIZE = 400


private const val COVER_MAX_DIMENSION = 800

private fun decodeAndDownscale(bytes: ByteArray): ImageBitmap {
    val original = SkiaImage.makeFromEncoded(bytes)
    val maxDim = maxOf(original.width, original.height)
    if (maxDim <= COVER_MAX_DIMENSION) {
        return original.use { it.toComposeImageBitmap() }
    }

    return original.use {
        val scale = COVER_MAX_DIMENSION.toFloat() / maxDim
        val targetWidth = (it.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (it.height * scale).toInt().coerceAtLeast(1)
        Surface.makeRasterN32Premul(targetWidth, targetHeight).use { surface ->
            surface.canvas.drawImageRect(
                it,
                Rect.makeWH(it.width.toFloat(), it.height.toFloat()),
                Rect.makeWH(targetWidth.toFloat(), targetHeight.toFloat()),
            )
            surface.makeImageSnapshot().use { scaled -> scaled.toComposeImageBitmap() }
        }
    }
}

private val coverImageCache = object : LinkedHashMap<String, ImageBitmap>(COVER_CACHE_MAX_SIZE, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>): Boolean {
        return size > COVER_CACHE_MAX_SIZE
    }
}

private fun coverCacheKey(file: File) = "${file.absolutePath}:${file.lastModified()}"

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
        val cached = coverFile?.let { synchronized(coverImageCache) { coverImageCache[coverCacheKey(it)] } }
        mutableStateOf(cached)
    }

    LaunchedEffect(coverFile?.absolutePath, coverFile?.lastModified()) {
        if (bitmap != null) return@LaunchedEffect
        bitmap = coverFile?.let { file ->
            withContext(Dispatchers.IO) {
                runCatching {
                    decodeAndDownscale(file.readBytes())
                }.getOrNull()?.also { decoded ->
                    synchronized(coverImageCache) { coverImageCache[coverCacheKey(file)] = decoded }
                }
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
