package com.chy.radiofrance.core.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.chy.radiofrance.core.model.Station
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/**
 * The Android [AudioPlayer]: a client of [PlaybackService], which does the playing.
 *
 * It holds a Media3 [MediaController], the same kind of client as the system UI or a
 * Bluetooth device: commands go to the service's session, and the controller mirrors
 * the session's player state back, which this class maps to [PlayerState].
 *
 * - **Application context only.** This is a process-wide singleton (Koin `single`):
 *   holding an Activity would leak it.
 * - **Main thread.** A MediaController must be used on the thread it was built on.
 *   Everything here runs on [Dispatchers.Main], and so do its listener callbacks.
 * - **Asynchronous connection.** The controller binds to the service and connects in
 *   the background. Commands sent before that are queued in [commands] and run in
 *   order once connected. Connecting starts as soon as this object exists, so if the
 *   service is already playing, [state] shows it without a command.
 * - **Lost connection.** If the service goes away (killed, or stopped after a swipe
 *   from recents), the controller is dropped and the next command reconnects.
 */
internal class MediaControllerAudioPlayer(context: Context) : AudioPlayer {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private sealed interface Command {
        data class Play(val station: Station) : Command

        data object Pause : Command

        data object Stop : Command
    }

    // UNLIMITED: send never suspends or drops, so the non-suspending AudioPlayer
    // methods can enqueue from anywhere. One reader below keeps the order.
    private val commands = Channel<Command>(Channel.UNLIMITED)

    /** Null until connected, and again after a disconnection. */
    private var controller: MediaController? = null

    /**
     * The station of the last play. The controller only knows a MediaItem (id, uri,
     * title); the UI needs the whole [Station], so we keep it and match it by id.
     */
    private var currentStation: Station? = null

    // Every change of the session's player (state, playWhenReady, error, item...)
    // arrives here, batched per message-loop iteration by onEvents.
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    init {
        scope.launch {
            connect()
            for (command in commands) {
                val connected = controller ?: connect()
                if (connected == null) {
                    onConnectionFailed(command)
                } else {
                    connected.run(command)
                }
            }
        }
    }

    override fun play(station: Station) {
        // Immediate feedback: the real states follow from the listener.
        _state.value = PlayerState.Buffering(station)
        commands.trySend(Command.Play(station))
    }

    override fun pause() {
        commands.trySend(Command.Pause)
    }

    override fun stop() {
        commands.trySend(Command.Stop)
    }

    /** Disconnects from the service. Called when Koin closes (process-wide otherwise). */
    fun release() {
        scope.cancel()
        commands.close()
        controller?.release() // Unbinds from the service; the controller is unusable after.
        controller = null
    }

    private fun MediaController.run(command: Command) {
        when (command) {
            is Command.Play -> {
                currentStation = command.station
                // Replaces whatever was playing. Setting the item again is also how a
                // resume from the app reconnects to the live edge.
                setMediaItem(command.station.toMediaItem())
                // Leaves STATE_IDLE: the player opens the stream and starts buffering.
                prepare()
                // playWhenReady = true: audio starts as soon as enough is buffered.
                // In the service this goes through LivePlayer.play().
                play()
            }

            // playWhenReady = false. Keeps the item and the notification (a stopped,
            // IDLE player would hide it), so the lock screen can still resume.
            Command.Pause -> pause()

            Command.Stop -> {
                // Stops loading and frees the connection: back to STATE_IDLE.
                stop()
                // No item left: Media3 removes the notification, the service can stop.
                clearMediaItems()
                currentStation = null
            }
        }
    }

    private suspend fun connect(): MediaController? = try {
        // Identifies the session to connect to: our PlaybackService, by component name.
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        MediaController.Builder(appContext, token)
            // Called when the session goes away (service destroyed or killed).
            .setListener(
                object : MediaController.Listener {
                    override fun onDisconnected(controller: MediaController) = onDisconnected()
                }
            )
            // Binds to the service and connects in the background. Returns a Guava
            // ListenableFuture, awaited here without blocking (kotlinx-coroutines-guava).
            .buildAsync()
            .await()
            .also { connected ->
                // Mirrors the session's player: same state, same events.
                connected.addListener(playerListener)
                controller = connected
                publish(connected)
            }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Typically the service can't be bound (e.g. a SecurityException). Nothing to
        // show beyond an error on the next play; the next command tries again.
        Log.w(TAG, "Could not connect to PlaybackService", e)
        null
    }

    private fun onDisconnected() {
        controller?.removeListener(playerListener)
        controller = null
        currentStation = null
        _state.value = PlayerState.Idle
    }

    private fun onConnectionFailed(command: Command) {
        if (command is Command.Play) {
            _state.value = PlayerState.Error(command.station, PlaybackError.StreamUnavailable)
        }
    }

    private fun publish(player: Player) {
        val station = currentStation?.takeIf { it.id == player.currentMediaItem?.mediaId }
        _state.value = playerStateOf(
            station = station,
            playbackState = player.playbackState,
            playWhenReady = player.playWhenReady,
            isPlaying = player.isPlaying,
            errorCode = player.playerError?.errorCode
        )
    }

    private companion object {
        const val TAG = "AudioPlayer"
    }
}

/**
 * What the session needs to play [this] and to describe it: the notification and the
 * lock screen show the title (station name) and the artist line (brand).
 */
private fun Station.toMediaItem(): MediaItem = MediaItem.Builder()
    // Our id, read back in publish() to know which station the player is on.
    .setMediaId(id)
    // The Icecast MP3 stream, played as given (ExoPlayer detects the format).
    .setUri(streamUrl)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(name)
            .setArtist(brandName)
            .build()
    )
    .build()
