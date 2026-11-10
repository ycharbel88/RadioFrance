package com.chy.radiofrance.feature.stations.domain

import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind

/**
 * The station list as the product wants it. 77 rows in one flat list is not
 * usable, so:
 * 1. "Stations": the brands' own streams.
 * 2. One section of webradios per brand, in API brand order.
 * 3. Local radios last, one section per brand.
 *
 * Empty sections are dropped. Inside a section, API order is kept.
 * The only entry point of the presentation layer into the data.
 */
class GetStationSectionsUseCase(private val repository: StationsRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): DataResult<List<StationSection>> =
        when (val result = repository.stations(forceRefresh)) {
            is DataResult.Success -> DataResult.Success(sectionsOf(result.value))
            is DataResult.Failure -> result
        }

    private fun sectionsOf(stations: List<Station>): List<StationSection> {
        val national = stations.filter { it.kind == StationKind.NATIONAL }
        val nationalSections = listOf(StationSection(SectionHeader.Stations, national))
        val webradioSections = stations.sectionsPerBrand(StationKind.WEBRADIO) { brandId, name ->
            SectionHeader.Webradios(brandId, name)
        }
        val localSections = stations.sectionsPerBrand(StationKind.LOCAL) { brandId, name ->
            SectionHeader.Locals(brandId, name)
        }
        return (nationalSections + webradioSections + localSections).filter {
            it.stations.isNotEmpty()
        }
    }

    // groupBy keeps the order in which brands first appear, i.e. API order.
    private fun List<Station>.sectionsPerBrand(
        kind: StationKind,
        header: (brandId: String, brandName: String) -> SectionHeader
    ): List<StationSection> = filter { it.kind == kind }
        .groupBy { it.brandId }
        .map { (brandId, radios) ->
            val brandName = radios.first().brandName
            StationSection(header(brandId, brandName), radios)
        }
}
