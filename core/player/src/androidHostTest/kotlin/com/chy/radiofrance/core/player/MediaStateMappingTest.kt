package com.chy.radiofrance.core.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Media3 -> PlayerState rules, on plain values. The service and the controller
 * themselves need a device: they are checked by hand.
 */
class MediaStateMappingTest {
    private val fip = Station(
        id = "FIP",
        name = "FIP",
        description = null,
        streamUrl = "https://example.com/fip.mp3",
        kind = StationKind.NATIONAL,
        brandId = "FIP",
        brandName = "FIP"
    )

    private fun state(
        station: Station? = fip,
        playbackState: Int = Player.STATE_READY,
        playWhenReady: Boolean = true,
        isPlaying: Boolean = true,
        errorCode: Int? = null
    ) = playerStateOf(station, playbackState, playWhenReady, isPlaying, errorCode)

    @Test
    fun no_station_is_idle() {
        assertEquals(PlayerState.Idle, state(station = null))
    }

    @Test
    fun an_error_wins_over_everything_else() {
        assertEquals(
            PlayerState.Error(fip, PlaybackError.Network),
            state(errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        )
    }

    @Test
    fun connection_errors_are_network_errors_and_the_rest_is_a_broken_stream() {
        assertEquals(
            PlaybackError.Network,
            playbackErrorOf(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        )
        assertEquals(
            PlaybackError.Network,
            playbackErrorOf(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
        )
        assertEquals(
            PlaybackError.StreamUnavailable,
            playbackErrorOf(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        )
        assertEquals(
            PlaybackError.StreamUnavailable,
            playbackErrorOf(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)
        )
    }

    @Test
    fun a_live_stream_that_ends_is_unavailable() {
        assertEquals(
            PlayerState.Error(fip, PlaybackError.StreamUnavailable),
            state(playbackState = Player.STATE_ENDED, isPlaying = false)
        )
    }

    @Test
    fun wanting_to_play_without_data_is_buffering() {
        val expected = PlayerState.Buffering(fip)
        assertEquals(expected, state(playbackState = Player.STATE_BUFFERING, isPlaying = false))
        assertEquals(expected, state(playbackState = Player.STATE_IDLE, isPlaying = false))
    }

    @Test
    fun ready_and_playing_is_playing() {
        assertEquals(PlayerState.Playing(fip), state())
    }

    @Test
    fun paused_by_the_user_or_by_a_lost_audio_focus_is_paused() {
        val expected = PlayerState.Paused(fip)
        // User pause, while ready or while buffering.
        assertEquals(expected, state(playWhenReady = false, isPlaying = false))
        assertEquals(
            expected,
            state(playbackState = Player.STATE_BUFFERING, playWhenReady = false, isPlaying = false)
        )
        // Transient focus loss: playWhenReady stays true, playback is suppressed.
        assertEquals(expected, state(playWhenReady = true, isPlaying = false))
    }
}
