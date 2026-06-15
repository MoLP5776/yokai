package com.yokai.metadata

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
enum class SeriesStatus {
    ONGOING,
    COMPLETED,
    HIATUS,
    CANCELLED,
    UNKNOWN,
}

@Serializable
enum class ReadingStatus {
    READING,
    COMPLETED,
    PLAN_TO_READ,
    DROPPED,
    PAUSED,
    NONE,
}

@Serializable
data class SeriesMetadata(
    val title: String,
    val description: String = "",
    val author: String = "",
    val artist: String = "",
    val status: SeriesStatus = SeriesStatus.UNKNOWN,
    val coverImagePath: String? = null,
    val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val aniListId: Int? = null,
    val readingStatus: ReadingStatus = ReadingStatus.NONE,
    val chapterReadState: Map<String, Boolean> = emptyMap(),
)

data class ChapterInfo(
    val filename: String,
    val chapterNumber: String,
    val chapterFloat: Float,
    val languageCode: String,
    val translator: String,
    val filePath: File,
)

val LANGUAGE_FLAGS: Map<String, String> = mapOf(
    "en" to "EN",
    "jp" to "JP",
    "ja" to "JP",
    "fr" to "FR",
    "de" to "DE",
    "es" to "ES",
    "pt" to "PT",
    "kr" to "KR",
    "ko" to "KR",
    "zh" to "ZH",
    "it" to "IT",
    "ru" to "RU",
    "pl" to "PL",
    "nl" to "NL",
    "tr" to "TR",
)

val LANGUAGE_NAMES: Map<String, String> = mapOf(
    "en" to "English",
    "jp" to "Japanese",
    "ja" to "Japanese",
    "fr" to "French",
    "de" to "German",
    "es" to "Spanish",
    "pt" to "Portuguese",
    "kr" to "Korean",
    "ko" to "Korean",
    "zh" to "Chinese",
    "it" to "Italian",
    "ru" to "Russian",
    "pl" to "Polish",
    "nl" to "Dutch",
    "tr" to "Turkish",
)