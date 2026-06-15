package com.yokai.reader

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")

object CbzReader {

    private val naturalOrderComparator: Comparator<String> = Comparator<String> { s1, s2 ->
        val regex = "(\\d+|\\D+)".toRegex()
        val parts1 = regex.findAll(s1).map { it.value }.toList()
        val parts2 = regex.findAll(s2).map { it.value }.toList()

        var i = 0
        while (i < parts1.size && i < parts2.size) {
            val part1 = parts1[i]
            val part2 = parts2[i]

            val num1 = part1.toIntOrNull()
            val num2 = part2.toIntOrNull()

            if (num1 != null && num2 != null) {
                val cmp = num1.compareTo(num2)
                if (cmp != 0) return@Comparator cmp
            } else {
                val cmp = part1.compareTo(part2)
                if (cmp != 0) return@Comparator cmp
            }
            i++
        }
        parts1.size.compareTo(parts2.size)
    }

    fun listPages(file: File): List<String> {
        return ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory }
                .filter { it.name.substringAfterLast('.').lowercase() in imageExtensions }
                .map { it.name }
                .sortedWith(naturalOrderComparator)
                .toList()
        }
    }

    fun loadPage(file: File, pageIndex: Int): ImageBitmap? {
        return runCatching {
            ZipFile(file).use { zip ->
                val allEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.name.substringAfterLast('.').lowercase() in imageExtensions }
                    .associateBy { it.name }

                val sortedEntryNames = allEntries.keys
                    .sortedWith(naturalOrderComparator)

                val targetEntryName = sortedEntryNames.getOrNull(pageIndex) ?: return null

                val entry = allEntries[targetEntryName] ?: return null

                val bytes = zip.getInputStream(entry).readBytes()
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        }.getOrNull()
    }
}