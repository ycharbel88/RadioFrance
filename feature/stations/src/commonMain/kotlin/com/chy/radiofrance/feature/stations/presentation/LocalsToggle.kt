package com.chy.radiofrance.feature.stations.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chy.radiofrance.core.designsystem.RadioFranceIcons
import com.chy.radiofrance.core.designsystem.StationMonogram
import com.chy.radiofrance.core.designsystem.stationBrand
import com.chy.radiofrance.feature.stations.resources.Res
import com.chy.radiofrance.feature.stations.resources.action_hide
import com.chy.radiofrance.feature.stations.resources.action_show
import com.chy.radiofrance.feature.stations.resources.locals_count
import com.chy.radiofrance.feature.stations.resources.state_collapsed
import com.chy.radiofrance.feature.stations.resources.state_expanded
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * "ICI · 45 radios locales / Afficher": shows or hides a brand's local radios.
 * Stateless: whether it is expanded lives in the screen, saved across rotation.
 * TalkBack reads "ICI · 45 radios locales, réduit, button, double-tap to Afficher".
 */
@Composable
internal fun LocalsToggle(
    row: ListRow.LocalsToggle,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = pluralStringResource(Res.plurals.locals_count, row.count, row.brandName, row.count)
    val action =
        stringResource(if (row.expanded) Res.string.action_hide else Res.string.action_show)
    val stateText =
        stringResource(if (row.expanded) Res.string.state_expanded else Res.string.state_collapsed)
    val chevronRotation by animateFloatAsState(if (row.expanded) 180f else 0f, label = "chevron")

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(role = Role.Button, onClickLabel = action, onClick = onToggle)
                .semantics(mergeDescendants = true) { stateDescription = stateText }
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            StationMonogram(stationBrand(row.brandId, row.brandName))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = action,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = painterResource(RadioFranceIcons.ExpandMore),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation)
            )
        }
    }
}
