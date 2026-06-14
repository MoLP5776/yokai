package com.yokai.watcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService

sealed class LibraryEvent {
    data class ChapterAdded(val seriesDir: File, val chapterFile: File) : LibraryEvent()
    data class ChapterRemoved(val seriesDir: File, val chapterFile: File) : LibraryEvent()
    data class SeriesAdded(val seriesDir: File) : LibraryEvent()
}

object LibraryWatcher {
    private val chapterExtensions = setOf("cbz", "zip")

    fun watch(libraryRoot: File): Flow<LibraryEvent> = callbackFlow {
        if (!libraryRoot.isDirectory) {
            close()
            return@callbackFlow
        }

        val watchService: WatchService = FileSystems.getDefault().newWatchService()
        val watchKeys = mutableMapOf<WatchKey, Path>()

        fun register(dir: File) {
            val path = dir.toPath()
            val key = path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
            )
            watchKeys[key] = path
        }

        register(libraryRoot)
        libraryRoot.listFiles()?.filter { it.isDirectory }?.forEach { register(it) }

        val worker = Thread {
            try {
                while (!isClosedForSend) {
                    val key = watchService.take()
                    val dir = watchKeys[key] ?: continue

                    for (event in key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue

                        @Suppress("UNCHECKED_CAST")
                        val changed = (event as WatchEvent<Path>).context()
                        val fullPath = dir.resolve(changed).toFile()

                        when {
                            fullPath.isDirectory && dir == libraryRoot.toPath() -> {
                                register(fullPath)
                                trySend(LibraryEvent.SeriesAdded(fullPath))
                            }

                            fullPath.extension.lowercase() in chapterExtensions -> {
                                val seriesDir = dir.toFile()
                                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                    trySend(LibraryEvent.ChapterAdded(seriesDir, fullPath))
                                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                                    trySend(LibraryEvent.ChapterRemoved(seriesDir, fullPath))
                                }
                            }
                        }
                    }

                    if (!key.reset()) {
                        watchKeys.remove(key)
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                runCatching { watchService.close() }
            }
        }

        worker.isDaemon = true
        worker.name = "yokai-library-watcher"
        worker.start()

        awaitClose {
            worker.interrupt()
            runCatching { watchService.close() }
        }
    }.flowOn(Dispatchers.IO)
}
