package com.chy.radiofrance.core.player

import com.chy.radiofrance.core.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The player of a platform that can't play yet (iOS today). It says so honestly:
 * [play] ends in [PlaybackError.NotSupported], and no state ever claims audio
 * is playing. The rest of the app (list, mini-player) works unchanged.
 */
internal class UnavailableAudioPlayer : AudioPlayer {
    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    override fun play(station: Station) {
        _state.value = PlayerState.Error(station, PlaybackError.NotSupported)
    }

    // Nothing is ever playing, so there is nothing to pause. Keeping the Error
    // state leaves the "not supported" message visible.
    override fun pause() = Unit

    override fun stop() {
        _state.value = PlayerState.Idle
    }
}
