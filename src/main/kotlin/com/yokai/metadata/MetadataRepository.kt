package com.yokai.metadata

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val METADATA_FILENAME = "yokai.json"
private const val ASSET_DIRNAME = ".yokai"
private val chapterExtensions = setOf("cbz", "zip")
private val coverExtensions = setOf("jpg", "jpeg", "png", "webp")
private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

object MetadataRepository {
    fun load(seriesDir: File): SeriesMetadata {
        val file = File(seriesDir, METADATA_FILENAME)
        if (!file.exists()) return SeriesMetadata(title = seriesDir.name)

        return runCatching {
            json.decodeFromString<SeriesMetadata>(file.readText())
        }.getOrElse {
            SeriesMetadata(title = seriesDir.name)
        }
    }

    fun save(seriesDir: File, metadata: SeriesMetadata) {
        File(seriesDir, METADATA_FILENAME).writeText(json.encodeToString(metadata))
    }

    fun resolveCoverFile(seriesDir: File, metadata: SeriesMetadata): File? {
        val relativePath = metadata.coverImagePath ?: return null
        return File(seriesDir, relativePath).takeIf { it.isFile }
    }

    fun copyCoverImage(seriesDir: File, source: File): String? {
        if (!source.isFile || source.extension.lowercase() !in coverExtensions) return null

        val assetDir = File(seriesDir, ASSET_DIRNAME).also { it.mkdirs() }
        val extension = source.extension.lowercase()
        val target = File(assetDir, "cover.$extension")
        if (source.canonicalFile != target.canonicalFile) {
            source.copyTo(target, overwrite = true)
        }
        return "$ASSET_DIRNAME/${target.name}"
    }

    fun markChapterRead(seriesDir: File, chapterFilename: String, read: Boolean) {
        val current = load(seriesDir)
        save(seriesDir, current.copy(chapterReadState = current.chapterReadState + (chapterFilename to read)))
    }

    fun discoverSeries(libraryRoot: File): List<File> {
        if (!libraryRoot.isDirectory) return emptyList()

        return libraryRoot.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory }
            ?.filter { dir ->
                dir.listFiles()?.any { file ->
                    file.name == METADATA_FILENAME || file.extension.lowercase() in chapterExtensions
                } == true
            }
            ?.sortedBy { dir ->
                val meta = runCatching { load(dir) }.getOrNull()
                (meta?.effectiveTitle ?: dir.name).lowercase()
            }
            ?.toList()
            ?: emptyList()
    }
}