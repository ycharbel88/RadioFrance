package com.chy.radiofrance.feature.stations.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.chy.radiofrance.core.designsystem.RadioFranceTheme
import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.player.PlaybackError
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.feature.stations.presentation.PreviewStations.fip
import com.chy.radiofrance.feature.stations.presentation.PreviewStations.fipJazz
import com.chy.radiofrance.feature.stations.presentation.PreviewStations.franceinfo
import com.chy.radiofrance.feature.stations.presentation.PreviewStations.iciAlsace

// Every screen state, every player state, both themes. Dark mode and font scale
// are forced through the theme and LocalDensity rather than the preview's uiMode
// and fontScale, so they work the same in every preview renderer.

@Composable
private fun Themed(darkTheme: Boolean, content: @Composable () -> Unit) {
    RadioFranceTheme(darkTheme = darkTheme) { Surface(content = content) }
}

@Composable
private fun ScreenPreview(
    state: StationsUiState,
    darkTheme: Boolean = false,
    expandedLocals: Set<String> = emptySet()
) = Themed(darkTheme) {
    StationsScreenContent(state = state, onEvent = {}, expandedLocals = expandedLocals)
}

private fun loaded(player: PlayerState) =
    StationsUiState(content = StationsContent.Loaded(PreviewStations.sections), player = player)

/** Every state a current station can be in, plus "not the current station". */
private fun allStates(station: Station): List<PlayerState?> = listOf(
    null,
    PlayerState.Buffering(station),
    PlayerState.Playing(station),
    PlayerState.Paused(station),
    PlayerState.Error(station, PlaybackError.Network)
)

// Screens, as in the mockup: dark with FIP Jazz playing, light with franceinfo loading.

@Preview(heightDp = 900)
@Composable
private fun DarkFipJazzPlayingPreview() =
    ScreenPreview(loaded(PlayerState.Playing(fipJazz)), darkTheme = true)

@Preview(heightDp = 900)
@Composable
private fun LightFranceinfoBufferingPreview() =
    ScreenPreview(loaded(PlayerState.Buffering(franceinfo)))

@Preview(heightDp = 1100)
@Composable
private fun LocalsExpandedPreview() = ScreenPreview(
    loaded(PlayerState.Playing(iciAlsace)),
    darkTheme = true,
    expandedLocals = setOf("FRANCEBLEU")
)

@Preview
@Composable
private fun RefreshFailedPreview() = ScreenPreview(
    loaded(PlayerState.Idle).copy(isRefreshing = true, refreshError = DataError.Network)
)

@Preview
@Composable
private fun LoadingPreview() = ScreenPreview(StationsUiState(content = StationsContent.Loading))

@Preview
@Composable
private fun EmptyPreview() = ScreenPreview(StationsUiState(content = StationsContent.Empty))

@Preview
@Composable
private fun ErrorRetryablePreview() = ScreenPreview(
    StationsUiState(content = StationsContent.Error(DataError.Network)),
    darkTheme = true
)

@Preview
@Composable
private fun ErrorNotRetryablePreview() =
    ScreenPreview(StationsUiState(content = StationsContent.Error(DataError.InvalidApiKey)))

// Cards and rows in every player state.

@Composable
private fun CardStates(darkTheme: Boolean) = Themed(darkTheme) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        allStates(fip).forEach { StationCard(fip, it, onClick = {}) }
        StationCard(franceinfo, PlayerState.Playing(franceinfo), onClick = {})
    }
}

@Preview
@Composable
private fun CardStatesLightPreview() = CardStates(darkTheme = false)

@Preview
@Composable
private fun CardStatesDarkPreview() = CardStates(darkTheme = true)

@Composable
private fun RowStates(darkTheme: Boolean) = Themed(darkTheme) {
    Column { allStates(fipJazz).forEach { StationItem(fipJazz, it, onClick = {}) } }
}

@Preview
@Composable
private fun RowStatesLightPreview() = RowStates(darkTheme = false)

@Preview
@Composable
private fun RowStatesDarkPreview() = RowStates(darkTheme = true)

// The locals toggle, collapsed and expanded.

@Preview
@Composable
private fun LocalsTogglePreview() = Themed(darkTheme = false) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LocalsToggle(
            ListRow.LocalsToggle(
                "FRANCEBLEU",
                "ICI",
                count = 45,
                expanded = false
            ),
            onToggle = {
            }
        )
        LocalsToggle(
            ListRow.LocalsToggle(
                "FRANCEBLEU",
                "ICI",
                count = 45,
                expanded = true
            ),
            onToggle = {
            }
        )
    }
}

// 200 % font scale (the Android maximum): the card grows, nothing is clipped, the
// monogram keeps its size.

@Preview(widthDp = 360)
@Composable
private fun LargeFontPreview() = Themed(darkTheme = true) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        Column(
            Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StationItem(fipJazz, PlayerState.Playing(fipJazz), onClick = {})
            StationCard(fip, PlayerState.Paused(fip), onClick = {
            }, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}
