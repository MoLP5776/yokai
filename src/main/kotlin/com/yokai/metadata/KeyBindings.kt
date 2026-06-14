package com.yokai.metadata

import kotlinx.serialization.Serializable

@Serializable
data class KeyBindings(
    val nextPageKey: String = "Right",
    val prevPageKey: String = "Left",
    val nextChapterKey: String = "ctrl+Right",
    val prevChapterKey: String = "ctrl+Left",
    val nextChapterAltKey: String = "]",
    val prevChapterAltKey: String = "[",
    val exitReaderKey: String = "Backspace",
    val closeKey: String = "Escape",
    val toggleReadingDirKey: String = "D",
    val togglePageStyleKey: String = "Q",
    val toggleDoublePageOffsetKey: String = "U",
    val toggleFullscreenKey: String = "F",
) {
    private fun normalizeKeyEventString(keyEventString: String): String {
        val match = Regex("Key\\(([^)]+)\\)").find(keyEventString)
        return match?.groupValues?.get(1)?.lowercase() ?: keyEventString.lowercase()
    }

    private fun isKeybindingMatch(
        bindingString: String,
        eventKeyString: String, // This is keyEvent.key.toString()
        ctrlPressed: Boolean,
        shiftPressed: Boolean,
        altPressed: Boolean
    ): Boolean {
        val normalizedEventKey = normalizeKeyEventString(eventKeyString)

        val parts = bindingString.split("+").map { it.trim().lowercase() }
        var requiredCtrl = false
        var requiredShift = false
        var requiredAlt = false
        var mainKey: String? = null

        for (part in parts) {
            when (part) {
                "ctrl" -> requiredCtrl = true
                "shift" -> requiredShift = true
                "alt" -> requiredAlt = true
                else -> mainKey = part
            }
        }

        if (mainKey == null) return false // A keybinding must have a main key

        // Compare main key
        if (mainKey != normalizedEventKey) return false

        // Compare modifiers
        if (requiredCtrl != ctrlPressed) return false
        if (requiredShift != shiftPressed) return false
        if (requiredAlt != altPressed) return false

        return true
    }

    fun matches(eventKey: String, ctrlPressed: Boolean, shiftPressed: Boolean, altPressed: Boolean): String? {
        if (isKeybindingMatch(nextPageKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "nextPage"
        if (isKeybindingMatch(prevPageKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "prevPage"
        if (isKeybindingMatch(nextChapterKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "nextChapter"
        if (isKeybindingMatch(prevChapterKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "prevChapter"
        if (isKeybindingMatch(nextChapterAltKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "nextChapter"
        if (isKeybindingMatch(prevChapterAltKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "prevChapter"
        if (isKeybindingMatch(exitReaderKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "exitReader"
        if (isKeybindingMatch(closeKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "closeReader"
        if (isKeybindingMatch(toggleReadingDirKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "toggleReadingDir"
        if (isKeybindingMatch(togglePageStyleKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "togglePageStyle"
        if (isKeybindingMatch(toggleDoublePageOffsetKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "toggleDoublePageOffset"
        if (isKeybindingMatch(toggleFullscreenKey, eventKey, ctrlPressed, shiftPressed, altPressed)) return "toggleFullscreen"

        return null
    }
}