package com.yokai.metadata

import java.io.File
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
enum class AppTheme(val displayName: String) {
    DEFAULT("Default"),
    CATPPUCCIN("Catppuccin"),
    NORD("Nord"),
    DRACULA("Dracula"),
    GRUVBOX("Gruvbox"),
    FOREST("Forest"),
    STRAWBERRY("Strawberry"),
    MONOCHROME("Monochrome"),
}

@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
}

@Serializable
data class AppPreferences(
    val libraryRootPath: String = "",
    val aniListAccessToken: String = "",
    val defaultSortDescending: Boolean = true,
    val defaultCategory: String? = null,
    val librarySortAscending: Boolean = true,
    val libraryGridColumns: Int = 6,
    val appTheme: AppTheme = AppTheme.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val pureBlackDarkMode: Boolean = false,
)

object PreferencesRepository {
    private val configDir: File by lazy {
        File(System.getProperty("user.home"), ".config/yokai").also { it.mkdirs() }
    }
    private val prefsFile: File get() = File(configDir, "preferences.json")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun load(): AppPreferences {
        if (!prefsFile.exists()) return AppPreferences()

        return runCatching {
            json.decodeFromString<AppPreferences>(prefsFile.readText())
        }.getOrElse {
            AppPreferences()
        }
    }

    fun save(prefs: AppPreferences) {
        prefsFile.writeText(json.encodeToString(prefs))
    }
}

object KeyBindingsRepository {
    private val configDir: File by lazy {
        File(System.getProperty("user.home"), ".config/yokai").also { it.mkdirs() }
    }
    private val bindingsFile: File get() = File(configDir, "keybindings.json")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun load(): KeyBindings {
        if (!bindingsFile.exists()) return KeyBindings()

        return runCatching {
            json.decodeFromString<KeyBindings>(bindingsFile.readText())
        }.getOrElse {
            KeyBindings()
        }
    }

    fun save(bindings: KeyBindings) {
        bindingsFile.writeText(json.encodeToString(bindings))
    }
}