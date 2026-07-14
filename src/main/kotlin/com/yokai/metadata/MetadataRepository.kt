package com.yokai.metadata

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val METADATA_FILENAME = "metadata.json"
private const val LEGACY_METADATA_FILENAME = "yokai.json"
private const val LEGACY_ASSET_DIRNAME = ".yokai"
private val chapterExtensions = setOf("cbz", "zip")
private val coverExtensions = setOf("jpg", "jpeg", "png", "webp")
private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

object MetadataRepository {
    private val libraryConfigDir: File by lazy {
        File(System.getProperty("user.home"), ".config/yokai/library").also { it.mkdirs() }
    }

    private fun seriesConfigDir(seriesDir: File): File =
        File(libraryConfigDir, seriesDir.name).also { it.mkdirs() }

    fun load(seriesDir: File): SeriesMetadata {
        val centralFile = File(seriesConfigDir(seriesDir), METADATA_FILENAME)
        if (centralFile.exists()) {
            return runCatching {
                json.decodeFromString<SeriesMetadata>(centralFile.readText())
            }.getOrElse { e ->
                println("Error deserializing metadata for series ${seriesDir.name}: ${e.message}")
                SeriesMetadata(title = seriesDir.name)
            }
        }

        val legacyFile = File(seriesDir, LEGACY_METADATA_FILENAME)
        if (!legacyFile.exists()) {
            println("No metadata found for series: ${seriesDir.name}. Returning default metadata.")
            return SeriesMetadata(title = seriesDir.name)
        }

        return runCatching {
            json.decodeFromString<SeriesMetadata>(legacyFile.readText())
        }.getOrElse { e ->
            println("Error deserializing legacy metadata for series ${seriesDir.name}: ${e.message}")
            SeriesMetadata(title = seriesDir.name)
        }
    }

    fun save(seriesDir: File, metadata: SeriesMetadata) {
        runCatching {
            File(seriesConfigDir(seriesDir), METADATA_FILENAME).writeText(json.encodeToString(metadata))
        }.onFailure { e ->
            println("Error saving metadata for series ${seriesDir.name}: ${e.message}")
        }
    }

    fun resolveCoverFile(seriesDir: File, metadata: SeriesMetadata): File? {
        val configDir = seriesConfigDir(seriesDir)
        val centralCover = configDir.listFiles()
            ?.firstOrNull { it.nameWithoutExtension == "cover" && it.extension.lowercase() in coverExtensions }
        if (centralCover != null) return centralCover

        val relativePath = metadata.coverImagePath ?: return null
        return File(seriesDir, relativePath).takeIf { it.isFile }
    }

    fun copyCoverImage(seriesDir: File, source: File): String? {
        if (!source.isFile || source.extension.lowercase() !in coverExtensions) return null

        val configDir = seriesConfigDir(seriesDir)
        val extension = source.extension.lowercase()
        configDir.listFiles()
            ?.filter { it.nameWithoutExtension == "cover" && it.extension.lowercase() in coverExtensions }
            ?.forEach { it.delete() }

        val target = File(configDir, "cover.$extension")
        if (source.canonicalFile != target.canonicalFile) {
            source.copyTo(target, overwrite = true)
        }
        return "cover.$extension"
    }

    fun removeSeriesConfig(seriesDir: File) {
        File(libraryConfigDir, seriesDir.name).deleteRecursively()
    }

    fun pruneChapterReadState(seriesDir: File) {
        val current = load(seriesDir)
        val existingFilenames = seriesDir.listFiles()
            ?.filter { it.extension.lowercase() in chapterExtensions }
            ?.map { it.name }
            ?.toSet() ?: emptySet()
        val pruned = current.chapterReadState.filterKeys { it in existingFilenames }
        if (pruned.size != current.chapterReadState.size) {
            save(seriesDir, current.copy(chapterReadState = pruned))
        }
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
                    file.name == LEGACY_METADATA_FILENAME || file.extension.lowercase() in chapterExtensions
                } == true
                || File(libraryConfigDir, dir.name).let { it.isDirectory && File(it, METADATA_FILENAME).exists() }
            }
            ?.map { dir ->
                val meta = runCatching { load(dir) }.getOrNull()
                dir to (meta?.effectiveTitle ?: dir.name).lowercase()
            }
            ?.sortedBy { (_, sortKey) -> sortKey }
            ?.map { (dir, _) -> dir }
            ?.toList()
            ?: emptyList()
    }

    fun migrateAll(libraryRoot: File): Int {
        if (!libraryRoot.isDirectory) return 0
        var count = 0
        libraryRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { seriesDir ->
                if (migrateSeriesDir(seriesDir)) count++
            }
        return count
    }

    private fun migrateSeriesDir(seriesDir: File): Boolean {
        var didMigrate = false
        val configDir = seriesConfigDir(seriesDir)

        val legacyMeta = File(seriesDir, LEGACY_METADATA_FILENAME)
        val newMeta = File(configDir, METADATA_FILENAME)
        if (legacyMeta.exists()) {
            if (!newMeta.exists()) {
                legacyMeta.copyTo(newMeta, overwrite = false)
            }
            legacyMeta.delete()
            didMigrate = true
        }

        val legacyAssetDir = File(seriesDir, LEGACY_ASSET_DIRNAME)
        if (legacyAssetDir.isDirectory) {
            legacyAssetDir.listFiles()?.forEach { file ->
                if (file.nameWithoutExtension == "cover" && file.extension.lowercase() in coverExtensions) {
                    file.copyTo(File(configDir, file.name), overwrite = true)
                    didMigrate = true
                }
            }
            legacyAssetDir.deleteRecursively()
        }

        if (didMigrate && newMeta.exists()) {
            runCatching {
                val meta = json.decodeFromString<SeriesMetadata>(newMeta.readText())
                val fixedPath = meta.coverImagePath?.removePrefix("$LEGACY_ASSET_DIRNAME/")
                if (fixedPath != meta.coverImagePath) {
                    newMeta.writeText(json.encodeToString(meta.copy(coverImagePath = fixedPath)))
                }
            }
        }

        return didMigrate
    }
}