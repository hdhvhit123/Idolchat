package com.fandom.auchatstudio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BrandViolet = Color(0xFF6C63FF)
val BrandPink = Color(0xFFFF6584)
val BrandTeal = Color(0xFF43D8C9)

private val LightColors = lightColorScheme(
    primary = BrandViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E1FF),
    onPrimaryContainer = Color(0xFF1B1464),
    secondary = BrandPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E0),
    onSecondaryContainer = Color(0xFF3F0016),
    tertiary = Color(0xFF00897B),
    tertiaryContainer = Color(0xFFC8F5EF),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B22),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B22),
    surfaceVariant = Color(0xFFE6E0EC),
    onSurfaceVariant = Color(0xFF48454F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F4FB),
    surfaceContainer = Color(0xFFF2EEF6),
    surfaceContainerHigh = Color(0xFFECE8F1),
    surfaceContainerHighest = Color(0xFFE6E2EB),
    outline = Color(0xFF79757F),
    outlineVariant = Color(0xFFCAC4D0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC5C0FF),
    onPrimary = Color(0xFF2A2185),
    primaryContainer = Color(0xFF4A42C9),
    onPrimaryContainer = Color(0xFFE4E1FF),
    secondary = Color(0xFFFFB1C1),
    onSecondary = Color(0xFF65002A),
    secondaryContainer = Color(0xFF8E1B42),
    onSecondaryContainer = Color(0xFFFFD9E0),
    tertiary = Color(0xFF7DDBCB),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFFC8F5EF),
    background = Color(0xFF14121A),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF14121A),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF48454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF0F0D14),
    surfaceContainerLow = Color(0xFF1C1A22),
    surfaceContainer = Color(0xFF211F27),
    surfaceContainerHigh = Color(0xFF2B2931),
    surfaceContainerHighest = Color(0xFF36343C),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF48454F),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

val ChatTextStyle = TextStyle(fontSize = 15.sp, lineHeight = 21.sp)

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
