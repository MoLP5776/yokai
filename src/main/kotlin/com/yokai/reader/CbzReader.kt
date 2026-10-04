package com.yokai.reader

import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntSize
import java.io.File
import java.util.zip.*
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image

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

    /**
     * Extracts all image pages from [cbzFile] into:
     *   ~/.config/yokai/extracted/<cbzFile.nameWithoutExtension>/
     *
     * The entire extracted/ directory is wiped first so only one chapter
     * is ever present on disk at a time, mirroring Houdoku's behaviour.
     *
     * Returns the directory the pages were extracted into, or null on failure.
     * This is a blocking call — always invoke it on Dispatchers.IO.
     */
    fun extractChapter(cbzFile: File): File? {
        return runCatching {
            val extractRoot = File(
                System.getProperty("user.home"),
                ".config/yokai/extracted"
            )

            if (extractRoot.exists()) extractRoot.deleteRecursively()
            extractRoot.mkdirs()

            val chapterDir = File(extractRoot, cbzFile.nameWithoutExtension)
            chapterDir.mkdirs()

            ZipFile(cbzFile).use { zip ->
                zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.name.substringAfterLast('.').lowercase() in imageExtensions }
                    .forEach { entry ->

                        val outFile = File(chapterDir, File(entry.name).name)
                        zip.getInputStream(entry).use { input ->
                            outFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
            }

            chapterDir
        }.getOrNull()
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

                decode(zip.getInputStream(entry).readBytes())
            }
        }.getOrNull()
    }

    fun loadTwoPages(file: File, pageIndex: Int): Pair<ImageBitmap?, ImageBitmap?> {
        return runCatching {
            ZipFile(file).use { zip ->
                val allEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.name.substringAfterLast('.').lowercase() in imageExtensions }
                    .associateBy { it.name }

                val sortedNames = allEntries.keys.sortedWith(naturalOrderComparator)

                fun loadAt(index: Int): ImageBitmap? {
                    val name = sortedNames.getOrNull(index) ?: return null
                    val entry = allEntries[name] ?: return null
                    return decode(zip.getInputStream(entry).readBytes())
                }

                Pair(loadAt(pageIndex), loadAt(pageIndex + 1))
            }
        }.getOrElse { Pair(null, null) }
    }

    /**
     * Loads a single page by its entry name (as returned by [listPages]).
     * Avoids re-listing and re-sorting the archive for every page, which matters
     * for the vertical reader where pages are loaded one by one while scrolling.
     */
    fun loadPage(file: File, entryName: String): ImageBitmap? {
        return runCatching {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(entryName) ?: return null
                decode(zip.getInputStream(entry).readBytes())
            }
        }.getOrNull()
    }

    /**
     * Reads the pixel size of each page from its header without decoding the pixels, so the
     * vertical reader can reserve every page's final height before its bitmap is loaded.
     * Pages whose header can't be read are left out.
     */
    fun loadPageSizes(file: File, entryNames: List<String>): Map<String, IntSize> {
        return runCatching {
            ZipFile(file).use { zip ->
                entryNames.mapNotNull { name ->
                    val entry = zip.getEntry(name) ?: return@mapNotNull null
                    runCatching {
                        Data.makeFromBytes(zip.getInputStream(entry).readBytes()).use { data ->
                            Codec.makeFromData(data).use { name to IntSize(it.width, it.height) }
                        }
                    }.getOrNull()
                }.toMap()
            }
        }.getOrElse { emptyMap() }
    }

    // The Skia Image holds native memory that is only released when closed (or much later
    // by the GC finalizer), so close it as soon as it has been converted into a bitmap.
    private fun decode(bytes: ByteArray): ImageBitmap =
        Image.makeFromEncoded(bytes).use { it.toComposeImageBitmap() }
}
