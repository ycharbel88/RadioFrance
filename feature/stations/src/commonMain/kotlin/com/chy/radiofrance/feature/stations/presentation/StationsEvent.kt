package com.chy.radiofrance.feature.stations.presentation

import com.chy.radiofrance.core.model.Station

/** What the user can do on the stations screen and the mini-player. */
sealed interface StationsEvent {
    /** A row was tapped: play that station. */
    data class StationClicked(val station: Station) : StationsEvent

    /** The "retry" action after a failed load. */
    data object Retry : StationsEvent

    /** Pull-to-refresh: ask the API again, keeping the list on screen. */
    data object Refresh : StationsEvent

    /** The play/pause button of the mini-player. */
    data object TogglePlayPause : StationsEvent
}
