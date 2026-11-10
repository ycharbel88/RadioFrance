package com.chy.radiofrance.core.network

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain

/**
 * Adds the API key to every request. A header rather than the `x-token` query
 * parameter (both work, checked on 2026-11-10): URLs end up in logs and proxies.
 */
internal class ApiKeyInterceptor(private val apiKey: String) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse = chain.proceed(request.newBuilder().addHeader(HEADER, apiKey).build())

    companion object {
        const val HEADER = "X-Token"
    }
}
