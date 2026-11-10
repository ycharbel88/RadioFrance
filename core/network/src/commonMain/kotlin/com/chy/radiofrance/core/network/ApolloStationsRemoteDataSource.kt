package com.chy.radiofrance.core.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.fetchPolicy
import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.network.graphql.StationsQuery

internal class ApolloStationsRemoteDataSource(
    private val apolloClient: ApolloClient,
    private val apiKey: String
) : StationsRemoteDataSource {
    override suspend fun stations(mode: FetchMode): DataResult<List<Station>> {
        // CI and fresh clones have no key: say so clearly, and spend no request.
        if (apiKey.isBlank()) return DataResult.Failure(DataError.MissingApiKey)

        val response = apolloClient
            .query(StationsQuery())
            .fetchPolicy(mode.toFetchPolicy())
            .execute()

        val data = response.data
        val exception = response.exception
        return when {
            data != null -> DataResult.Success(data.toStations())

            exception != null -> DataResult.Failure(exception.toDataError())

            // HTTP 200 carrying GraphQL errors and no data.
            else -> DataResult.Failure(DataError.Server(httpStatus = null))
        }
    }
}

private fun FetchMode.toFetchPolicy(): FetchPolicy = when (this) {
    FetchMode.CacheFirst -> FetchPolicy.CacheFirst
    FetchMode.NetworkOnly -> FetchPolicy.NetworkOnly
}
