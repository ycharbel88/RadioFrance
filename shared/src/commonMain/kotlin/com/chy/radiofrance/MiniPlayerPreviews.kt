package com.chy.radiofrance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.chy.radiofrance.core.designsystem.RadioFranceTheme
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import com.chy.radiofrance.core.player.PlaybackError
import com.chy.radiofrance.core.player.PlayerState

// Every mini-player state, light and dark (dark forced through the theme), on two
// brands: white text on FIP pink, black text on franceinfo yellow.

private fun station(id: String, name: String, brandId: String, brandName: String) = Station(
    id = id,
    name = name,
    description = null,
    streamUrl = "https://example.com/$id.mp3",
    kind = StationKind.WEBRADIO,
    brandId = brandId,
    brandName = brandName
)

private val fipJazz = station("FIP_JAZZ", "FIP Jazz", "FIP", "FIP")
private val franceinfo = station("FRANCEINFO", "franceinfo", "FRANCEINFO", "franceinfo")

private val allStates = listOf(
    PlayerState.Playing(fipJazz),
    PlayerState.Buffering(franceinfo),
    PlayerState.Paused(fipJazz),
    PlayerState.Error(franceinfo, PlaybackError.Network),
    PlayerState.Error(fipJazz, PlaybackError.NotSupported)
)

@Composable
private fun AllStates(darkTheme: Boolean) {
    RadioFranceTheme(darkTheme = darkTheme) {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                allStates.forEach { MiniPlayerContent(player = it, onTogglePlayPause = {}) }
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

@Preview(widthDp = 360)
@Composable
private fun MiniPlayerLargeFontPreview() {
    RadioFranceTheme(darkTheme = true) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
            MiniPlayerContent(player = PlayerState.Playing(fipJazz), onTogglePlayPause = {})
        }
    }
}
