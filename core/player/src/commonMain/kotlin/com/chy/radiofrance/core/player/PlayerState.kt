package com.chy.radiofrance.core.player

import com.chy.radiofrance.core.model.Station

/** The player as the UI (station list, mini-player) sees it. */
sealed interface PlayerState {
    /** Nothing loaded: app start, or after [AudioPlayer.stop]. */
    data object Idle : PlayerState

    /** [station] is connecting or refilling its buffer. */
    data class Buffering(val station: Station) : PlayerState

    /** [station] is audible. */
    data class Playing(val station: Station) : PlayerState

    /** [station] is paused by the user (or by a lost audio focus). */
    data class Paused(val station: Station) : PlayerState

    /** [station] could not be played. Playing it again is the retry. */
    data class Error(val station: Station, val reason: PlaybackError) : PlayerState
}

/** The station this state is about, or null when [PlayerState.Idle]. */
val PlayerState.station: Station?
    get() = when (this) {
        PlayerState.Idle -> null
        is PlayerState.Buffering -> station
        is PlayerState.Playing -> station
        is PlayerState.Paused -> station
        is PlayerState.Error -> station
    }

/** Why playback failed. Typed so the UI owns the wording and its translations. */
enum class PlaybackError {
    /** No connection, or it dropped. Worth a retry. */
    Network,

    /** The stream answered with an error or a format the player can't read. */
    StreamUnavailable,

    /** This platform has no player yet (iOS today). Retrying won't help. */
    NotSupported
}
