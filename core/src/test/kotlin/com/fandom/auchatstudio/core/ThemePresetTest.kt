package com.fandom.auchatstudio.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemePresetTest {
    @Test
    fun `next cycles through all presets and wraps around`() {
        assertThat(ThemePreset.LYSN_BUBBLE.next()).isEqualTo(ThemePreset.WEVERSE_DM)
        assertThat(ThemePreset.WEVERSE_DM.next()).isEqualTo(ThemePreset.INSTAGRAM_DM)
        assertThat(ThemePreset.INSTAGRAM_DM.next()).isEqualTo(ThemePreset.LYSN_BUBBLE)
    }

    @Test
    fun `fromName falls back to default for unknown or null`() {
        assertThat(ThemePreset.fromName("WEVERSE_DM")).isEqualTo(ThemePreset.WEVERSE_DM)
        assertThat(ThemePreset.fromName("nope")).isEqualTo(ThemePreset.Default)
        assertThat(ThemePreset.fromName(null)).isEqualTo(ThemePreset.Default)
    }

    @Test
    fun `message type fromName falls back to text`() {
        assertThat(MessageType.fromName("VOICE")).isEqualTo(MessageType.VOICE)
        assertThat(MessageType.fromName("garbage")).isEqualTo(MessageType.TEXT)
    }
}
