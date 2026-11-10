package com.chy.radiofrance.feature.stations.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chy.radiofrance.core.designsystem.Equalizer
import com.chy.radiofrance.core.designsystem.RadioFranceIcons
import com.chy.radiofrance.core.designsystem.StationMonogram
import com.chy.radiofrance.core.designsystem.brandAccent
import com.chy.radiofrance.core.designsystem.stationBrand
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.player.PlayerState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A compact row (webradio, local radio): monogram, name, "Webradio · FIP". Stateless.
 *
 * The current station's row is tinted with 12 % of its brand colour, gets a brand
 * accent bar on the left and the playback indicator on the right.
 *
 * @param playerState the player state when this row is the current station,
 *   null otherwise.
 */
@Composable
internal fun StationItem(
    station: Station,
    playerState: PlayerState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = stationBrand(station.brandId, station.brandName)
    // Lifted toward white on the dark theme, darkened on the light one, until 3:1.
    val accent = brandAccent(brand.color, MaterialTheme.colorScheme.background)
    val isCurrent = playerState != null
    val stateText = playerState?.let(::playerStateResource)?.let { stringResource(it) }
    val subtitle = subtitleResource(station.kind)?.let { stringResource(it, station.brandName) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            // Lets the accent bar take the row's height, whatever the font scale.
            .height(IntrinsicSize.Min)
            .heightIn(min = 64.dp)
            .background(if (isCurrent) brand.color.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            // One node for TalkBack: "FIP Jazz, Webradio · FIP, En lecture, button".
            .semantics(mergeDescendants = true) {
                if (stateText != null) stateDescription = stateText
            }
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(if (isCurrent) accent else Color.Transparent)
        )
        Spacer(Modifier.width(12.dp))
        StationMonogram(brand)
        Spacer(Modifier.width(16.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (playerState != null) {
            Spacer(Modifier.width(8.dp))
            PlaybackIndicator(
                playerState,
                tint = accent,
                errorTint = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.width(16.dp))
    }
}

/**
 * What the current station is doing, on its row or card: an animated equalizer
 * while playing, a spinner while buffering, a pause or error mark. Decorative: the
 * row announces the state once, through stateDescription.
 */
@Composable
internal fun PlaybackIndicator(state: PlayerState, tint: Color, errorTint: Color = tint) {
    when (state) {
        PlayerState.Idle -> Unit

        is PlayerState.Buffering ->
            CircularProgressIndicator(
                color = tint,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )

        is PlayerState.Playing -> Equalizer(color = tint)

        is PlayerState.Paused -> Icon(
            painter = painterResource(RadioFranceIcons.Pause),
            contentDescription = null,
            tint = tint
        )

        is PlayerState.Error -> Icon(
            painter = painterResource(RadioFranceIcons.Error),
            contentDescription = null,
            tint = errorTint
        )
    }
}
