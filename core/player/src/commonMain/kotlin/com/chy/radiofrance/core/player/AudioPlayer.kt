package com.chy.radiofrance.core.player

import com.chy.radiofrance.core.model.Station
import kotlinx.coroutines.flow.StateFlow

/**
 * Plays one live station at a time. The UI only ever sees this interface; each
 * platform binds its own implementation in Koin.
 *
 * Commands are fire-and-forget (not `suspend`): platform players are asynchronous
 * and report what actually happens through [state].
 */
interface AudioPlayer {
    /** What the player is doing now. Starts as [PlayerState.Idle]. */
    val state: StateFlow<PlayerState>

    /**
     * Starts [station]. Also how to resume after a pause: for a live stream,
     * resuming means reconnecting to the live edge, so there is no `resume()`.
     */
    fun play(station: Station)

    /** Pauses the current station. Does nothing when nothing is playing. */
    fun pause()

    /** Stops and forgets the current station: back to [PlayerState.Idle]. */
    fun stop()
}
