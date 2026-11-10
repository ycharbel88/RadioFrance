package com.chy.radiofrance.core.model

/**
 * A playable radio station. Only things with a live stream are stations: a brand
 * without a stream (ICI) is not one, it only groups its local radios.
 *
 * @property id stable API id (e.g. `FIP_ROCK`). Used as list key and current-station id.
 * @property description null when the API sends nothing or a blank string.
 * @property streamUrl progressive MP3 over HTTPS (Icecast), played as given.
 * @property brandId id of the brand the station belongs to (e.g. `FIP`).
 * @property brandName display name of that brand (e.g. "FIP", "ICI").
 */
data class Station(
    val id: String,
    val name: String,
    val description: String?,
    val streamUrl: String,
    val kind: StationKind,
    val brandId: String,
    val brandName: String
)

/** Where a station comes from in the API. Drives how the list is grouped. */
enum class StationKind {
    /** The brand's own stream (France Inter, FIP...). */
    NATIONAL,

    /** A thematic webradio of a brand (FIP Rock, La musique d'Inter...). */
    WEBRADIO,

    /** A local radio (ICI Alsace...). */
    LOCAL
}
