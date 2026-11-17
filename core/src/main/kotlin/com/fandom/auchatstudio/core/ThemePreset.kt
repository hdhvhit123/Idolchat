package com.fandom.auchatstudio.core

/**
 * The three chat skins described in the design document. The enum name is what gets
 * persisted in Room, so never rename an entry without a migration.
 *
 * The names shown to people are generic on purpose: the skins are inspired by well-known
 * messaging apps, but other companies' brand names must not appear in the app or the store
 * listing.
 */
enum class ThemePreset(val displayName: String, val tagline: String) {
    LYSN_BUBBLE("Pastel", "Pastel dịu dàng, bong bóng bo tròn"),
    WEVERSE_DM("Neon", "Nền tối, viền neon công nghệ"),
    INSTAGRAM_DM("Hoàng hôn", "Đen OLED, gradient hoàng hôn");

    /** One-tap switcher: cycles through presets in declaration order. */
    fun next(): ThemePreset = entries[(ordinal + 1) % entries.size]

    companion object {
        val Default = LYSN_BUBBLE

        fun fromName(name: String?): ThemePreset =
            entries.firstOrNull { it.name == name } ?: Default
    }
}
