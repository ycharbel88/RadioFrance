package com.chy.radiofrance

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chy.radiofrance.core.designsystem.RadioFranceTheme
import com.chy.radiofrance.feature.stations.presentation.StationsScreen

/**
 * The app shell, shown by MainActivity on Android and MainViewController on iOS.
 * One screen today; this is where navigation would go when a second one comes.
 *
 * The screen draws its own top bar (it knows the station count). The shell only
 * adds the mini-player, shared by every future screen.
 */
@Composable
fun App() {
    RadioFranceTheme {
        Scaffold(
            // The top bar handles the status bar, the list the side cutouts. Only the
            // bottom (navigation bar) is left: used when the mini-player is hidden.
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
            // Gets the same StationsViewModel instance through koinViewModel():
            // no state passes through App. Collapses when nothing is playing.
            bottomBar = { MiniPlayer() }
        ) { innerPadding ->
            StationsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
