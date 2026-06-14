package com.yokai.reader

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")

object CbzReader {

    // Custom comparator for natural sorting (alphanumeric sorting)
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
                // Both are numbers, compare numerically
                val cmp = num1.compareTo(num2)
                if (cmp != 0) return@Comparator cmp
            } else {
                // At least one is not a number, compare lexicographically
                val cmp = part1.compareTo(part2)
                if (cmp != 0) return@Comparator cmp
            }
            i++
        }
        // If one list of parts is a prefix of the other, the longer one comes last
        parts1.size.compareTo(parts2.size)
    }

    fun listPages(file: File): List<String> {
        return ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory }
                .filter { it.name.substringAfterLast('.').lowercase() in imageExtensions }
                .map { it.name }
                .sortedWith(naturalOrderComparator) // Use custom natural order comparator
                .toList()
        }
    }

    fun loadPage(file: File, pageIndex: Int): ImageBitmap? {
        return runCatching {
            ZipFile(file).use { zip ->
                val allEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.name.substringAfterLast('.').lowercase() in imageExtensions }
                    .associateBy { it.name } // Create a map from name to ZipEntry for quick lookup

                val sortedEntryNames = allEntries.keys
                    .sortedWith(naturalOrderComparator) // Sort only the names

                val targetEntryName = sortedEntryNames.getOrNull(pageIndex) ?: return null

                // Retrieve the actual ZipEntry using the sorted name
                val entry = allEntries[targetEntryName] ?: return null

                val bytes = zip.getInputStream(entry).readBytes()
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        }.getOrNull()
    }
}