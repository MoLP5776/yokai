package com.yokai.anilist

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

private const val ANILIST_API = "https://graphql.anilist.co"

object AniListAuth {
    private const val CLIENT_ID = "YOUR_CLIENT_ID"
    private const val REDIRECT_URI = "https://anilist.co/api/v2/oauth/pin"

    fun buildAuthUrl(): String =
        "https://anilist.co/api/v2/oauth/authorize" +
                "?client_id=$CLIENT_ID" +
                "&redirect_uri=$REDIRECT_URI" +
                "&response_type=token"
}

@Serializable
data class AniListManga(
    val id: Int,
    val title: AniListTitle,
    val description: String?,
    val status: String?,
    val chapters: Int?,
    val coverImage: AniListCover?,
    val staff: AniListStaffConnection? = null,
)

@Serializable
data class AniListTitle(val romaji: String?, val english: String?, val native: String?)

@Serializable
data class AniListCover(val large: String?, val medium: String?)

@Serializable
data class AniListStaffConnection(val edges: List<AniListStaffEdge>? = null)

@Serializable
data class AniListStaffEdge(val role: String?, val node: AniListStaffNode?)

@Serializable
data class AniListStaffNode(val name: AniListStaffName?)

@Serializable
data class AniListStaffName(val full: String?)

class AniListClient(private val accessToken: String) {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun searchManga(query: String, perPage: Int = 10): List<AniListManga> {
        val gql = """
            query (${'$'}search: String, ${'$'}perPage: Int) {
              Page(perPage: ${'$'}perPage) {
                media(search: ${'$'}search, type: MANGA) {
                  id
                  title { romaji english native }
                  description(asHtml: false)
                  status
                  chapters
                  coverImage { large medium }
                  staff { edges { role node { name { full } } } }
                }
              }
            }
        """.trimIndent()
        val body = buildJsonObject {
            put("query", gql)
            put(
                "variables",
                buildJsonObject {
                    put("search", query)
                    put("perPage", perPage)
                },
            )
        }

        return runCatching {
            val response: JsonObject = httpClient.post(ANILIST_API) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                bearerAuth(accessToken)
                setBody(body)
            }.body()
            val mediaArray = response["data"]
                ?.jsonObject?.get("Page")
                ?.jsonObject?.get("media")
                ?.jsonArray
                ?: return emptyList()
            json.decodeFromJsonElement<List<AniListManga>>(mediaArray)
        }.getOrElse {
            emptyList()
        }
    }

    suspend fun updateProgress(mediaId: Int, progress: Int, status: String = "CURRENT"): Boolean {
        val gql = """
            mutation (${'$'}mediaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus) {
              SaveMediaListEntry(mediaId: ${'$'}mediaId, progress: ${'$'}progress, status: ${'$'}status) {
                id
                progress
                status
              }
            }
        """.trimIndent()
        val body = buildJsonObject {
            put("query", gql)
            put(
                "variables",
                buildJsonObject {
                    put("mediaId", mediaId)
                    put("progress", progress)
                    put("status", status)
                },
            )
        }

        return runCatching {
            val response: JsonObject = httpClient.post(ANILIST_API) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                bearerAuth(accessToken)
                setBody(body)
            }.body()
            response["errors"] == null
        }.getOrElse {
            false
        }
    }

    fun close() = httpClient.close()
}
