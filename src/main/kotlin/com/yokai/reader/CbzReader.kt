package com.yokai.reader

import androidx.compose.ui.graphics.*
import java.io.File
import java.util.zip.*
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")

// Single/double page mode only ever holds 1-2 decoded pages at once, so a higher cap is fine.
private const val PAGE_MAX_DIMENSION = 2400

// Long Strip holds every page of the chapter decoded at once, so it needs a tighter cap
// to keep total memory bounded.
private const val VERTICAL_PAGE_MAX_DIMENSION = 1600

/**
 * Decodes [bytes] into a downscaled [ImageBitmap], capping the largest dimension at
 * [maxDimension]. The intermediate Skia [Image] is a native, off-heap resource, so it's
 * always closed via [use] rather than left for the GC/finalizer to eventually reclaim.
 */
private fun decodeAndDownscale(bytes: ByteArray, maxDimension: Int): ImageBitmap {
    return Image.makeFromEncoded(bytes).use { original ->
        val maxDim = maxOf(original.width, original.height)
        if (maxDim <= maxDimension) {
            original.toComposeImageBitmap()
        } else {
            val scale = maxDimension.toFloat() / maxDim
            val targetWidth = (original.width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)
            Surface.makeRasterN32Premul(targetWidth, targetHeight).use { surface ->
                surface.canvas.drawImageRect(
                    original,
                    Rect.makeWH(original.width.toFloat(), original.height.toFloat()),
                    Rect.makeWH(targetWidth.toFloat(), targetHeight.toFloat()),
                )
                surface.makeImageSnapshot().use { scaled -> scaled.toComposeImageBitmap() }
            }
        }
    }
}

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

                val bytes = zip.getInputStream(entry).readBytes()
                decodeAndDownscale(bytes, PAGE_MAX_DIMENSION)
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
                    val bytes = zip.getInputStream(entry).readBytes()
                    return decodeAndDownscale(bytes, PAGE_MAX_DIMENSION)
                }

                Pair(loadAt(pageIndex), loadAt(pageIndex + 1))
            }
        }.getOrElse { Pair(null, null) }
    }

    fun loadAllPages(file: File): List<ImageBitmap> {
        return runCatching {
            ZipFile(file).use { zip ->
                val allEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.name.substringAfterLast('.').lowercase() in imageExtensions }
                    .associateBy { it.name }

                allEntries.keys
                    .sortedWith(naturalOrderComparator)
                    .mapNotNull { name ->
                        val entry = allEntries[name] ?: return@mapNotNull null
                        runCatching {
                            val bytes = zip.getInputStream(entry).readBytes()
                            decodeAndDownscale(bytes, VERTICAL_PAGE_MAX_DIMENSION)
                        }.getOrNull()
                    }
            }
        }.getOrElse { emptyList() }
    }
}