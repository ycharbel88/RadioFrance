package com.chy.radiofrance.feature.stations

import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.feature.stations.domain.StationsRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * Answers [result] and records every `forceRefresh` it receives. Set [gate] to
 * hold the next calls "in flight" until the test completes it.
 */
internal class FakeStationsRepository(var result: DataResult<List<Station>>) : StationsRepository {
    val forceRefreshCalls = mutableListOf<Boolean>()

    var gate: CompletableDeferred<Unit>? = null

    override suspend fun stations(forceRefresh: Boolean): DataResult<List<Station>> {
        forceRefreshCalls += forceRefresh
        gate?.await()
        return result
    }
}
