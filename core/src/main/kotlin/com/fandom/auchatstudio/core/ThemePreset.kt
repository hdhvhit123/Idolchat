package com.fandom.auchatstudio.core

/**
 * The three platform presets described in the design document. The enum name is what gets
 * persisted in Room, so never rename an entry without a migration.
 */
enum class ThemePreset(val displayName: String, val tagline: String) {
    LYSN_BUBBLE("Lysn Bubble", "Pastel dịu dàng, bong bóng bo tròn"),
    WEVERSE_DM("Weverse DM", "Nền tối, viền neon công nghệ"),
    INSTAGRAM_DM("Instagram DM", "Đen OLED, gradient hoàng hôn");

    /** One-tap switcher: cycles through presets in declaration order. */
    fun next(): ThemePreset = entries[(ordinal + 1) % entries.size]

    companion object {
        val Default = LYSN_BUBBLE

        fun fromName(name: String?): ThemePreset =
            entries.firstOrNull { it.name == name } ?: Default
    }
}
