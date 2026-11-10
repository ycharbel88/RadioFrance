package com.chy.radiofrance.feature.stations.presentation

import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import com.chy.radiofrance.feature.stations.domain.SectionHeader
import com.chy.radiofrance.feature.stations.domain.StationSection

/** Sample data for the previews only. Same shape as the API, made-up descriptions. */
internal object PreviewStations {
    private fun station(
        id: String,
        name: String,
        kind: StationKind,
        brandId: String,
        brandName: String,
        description: String? = null
    ) = Station(
        id = id,
        name = name,
        description = description,
        streamUrl = "https://icecast.radiofrance.fr/$id.mp3",
        kind = kind,
        brandId = brandId,
        brandName = brandName
    )

    val inter = station(
        "FRANCEINTER",
        "France Inter",
        StationKind.NATIONAL,
        "FRANCEINTER",
        "France Inter",
        "Sample description: news, culture and music, all day long."
    )
    val fip = station(
        "FIP",
        "FIP",
        StationKind.NATIONAL,
        "FIP",
        "FIP",
        "Sample description that is long enough to need a second line, to check the ellipsis."
    )
    private val fipRock = station("FIP_ROCK", "FIP Rock", StationKind.WEBRADIO, "FIP", "FIP")
    private val fipJazz = station("FIP_JAZZ", "FIP Jazz", StationKind.WEBRADIO, "FIP", "FIP")
    private val iciAlsace =
        station("FRANCEBLEU_ALSACE", "ICI Alsace", StationKind.LOCAL, "FRANCEBLEU", "ICI")

    val sections = listOf(
        StationSection(SectionHeader.Stations, listOf(inter, fip)),
        StationSection(SectionHeader.Webradios("FIP", "FIP"), listOf(fipRock, fipJazz)),
        StationSection(SectionHeader.Locals("FRANCEBLEU", "ICI"), listOf(iciAlsace))
    )
}
