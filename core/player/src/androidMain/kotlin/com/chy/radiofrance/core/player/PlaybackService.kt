package com.chy.radiofrance.core.player

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns playback on Android: the [ExoPlayer] and the [MediaSession] that exposes it.
 *
 * A MediaSessionService is a foreground service that Media3 starts and stops on
 * its own as playback starts and stops. In exchange it gives, from the session's
 * [androidx.media3.common.MediaMetadata]: the media notification, lock screen
 * controls, headset/Bluetooth buttons, and playback that outlives the Activity.
 *
 * The app never touches this class directly: [MediaControllerAudioPlayer] connects
 * to it through a MediaController, like any other client (system UI, Android Auto).
 */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    // onCreate is where Media3 docs build the player and the session: the service
    // is created once, and every client then connects to the same session.
    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this)
            // handleAudioFocus = true: ExoPlayer requests audio focus on play and
            // reacts to its loss on its own (pause for a call or another player,
            // lower the volume for a notification sound). No focus code of ours.
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // handleAudioFocus
            )
            // Pause when headphones are unplugged (ACTION_AUDIO_BECOMING_NOISY),
            // instead of suddenly playing through the speaker.
            .setHandleAudioBecomingNoisy(true)
            // Holds a CPU and a Wi-Fi lock while playing, so a stream keeps loading
            // with the screen off. Needs the WAKE_LOCK permission (manifest).
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        // Debug builds only: logs every player event (state, loading, load starts,
        // errors) under the "EventLogger" tag, to check on a device that a resume
        // opens a new connection.
        // The app's debuggable flag is the debug signal: this library module has no
        // build types of its own.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            exoPlayer.addAnalyticsListener(LoadEventLogger())
        }

        // The session gets the live wrapper, not the ExoPlayer itself: every play
        // command (app, notification, lock screen, headset) goes through it.
        mediaSession = MediaSession.Builder(this, LivePlayer(exoPlayer))
            // Tapping the notification opens the app. The launch intent comes from
            // the package manager, so this module never names MainActivity.
            .apply { launchIntent()?.let(::setSessionActivity) }
            .build()
    }

    // Clients (our MediaController, the system UI) get the session from here. Only
    // our own process and the system talk to it, so every controller is accepted.
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // The user swiped the app away from recent tasks. If the radio is playing, it
    // keeps playing (that's the point of the service). Otherwise there is nothing to
    // keep: stop the service, and with it the notification.
    // This matches Media3's default; it is written out so the rule is visible here.
    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!isPlaybackOngoing) pauseAllPlayersAndStopSelf()
    }

    // Release order from the Media3 docs: the player first (stops audio, frees the
    // codecs and the connection), then the session, then super.
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun launchIntent(): PendingIntent? {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}

/**
 * Live radio, not on-demand: resuming must play what is on air *now*.
 *
 * A plain ExoPlayer resumes where it paused: it plays its stale buffer, then reads
 * an HTTP connection the Icecast server may have dropped during the pause. So when
 * playback resumes after a pause, a stop or an error, [play] first jumps to the
 * default position (the live edge) and prepares again, which drops the buffer and
 * opens a new connection. Every resume path goes through here, because the session
 * holds this player: the app, the notification, the lock screen, headset buttons.
 */
private class LivePlayer(player: Player) : ForwardingPlayer(player) {
    override fun play() {
        if (shouldRejoinLive(playWhenReady, playbackState, currentMediaItem != null)) {
            seekToDefaultPosition()
            prepare()
        }
        super.play()
    }
}

/**
 * Whether a play command must reconnect to the live edge: there is a station, and
 * playback was interrupted (paused, stopped, ended or failed). A play while already
 * playing or starting changes nothing.
 */
internal fun shouldRejoinLive(
    playWhenReady: Boolean,
    playbackState: Int,
    hasCurrentItem: Boolean
): Boolean {
    if (!hasCurrentItem) return false
    return !playWhenReady || playbackState == Player.STATE_IDLE ||
        playbackState == Player.STATE_ENDED
}
