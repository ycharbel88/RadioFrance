package com.chy.radiofrance.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight

// "On air": a near-black, neutral canvas so the brand colours (cards, mini-player)
// carry the identity. The light scheme mirrors it on a soft grey.
// Dynamic color (Android 12 wallpaper colours) is never used: it would compete
// with the brand colours, and it doesn't exist on iOS.

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF5A6E),
    onPrimary = Color(0xFF2A0006),
    background = Color(0xFF111215),
    onBackground = Color(0xFFF2F2F5),
    surface = Color(0xFF111215),
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = Color(0xFF2A2D33),
    onSurfaceVariant = Color(0xFFA0A3AB),
    surfaceContainer = Color(0xFF1C1E22),
    surfaceContainerHigh = Color(0xFF24272C),
    outline = Color(0xFF3A3D44)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC8001A),
    onPrimary = Color.White,
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF15171A),
    surface = Color(0xFFF4F5F7),
    onSurface = Color(0xFF15171A),
    surfaceVariant = Color(0xFFE3E5EA),
    onSurfaceVariant = Color(0xFF5F636B),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFECEEF2),
    outline = Color(0xFFC9CCD3)
)

/** Material defaults, with the heavy titles of the mockup. No custom font file. */
private val RadioFranceTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

/**
 * The app theme: dark-first "On air" schemes, following the system setting.
 *
 * @param darkTheme forced by previews to show both modes; the system decides otherwise.
 */
@Composable
fun RadioFranceTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RadioFranceTypography,
        content = content
    )
}

/** Whether the current theme is the dark one (for brand accents that need lifting). */
val isDarkTheme: Boolean
    @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f
