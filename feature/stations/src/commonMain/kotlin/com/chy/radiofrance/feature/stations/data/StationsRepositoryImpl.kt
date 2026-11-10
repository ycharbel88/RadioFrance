package com.chy.radiofrance.feature.stations.data

import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.network.FetchMode
import com.chy.radiofrance.core.network.StationsRemoteDataSource
import com.chy.radiofrance.feature.stations.domain.StationsRepository

/**
 * Thin on purpose: it only decides between cache and network. It is the seam
 * where a second source (e.g. a disk cache) would plug in without touching the
 * domain.
 */
internal class StationsRepositoryImpl(private val remote: StationsRemoteDataSource) :
    StationsRepository {
    override suspend fun stations(forceRefresh: Boolean): DataResult<List<Station>> =
        remote.stations(if (forceRefresh) FetchMode.NetworkOnly else FetchMode.CacheFirst)
}
