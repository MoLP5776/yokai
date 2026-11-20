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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.yokai.metadata.MetadataRepository
import com.yokai.image.ImageDecodeDispatcher
import com.yokai.image.toReleasableImageBitmap
import com.yokai.metadata.SeriesMetadata
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Image as SkiaImage
import java.io.File

// Covers are drawn at a few hundred pixels wide in the grid, so 600px on the long side still
// looks sharp (also on HiDPI) while taking about a third of the memory of 800px.
private const val COVER_MAX_DIMENSION = 600

// The cache is bounded by the pixel memory it holds rather than by a number of entries:
// 400 covers at 800px used to be able to pin roughly 680 MB of native memory.
private const val COVER_CACHE_MAX_BYTES = 96L * 1024 * 1024

private fun decodeAndDownscale(bytes: ByteArray): ImageBitmap {
    val original = SkiaImage.makeFromEncoded(bytes)
    val maxDim = maxOf(original.width, original.height)
    if (maxDim <= COVER_MAX_DIMENSION) {
        return original.use { it.toReleasableImageBitmap() }
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
            surface.makeImageSnapshot().use { scaled -> scaled.toReleasableImageBitmap() }
        }
    }
}

private object CoverImageCache {
    private val entries = LinkedHashMap<String, ImageBitmap>(64, 0.75f, true)
    private var totalBytes = 0L

    private val ImageBitmap.byteSize get() = width.toLong() * height * 4

    @Synchronized
    operator fun get(key: String): ImageBitmap? = entries[key]

    // Evicted bitmaps are not released here: a card on screen may still be drawing one. Once
    // no card references it, the GC (see the periodic GC in build.gradle.kts) frees it.
    @Synchronized
    operator fun set(key: String, bitmap: ImageBitmap) {
        entries.put(key, bitmap)?.let { totalBytes -= it.byteSize }
        totalBytes += bitmap.byteSize
        val iterator = entries.values.iterator()
        while (totalBytes > COVER_CACHE_MAX_BYTES && entries.size > 1 && iterator.hasNext()) {
            totalBytes -= iterator.next().byteSize
            iterator.remove()
        }
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
        val cached = coverFile?.let { CoverImageCache[coverCacheKey(it)] }
        mutableStateOf(cached)
    }

    LaunchedEffect(coverFile?.absolutePath, coverFile?.lastModified()) {
        if (bitmap != null) return@LaunchedEffect
        bitmap = coverFile?.let { file ->
            withContext(ImageDecodeDispatcher) {
                runCatching {
                    decodeAndDownscale(file.readBytes())
                }.getOrNull()?.also { decoded ->
                    CoverImageCache[coverCacheKey(file)] = decoded
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
