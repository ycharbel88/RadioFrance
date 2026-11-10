package com.chy.radiofrance.feature.stations.presentation

import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.feature.stations.domain.SectionHeader
import com.chy.radiofrance.feature.stations.domain.StationSection

/**
 * One line of the station list. The sections become a flat list of rows, so the
 * screen stays ONE LazyColumn (the brief asks for a vertically scrollable list)
 * and the layout rules below are unit-tested without a UI.
 */
internal sealed interface ListRow {
    /** Stable LazyColumn key: rows keep their identity and animations across updates. */
    val key: String

    data class SectionTitle(val header: SectionHeader) : ListRow {
        override val key = "title:" + when (header) {
            SectionHeader.Stations -> "stations"
            is SectionHeader.Webradios -> "webradios:${header.brandId}"
            is SectionHeader.Locals -> "locals:${header.brandId}"
        }
    }

    /** A brand's own stream: a large gradient card. */
    data class BrandCard(val station: Station) : ListRow {
        override val key = station.id
    }

    /** A webradio or a local radio: a compact row. */
    data class CompactRow(val station: Station) : ListRow {
        override val key = station.id
    }

    /** The card that shows or hides a brand's local radios (45 for ICI). */
    data class LocalsToggle(
        val brandId: String,
        val brandName: String,
        val count: Int,
        val expanded: Boolean
    ) : ListRow {
        override val key = "locals-toggle:$brandId"
    }
}

/**
 * The rows for [sections], in order:
 * - national stations: a title, then one card each;
 * - webradios: a title, then one compact row each;
 * - local radios: no title, one toggle, and the rows only when the brand is in
 *   [expandedLocals]. Collapsed by default: 45 locals would bury everything else.
 */
internal fun listRows(sections: List<StationSection>, expandedLocals: Set<String>): List<ListRow> =
    sections.flatMap { section ->
        when (val header = section.header) {
            SectionHeader.Stations ->
                listOf(ListRow.SectionTitle(header)) +
                    section.stations.map { ListRow.BrandCard(it) }

            is SectionHeader.Webradios ->
                listOf(ListRow.SectionTitle(header)) +
                    section.stations.map { ListRow.CompactRow(it) }

            is SectionHeader.Locals -> {
                val expanded = header.brandId in expandedLocals
                val toggle = ListRow.LocalsToggle(
                    brandId = header.brandId,
                    brandName = header.brandName,
                    count = section.stations.size,
                    expanded = expanded
                )
                listOf(toggle) +
                    if (expanded) section.stations.map { ListRow.CompactRow(it) } else emptyList()
            }
        }
    }
