package com.chy.radiofrance.core.player

import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import kotlin.test.Test
import kotlin.test.assertEquals

class UnavailableAudioPlayerTest {
    private fun station(id: String) = Station(
        id = id,
        name = id,
        description = null,
        streamUrl = "https://icecast.radiofrance.fr/$id.mp3",
        kind = StationKind.NATIONAL,
        brandId = id,
        brandName = id
    )

    private val fip = station("FIP")
    private val inter = station("FRANCEINTER")

    @Test
    fun `starts idle`() {
        assertEquals(PlayerState.Idle, UnavailableAudioPlayer().state.value)
    }

    @Test
    fun `play ends in a not supported error for that station`() {
        val player = UnavailableAudioPlayer()

        player.play(fip)

        assertEquals(PlayerState.Error(fip, PlaybackError.NotSupported), player.state.value)
    }

    @Test
    fun `pause keeps the error visible`() {
        val player = UnavailableAudioPlayer()
        player.play(fip)

        player.pause()

        assertEquals(PlayerState.Error(fip, PlaybackError.NotSupported), player.state.value)
    }

    @Test
    fun `pause when idle stays idle`() {
        val player = UnavailableAudioPlayer()

        player.pause()

        assertEquals(PlayerState.Idle, player.state.value)
    }

    @Test
    fun `stop goes back to idle`() {
        val player = UnavailableAudioPlayer()
        player.play(fip)

        player.stop()

        assertEquals(PlayerState.Idle, player.state.value)
    }

    @Test
    fun `playing another station replaces the station`() {
        val player = UnavailableAudioPlayer()
        player.play(fip)

        player.play(inter)

        assertEquals(PlayerState.Error(inter, PlaybackError.NotSupported), player.state.value)
    }
}
