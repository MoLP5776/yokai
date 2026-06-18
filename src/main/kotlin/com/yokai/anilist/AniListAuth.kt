package com.yokai.anilist

object AniListAuth {

    const val CLIENT_ID = "43905"

    private const val REDIRECT_URI =
        "https://anilist.co/api/v2/oauth/pin"

    fun buildAuthUrl(): String {
        return buildString {
            append("https://anilist.co/api/v2/oauth/authorize")
            append("?client_id=$CLIENT_ID")
            append("&response_type=token")
            append("&redirect_uri=$REDIRECT_URI")
        }
    }
}