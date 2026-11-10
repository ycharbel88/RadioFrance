package com.chy.radiofrance.feature.stations.presentation

import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import com.chy.radiofrance.core.model.StationKind.LOCAL
import com.chy.radiofrance.core.model.StationKind.NATIONAL
import com.chy.radiofrance.core.model.StationKind.WEBRADIO
import com.chy.radiofrance.feature.stations.domain.SectionHeader
import com.chy.radiofrance.feature.stations.domain.StationSection

/** Sample data for the previews only. Same ids and shape as the API, fewer stations. */
internal object PreviewStations {
    private fun station(
        id: String,
        name: String,
        kind: StationKind,
        brandId: String,
        brandName: String
    ) = Station(
        id = id,
        name = name,
        description = null,
        streamUrl = "https://icecast.radiofrance.fr/$id.mp3",
        kind = kind,
        brandId = brandId,
        brandName = brandName
    )

    val inter = station("FRANCEINTER", "France Inter", NATIONAL, "FRANCEINTER", "France Inter")
    val franceinfo = station("FRANCEINFO", "franceinfo", NATIONAL, "FRANCEINFO", "franceinfo")
    val fip = station("FIP", "FIP", NATIONAL, "FIP", "FIP")
    private val culture =
        station("FRANCECULTURE", "France Culture", NATIONAL, "FRANCECULTURE", "France Culture")
    val fipJazz = station("FIP_JAZZ", "FIP Jazz", WEBRADIO, "FIP", "FIP")
    private val fipRock = station("FIP_ROCK", "FIP Rock", WEBRADIO, "FIP", "FIP")
    private val laJazz =
        station("FRANCEMUSIQUE_LA_JAZZ", "La Jazz", WEBRADIO, "FRANCEMUSIQUE", "France Musique")
    private val pianoZen =
        station("FRANCEMUSIQUE_PIANO_ZEN", "Piano Zen", WEBRADIO, "FRANCEMUSIQUE", "France Musique")
    val iciAlsace = station("FRANCEBLEU_ALSACE", "ICI Alsace", LOCAL, "FRANCEBLEU", "ICI")
    private val iciNord = station("FRANCEBLEU_NORD", "ICI Nord", LOCAL, "FRANCEBLEU", "ICI")
    private val iciProvence =
        station("FRANCEBLEU_PROVENCE", "ICI Provence", LOCAL, "FRANCEBLEU", "ICI")

    val sections = listOf(
        StationSection(SectionHeader.Stations, listOf(inter, franceinfo, fip, culture)),
        StationSection(SectionHeader.Webradios("FIP", "FIP"), listOf(fipJazz, fipRock)),
        StationSection(
            SectionHeader.Webradios("FRANCEMUSIQUE", "France Musique"),
            listOf(laJazz, pianoZen)
        ),
        StationSection(
            SectionHeader.Locals("FRANCEBLEU", "ICI"),
            listOf(iciAlsace, iciNord, iciProvence)
        )
    )
}
