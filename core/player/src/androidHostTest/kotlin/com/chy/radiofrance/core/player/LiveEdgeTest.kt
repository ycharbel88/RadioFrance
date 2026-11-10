package com.chy.radiofrance.core.player

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** When a resume rejoins live, and how the lag behind live adds up. Times in ms. */
class LiveEdgeTest {
    private val threshold = LIVE_REJOIN_THRESHOLD.inWholeMilliseconds

    /** A paused player (READY, not playWhenReady) with a station. */
    private fun paused(behindLiveMs: Long = 0, currentPauseMs: Long) = shouldRejoinLive(
        playWhenReady = false,
        playbackState = Player.STATE_READY,
        hasCurrentItem = true,
        behindLiveMs = behindLiveMs,
        currentPauseMs = currentPauseMs
    )

    @Test
    fun the_threshold_is_one_minute() {
        assertEquals(60_000L, threshold)
    }

    @Test
    fun a_pause_rejoins_live_only_when_strictly_longer_than_the_threshold() {
        assertFalse(paused(currentPauseMs = 0))
        assertFalse(paused(currentPauseMs = threshold - 1))
        assertFalse(paused(currentPauseMs = threshold))
        assertTrue(paused(currentPauseMs = threshold + 1))
    }

    @Test
    fun the_lag_from_earlier_pauses_counts_toward_the_threshold() {
        assertFalse(paused(behindLiveMs = 40_000, currentPauseMs = 20_000))
        assertTrue(paused(behindLiveMs = 40_000, currentPauseMs = 20_001))
    }

    @Test
    fun stopped_failed_or_ended_always_rejoins_live_even_after_a_short_pause() {
        for (state in listOf(Player.STATE_IDLE, Player.STATE_ENDED)) {
            assertTrue(
                shouldRejoinLive(false, state, true, behindLiveMs = 0, currentPauseMs = 1_000)
            )
            assertTrue(shouldRejoinLive(true, state, true, behindLiveMs = 0, currentPauseMs = 0))
        }
    }

    @Test
    fun a_paused_player_still_buffering_follows_the_threshold() {
        assertFalse(
            shouldRejoinLive(false, Player.STATE_BUFFERING, true, 0, currentPauseMs = 1_000)
        )
    }

    @Test
    fun a_play_while_playing_or_without_a_station_never_reconnects() {
        assertFalse(
            shouldRejoinLive(true, Player.STATE_READY, true, behindLiveMs = threshold * 10, 0)
        )
        assertFalse(
            shouldRejoinLive(false, Player.STATE_IDLE, false, behindLiveMs = threshold * 10, 0)
        )
    }

    // LiveLag: the accumulation, with a fake clock.

    private val lag = LiveLag()

    private fun pauseAt(nowMs: Long) = lag.onPaused(nowMs)

    private fun resumeAt(nowMs: Long) = lag.onPlay(nowMs, false, Player.STATE_READY, true)

    @Test
    fun three_30_second_pauses_make_the_third_resume_rejoin_live() {
        pauseAt(0)
        assertFalse(resumeAt(30_000), "1st resume: 30 s behind")
        pauseAt(40_000)
        assertFalse(resumeAt(70_000), "2nd resume: 60 s behind, not longer than the threshold")
        pauseAt(80_000)
        assertTrue(resumeAt(110_000), "3rd resume: 90 s behind")
    }

    @Test
    fun rejoining_live_resets_the_lag() {
        pauseAt(0)
        assertTrue(resumeAt(61_000))
        pauseAt(70_000)
        assertFalse(resumeAt(100_000), "only 30 s behind since the rejoin")
    }

    @Test
    fun a_new_station_resets_the_lag_and_the_current_pause() {
        pauseAt(0)
        assertFalse(resumeAt(50_000))
        pauseAt(60_000)
        lag.onNewStation()
        assertFalse(resumeAt(100_000), "the new station starts on live")
        pauseAt(110_000)
        assertFalse(resumeAt(150_000), "40 s behind the new station only")
    }

    @Test
    fun only_the_first_pause_signal_starts_the_pause() {
        pauseAt(0)
        pauseAt(50_000) // A second "paused" event (e.g. headphones, then user) changes nothing.
        assertTrue(resumeAt(61_000))
    }

    @Test
    fun a_play_without_a_pause_adds_no_lag() {
        assertFalse(
            lag.onPlay(10_000, playWhenReady = true, Player.STATE_READY, hasCurrentItem = true)
        )
        pauseAt(20_000)
        assertFalse(resumeAt(80_000), "exactly 60 s behind")
    }

    @Test
    fun an_error_rejoins_live_and_resets_the_lag() {
        pauseAt(0)
        assertFalse(resumeAt(50_000))
        assertTrue(lag.onPlay(55_000, false, Player.STATE_IDLE, true), "failed: always reconnect")
        pauseAt(60_000)
        assertFalse(resumeAt(110_000), "50 s behind since the reconnection")
    }
}
