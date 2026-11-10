package com.chy.radiofrance.feature.stations.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chy.radiofrance.core.designsystem.brandGradient
import com.chy.radiofrance.core.designsystem.onBrandColor
import com.chy.radiofrance.core.designsystem.stationBrand
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.feature.stations.resources.Res
import com.chy.radiofrance.feature.stations.resources.live_chip
import org.jetbrains.compose.resources.stringResource

/**
 * A brand's own stream (France Inter, FIP...): a large card in the brand gradient,
 * the name in bold and a "● DIRECT" chip. Stateless.
 *
 * Text and marks use [onBrandColor] of the gradient, so franceinfo yellow gets
 * black text. The card has a minimum height, not a fixed one: at 200 % font scale
 * it grows instead of clipping the name.
 */
@Composable
internal fun StationCard(
    station: Station,
    playerState: PlayerState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stops = brandGradient(stationBrand(station.brandId, station.brandName).color)
    val content = onBrandColor(stops.first())
    val stateText = playerState?.let(::playerStateResource)?.let { stringResource(it) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(stops))
            .clickable(role = Role.Button, onClick = onClick)
            // One node for TalkBack: "France Inter, DIRECT, En lecture, button".
            .semantics(mergeDescendants = true) {
                if (stateText != null) stateDescription = stateText
            }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleLarge,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            LiveChip(content)
        }
        if (playerState != null) {
            Spacer(Modifier.width(12.dp))
            PlaybackIndicator(playerState, tint = content)
        }
    }
}

/**
 * "● DIRECT": every station here is a live stream. A translucent dark pill keeps
 * the text readable on any brand colour, whatever its text colour.
 */
@Composable
private fun LiveChip(content: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(LiveRed)
        )
        Text(
            text = stringResource(Res.string.live_chip),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = content
        )
    }
}

/** The "on air" dot. Decorative: the word DIRECT says it. */
private val LiveRed = Color(0xFFFF3B30)
