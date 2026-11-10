package com.chy.radiofrance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chy.radiofrance.core.designsystem.Equalizer
import com.chy.radiofrance.core.designsystem.RadioFranceIcons
import com.chy.radiofrance.core.designsystem.StationMonogram
import com.chy.radiofrance.core.designsystem.miniPlayerGradient
import com.chy.radiofrance.core.designsystem.onBrandColor
import com.chy.radiofrance.core.designsystem.stationBrand
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

/**
 * Stateless mini-player: a floating card in the current station's brand colour.
 * Slides in when a station is loaded and out when the player goes back to Idle;
 * its colour cross-fades when the station changes.
 */
@Composable
internal fun MiniPlayerContent(
    player: PlayerState,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The exit animation still needs something to draw once the player is Idle:
    // keep the last state that had a station. A plain cache, not observable state.
    val last = remember { LastShown() }
    if (player.station != null) last.state = player

    AnimatedVisibility(
        visible = player.station != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        last.state?.let { MiniPlayerCard(it, onTogglePlayPause) }
    }
}

private class LastShown {
    var state: PlayerState? = null
}

@Composable
private fun MiniPlayerCard(player: PlayerState, onTogglePlayPause: () -> Unit) {
    val station = player.station ?: return
    val brand = stationBrand(station.brandId, station.brandName)
    val stops = miniPlayerGradient(brand.color)
    // Animated, so switching from FIP to franceinfo blends pink into yellow.
    val start by animateColorAsState(stops.first(), label = "start")
    val end by animateColorAsState(stops.last(), label = "end")
    val content by animateColorAsState(onBrandColor(stops.first()), label = "content")
    val shape = RoundedCornerShape(24.dp)

    // Floats above the navigation bar (edge-to-edge) and the side cutouts, with the
    // same 600 dp maximum width as the list on wide windows.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .shadow(8.dp, shape)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(start, end)))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                // One node for TalkBack: "FIP Jazz, En lecture". The button is separate.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) { }
                ) {
                    StationMonogram(brand, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        StatusLine(player, content)
                    }
                }
                if (canTogglePlayback(player)) {
                    Spacer(Modifier.width(8.dp))
                    PlayPauseButton(
                        player,
                        background = content,
                        icon = start,
                        onClick = onTogglePlayPause
                    )
                }
            }
            if (player is PlayerState.Buffering) {
                LinearProgressIndicator(
                    color = content,
                    trackColor = content.copy(alpha = 0.2f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }
    }
}

// "En lecture" with a small equalizer, "Chargement", "En pause", or why it failed.
// The error stays in the content colour: the error red can't be trusted on every
// brand colour, the words say it.
@Composable
private fun StatusLine(player: PlayerState, content: Color) {
    val text = when (player) {
        is PlayerState.Error -> stringResource(playbackErrorResource(player.reason))
        else -> playerStateResource(player)?.let { stringResource(it) } ?: return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (player is PlayerState.Playing) Equalizer(color = content, size = 14.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = content.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// A large round button: pause while playing or connecting, play otherwise (after
// an error, play is the retry). Filled with the text colour, icon in the brand colour.
@Composable
private fun PlayPauseButton(
    player: PlayerState,
    background: Color,
    icon: Color,
    onClick: () -> Unit
) {
    val showsPause = player is PlayerState.Playing || player is PlayerState.Buffering
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = background,
            contentColor = icon
        ),
        modifier = Modifier.size(56.dp)
    ) {
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
