package com.fandom.auchatstudio.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextUtilsTest {
    @Test
    fun `normalizeName trims and collapses whitespace`() {
        assertThat(NameRules.normalizeName("  Minho   (Bias) ")).isEqualTo("Minho (Bias)")
    }

    @Test
    fun `normalizeName rejects blank`() {
        assertThat(NameRules.normalizeName("   \n ")).isNull()
    }

    @Test
    fun `normalizeName caps length`() {
        val name = NameRules.normalizeName("a".repeat(100))
        assertThat(name).hasLength(NameRules.MAX_NAME_LENGTH)
    }

    @Test
    fun `initials ignore bracket notes and support vietnamese`() {
        assertThat(NameRules.initialsOf("Minho (Bias)")).isEqualTo("M")
        assertThat(NameRules.initialsOf("Y/N (Bạn)")).isEqualTo("YN")
        assertThat(NameRules.initialsOf("Lee Know")).isEqualTo("LK")
        assertThat(NameRules.initialsOf("đức hiền")).isEqualTo("ĐH")
        assertThat(NameRules.initialsOf("(???)")).isEqualTo("(")
        assertThat(NameRules.initialsOf("")).isEqualTo("?")
    }

    @Test
    fun `voice durations format as m ss`() {
        assertThat(VoiceFormat.format(7)).isEqualTo("0:07")
        assertThat(VoiceFormat.format(65)).isEqualTo("1:05")
        assertThat(VoiceFormat.format(-3)).isEqualTo("0:00")
        assertThat(VoiceFormat.clampSeconds(0)).isEqualTo(VoiceFormat.MIN_SECONDS)
        assertThat(VoiceFormat.clampSeconds(9999)).isEqualTo(VoiceFormat.MAX_SECONDS)
    }

    @Test
    fun `time labels are normalised`() {
        assertThat(TimeLabel.normalize("9:05")).isEqualTo("09:05")
        assertThat(TimeLabel.normalize("23h41")).isEqualTo("23:41")
        assertThat(TimeLabel.normalize(" 7.30 ")).isEqualTo("07:30")
        assertThat(TimeLabel.normalize("24:00")).isNull()
        assertThat(TimeLabel.normalize("12:60")).isNull()
        assertThat(TimeLabel.normalize("hello")).isNull()
    }
}
