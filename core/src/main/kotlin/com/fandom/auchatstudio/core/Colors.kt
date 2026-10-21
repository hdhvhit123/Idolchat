package com.fandom.auchatstudio.core

/** Colours are stored as ARGB Longs (e.g. 0xFFFF6584) exactly as the design document does. */
object RolePalette {
    val colors: List<Long> = listOf(
        0xFFFF6584, // hồng
        0xFF6C63FF, // tím
        0xFF43D8C9, // ngọc
        0xFFFFB347, // cam
        0xFF7CB8EB, // xanh pastel
        0xFF00C853, // xanh lá
        0xFFE040FB, // tím hồng
        0xFFFF5252, // đỏ
        0xFF8D6E63, // nâu
        0xFF546E7A, // xám xanh
    )

    /** First palette colour not already used, so new characters are easy to tell apart. */
    fun nextFree(used: Collection<Long>): Long =
        colors.firstOrNull { it !in used } ?: colors[used.size % colors.size]
}

object ColorMath {
    /** WCAG relative luminance of an ARGB colour, in 0..1. */
    fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF).toDouble() / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    /**
     * True when dark text should be used on [argb]. White is favoured for mid-tones because it
     * looks cleaner on the saturated role colours, so the switch happens above luminance 0.4.
     */
    fun prefersDarkText(argb: Long): Boolean = luminance(argb) > 0.4
}
