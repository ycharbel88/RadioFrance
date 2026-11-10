package com.chy.radiofrance.feature.stations.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.player.station
import com.chy.radiofrance.feature.stations.domain.SectionHeader
import com.chy.radiofrance.feature.stations.domain.StationSection
import com.chy.radiofrance.feature.stations.resources.Res
import com.chy.radiofrance.feature.stations.resources.action_refresh
import com.chy.radiofrance.feature.stations.resources.action_retry
import com.chy.radiofrance.feature.stations.resources.empty_message
import com.chy.radiofrance.feature.stations.resources.loading
import com.chy.radiofrance.feature.stations.resources.refresh_failed_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The stations screen, wired to its ViewModel. The only composable of the
 * feature that knows Koin and the ViewModel; everything below is stateless.
 *
 * `koinViewModel()` returns the instance of the current ViewModelStoreOwner
 * (the Activity on Android, the ComposeUIViewController on iOS), so it survives
 * rotation, and the mini-player (step 5) shares the same instance.
 */
@Composable
fun StationsScreen(modifier: Modifier = Modifier, viewModel: StationsViewModel = koinViewModel()) {
    // Stops collecting when the app goes to the background (lifecycle-aware),
    // which lets the ViewModel's WhileSubscribed(5_000) stop its upstream too.
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StationsScreenContent(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

/**
 * The whole screen as a function of [state]. Previews call it directly.
 *
 * Pull-to-refresh wraps every state, but only scrollable content can be pulled:
 * the list and the empty state. Loading and error are not scrollable, so they
 * can't be pulled; this matches the ViewModel, which ignores Refresh there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StationsScreenContent(
    state: StationsUiState,
    onEvent: (StationsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(StationsEvent.Refresh) },
        modifier = modifier.fillMaxSize()
    ) {
        when (val content = state.content) {
            StationsContent.Loading -> LoadingState()

            StationsContent.Empty -> MessageState(
                message = Res.string.empty_message,
                action = Res.string.action_refresh,
                onAction = { onEvent(StationsEvent.Retry) },
                scrollable = true
            )

            is StationsContent.Error -> {
                val error = errorUi(content.error)
                MessageState(
                    message = error.message,
                    action = Res.string.action_retry.takeIf { error.canRetry },
                    onAction = { onEvent(StationsEvent.Retry) },
                    scrollable = false
                )
            }

            is StationsContent.Loaded -> StationList(
                sections = content.sections,
                state = state,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun StationList(
    sections: List<StationSection>,
    state: StationsUiState,
    onEvent: (StationsEvent) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        state.refreshError?.let { error ->
            item(key = "refresh-error") { RefreshErrorBanner(error) }
        }
        for (section in sections) {
            // Stable keys: rows keep their identity (and their animations, and
            // the scroll position) when a refresh changes the list.
            stickyHeader(key = section.header.key, contentType = "header") {
                SectionTitle(section.header)
            }
            items(section.stations, key = { it.id }, contentType = { "station" }) { station ->
                StationItem(
                    station = station,
                    playerState = state.player.takeIf { it.station?.id == station.id },
                    onClick = { onEvent(StationsEvent.StationClicked(station)) }
                )
            }
        }
    }
}

private val SectionHeader.key: String
    get() = when (this) {
        SectionHeader.Stations -> "header:stations"
        is SectionHeader.Webradios -> "header:webradios:$brandId"
        is SectionHeader.Locals -> "header:locals:$brandId"
    }

@Composable
private fun SectionTitle(header: SectionHeader) {
    // Opaque background: a sticky header covers the rows scrolling under it.
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = headerText(header),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() }
        )
    }
}

/** A failed pull-to-refresh: the list stays, this says it may be stale. */
@Composable
private fun RefreshErrorBanner(error: DataError) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(Res.string.refresh_failed_title),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = stringResource(errorUi(error).message),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LoadingState() {
    val description = stringResource(Res.string.loading)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.semantics {
                contentDescription = description
            }
        )
    }
}

/**
 * A centred message, with a button when [action] is not null.
 *
 * @param scrollable true lets pull-to-refresh work on this state. A one-item
 *   LazyColumn, because a plain scrollable Column measures its content with an
 *   infinite height and can't centre it.
 */
@Composable
private fun MessageState(
    message: StringResource,
    action: StringResource?,
    onAction: () -> Unit,
    scrollable: Boolean
) {
    if (scrollable) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { Message(message, action, onAction, Modifier.fillParentMaxSize()) }
        }
    } else {
        Message(message, action, onAction, Modifier.fillMaxSize())
    }
}

@Composable
private fun Message(
    message: StringResource,
    action: StringResource?,
    onAction: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) { Text(stringResource(action)) }
        }
    }
}
