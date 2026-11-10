package com.chy.radiofrance.core.network

import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station

/**
 * The Radio France API, as seen by the rest of the app: core:model types in,
 * core:model types out. Nothing about GraphQL or Apollo leaks through it.
 */
interface StationsRemoteDataSource {
    /** Every playable station, in API order (each brand, then its webradios, then its local radios). */
    suspend fun stations(mode: FetchMode): DataResult<List<Station>>
}

/** Where the data may come from. Our own enum, so callers never see Apollo's `FetchPolicy`. */
enum class FetchMode {
    /** Memory cache if present, network otherwise. Costs 0 requests after the first load. */
    CacheFirst,

    /** Always the network (pull-to-refresh). Updates the cache. */
    NetworkOnly
}
