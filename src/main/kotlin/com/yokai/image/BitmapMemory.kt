package com.yokai.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

/*
 * A decoded ImageBitmap keeps its pixels in native (Skia) memory, while the JVM only sees a
 * tiny wrapper object. That memory is otherwise freed only when the GC happens to collect the
 * wrapper, and small wrappers never put enough pressure on the heap for that to happen soon.
 * Flipping through 100 pages could pin ~1.5 GB of dead pixels this way, so bitmaps that have a
 * single owner are freed explicitly as soon as nothing shows them anymore.
 */

// Decoding a page or cover briefly needs the compressed bytes plus the full pixel buffer.
// Capping how many run at once keeps that peak low when a whole grid of covers or a long
// vertical strip requests images at the same moment.
val ImageDecodeDispatcher = Dispatchers.IO.limitedParallelism(
    Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
)

/**
 * Decodes this image into an [ImageBitmap] whose pixels can actually be freed with [release].
 *
 * Use this instead of Compose's `toComposeImageBitmap()`: that one draws into the bitmap
 * through a Skia Canvas it never closes, and the canvas keeps its own reference to the pixel
 * buffer. Closing such a bitmap frees nothing until the GC eventually finalizes the canvas.
 */
fun Image.toReleasableImageBitmap(): ImageBitmap {
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL))
    if (!readPixels(bitmap)) {
        bitmap.close()
        error("Could not decode image")
    }
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}

/** Frees the bitmap's native pixels. Only call this once nothing can draw it anymore. */
fun ImageBitmap.release() {
    val bitmap = asSkiaBitmap()
    if (!bitmap.isClosed) bitmap.close()
}

/**
 * Runs [load] on [ImageDecodeDispatcher]. If the caller is cancelled while the image is being
 * decoded (for example because the reader already moved to another page), the result never
 * reaches the UI, so it is freed right away instead of being left for the GC.
 */
suspend fun loadBitmap(load: () -> ImageBitmap?): ImageBitmap? {
    var result: ImageBitmap? = null
    try {
        return withContext(ImageDecodeDispatcher) { load().also { result = it } }
    } catch (e: CancellationException) {
        result?.release()
        throw e
    }
}

/**
 * Frees [bitmap] once it is replaced by another value or leaves the composition. Place it in
 * the same composable that draws the bitmap: the old bitmap is released during the same
 * recomposition that stops drawing it, before the next frame is rendered.
 */
@Composable
fun ReleaseOnDispose(bitmap: ImageBitmap?) {
    DisposableEffect(bitmap) {
        onDispose { bitmap?.release() }
    }
}
