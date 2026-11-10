package com.chy.radiofrance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chy.radiofrance.core.designsystem.RadioFranceTheme
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import com.chy.radiofrance.core.player.PlaybackError
import com.chy.radiofrance.core.player.PlayerState

// Every mini-player state, light and dark (dark forced through the theme).

private val fip = Station(
    id = "FIP",
    name = "FIP",
    description = "Éclectique",
    streamUrl = "https://example.com/fip.mp3",
    kind = StationKind.NATIONAL,
    brandId = "FIP",
    brandName = "FIP"
)

@Composable
private fun AllStates(darkTheme: Boolean) {
    RadioFranceTheme(darkTheme = darkTheme) {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PlayerState.Buffering(fip),
                    PlayerState.Playing(fip),
                    PlayerState.Paused(fip),
                    PlayerState.Error(fip, PlaybackError.Network),
                    PlayerState.Error(fip, PlaybackError.NotSupported)
                ).forEach { MiniPlayerContent(player = it, onTogglePlayPause = {}) }
            }
        }
    }
}

@Preview
@Composable
private fun MiniPlayerLightPreview() = AllStates(darkTheme = false)

@Preview
@Composable
private fun MiniPlayerDarkPreview() = AllStates(darkTheme = true)
