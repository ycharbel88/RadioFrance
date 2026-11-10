package com.chy.radiofrance.feature.stations.domain

import com.chy.radiofrance.core.model.Station

/** One block of the station list: a header and its stations, already in display order. */
data class StationSection(val header: SectionHeader, val stations: List<Station>)

/**
 * What a section is about. Typed instead of a ready-made title, so the UI owns
 * the wording and its translations.
 */
sealed interface SectionHeader {
    /** The brands' own streams (France Inter, franceinfo, FIP...). */
    data object Stations : SectionHeader

    /** The webradios of one brand. */
    data class Webradios(val brandId: String, val brandName: String) : SectionHeader

    /** The local radios of one brand (today only ICI). */
    data class Locals(val brandId: String, val brandName: String) : SectionHeader
}
