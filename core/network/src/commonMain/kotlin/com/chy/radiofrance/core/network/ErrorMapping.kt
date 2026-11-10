package com.chy.radiofrance.core.network

import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloOfflineException
import com.apollographql.apollo.exception.CacheMissException
import com.chy.radiofrance.core.model.DataError

/**
 * Apollo failure → [DataError].
 *
 * With CacheFirst, a cache miss followed by a network failure comes back as the
 * CacheMissException, with the network failure in `suppressed`. The cache miss
 * is expected and says nothing, so we look for the real cause first.
 */
internal fun ApolloException.toDataError(): DataError {
    val cause =
        (listOf<Throwable>(this) + suppressedExceptions).firstOrNull { it !is CacheMissException }
            ?: this
    return when (cause) {
        is ApolloHttpException -> httpStatusToDataError(cause.statusCode)
        is ApolloNetworkException, is ApolloOfflineException -> DataError.Network
        else -> DataError.Server(httpStatus = null)
    }
}

internal fun httpStatusToDataError(status: Int): DataError = when (status) {
    // Observed: 403 + {"message":"Unauthorized: Invalid x-token"} (2026-11-10).
    403 -> DataError.InvalidApiKey

    // Assumed HTTP standard for rate limiting, not observed.
    429 -> DataError.QuotaExceeded

    else -> DataError.Server(httpStatus = status)
}
