package com.chy.radiofrance.feature.stations.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.player.station
import com.chy.radiofrance.feature.stations.domain.SectionHeader
import com.chy.radiofrance.feature.stations.domain.StationSection
import com.chy.radiofrance.feature.stations.resources.Res
import com.chy.radiofrance.feature.stations.resources.action_refresh
import com.chy.radiofrance.feature.stations.resources.action_retry
import com.chy.radiofrance.feature.stations.resources.app_title
import com.chy.radiofrance.feature.stations.resources.empty_message
import com.chy.radiofrance.feature.stations.resources.loading
import com.chy.radiofrance.feature.stations.resources.refresh_failed_title
import com.chy.radiofrance.feature.stations.resources.stations_live
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The stations screen, wired to its ViewModel. The only composable of the
 * feature that knows Koin and the ViewModel; everything below is stateless.
 *
 * `koinViewModel()` returns the instance of the current ViewModelStoreOwner
 * (the Activity on Android, the ComposeUIViewController on iOS), so it survives
 * rotation, and the mini-player shares the same instance.
 *
 * Which local-radio groups are expanded is pure UI state: it lives here, in
 * rememberSaveable, so it survives rotation without going through the ViewModel.
 */
@Composable
fun StationsScreen(modifier: Modifier = Modifier, viewModel: StationsViewModel = koinViewModel()) {
    // Stops collecting when the app goes to the background (lifecycle-aware),
    // which lets the ViewModel's WhileSubscribed(5_000) stop its upstream too.
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // A List, not a Set: saveable in an Android Bundle as is.
    var expandedLocals by rememberSaveable { mutableStateOf(listOf<String>()) }
    StationsScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        expandedLocals = expandedLocals.toSet(),
        onToggleLocals = { brandId ->
            expandedLocals = if (brandId in expandedLocals) {
                expandedLocals - brandId
            } else {
                expandedLocals + brandId
            }
        },
        modifier = modifier
    )
}

/**
 * The whole screen as a function of [state]. Previews call it directly.
 *
 * A large "Radio France" title with the station count, collapsing as the list
 * scrolls, above the list. Pull-to-refresh wraps every state, but only scrollable
 * content can be pulled: the list and the empty state. Loading and error are not
 * scrollable; this matches the ViewModel, which ignores Refresh there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StationsScreenContent(
    state: StationsUiState,
    onEvent: (StationsEvent) -> Unit,
    modifier: Modifier = Modifier,
    expandedLocals: Set<String> = emptySet(),
    onToggleLocals: (brandId: String) -> Unit = {}
) {
    // exitUntilCollapsed: the large title shrinks to a small bar, which stays.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Column(
        modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        StationsTopBar(stationCount = state.stationCount, scrollBehavior = scrollBehavior)
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(StationsEvent.Refresh) },
            modifier = Modifier.fillMaxSize()
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
                    onEvent = onEvent,
                    expandedLocals = expandedLocals,
                    onToggleLocals = onToggleLocals
                )
            }
        }
    }
}

// "Radio France" / "77 stations en direct". The expressive (flexible) large bar is
// the one with a subtitle slot. The subtitle appears once there is a list.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StationsTopBar(stationCount: Int?, scrollBehavior: TopAppBarScrollBehavior) {
    val background = MaterialTheme.colorScheme.background
    LargeFlexibleTopAppBar(
        title = { Text(stringResource(Res.string.app_title)) },
        subtitle = {
            if (stationCount != null) {
                Text(pluralStringResource(Res.plurals.stations_live, stationCount, stationCount))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = background,
            scrolledContainerColor = background
        ),
        scrollBehavior = scrollBehavior
    )
}

/**
 * ONE vertical list (the brief asks for a vertically scrollable list): cards, rows
 * and the locals toggle are rows of the same LazyColumn, built by [listRows].
 *
 * On a wide window (landscape, tablet) the list keeps a phone-like width, centred,
 * and stays clear of a camera cutout on the side.
 */
@Composable
private fun StationList(
    sections: List<StationSection>,
    state: StationsUiState,
    onEvent: (StationsEvent) -> Unit,
    expandedLocals: Set<String>,
    onToggleLocals: (String) -> Unit
) {
    val rows = remember(sections, expandedLocals) { listRows(sections, expandedLocals) }
    fun playerStateOf(station: Station) = state.player.takeIf { it.station?.id == station.id }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            state.refreshError?.let { error ->
                item(key = "refresh-error") { RefreshErrorBanner(error, Modifier.animateItem()) }
            }
            // Stable keys: rows keep their identity (and their animations, and the
            // scroll position) when a refresh or a toggle changes the list.
            for (row in rows) {
                when (row) {
                    is ListRow.SectionTitle -> stickyHeader(key = row.key, contentType = "title") {
                        SectionTitle(row.header)
                    }

                    is ListRow.BrandCard -> item(key = row.key, contentType = "card") {
                        StationCard(
                            station = row.station,
                            playerState = playerStateOf(row.station),
                            onClick = { onEvent(StationsEvent.StationClicked(row.station)) },
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    is ListRow.CompactRow -> item(key = row.key, contentType = "row") {
                        StationItem(
                            station = row.station,
                            playerState = playerStateOf(row.station),
                            onClick = { onEvent(StationsEvent.StationClicked(row.station)) },
                            modifier = Modifier.animateItem()
                        )
                    }

                    is ListRow.LocalsToggle -> item(key = row.key, contentType = "toggle") {
                        LocalsToggle(
                            row = row,
                            onToggle = { onToggleLocals(row.brandId) },
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

// "STATIONS", "WEBRADIOS FIP": shown in capitals, announced in normal case (a
// screen reader may spell out a word written in capitals).
@Composable
private fun SectionTitle(header: SectionHeader) {
    val text = headerText(header)
    // Opaque background: a sticky header covers the rows scrolling under it.
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                .semantics {
                    heading()
                    contentDescription = text
                }
        )
    }
}

/** A failed pull-to-refresh: the list stays, this says it may be stale. */
@Composable
private fun RefreshErrorBanner(error: DataError, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
