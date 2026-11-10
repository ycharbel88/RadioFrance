package com.chy.radiofrance.feature.stations.presentation

import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.core.player.station
import com.chy.radiofrance.feature.stations.domain.StationSection

/**
 * Everything the stations screen and the mini-player draw. Immutable: the screen
 * renders it and sends [StationsEvent]s back, nothing else.
 *
 * @property content the list itself, or why there is none.
 * @property isRefreshing a pull-to-refresh is running; [content] stays on screen.
 * @property refreshError the last pull-to-refresh failed, but [content] is still
 *   shown. Cleared by the next refresh.
 * @property player what the player is doing. Also feeds the mini-player.
 */
data class StationsUiState(
    val content: StationsContent = StationsContent.Loading,
    val isRefreshing: Boolean = false,
    val refreshError: DataError? = null,
    val player: PlayerState = PlayerState.Idle
) {
    /** Id of the station the player is about (playing, paused, failed...), to highlight its row. */
    val currentStationId: String? get() = player.station?.id

    /** How many stations the list holds ("77 stations en direct"), or null while there is no list. */
    val stationCount: Int?
        get() = (content as? StationsContent.Loaded)?.sections?.sumOf { it.stations.size }
}

/** The four ways the list area can look. */
sealed interface StationsContent {
    /** First load, or a retry after an error. Nothing to show yet. */
    data object Loading : StationsContent

    /** The API answered, with no station. */
    data object Empty : StationsContent

    /** No list could be loaded. The UI picks the message and action per [error]. */
    data class Error(val error: DataError) : StationsContent

    /** The list, already split into sections in display order. */
    data class Loaded(val sections: List<StationSection>) : StationsContent
}
