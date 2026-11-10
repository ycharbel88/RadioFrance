package com.chy.radiofrance.core.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.chy.radiofrance.core.model.Station

// Media3 player snapshot -> PlayerState. Pure functions on plain values (the
// Player.STATE_* and error codes are int constants), so they run in JVM unit tests
// without a device.

/**
 * @param station the station being played, or null when the player has no item.
 * @param playbackState one of `Player.STATE_*`.
 * @param errorCode `PlaybackException.errorCode` of the current error, or null.
 */
internal fun playerStateOf(
    station: Station?,
    playbackState: Int,
    playWhenReady: Boolean,
    isPlaying: Boolean,
    errorCode: Int?
): PlayerState = when {
    station == null -> PlayerState.Idle

    errorCode != null -> PlayerState.Error(station, playbackErrorOf(errorCode))

    // A live stream never ends on its own: the server closed it.
    playbackState == Player.STATE_ENDED ->
        PlayerState.Error(station, PlaybackError.StreamUnavailable)

    // Wants to play but has nothing to play yet: connecting, or refilling.
    playWhenReady && playbackState in setOf(Player.STATE_BUFFERING, Player.STATE_IDLE) ->
        PlayerState.Buffering(station)

    playbackState == Player.STATE_READY && isPlaying -> PlayerState.Playing(station)

    // Paused by the user, by a lost audio focus (playWhenReady stays true but
    // playback is suppressed), or by unplugged headphones.
    else -> PlayerState.Paused(station)
}

/** Connection problems are worth a retry; anything else means this stream is broken. */
internal fun playbackErrorOf(errorCode: Int): PlaybackError = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> PlaybackError.Network

    else -> PlaybackError.StreamUnavailable
}
