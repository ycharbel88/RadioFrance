package com.chy.radiofrance.core.testing

import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.player.AudioPlayer
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.core.player.station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An [AudioPlayer] for tests. It records every command in [calls] and reacts
 * like an ideal player: [play] goes straight to Playing, [pause] to Paused,
 * [stop] to Idle. Use [emit] to simulate what a real player reports on its own
 * (buffering, errors).
 */
class FakeAudioPlayer : AudioPlayer {
    sealed interface Call {
        data class Play(val station: Station) : Call

        data object Pause : Call

        data object Stop : Call
    }

    private val _calls = mutableListOf<Call>()

    /** Every command received, in order. */
    val calls: List<Call> get() = _calls.toList()

    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    override fun play(station: Station) {
        _calls += Call.Play(station)
        _state.value = PlayerState.Playing(station)
    }

    override fun pause() {
        _calls += Call.Pause
        _state.value.station?.let { _state.value = PlayerState.Paused(it) }
    }

    override fun stop() {
        _calls += Call.Stop
        _state.value = PlayerState.Idle
    }

    /** Sets the state directly, as the platform player would. Not recorded in [calls]. */
    fun emit(state: PlayerState) {
        _state.value = state
    }
}
