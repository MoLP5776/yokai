package com.yokai.anilist

import kotlinx.serialization.Serializable

@Serializable
data class AniListManga(
    val id: Int,
    val title: AniListTitle,
    val description: String? = null,
    val status: String? = null,
    val chapters: Int? = null,
    val coverImage: AniListCover? = null,
    val staff: AniListStaffConnection? = null
)

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class AniListCover(
    val large: String? = null,
    val medium: String? = null
)

@Serializable
data class AniListStaffConnection(
    val edges: List<AniListStaffEdge>? = null
)

@Serializable
data class AniListStaffEdge(
    val role: String? = null,
    val node: AniListStaffNode? = null
)

@Serializable
data class AniListStaffNode(
    val name: AniListStaffName? = null
)

@Serializable
data class AniListStaffName(
    val full: String? = null
)

@Serializable
data class ViewerResponse(
    val data: ViewerData
)

@Serializable
data class ViewerData(
    val Viewer: Viewer
)

@Serializable
data class Viewer(
    val id: Int,
    val name: String,
    val mediaListOptions: MediaListOptions
)

@Serializable
data class MediaListOptions(
    val scoreFormat: String
)

data class AniListLibraryEntry(
    val id: Int,
    val mediaId: Int,
    val title: String,
    val description: String?,
    val coverUrl: String?,
    val progress: Int,
    val score: Float,
    val status: String
)