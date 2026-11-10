package com.chy.radiofrance.feature.stations.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.feature.stations.resources.Res
import com.chy.radiofrance.feature.stations.resources.ic_equalizer
import com.chy.radiofrance.feature.stations.resources.ic_error
import com.chy.radiofrance.feature.stations.resources.ic_pause
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One station row. Stateless: it draws [station] and reports taps.
 *
 * @param playerState the player state when this row is the current station,
 *   null otherwise. Drives the highlight and the trailing indicator.
 */
@Composable
internal fun StationItem(
    station: Station,
    playerState: PlayerState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The visual indicator has no content description: the row announces its
    // state once, through stateDescription, instead of the icon repeating it.
    val stateText = playerState?.let(::playerStateResource)?.let { stringResource(it) }
    val description = station.description
    ListItem(
        headlineContent = { Text(station.name) },
        supportingContent = description?.let {
            { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        },
        trailingContent = playerState?.let { { PlaybackIndicator(it) } },
        colors = if (playerState != null) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.colors()
        },
        // ListItem is at least 56dp high: above the 48dp touch target minimum.
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { if (stateText != null) stateDescription = stateText }
    )
}

@Composable
private fun PlaybackIndicator(state: PlayerState) {
    when (state) {
        PlayerState.Idle -> Unit

        is PlayerState.Buffering ->
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)

        is PlayerState.Playing -> Icon(
            painter = painterResource(Res.drawable.ic_equalizer),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        is PlayerState.Paused -> Icon(
            painter = painterResource(Res.drawable.ic_pause),
            contentDescription = null
        )

        is PlayerState.Error -> Icon(
            painter = painterResource(Res.drawable.ic_error),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
    }
}
