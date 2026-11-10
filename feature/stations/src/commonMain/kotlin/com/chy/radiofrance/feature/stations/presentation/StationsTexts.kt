package com.chy.radiofrance.feature.stations.presentation

import androidx.compose.runtime.Composable
import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.player.PlaybackError
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.feature.stations.domain.SectionHeader
import com.chy.radiofrance.feature.stations.resources.Res
import com.chy.radiofrance.feature.stations.resources.error_invalid_api_key
import com.chy.radiofrance.feature.stations.resources.error_missing_api_key
import com.chy.radiofrance.feature.stations.resources.error_network
import com.chy.radiofrance.feature.stations.resources.error_quota_exceeded
import com.chy.radiofrance.feature.stations.resources.error_server
import com.chy.radiofrance.feature.stations.resources.header_locals
import com.chy.radiofrance.feature.stations.resources.header_stations
import com.chy.radiofrance.feature.stations.resources.header_webradios
import com.chy.radiofrance.feature.stations.resources.playback_error_network
import com.chy.radiofrance.feature.stations.resources.playback_error_not_supported
import com.chy.radiofrance.feature.stations.resources.playback_error_stream_unavailable
import com.chy.radiofrance.feature.stations.resources.player_buffering
import com.chy.radiofrance.feature.stations.resources.player_error
import com.chy.radiofrance.feature.stations.resources.player_paused
import com.chy.radiofrance.feature.stations.resources.player_playing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Typed state -> words. The domain and the ViewModel never hold text; the UI
// picks it here. Plain functions (not composables) so the rules are unit-tested.

/** What to say about a [DataError], and whether offering "Retry" makes sense. */
internal data class ErrorUi(val message: StringResource, val canRetry: Boolean)

/**
 * A retry only helps when the problem is temporary. A missing or refused key, or
 * the daily quota, won't fix itself in a second: no button there.
 */
internal fun errorUi(error: DataError): ErrorUi = when (error) {
    DataError.MissingApiKey -> ErrorUi(Res.string.error_missing_api_key, canRetry = false)
    DataError.InvalidApiKey -> ErrorUi(Res.string.error_invalid_api_key, canRetry = false)
    DataError.QuotaExceeded -> ErrorUi(Res.string.error_quota_exceeded, canRetry = false)
    DataError.Network -> ErrorUi(Res.string.error_network, canRetry = true)
    is DataError.Server -> ErrorUi(Res.string.error_server, canRetry = true)
}

/** The section title template. Webradios and locals take the brand name as argument. */
internal fun headerResource(header: SectionHeader): StringResource = when (header) {
    SectionHeader.Stations -> Res.string.header_stations
    is SectionHeader.Webradios -> Res.string.header_webradios
    is SectionHeader.Locals -> Res.string.header_locals
}

@Composable
internal fun headerText(header: SectionHeader): String = when (header) {
    SectionHeader.Stations -> stringResource(headerResource(header))
    is SectionHeader.Webradios -> stringResource(headerResource(header), header.brandName)
    is SectionHeader.Locals -> stringResource(headerResource(header), header.brandName)
}

/** Spoken state of a row (screen readers) and the mini-player label. Null when idle. */
internal fun playerStateResource(state: PlayerState): StringResource? = when (state) {
    PlayerState.Idle -> null
    is PlayerState.Buffering -> Res.string.player_buffering
    is PlayerState.Playing -> Res.string.player_playing
    is PlayerState.Paused -> Res.string.player_paused
    is PlayerState.Error -> Res.string.player_error
}

/** Why playback failed, in words. Used by the mini-player (step 5). */
internal fun playbackErrorResource(error: PlaybackError): StringResource = when (error) {
    PlaybackError.Network -> Res.string.playback_error_network
    PlaybackError.StreamUnavailable -> Res.string.playback_error_stream_unavailable
    PlaybackError.NotSupported -> Res.string.playback_error_not_supported
}
