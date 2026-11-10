package com.chy.radiofrance.core.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.http.HttpEngine
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.normalizedCache

internal const val RADIO_FRANCE_URL = "https://openapi.radiofrance.fr/v1/graphql"

/**
 * The one Apollo client of the app.
 *
 * @param httpEngine OkHttp on Android, NSURLSession on iOS. Tests pass a fake one.
 */
internal fun buildApolloClient(
    apiKey: String,
    httpEngine: HttpEngine = DefaultHttpEngine()
): ApolloClient = ApolloClient
    .Builder()
    .serverUrl(RADIO_FRANCE_URL)
    .httpEngine(httpEngine)
    .addHttpInterceptor(ApiKeyInterceptor(apiKey))
    // Memory only: the whole list is ~18 KB and costs 1 request to reload.
    // Empty policies: no @typePolicy, so records are keyed by their path.
    .normalizedCache(
        normalizedCacheFactory = MemoryCacheFactory(maxSizeBytes = 1024 * 1024),
        typePolicies = emptyMap(),
        fieldPolicies = emptyMap()
    ).build()
