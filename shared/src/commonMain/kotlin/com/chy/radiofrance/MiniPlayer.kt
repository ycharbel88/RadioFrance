package com.chy.radiofrance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chy.radiofrance.core.designsystem.RadioFranceIcons
import com.chy.radiofrance.core.player.PlaybackError
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.core.player.station
import com.chy.radiofrance.feature.stations.presentation.StationsEvent
import com.chy.radiofrance.feature.stations.presentation.StationsViewModel
import com.chy.radiofrance.feature.stations.presentation.playbackErrorResource
import com.chy.radiofrance.feature.stations.presentation.playerStateResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import radiofrance.shared.generated.resources.Res
import radiofrance.shared.generated.resources.action_pause
import radiofrance.shared.generated.resources.action_play

/**
 * The bar at the bottom of the app: the current station, what the player is doing,
 * and play/pause. Stateful wrapper: it reads the player state from the same
 * [StationsViewModel] instance as the list (same ViewModelStoreOwner, same key), so
 * nothing passes through App.
 */
@Composable
fun MiniPlayer(modifier: Modifier = Modifier, viewModel: StationsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MiniPlayerContent(
        player = state.player,
        onTogglePlayPause = { viewModel.onEvent(StationsEvent.TogglePlayPause) },
        modifier = modifier
    )
}

/** Stateless mini-player. Draws nothing when [player] is Idle, so the bar collapses. */
@Composable
internal fun MiniPlayerContent(
    player: PlayerState,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val station = player.station ?: return
    Surface(tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) {
        // Edge-to-edge: keep the content above the system navigation bar.
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            if (player is PlayerState.Buffering) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusLine(player)
                }
                if (canTogglePlayback(player)) PlayPauseButton(player, onTogglePlayPause)
            }
        }
    }
}

@Composable
private fun StatusLine(player: PlayerState) {
    val text = when (player) {
        is PlayerState.Error -> stringResource(playbackErrorResource(player.reason))
        else -> playerStateResource(player)?.let { stringResource(it) } ?: return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (player is PlayerState.Error) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

// Pause while playing or connecting; play otherwise (after an error, play is the retry).
@Composable
private fun PlayPauseButton(player: PlayerState, onClick: () -> Unit) {
    val showsPause = player is PlayerState.Playing || player is PlayerState.Buffering
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(
                if (showsPause) RadioFranceIcons.Pause else RadioFranceIcons.Play
            ),
            contentDescription = stringResource(
                if (showsPause) Res.string.action_pause else Res.string.action_play
            )
        )
    }
}

/**
 * Whether the play/pause button makes sense. Not when idle (no station), and not
 * when the platform can't play at all: retrying a NotSupported error can't help,
 * the same rule as the list's Retry button.
 */
internal fun canTogglePlayback(player: PlayerState): Boolean = when (player) {
    PlayerState.Idle -> false
    is PlayerState.Error -> player.reason != PlaybackError.NotSupported
    else -> true
}
