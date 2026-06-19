package com.yokai.anilist

class AniListRepository(
    private val client: AniListClient
) {

    suspend fun search(
        query: String
    ) = client.searchManga(query)

    suspend fun updateProgress(
        mediaId: Int,
        chapter: Int
    ) {
        client.updateLibraryEntry(
            mediaId = mediaId,
            progress = chapter,
            status = "CURRENT"
        )
    }

    suspend fun markCompleted(
        mediaId: Int,
        chapterCount: Int
    ) {
        client.updateLibraryEntry(
            mediaId = mediaId,
            progress = chapterCount,
            status = "COMPLETED"
        )
    }

    suspend fun getEntry(
        mediaId: Int
    ) = client.getLibraryEntry(mediaId)
}