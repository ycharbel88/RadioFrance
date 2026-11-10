package com.chy.radiofrance.core.network

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpEngine
import okio.Buffer

/**
 * Replaces OkHttp / NSURLSession under a real Apollo client. Everything above it
 * runs for real: our interceptor, HTTP status handling, JSON parsing, the cache.
 * No socket is opened.
 */
internal class FakeHttpEngine(private val respond: (HttpRequest) -> HttpResponse) : HttpEngine {
    val requests = mutableListOf<HttpRequest>()

    override suspend fun execute(request: HttpRequest): HttpResponse {
        requests += request
        return respond(request)
    }
}

internal fun jsonResponse(status: Int, body: String): HttpResponse = HttpResponse
    .Builder(statusCode = status)
    .addHeader("Content-Type", "application/json; charset=utf-8")
    // A fresh Buffer per response: a BufferedSource can be read only once.
    .body(Buffer().writeUtf8(body))
    .build()
