package com.chy.radiofrance.core.player

import androidx.media3.common.Player
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * How far behind the live broadcast the listener may drift before a resume
 * reconnects to it.
 *
 * The trade-off: every new connection to the Radio France stream starts with a
 * pre-roll ad (and a few seconds of buffering). Resuming the buffer instead costs
 * no ad, but leaves the listener as far behind live as the time spent paused.
 * Under a minute, being slightly late beats hearing an ad on every pause; beyond
 * that, being on air wins (and the server has likely dropped the idle connection).
 */
internal val LIVE_REJOIN_THRESHOLD: Duration = 60.seconds

/**
 * Whether a play command must reconnect to the live edge.
 *
 * - No station: nothing to reconnect.
 * - Stopped or failed (`STATE_IDLE`, an error leaves the player there) or ended:
 *   there is no buffer to resume, always reconnect.
 * - Already playing or starting: nothing to do.
 * - Paused: reconnect only if the total lag, [behindLiveMs] from earlier pauses plus
 *   [currentPauseMs], is longer than [LIVE_REJOIN_THRESHOLD].
 */
internal fun shouldRejoinLive(
    playWhenReady: Boolean,
    playbackState: Int,
    hasCurrentItem: Boolean,
    behindLiveMs: Long,
    currentPauseMs: Long
): Boolean = when {
    !hasCurrentItem -> false
    playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED -> true
    playWhenReady -> false
    else -> behindLiveMs + currentPauseMs > LIVE_REJOIN_THRESHOLD.inWholeMilliseconds
}

/**
 * Tracks how far behind live the player is: the sum of the pauses resumed from the
 * buffer since the last (re)connection. Pure (times are passed in, in ms), so the
 * accumulation is unit-tested with a fake clock. Used by the service's LivePlayer.
 */
internal class LiveLag {
    private var behindLiveMs = 0L

    /** When the current pause started, or null when not paused. */
    private var pausedAtMs: Long? = null

    /** The player stopped wanting to play (user, lost audio focus, unplugged headphones). */
    fun onPaused(nowMs: Long) {
        if (pausedAtMs == null) pausedAtMs = nowMs
    }

    /** A new station was loaded: it starts on live. */
    fun onNewStation() {
        behindLiveMs = 0
        pausedAtMs = null
    }

    /**
     * A play command arrived. Returns whether to rejoin live, and books the pause:
     * reset to 0 on a rejoin, added to the lag when resuming the buffer.
     */
    fun onPlay(
        nowMs: Long,
        playWhenReady: Boolean,
        playbackState: Int,
        hasCurrentItem: Boolean
    ): Boolean {
        val currentPauseMs = pausedAtMs?.let { nowMs - it } ?: 0L
        val rejoin =
            shouldRejoinLive(
                playWhenReady,
                playbackState,
                hasCurrentItem,
                behindLiveMs,
                currentPauseMs
            )
        behindLiveMs = if (rejoin) 0L else behindLiveMs + currentPauseMs
        pausedAtMs = null
        return rejoin
    }
}
