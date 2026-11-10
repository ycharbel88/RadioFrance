package com.chy.radiofrance.feature.stations.domain

import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station

/** The stations, wherever they come from. Only [GetStationSectionsUseCase] calls it. */
interface StationsRepository {
    /**
     * @param forceRefresh false: cached list if there is one (0 requests).
     *   true: always ask the API (pull-to-refresh).
     */
    suspend fun stations(forceRefresh: Boolean): DataResult<List<Station>>
}
