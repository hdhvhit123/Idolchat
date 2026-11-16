package com.fandom.auchatstudio.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.fandom.auchatstudio.core.ColorMath
import com.fandom.auchatstudio.ui.chat.ChatStyle
import com.fandom.auchatstudio.ui.theme.BrandGradientColors
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Guards the colour pairs that carry text against drifting below WCAG AA (4.5:1). */
class ThemeContrastTest {
    private fun Color.argb(): Long = toArgb().toLong() and 0xFFFFFFFFL

    private fun contrast(a: Long, b: Long): Double {
        val la = ColorMath.luminance(a)
        val lb = ColorMath.luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun whiteTextIsReadableAcrossTheBrandGradient() {
        BrandGradientColors.forEach { stop ->
            assertThat(contrast(0xFFFFFFFF, stop.argb())).isAtLeast(4.5)
        }
    }

    @Test
    fun outgoingBubbleTextIsReadableOnPastelAndNeonSkins() {
        listOf(ChatStyle.Lysn, ChatStyle.Weverse).forEach { style ->
            style.outgoingGradient.forEach { stop ->
                assertThat(contrast(style.outgoingText, stop)).isAtLeast(4.5)
            }
        }
    }

    @Test
    fun chatHeaderTextIsReadableInEverySkin() {
        listOf(ChatStyle.Lysn, ChatStyle.Weverse, ChatStyle.Instagram).forEach { style ->
            assertThat(contrast(style.headerContent, style.headerBackground)).isAtLeast(4.5)
            assertThat(contrast(style.incomingText, style.incomingBubble)).isAtLeast(4.5)
        }
    }
}
