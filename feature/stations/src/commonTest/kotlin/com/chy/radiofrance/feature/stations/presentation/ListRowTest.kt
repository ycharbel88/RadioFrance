package com.chy.radiofrance.feature.stations.presentation

import com.chy.radiofrance.core.model.StationKind.LOCAL
import com.chy.radiofrance.core.model.StationKind.NATIONAL
import com.chy.radiofrance.core.model.StationKind.WEBRADIO
import com.chy.radiofrance.feature.stations.domain.SectionHeader
import com.chy.radiofrance.feature.stations.domain.StationSection
import com.chy.radiofrance.feature.stations.station
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListRowTest {
    private val inter = station("FRANCEINTER", NATIONAL, "FRANCEINTER")
    private val fip = station("FIP", NATIONAL, "FIP")
    private val fipRock = station("FIP_ROCK", WEBRADIO, "FIP")
    private val alsace = station("FRANCEBLEU_ALSACE", LOCAL, "FRANCEBLEU", "ICI")
    private val nord = station("FRANCEBLEU_NORD", LOCAL, "FRANCEBLEU", "ICI")

    private val stations = StationSection(SectionHeader.Stations, listOf(inter, fip))
    private val webradios = StationSection(SectionHeader.Webradios("FIP", "FIP"), listOf(fipRock))
    private val locals =
        StationSection(SectionHeader.Locals("FRANCEBLEU", "ICI"), listOf(alsace, nord))
    private val sections = listOf(stations, webradios, locals)

    @Test
    fun national_stations_are_cards_and_webradios_are_compact_rows_under_a_title() {
        assertEquals(
            listOf(
                ListRow.SectionTitle(SectionHeader.Stations),
                ListRow.BrandCard(inter),
                ListRow.BrandCard(fip),
                ListRow.SectionTitle(SectionHeader.Webradios("FIP", "FIP")),
                ListRow.CompactRow(fipRock)
            ),
            listRows(listOf(stations, webradios), expandedLocals = emptySet())
        )
    }

    @Test
    fun local_radios_are_collapsed_behind_one_toggle_by_default() {
        assertEquals(
            listOf(ListRow.LocalsToggle("FRANCEBLEU", "ICI", count = 2, expanded = false)),
            listRows(listOf(locals), expandedLocals = emptySet())
        )
    }

    @Test
    fun an_expanded_brand_shows_its_local_radios_after_the_toggle() {
        assertEquals(
            listOf(
                ListRow.LocalsToggle("FRANCEBLEU", "ICI", count = 2, expanded = true),
                ListRow.CompactRow(alsace),
                ListRow.CompactRow(nord)
            ),
            listRows(listOf(locals), expandedLocals = setOf("FRANCEBLEU"))
        )
    }

    @Test
    fun keys_are_unique_even_when_everything_is_expanded() {
        val keys = listRows(sections, expandedLocals = setOf("FRANCEBLEU")).map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun a_station_keeps_its_key_whatever_its_row() {
        // Same key as in the step 4 list: scroll position and animations follow the station.
        assertEquals("FIP", ListRow.BrandCard(fip).key)
        assertEquals("FIP_ROCK", ListRow.CompactRow(fipRock).key)
    }

    @Test
    fun the_station_count_is_the_whole_list_and_null_without_one() {
        assertEquals(5, StationsUiState(content = StationsContent.Loaded(sections)).stationCount)
        assertNull(StationsUiState(content = StationsContent.Loading).stationCount)
    }

    @Test
    fun only_webradios_and_local_radios_have_a_subtitle() {
        assertNull(subtitleResource(NATIONAL))
        assertEquals(2, listOf(WEBRADIO, LOCAL).mapNotNull { subtitleResource(it) }.distinct().size)
    }
}
