package com.chy.radiofrance

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chy.radiofrance.core.designsystem.RadioFranceTheme
import com.chy.radiofrance.feature.stations.presentation.StationsScreen
import org.jetbrains.compose.resources.stringResource
import radiofrance.shared.generated.resources.Res
import radiofrance.shared.generated.resources.app_title

/**
 * The app shell, shown by MainActivity on Android and MainViewController on iOS.
 * One screen today; this is where navigation would go when a second one comes.
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in Material 3.
@Composable
fun App() {
    RadioFranceTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text(stringResource(Res.string.app_title)) }) },
            // Mini-player slot, filled in step 5. It will get the same
            // StationsViewModel instance through koinViewModel(): no state passes
            // through App.
            bottomBar = {}
        ) { innerPadding ->
            // Padding, not contentPadding: the sticky section headers must stick
            // under the top bar, not behind it.
            StationsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
