package com.yokai.reader

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File
import java.util.zip.ZipFile

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")

object CbzReader {
    fun listPages(file: File): List<String> {
        return ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory }
                .filter { it.name.substringAfterLast('.').lowercase() in imageExtensions }
                .map { it.name }
                .sortedWith(naturalOrder())
                .toList()
        }
    }

    fun loadPage(file: File, pageIndex: Int): ImageBitmap? {
        return runCatching {
            ZipFile(file).use { zip ->
                val entries = zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.name.substringAfterLast('.').lowercase() in imageExtensions }
                    .sortedWith(compareBy { it.name })
                    .toList()
                val entry = entries.getOrNull(pageIndex) ?: return null
                val bytes = zip.getInputStream(entry).readBytes()
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        }.getOrNull()
    }
}
