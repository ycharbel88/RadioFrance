package com.chy.radiofrance.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The app theme. Minimal on purpose for now: stock Material 3 light and dark
 * schemes, following the system setting. Brand colours and typography come with
 * the look & feel step (6).
 *
 * @param darkTheme forced by previews to show both modes; the system decides otherwise.
 */
@Composable
fun RadioFranceTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content
    )
}
