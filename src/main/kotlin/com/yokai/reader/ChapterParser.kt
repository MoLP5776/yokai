package com.yokai.reader

import com.yokai.metadata.ChapterInfo
import java.io.File

object ChapterParser {
    private val chapterRegex = Regex("""(?:Ch(?:apter)?\.?\s*)([\d]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
    private val languageRegex = Regex("""\(([a-zA-Z]{2})\)""")
    private val translatorRegex = Regex("""\[([^\]]+)]""")
    private val chapterExtensions = setOf("cbz", "zip")

    fun parse(file: File): ChapterInfo {
        val name = file.nameWithoutExtension
        val chapterNumber = chapterRegex.find(name)?.groupValues?.get(1) ?: "0"
        val languageCode = languageRegex.find(name)?.groupValues?.get(1)?.lowercase() ?: "en"
        val translator = translatorRegex.find(name)?.groupValues?.get(1) ?: "Unknown"

        return ChapterInfo(
            filename = file.name,
            chapterNumber = chapterNumber,
            chapterFloat = chapterNumber.toFloatOrNull() ?: 0f,
            languageCode = languageCode,
            translator = translator,
            filePath = file,
        )
    }

    fun scanSeries(seriesDir: File): List<ChapterInfo> {
        return seriesDir
            .listFiles { file -> file.extension.lowercase() in chapterExtensions }
            ?.map { parse(it) }
            ?.sortedWith(compareBy<ChapterInfo> { it.chapterFloat }.thenBy { it.filename.lowercase() })
            ?: emptyList()
    }
}
