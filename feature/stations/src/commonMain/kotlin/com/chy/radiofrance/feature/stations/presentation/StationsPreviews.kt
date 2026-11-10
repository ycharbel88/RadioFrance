package com.chy.radiofrance.feature.stations.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.chy.radiofrance.core.designsystem.RadioFranceTheme
import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.player.PlaybackError
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.feature.stations.presentation.PreviewStations.fip
import com.chy.radiofrance.feature.stations.presentation.PreviewStations.inter

// One preview per screen state, plus dark mode. Dark mode is forced through the
// theme parameter rather than the preview's uiMode, so it works the same in
// every preview renderer.

@Composable
private fun ScreenPreview(state: StationsUiState, darkTheme: Boolean = false) {
    RadioFranceTheme(darkTheme = darkTheme) {
        Surface { StationsScreenContent(state = state, onEvent = {}) }
    }
}

private val loaded = StationsUiState(
    content = StationsContent.Loaded(PreviewStations.sections),
    player = PlayerState.Playing(fip)
)

@Preview
@Composable
private fun LoadingPreview() = ScreenPreview(StationsUiState(content = StationsContent.Loading))

@Preview
@Composable
private fun EmptyPreview() = ScreenPreview(StationsUiState(content = StationsContent.Empty))

@Preview
@Composable
private fun ErrorRetryablePreview() =
    ScreenPreview(StationsUiState(content = StationsContent.Error(DataError.Network)))

@Preview
@Composable
private fun ErrorNotRetryablePreview() =
    ScreenPreview(StationsUiState(content = StationsContent.Error(DataError.InvalidApiKey)))

@Preview
@Composable
private fun LoadedPreview() = ScreenPreview(loaded)

@Preview
@Composable
private fun RefreshingWithErrorPreview() =
    ScreenPreview(loaded.copy(isRefreshing = true, refreshError = DataError.Network))

@Preview
@Composable
private fun LoadedDarkPreview() = ScreenPreview(loaded, darkTheme = true)

@Preview
@Composable
private fun ErrorDarkPreview() = ScreenPreview(
    StationsUiState(content = StationsContent.Error(DataError.Network)),
    darkTheme = true
)

@Preview
@Composable
private fun StationItemStatesPreview() {
    RadioFranceTheme {
        Surface {
            Column {
                StationItem(inter, playerState = null, onClick = {})
                StationItem(fip, PlayerState.Buffering(fip), onClick = {})
                StationItem(fip, PlayerState.Playing(fip), onClick = {})
                StationItem(fip, PlayerState.Paused(fip), onClick = {})
                StationItem(fip, PlayerState.Error(fip, PlaybackError.Network), onClick = {})
            }
        }
    }
}
