package com.yokai.anilist

enum class TrackStatus {
    READING,
    PLANNING,
    COMPLETED,
    DROPPED,
    PAUSED
}

object AniListStatus {

    // Maps AniList API status strings to internal TrackStatus enum.
    // REREADING is treated as READING for internal consistency.
    private val STATUS_MAP = mapOf(
        "CURRENT" to TrackStatus.READING,
        "PLANNING" to TrackStatus.PLANNING,
        "COMPLETED" to TrackStatus.COMPLETED,
        "DROPPED" to TrackStatus.DROPPED,
        "PAUSED" to TrackStatus.PAUSED,
        "REREADING" to TrackStatus.READING
    )

    /**
     * Converts an internal [TrackStatus] to its corresponding AniList API string representation.
     * Note: AniList's "REREADING" status is mapped to "CURRENT" when updating.
     */
    fun toAniList(status: TrackStatus): String =
        when (status) {
            TrackStatus.READING -> "CURRENT"
            TrackStatus.PLANNING -> "PLANNING"
            TrackStatus.COMPLETED -> "COMPLETED"
            TrackStatus.DROPPED -> "DROPPED"
            TrackStatus.PAUSED -> "PAUSED"
        }
}