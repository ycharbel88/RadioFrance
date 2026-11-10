package com.chy.radiofrance.feature.stations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.player.AudioPlayer
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.feature.stations.domain.GetStationSectionsUseCase
import com.chy.radiofrance.feature.stations.domain.StationSection
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The stations screen: loads the sectioned list once, refreshes it on demand,
 * and turns taps into player commands.
 *
 * Its only data entry point is [GetStationSectionsUseCase]: it never sees the
 * repository, the cache or the network. The player state is not copied here: it
 * is read live from [AudioPlayer.state], so the screen and the mini-player always
 * show what the player really does.
 *
 * Threading: everything runs in [viewModelScope] (main thread). The use case is
 * main-safe, since Apollo does its I/O on its own dispatcher.
 */
class StationsViewModel(
    private val getStationSections: GetStationSectionsUseCase,
    private val player: AudioPlayer
) : ViewModel() {
    /** The part of the screen this ViewModel owns; the player part comes from [player]. */
    private data class ListState(
        val content: StationsContent = StationsContent.Loading,
        val isRefreshing: Boolean = false,
        val refreshError: DataError? = null
    ) {
        fun toUiState(playerState: PlayerState) =
            StationsUiState(content, isRefreshing, refreshError, playerState)
    }

    private val list = MutableStateFlow(ListState())

    /** The one request in flight, if any. Used to never send two at once. */
    private var loadJob: Job? = null

    /**
     * Stops listening 5 s after the last collector leaves: a rotation (a few hundred
     * ms) keeps the flow alive, while a screen left in the background stops it.
     */
    val uiState: StateFlow<StationsUiState> =
        combine(list, player.state) { list, playerState -> list.toUiState(playerState) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = list.value.toUiState(player.state.value)
            )

    init {
        // Cache first: recreating the screen costs no request.
        load(forceRefresh = false)
    }

    fun onEvent(event: StationsEvent) {
        when (event) {
            is StationsEvent.StationClicked -> onStationClicked(event.station)
            StationsEvent.Retry -> retry()
            StationsEvent.Refresh -> refresh()
            StationsEvent.TogglePlayPause -> togglePlayPause()
        }
    }

    // Only meaningful when there is no list to show. A retry while loading is ignored.
    private fun retry() {
        val content = list.value.content
        if (content !is StationsContent.Error && content !is StationsContent.Empty) return
        if (loadJob?.isActive == true) return
        list.update { it.copy(content = StationsContent.Loading) }
        load(forceRefresh = false)
    }

    // Only meaningful when an answer is on screen. While loading or in error, Retry
    // is the action. A second pull while one is running sends no second request.
    private fun refresh() {
        val content = list.value.content
        if (content !is StationsContent.Loaded && content !is StationsContent.Empty) return
        if (loadJob?.isActive == true) return
        list.update { it.copy(isRefreshing = true, refreshError = null) }
        load(forceRefresh = true)
    }

    private fun load(forceRefresh: Boolean) {
        loadJob = viewModelScope.launch {
            val result = getStationSections(forceRefresh)
            list.update { it.after(result) }
        }
    }

    private fun ListState.after(result: DataResult<List<StationSection>>): ListState =
        when (result) {
            is DataResult.Success -> ListState(content = contentOf(result.value))

            // A failed refresh keeps what is on screen and reports the error on the side.
            is DataResult.Failure -> if (isRefreshing) {
                copy(isRefreshing = false, refreshError = result.error)
            } else {
                ListState(content = StationsContent.Error(result.error))
            }
        }

    private fun contentOf(sections: List<StationSection>): StationsContent =
        if (sections.isEmpty()) StationsContent.Empty else StationsContent.Loaded(sections)

    // Tapping the station that is already playing (or starting) does nothing:
    // restarting a live stream would only cut the audio.
    private fun onStationClicked(station: Station) {
        val current = player.state.value
        val alreadyOn = when (current) {
            is PlayerState.Playing -> current.station.id == station.id
            is PlayerState.Buffering -> current.station.id == station.id
            else -> false
        }
        if (!alreadyOn) player.play(station)
    }

    // Resume is `play(station)`: the player resumes its buffer, or rejoins live after a
    // long lag or an error (Android: LivePlayer).
    private fun togglePlayPause() {
        when (val current = player.state.value) {
            PlayerState.Idle -> Unit
            is PlayerState.Playing, is PlayerState.Buffering -> player.pause()
            is PlayerState.Paused -> player.play(current.station)
            is PlayerState.Error -> player.play(current.station)
        }
    }
}
