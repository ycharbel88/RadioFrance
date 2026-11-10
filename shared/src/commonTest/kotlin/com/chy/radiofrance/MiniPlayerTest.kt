package com.chy.radiofrance

import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import com.chy.radiofrance.core.player.PlaybackError
import com.chy.radiofrance.core.player.PlayerState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiniPlayerTest {
    private val fip = Station(
        id = "FIP",
        name = "FIP",
        description = null,
        streamUrl = "https://example.com/fip.mp3",
        kind = StationKind.NATIONAL,
        brandId = "FIP",
        brandName = "FIP"
    )

    @Test
    fun the_button_shows_whenever_play_or_pause_can_help() {
        assertTrue(canTogglePlayback(PlayerState.Buffering(fip)))
        assertTrue(canTogglePlayback(PlayerState.Playing(fip)))
        assertTrue(canTogglePlayback(PlayerState.Paused(fip)))
        assertTrue(canTogglePlayback(PlayerState.Error(fip, PlaybackError.Network)))
        assertTrue(canTogglePlayback(PlayerState.Error(fip, PlaybackError.StreamUnavailable)))
    }

    @Test
    fun no_button_when_idle_or_when_the_platform_cannot_play() {
        assertFalse(canTogglePlayback(PlayerState.Idle))
        assertFalse(canTogglePlayback(PlayerState.Error(fip, PlaybackError.NotSupported)))
    }
}
