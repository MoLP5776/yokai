package com.yokai.anilist

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.File

class AniListPreferences {

    @Serializable
    private data class Settings(
        val token: String? = null
    )

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val configDir =
        File(System.getProperty("user.home"), ".config/yokai")

    private val configFile =
        File(configDir, "anilist.json")

    init {
        configDir.mkdirs()
    }

    fun saveToken(token: String) {
        configFile.writeText(
            json.encodeToString(
                Settings(token)
            )
        )
    }

    fun getToken(): String? {

        if (!configFile.exists()) {
            return null
        }

        return try {
            json.decodeFromString<Settings>(
                configFile.readText()
            ).token
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        configFile.delete()
    }
}