package com.fandom.auchatstudio.ui.chat

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.fandom.auchatstudio.core.ThemePreset

/**
 * Visual language of one platform preset. Colours are ARGB Longs so the same spec drives both
 * the Compose UI and the off-screen exporter (android.graphics).
 */
data class ChatStyle(
    val preset: ThemePreset,
    val background: Long,
    val headerBackground: Long,
    val headerContent: Long,
    val headerSubtle: Long,
    val incomingBubble: Long,
    val incomingText: Long,
    /** Outgoing bubbles use a gradient; a single entry means a flat colour. */
    val outgoingGradient: List<Long>,
    val outgoingText: Long,
    val bubbleBorder: Long?,
    val bubbleBorderWidthDp: Float,
    val cornerDp: Float,
    val shadow: Boolean,
    val senderName: Long,
    val narrationBackground: Long,
    val narrationText: Long,
    val timeText: Long,
    val accent: Long,
    val isDark: Boolean,
) {
    val outgoingBrush: Brush
        get() = if (outgoingGradient.size == 1) {
            Brush.linearGradient(listOf(Color(outgoingGradient[0]), Color(outgoingGradient[0])))
        } else {
            Brush.linearGradient(outgoingGradient.map(::Color))
        }

    companion object {
        val Lysn = ChatStyle(
            preset = ThemePreset.LYSN_BUBBLE,
            background = 0xFFF3F6FB,
            headerBackground = 0xFFFFFFFF,
            headerContent = 0xFF1F2A44,
            headerSubtle = 0xFF8A97AE,
            incomingBubble = 0xFFFFFFFF,
            incomingText = 0xFF1F2A44,
            outgoingGradient = listOf(0xFF7CB8EB, 0xFF9AC9F2),
            outgoingText = 0xFFFFFFFF,
            bubbleBorder = 0xFFFFFFFF,
            bubbleBorderWidthDp = 1.5f,
            cornerDp = 18f,
            shadow = true,
            senderName = 0xFF5C6B87,
            narrationBackground = 0x1F7CB8EB,
            narrationText = 0xFF5C6B87,
            timeText = 0xFF9AA5B8,
            accent = 0xFF7CB8EB,
            isDark = false,
        )

        val Weverse = ChatStyle(
            preset = ThemePreset.WEVERSE_DM,
            background = 0xFF121214,
            headerBackground = 0xFF18181C,
            headerContent = 0xFFF2F2F2,
            headerSubtle = 0xFF7DFFB8,
            incomingBubble = 0xFF1E1E24,
            incomingText = 0xFFEDEDED,
            outgoingGradient = listOf(0xFF00E676, 0xFF00BFA5),
            outgoingText = 0xFF04140C,
            bubbleBorder = 0x9900E676,
            bubbleBorderWidthDp = 1f,
            cornerDp = 14f,
            shadow = false,
            senderName = 0xFF00E676,
            narrationBackground = 0x2200E676,
            narrationText = 0xFFB9F6CA,
            timeText = 0xFF6B6B78,
            accent = 0xFF00E676,
            isDark = true,
        )

        val Instagram = ChatStyle(
            preset = ThemePreset.INSTAGRAM_DM,
            background = 0xFF000000,
            headerBackground = 0xFF000000,
            headerContent = 0xFFFFFFFF,
            headerSubtle = 0xFFA8A8A8,
            incomingBubble = 0xFF262626,
            incomingText = 0xFFFFFFFF,
            outgoingGradient = listOf(0xFF7638FA, 0xFFC837AB, 0xFFF77737),
            outgoingText = 0xFFFFFFFF,
            bubbleBorder = 0xFF363636,
            bubbleBorderWidthDp = 0.5f,
            cornerDp = 22f,
            shadow = false,
            senderName = 0xFFA8A8A8,
            narrationBackground = 0x00000000,
            narrationText = 0xFFA8A8A8,
            timeText = 0xFF737373,
            accent = 0xFFC837AB,
            isDark = true,
        )

        fun of(preset: ThemePreset): ChatStyle = when (preset) {
            ThemePreset.LYSN_BUBBLE -> Lysn
            ThemePreset.WEVERSE_DM -> Weverse
            ThemePreset.INSTAGRAM_DM -> Instagram
        }
    }
}
