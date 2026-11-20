package com.yokai.reader

import androidx.compose.ui.graphics.ImageBitmap
import com.yokai.image.loadBitmap
import com.yokai.image.release
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.io.File

/**
 * Decoded pages of one chapter around the current reading position.
 *
 * Flipping back and forth reuses the same bitmaps instead of decoding a fresh 10-20 MB bitmap
 * (and uploading a fresh GPU texture) on every page turn, and the next pages are decoded ahead
 * of time so turning a page is instant. Pages that fall outside the window are released.
 *
 * Must only be used from the UI thread.
 */
class PageCache(
    private val file: File,
    private val pageNames: List<String>,
    private val scope: CoroutineScope,
) {
    private val pages = HashMap<Int, Deferred<ImageBitmap?>>()

    /** Starts decoding [index] if it isn't cached or loading yet. */
    fun prefetch(index: Int) {
        request(index)
    }

    suspend fun get(index: Int): ImageBitmap? = request(index)?.await()

    private fun request(index: Int): Deferred<ImageBitmap?>? {
        if (index !in pageNames.indices) return null
        return pages.getOrPut(index) {
            scope.async { loadBitmap { CbzReader.loadPage(file, pageNames[index]) } }
        }
    }

    /**
     * Releases every page outside [keep]. Callers must make sure none of those pages are still
     * displayed, i.e. a frame with the new pages has been composed.
     */
    fun retainOnly(keep: IntRange) {
        val iterator = pages.entries.iterator()
        while (iterator.hasNext()) {
            val (index, page) = iterator.next()
            if (index !in keep) {
                iterator.remove()
                page.discard()
            }
        }
    }

    fun clear() = retainOnly(IntRange.EMPTY)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Deferred<ImageBitmap?>.discard() {
        // A finished page is released right away (the handler runs immediately); one that is
        // still decoding is cancelled, and loadBitmap releases it if the decode completes anyway.
        invokeOnCompletion { cause -> if (cause == null) getCompleted()?.release() }
        cancel()
    }
}
