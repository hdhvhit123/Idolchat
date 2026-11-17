package com.fandom.auchatstudio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fandom.auchatstudio.R

/*
 * "Lightstick Pop": the look of a concert light stick in a dark hall. A violet-to-pink glow is
 * the one loud element (primary actions, the "continue" hero); everything else stays calm so the
 * three chat skins remain the stars. Rounded geometric type, pill shapes, soft tinted surfaces.
 */

val BrandViolet = Color(0xFF6C4DFF)
val BrandPink = Color(0xFFFF5C9A)
val BrandMint = Color(0xFF2ED3B7)

/** Gradient colours are dark enough at both ends for white text (checked in ThemeContrastTest). */
val BrandGradientColors = listOf(Color(0xFF6C4DFF), Color(0xFFC2337F))
val BrandGradient: Brush get() = Brush.linearGradient(BrandGradientColors)

private val LightColors = lightColorScheme(
    primary = BrandViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E0FF),
    onPrimaryContainer = Color(0xFF1F0F66),
    secondary = Color(0xFFC2185B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E6),
    onSecondaryContainer = Color(0xFF3E001D),
    tertiary = Color(0xFF00796B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC4F5EC),
    onTertiaryContainer = Color(0xFF00201B),
    background = Color(0xFFFCF9FF),
    onBackground = Color(0xFF1B1A24),
    surface = Color(0xFFFCF9FF),
    onSurface = Color(0xFF1B1A24),
    surfaceVariant = Color(0xFFE7E1F2),
    onSurfaceVariant = Color(0xFF494557),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F2FC),
    surfaceContainer = Color(0xFFF0ECF8),
    surfaceContainerHigh = Color(0xFFEAE6F3),
    surfaceContainerHighest = Color(0xFFE4E0EE),
    outline = Color(0xFF7A7589),
    outlineVariant = Color(0xFFCBC5D8),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC9BFFF),
    onPrimary = Color(0xFF2B1A8A),
    primaryContainer = Color(0xFF4A35C7),
    onPrimaryContainer = Color(0xFFE7E0FF),
    secondary = Color(0xFFFFB0CC),
    onSecondary = Color(0xFF5E0031),
    secondaryContainer = Color(0xFF85134A),
    onSecondaryContainer = Color(0xFFFFD9E6),
    tertiary = Color(0xFF7FDCCB),
    onTertiary = Color(0xFF003730),
    tertiaryContainer = Color(0xFF005046),
    onTertiaryContainer = Color(0xFFC4F5EC),
    background = Color(0xFF121019),
    onBackground = Color(0xFFE7E1EE),
    surface = Color(0xFF121019),
    onSurface = Color(0xFFE7E1EE),
    surfaceVariant = Color(0xFF494557),
    onSurfaceVariant = Color(0xFFCBC5D8),
    surfaceContainerLowest = Color(0xFF0D0B13),
    surfaceContainerLow = Color(0xFF1A1823),
    surfaceContainer = Color(0xFF1F1D29),
    surfaceContainerHigh = Color(0xFF2A2734),
    surfaceContainerHighest = Color(0xFF35323F),
    outline = Color(0xFF958FA3),
    outlineVariant = Color(0xFF494557),
)

/** Be Vietnam Pro (SIL Open Font License): designed for Vietnamese, so stacked diacritics sit well. */
val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
)

private fun TextStyle.app(weight: FontWeight? = null, tighten: Boolean = false) = copy(
    fontFamily = BeVietnamPro,
    fontWeight = weight ?: fontWeight,
    letterSpacing = if (tighten) (-0.01).em else letterSpacing,
)

private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.app(FontWeight.Bold, tighten = true),
        displayMedium = displayMedium.app(FontWeight.Bold, tighten = true),
        displaySmall = displaySmall.app(FontWeight.Bold, tighten = true),
        headlineLarge = headlineLarge.app(FontWeight.Bold, tighten = true),
        headlineMedium = headlineMedium.app(FontWeight.Bold, tighten = true),
        headlineSmall = headlineSmall.app(FontWeight.Bold, tighten = true),
        titleLarge = titleLarge.app(FontWeight.Bold),
        titleMedium = titleMedium.app(FontWeight.SemiBold),
        titleSmall = titleSmall.app(FontWeight.SemiBold),
        bodyLarge = bodyLarge.app(),
        bodyMedium = bodyMedium.app(),
        bodySmall = bodySmall.app(),
        labelLarge = labelLarge.app(FontWeight.SemiBold),
        labelMedium = labelMedium.app(FontWeight.Medium),
        labelSmall = labelSmall.app(FontWeight.Medium),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Spacing scale used across screens so gaps stay consistent. */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val screen = 20.dp
}

/**
 * Chat bubbles deliberately use the system font: they imitate other apps, and the long-image
 * exporter draws with the same typeface, so what you see is what gets exported.
 */
val ChatTextStyle = TextStyle(fontFamily = FontFamily.Default, fontSize = 15.sp, lineHeight = 21.sp)

@Composable
fun AUChatStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
