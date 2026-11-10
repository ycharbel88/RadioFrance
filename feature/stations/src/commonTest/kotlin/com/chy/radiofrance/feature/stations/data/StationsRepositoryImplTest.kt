package com.chy.radiofrance.feature.stations.data

import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import com.chy.radiofrance.core.network.FetchMode
import com.chy.radiofrance.core.network.StationsRemoteDataSource
import com.chy.radiofrance.feature.stations.station
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class StationsRepositoryImplTest {
    private class FakeStationsRemoteDataSource(private val result: DataResult<List<Station>>) :
        StationsRemoteDataSource {
        val modes = mutableListOf<FetchMode>()

        override suspend fun stations(mode: FetchMode): DataResult<List<Station>> {
            modes += mode
            return result
        }
    }

    private val stations = listOf(station("FIP", StationKind.NATIONAL, "FIP"))

    @Test
    fun `no forced refresh reads the cache first`() = runTest {
        val remote = FakeStationsRemoteDataSource(DataResult.Success(stations))

        val result = StationsRepositoryImpl(remote).stations(forceRefresh = false)

        assertEquals(listOf(FetchMode.CacheFirst), remote.modes)
        assertEquals(DataResult.Success(stations), result)
    }

    @Test
    fun `forced refresh goes to the network`() = runTest {
        val remote = FakeStationsRemoteDataSource(DataResult.Success(stations))

        StationsRepositoryImpl(remote).stations(forceRefresh = true)

        assertEquals(listOf(FetchMode.NetworkOnly), remote.modes)
    }

    @Test
    fun `errors pass through`() = runTest {
        val remote = FakeStationsRemoteDataSource(DataResult.Failure(DataError.Network))

        val result = StationsRepositoryImpl(remote).stations(forceRefresh = false)

        assertEquals(DataResult.Failure(DataError.Network), result)
    }
}
