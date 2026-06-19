package com.yokai.anilist

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

object AniListAuth {

    const val CLIENT_ID = "43905"

    private const val CLIENT_SECRET = "vTgsODxclVK3E1xceBitrx6uLfgZ6I0HIFvFfsUL"

    private const val REDIRECT_URI =
        "https://anilist.co/api/v2/oauth/pin"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    // AniList's OAuth server (Laravel Passport) only supports the authorization-code
    // grant for registered clients; implicit grant returns "unsupported_grant_type".
    fun buildAuthUrl(): String {
        return buildString {
            append("https://anilist.co/api/v2/oauth/authorize")
            append("?client_id=$CLIENT_ID")
            append("&response_type=code")
            append("&redirect_uri=$REDIRECT_URI")
        }
    }

    /**
     * Exchanges an authorization code for an access token.
     * @param code The authorization code shown on AniList's redirect page.
     * @return The access token string, or null if the exchange fails.
     */
    suspend fun exchangeCodeForToken(code: String): String? {
        return try {
            val response: JsonObject = httpClient.post("https://anilist.co/api/v2/oauth/token") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("grant_type", "authorization_code")
                        put("client_id", CLIENT_ID)
                        put("client_secret", CLIENT_SECRET)
                        put("redirect_uri", REDIRECT_URI)
                        put("code", code)
                    }
                )
            }.body()

            response["access_token"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            println("Error exchanging code for token: ${e.message}")
            null
        }
    }
}
