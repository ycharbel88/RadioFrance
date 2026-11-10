package com.chy.radiofrance.feature.stations.domain

import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind.LOCAL
import com.chy.radiofrance.core.model.StationKind.NATIONAL
import com.chy.radiofrance.core.model.StationKind.WEBRADIO
import com.chy.radiofrance.feature.stations.FakeStationsRepository
import com.chy.radiofrance.feature.stations.station
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class GetStationSectionsUseCaseTest {
    // Same shape as the API: each brand, then its webradios, then its locals.
    private val inter = station("FRANCEINTER", NATIONAL, "FRANCEINTER", "France Inter")
    private val interMusic =
        station("FRANCEINTER_LA_MUSIQUE_INTER", WEBRADIO, "FRANCEINTER", "France Inter")
    private val fip = station("FIP", NATIONAL, "FIP")
    private val fipRock = station("FIP_ROCK", WEBRADIO, "FIP")
    private val fipJazz = station("FIP_JAZZ", WEBRADIO, "FIP")
    private val iciChanson = station("FRANCEBLEU_CHANSON_FRANCAISE", WEBRADIO, "FRANCEBLEU", "ICI")
    private val iciAlsace = station("FRANCEBLEU_ALSACE", LOCAL, "FRANCEBLEU", "ICI")
    private val iciParis = station("FRANCEBLEU_PARIS", LOCAL, "FRANCEBLEU", "ICI")
    private val apiOrder =
        listOf(inter, interMusic, fip, fipRock, fipJazz, iciChanson, iciAlsace, iciParis)

    private suspend fun sectionsOf(stations: List<Station>): List<StationSection> {
        val result =
            GetStationSectionsUseCase(FakeStationsRepository(DataResult.Success(stations)))()
        return assertIs<DataResult.Success<List<StationSection>>>(result).value
    }

    @Test
    fun `brands first then webradios per brand in API order then locals`() = runTest {
        val sections = sectionsOf(apiOrder)

        assertEquals(
            listOf(
                StationSection(SectionHeader.Stations, listOf(inter, fip)),
                StationSection(
                    SectionHeader.Webradios("FRANCEINTER", "France Inter"),
                    listOf(interMusic)
                ),
                StationSection(SectionHeader.Webradios("FIP", "FIP"), listOf(fipRock, fipJazz)),
                StationSection(SectionHeader.Webradios("FRANCEBLEU", "ICI"), listOf(iciChanson)),
                StationSection(
                    SectionHeader.Locals("FRANCEBLEU", "ICI"),
                    listOf(iciAlsace, iciParis)
                )
            ),
            sections
        )
    }

    @Test
    fun `empty sections are dropped`() = runTest {
        val sections = sectionsOf(listOf(iciAlsace, iciParis))

        assertEquals(listOf(SectionHeader.Locals("FRANCEBLEU", "ICI")), sections.map { it.header })
    }

    @Test
    fun `no stations gives no sections`() = runTest {
        assertEquals(emptyList(), sectionsOf(emptyList()))
    }

    @Test
    fun `does not force a refresh by default`() = runTest {
        val repository = FakeStationsRepository(DataResult.Success(apiOrder))

        GetStationSectionsUseCase(repository)()

        assertEquals(listOf(false), repository.forceRefreshCalls)
    }

    @Test
    fun `passes forceRefresh to the repository`() = runTest {
        val repository = FakeStationsRepository(DataResult.Success(apiOrder))

        GetStationSectionsUseCase(repository)(forceRefresh = true)

        assertEquals(listOf(true), repository.forceRefreshCalls)
    }

    @Test
    fun `errors pass through`() = runTest {
        val result =
            GetStationSectionsUseCase(
                FakeStationsRepository(DataResult.Failure(DataError.InvalidApiKey))
            )()

        assertEquals(DataResult.Failure(DataError.InvalidApiKey), result)
    }
}
