package com.fandom.auchatstudio.core

object NameRules {
    const val MAX_NAME_LENGTH = 30
    const val MAX_TITLE_LENGTH = 60

    /** Trims, collapses inner whitespace and caps the length. Returns null for a blank name. */
    fun normalizeName(input: String, maxLength: Int = MAX_NAME_LENGTH): String? {
        val cleaned = input.trim().replace(Regex("\\s+"), " ")
        if (cleaned.isEmpty()) return null
        return cleaned.take(maxLength).trimEnd()
    }

    /**
     * Initials for an avatar placeholder. Text in brackets is a note ("Minho (Bias)") and is
     * ignored, so "Minho (Bias)" gives "M" and "Y/N (Bạn)" gives "YN".
     */
    fun initialsOf(name: String): String {
        val base = name.replace(Regex("\\(.*?\\)|\\[.*?]"), " ").trim()
        val words = base.split(Regex("[\\s/_-]+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return name.trim().firstOrNull()?.uppercase() ?: "?"
        if (words.size == 1) return words[0].first().uppercase()
        return (words[0].first().toString() + words[1].first()).uppercase()
    }
}

object VoiceFormat {
    const val MIN_SECONDS = 1
    const val MAX_SECONDS = 300

    fun clampSeconds(seconds: Int): Int = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)

    /** 7 -> "0:07", 65 -> "1:05". */
    fun format(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }
}

object TimeLabel {
    private val pattern = Regex("^([01]?\\d|2[0-3])[:hH.]([0-5]\\d)$")

    /** Accepts "9:05", "09:05", "23h41", "7.30"; returns "HH:mm" or null when invalid. */
    fun normalize(input: String): String? {
        val match = pattern.matchEntire(input.trim()) ?: return null
        val (h, m) = match.destructured
        return "%02d:%s".format(h.toInt(), m)
    }
}
