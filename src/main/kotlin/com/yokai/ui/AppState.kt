package com.yokai.ui

import androidx.compose.runtime.*
import com.yokai.anilist.AniListClient
import com.yokai.metadata.*
import com.yokai.reader.*
import com.yokai.watcher.*
import kotlinx.coroutines.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class AppState(private val scope: CoroutineScope) {
    var prefs by mutableStateOf(PreferencesRepository.load())
        private set

    var keybindings by mutableStateOf(KeyBindingsRepository.load())

    var seriesList by mutableStateOf<List<File>>(emptyList())
        private set
    var selectedSeries by mutableStateOf<File?>(null)
        private set
    var selectedSeriesMetadata by mutableStateOf<SeriesMetadata?>(null)
        private set
    var selectedSeriesChapters by mutableStateOf<List<ChapterInfo>>(emptyList())
        private set

    var readerChapter by mutableStateOf<ChapterInfo?>(null)
        private set
    var selectedCategory by mutableStateOf<String?>(null)
    var aniListClient by mutableStateOf<AniListClient?>(null)
        private set
    var currentScreen by mutableStateOf<Screen>(Screen.Library)
    var libraryError by mutableStateOf<String?>(null)
        private set

    var selectedChapters by mutableStateOf<Set<String>>(emptySet())
        private set

    var seriesMetadataMap by mutableStateOf<Map<File, SeriesMetadata>>(emptyMap())
        private set
    var seriesChapterCounts by mutableStateOf<Map<File, Int>>(emptyMap())
        private set

    var isMultiSelectMode by mutableStateOf(false)
        private set
    var selectedSeriesList by mutableStateOf<Set<File>>(emptySet())
        private set

    var noNextChapterAvailable by mutableStateOf(false)
        private set

    private var watcherJob: Job? = null

    init {
        if (prefs.aniListAccessToken.isNotBlank()) {
            aniListClient = AniListClient(prefs.aniListAccessToken)
        }
        refreshLibrary()
        restartWatcher()

        prefs.defaultCategory?.let { category ->
            selectedCategory = category
        }
    }

    fun refreshLibrary() {
        val root = prefs.libraryRootPath
        if (root.isBlank()) {
            seriesList = emptyList()
            libraryError = null
            seriesMetadataMap = emptyMap()
            seriesChapterCounts = emptyMap()
            return
        }

        val rootFile = File(root)
        if (!rootFile.isDirectory) {
            seriesList = emptyList()
            libraryError = "Library folder does not exist."
            seriesMetadataMap = emptyMap()
            seriesChapterCounts = emptyMap()
            return
        }

        libraryError = null
        val discoveredSeries = MetadataRepository.discoverSeries(rootFile)
        seriesList = discoveredSeries
        seriesMetadataMap = discoveredSeries.associateWith { MetadataRepository.load(it) }
        seriesChapterCounts = discoveredSeries.associateWith { ChapterParser.scanSeries(it).size }
    }

    fun selectSeries(dir: File) {
        selectedSeries = dir
        selectedSeriesMetadata = seriesMetadataMap[dir] ?: MetadataRepository.load(dir)
        selectedSeriesChapters = ChapterParser.scanSeries(dir)
        currentScreen = Screen.SeriesDetail
    }

    fun markChapterRead(seriesDir: File, chapter: ChapterInfo, read: Boolean) {
        MetadataRepository.markChapterRead(seriesDir, chapter.filename, read)
        seriesMetadataMap = seriesMetadataMap.toMutableMap().apply {
            this[seriesDir] = MetadataRepository.load(seriesDir)
        }
        if (seriesDir == selectedSeries) {
            selectedSeriesMetadata = seriesMetadataMap[seriesDir]
        }
    }

    fun markAllChaptersRead(seriesDir: File, read: Boolean) {
        val chapters = ChapterParser.scanSeries(seriesDir)
        chapters.forEach { chapter ->
            MetadataRepository.markChapterRead(seriesDir, chapter.filename, read)
        }
        seriesMetadataMap = seriesMetadataMap.toMutableMap().apply {
            this[seriesDir] = MetadataRepository.load(seriesDir)
        }
        if (seriesDir == selectedSeries) {
            selectedSeriesMetadata = seriesMetadataMap[seriesDir]
        }
    }

    fun saveMetadata(metadata: SeriesMetadata) {
        val dir = selectedSeries ?: return
        MetadataRepository.save(dir, metadata)
        seriesMetadataMap = seriesMetadataMap.toMutableMap().apply {
            this[dir] = metadata
        }
        selectedSeriesMetadata = metadata
        refreshLibrary()
    }

    fun openReader(chapter: ChapterInfo) {
        readerChapter = chapter
        noNextChapterAvailable = false // Reset when a new chapter is opened
        currentScreen = Screen.Reader
        scope.launch(Dispatchers.IO) {
            CbzReader.extractChapter(chapter.filePath)
        }
    }

    fun nextChapter() {
        val current = readerChapter ?: return
        val chapters = selectedSeriesChapters
        val currentIndex = chapters.indexOfFirst { it.filePath == current.filePath }
        if (currentIndex >= 0 && currentIndex < chapters.lastIndex) {
            openReader(chapters[currentIndex + 1])
        } else {
            noNextChapterAvailable = true
        }
    }

    fun previousChapter() {
        val current = readerChapter ?: return
        val chapters = selectedSeriesChapters
        val currentIndex = chapters.indexOfFirst { it.filePath == current.filePath }
        if (currentIndex > 0) {
            openReader(chapters[currentIndex - 1])
        }
    }

    fun clearNoNextChapter() {
        noNextChapterAvailable = false
    }

    fun previousChapterAtLastPage() {
        val current = readerChapter ?: return
        val chapters = selectedSeriesChapters
        val currentIndex = chapters.indexOfFirst { it.filePath == current.filePath }
        if (currentIndex > 0) {
            openReaderAtLastPage(chapters[currentIndex - 1])
        }
    }

    var openAtLastPage by mutableStateOf(false)
        private set

    fun consumeOpenAtLastPage() {
        openAtLastPage = false
    }

    private fun openReaderAtLastPage(chapter: ChapterInfo) {
        openAtLastPage = true
        readerChapter = chapter
        noNextChapterAvailable = false
        currentScreen = Screen.Reader
        scope.launch(Dispatchers.IO) {
            CbzReader.extractChapter(chapter.filePath)
        }
    }

    fun closeReader() {
        readerChapter = null
        noNextChapterAvailable = false // Reset when reader is closed
        currentScreen = Screen.SeriesDetail
    }

    fun toggleChapterSelection(filename: String) {
        selectedChapters = if (selectedChapters.contains(filename)) {
            selectedChapters - filename
        } else {
            selectedChapters + filename
        }
    }

    fun setChapterSelection(filenames: Set<String>) {
        selectedChapters = filenames
    }

    fun clearChapterSelection() {
        selectedChapters = emptySet()
    }

    fun selectAllChapters() {
        selectedChapters = selectedSeriesChapters.map { it.filename }.toSet()
    }

    fun selectPrevious(currentChapter: ChapterInfo) {
        val chapters = selectedSeriesChapters
        val currentIndex = chapters.indexOfFirst { it.filename == currentChapter.filename }
        if (currentIndex > 0) {
            val chaptersToSelect = chapters.subList(0, currentIndex).map { it.filename }.toSet()
            selectedChapters = chaptersToSelect
        }
    }

    fun markSelectedChaptersRead(seriesDir: File, read: Boolean) {
        selectedChapters.forEach { filename ->
            MetadataRepository.markChapterRead(seriesDir, filename, read)
        }
        seriesMetadataMap = seriesMetadataMap.toMutableMap().apply {
            this[seriesDir] = MetadataRepository.load(seriesDir)
        }
        if (seriesDir == selectedSeries) {
            selectedSeriesMetadata = seriesMetadataMap[seriesDir]
        }
    }

    fun removeSeries(dir: File): Boolean {
        val removed = dir.deleteRecursively()
        if (removed) {
            seriesMetadataMap = seriesMetadataMap.toMutableMap().apply {
                remove(dir)
            }
            seriesChapterCounts = seriesChapterCounts.toMutableMap().apply {
                remove(dir)
            }
            seriesList = seriesList.filter { it != dir }
            if (dir == selectedSeries) {
                selectedSeries = null
                selectedSeriesMetadata = null
                selectedSeriesChapters = emptyList()
            }
        }
        return removed
    }

    fun updateSeriesCategories(dir: File, categories: List<String>) {
        val metadata = MetadataRepository.load(dir)
        val updated = metadata.copy(categories = categories)
        MetadataRepository.save(dir, updated)
        seriesMetadataMap = seriesMetadataMap.toMutableMap().apply {
            this[dir] = updated
        }
        if (dir == selectedSeries) {
            selectedSeriesMetadata = updated
        }
        refreshLibrary()
    }

    fun updateLibraryPath(path: String) {
        prefs = prefs.copy(libraryRootPath = path.trim())
        PreferencesRepository.save(prefs)
        refreshLibrary()
        restartWatcher()
    }

    fun saveAniListToken(token: String) {
        prefs = prefs.copy(aniListAccessToken = token.trim())
        PreferencesRepository.save(prefs)
        aniListClient?.close()
        aniListClient = if (prefs.aniListAccessToken.isNotBlank()) AniListClient(prefs.aniListAccessToken) else null
    }

    fun updateDefaultCategory(category: String?) {
        prefs = prefs.copy(defaultCategory = category)
        PreferencesRepository.save(prefs)
        selectedCategory = category
    }

    val allCategories: List<String>
        get() = seriesList
            .mapNotNull { seriesMetadataMap[it] }
            .flatMap { it.categories }
            .distinct()
            .sorted()

    val filteredSeriesList: List<File>
        get() = selectedCategory?.let { category ->
            seriesList.filter { dir -> seriesMetadataMap[dir]?.categories?.contains(category) ?: false }
        } ?: seriesList

    fun readChapterCount(seriesDir: File): Int {
        return seriesMetadataMap[seriesDir]?.chapterReadState?.values?.count { it } ?: 0
    }

    fun toggleMultiSelectMode() {
        isMultiSelectMode = !isMultiSelectMode
        if (!isMultiSelectMode) {
            selectedSeriesList = emptySet()
        }
    }

    fun toggleSeriesSelection(seriesDir: File) {
        selectedSeriesList = if (selectedSeriesList.contains(seriesDir)) {
            selectedSeriesList - seriesDir
        } else {
            selectedSeriesList + seriesDir
        }
    }

    private fun restartWatcher() {
        watcherJob?.cancel()
        val root = prefs.libraryRootPath
        if (root.isBlank()) return

        val rootFile = File(root)
        if (!rootFile.isDirectory) return

        watcherJob = scope.launch(Dispatchers.IO) {
            LibraryWatcher.watch(rootFile).collectLatest { event ->
                withContext(Dispatchers.Main) {
                    when (event) {
                        is LibraryEvent.SeriesAdded -> refreshLibrary()
                        is LibraryEvent.ChapterAdded -> {
                            seriesChapterCounts = seriesChapterCounts.toMutableMap().apply {
                                this[event.seriesDir] = ChapterParser.scanSeries(event.seriesDir).size
                            }
                            if (event.seriesDir == selectedSeries) {
                                selectedSeriesChapters = ChapterParser.scanSeries(event.seriesDir)
                            }
                            seriesMetadataMap = seriesMetadataMap.toMutableMap().apply {
                                this[event.seriesDir] = MetadataRepository.load(event.seriesDir)
                            }
                            refreshLibrary()
                        }

                        is LibraryEvent.ChapterRemoved -> {
                            seriesChapterCounts = seriesChapterCounts.toMutableMap().apply {
                                this[event.seriesDir] = ChapterParser.scanSeries(event.seriesDir).size
                            }
                            if (event.seriesDir == selectedSeries) {
                                selectedSeriesChapters = ChapterParser.scanSeries(event.seriesDir)
                            }
                            seriesMetadataMap = seriesMetadataMap.toMutableMap().apply {
                                this[event.seriesDir] = MetadataRepository.load(event.seriesDir)
                            }
                            refreshLibrary()
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen {
    data object Library : Screen()
    data object SeriesDetail : Screen()
    data object Reader : Screen()
    data object Settings : Screen()
    data object KeyBindings : Screen()
}