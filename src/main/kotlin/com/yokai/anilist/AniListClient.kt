package com.yokai.anilist

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

private const val ANILIST_API =
    "https://graphql.anilist.co"

class AniListClient(
    private val accessToken: String
) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private var userId: Int? = null
    private var scoreFormat: String? = null

    suspend fun getUsername(): String? {

        val query = """
            query {
                Viewer {
                    id
                    name
                    mediaListOptions {
                        scoreFormat
                    }
                }
            }
        """.trimIndent()

        return try {

            val response: JsonObject =
                httpClient.post(ANILIST_API) {
                    bearerAuth(accessToken)
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("query", query)
                    })
                }.body()

            val viewer =
                response["data"]!!
                    .jsonObject["Viewer"]!!
                    .jsonObject

            userId =
                viewer["id"]!!
                    .jsonPrimitive.int

            scoreFormat =
                viewer["mediaListOptions"]!!
                    .jsonObject["scoreFormat"]!!
                    .jsonPrimitive.content

            viewer["name"]!!
                .jsonPrimitive.content

        } catch (_: Exception) {
            null
        }
    }

    suspend fun searchManga(
        queryText: String
    ): List<AniListManga> {

        val query = """
            query (${'$'}query: String) {
              Page(perPage: 10) {
                media(
                  search: ${'$'}query,
                  type: MANGA,
                  format_not_in: [NOVEL]
                ) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  description
                  status
                  chapters
                  coverImage {
                    large
                    medium
                  }
                }
              }
            }
        """.trimIndent()

        return try {

            val response: JsonObject =
                httpClient.post(ANILIST_API) {
                    bearerAuth(accessToken)
                    contentType(ContentType.Application.Json)

                    setBody(
                        buildJsonObject {
                            put("query", query)

                            put(
                                "variables",
                                buildJsonObject {
                                    put("query", queryText)
                                }
                            )
                        }
                    )
                }.body()

            val media =
                response["data"]
                    ?.jsonObject
                    ?.get("Page")
                    ?.jsonObject
                    ?.get("media")
                    ?.jsonArray
                    ?: return emptyList()

            json.decodeFromJsonElement(media)

        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getLibraryEntry(
        mediaId: Int
    ): AniListLibraryEntry? {

        if (userId == null)
            getUsername()

        val query = """
            query (
              ${'$'}userId: Int!,
              ${'$'}mediaId: Int!
            ) {
              MediaList(
                userId: ${'$'}userId,
                type: MANGA,
                mediaId: ${'$'}mediaId
              ) {
                id
                status
                score
                progress

                media {
                  id

                  title {
                    romaji
                  }

                  description

                  coverImage {
                    large
                  }
                }
              }
            }
        """.trimIndent()

        return try {

            val response: JsonObject =
                httpClient.post(ANILIST_API) {

                    bearerAuth(accessToken)
                    contentType(ContentType.Application.Json)

                    setBody(
                        buildJsonObject {
                            put("query", query)

                            put(
                                "variables",
                                buildJsonObject {
                                    put("userId", userId!!)
                                    put("mediaId", mediaId)
                                }
                            )
                        }
                    )
                }.body()

            val entry =
                response["data"]
                    ?.jsonObject
                    ?.get("MediaList")
                    ?.jsonObject
                    ?: return null

            AniListLibraryEntry(
                id = entry["id"]!!.jsonPrimitive.int,
                mediaId = entry["media"]!!
                    .jsonObject["id"]!!
                    .jsonPrimitive.int,
                title = entry["media"]!!
                    .jsonObject["title"]!!
                    .jsonObject["romaji"]!!
                    .jsonPrimitive.content,
                description = entry["media"]!!
                    .jsonObject["description"]
                    ?.jsonPrimitive
                    ?.content,
                coverUrl = entry["media"]!!
                    .jsonObject["coverImage"]!!
                    .jsonObject["large"]
                    ?.jsonPrimitive
                    ?.content,
                progress = entry["progress"]!!
                    .jsonPrimitive.int,
                score = entry["score"]!!
                    .jsonPrimitive.float,
                status = entry["status"]!!
                    .jsonPrimitive.content
            )

        } catch (_: Exception) {
            null
        }
    }

    suspend fun addLibraryEntry(
        mediaId: Int,
        progress: Int,
        status: String = "CURRENT"
    ): Boolean {

        val mutation = """
            mutation (
                ${'$'}mediaId: Int,
                ${'$'}progress: Int,
                ${'$'}status: MediaListStatus
            ) {
                SaveMediaListEntry(
                    mediaId: ${'$'}mediaId,
                    progress: ${'$'}progress,
                    status: ${'$'}status
                ) {
                    id
                }
            }
        """.trimIndent()

        return try {

            val response: JsonObject =
                httpClient.post(ANILIST_API) {

                    bearerAuth(accessToken)
                    contentType(ContentType.Application.Json)

                    setBody(
                        buildJsonObject {
                            put("query", mutation)

                            put(
                                "variables",
                                buildJsonObject {
                                    put("mediaId", mediaId)
                                    put("progress", progress)
                                    put("status", status)
                                }
                            )
                        }
                    )
                }.body()

            response["errors"] == null

        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateLibraryEntry(
        mediaId: Int,
        progress: Int,
        score: Float = 0f,
        status: String = "CURRENT"
    ): Boolean {

        var existing =
            getLibraryEntry(mediaId)

        if (existing == null) {

            addLibraryEntry(
                mediaId = mediaId,
                progress = progress,
                status = status
            )

            existing =
                getLibraryEntry(mediaId)
                    ?: return false
        }

        val mutation = """
            mutation (
              ${'$'}listId: Int,
              ${'$'}progress: Int,
              ${'$'}status: MediaListStatus,
              ${'$'}score: Float
            ) {
              SaveMediaListEntry(
                id: ${'$'}listId,
                progress: ${'$'}progress,
                status: ${'$'}status,
                score: ${'$'}score
              ) {
                id
              }
            }
        """.trimIndent()

        return try {

            val response: JsonObject =
                httpClient.post(ANILIST_API) {

                    bearerAuth(accessToken)
                    contentType(ContentType.Application.Json)

                    setBody(
                        buildJsonObject {

                            put("query", mutation)

                            put(
                                "variables",
                                buildJsonObject {
                                    put("listId", existing.id)
                                    put("progress", progress)
                                    put("status", status)
                                    put("score", score)
                                }
                            )
                        }
                    )
                }.body()

            response["errors"] == null

        } catch (_: Exception) {
            false
        }
    }

    fun close() {
        httpClient.close()
    }
}