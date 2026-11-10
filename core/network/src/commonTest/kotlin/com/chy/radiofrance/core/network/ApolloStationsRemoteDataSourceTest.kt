package com.chy.radiofrance.core.network

import com.apollographql.apollo.exception.ApolloNetworkException
import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ApolloStationsRemoteDataSourceTest {
    private val apiKey = "00000000-0000-0000-0000-000000000000"

    private fun dataSource(engine: FakeHttpEngine, key: String = apiKey) =
        ApolloStationsRemoteDataSource(buildApolloClient(key, engine), key)

    private fun DataResult<List<Station>>.stations(): List<Station> =
        assertIs<DataResult.Success<List<Station>>>(this).value

    private fun DataResult<List<Station>>.error(): DataError =
        assertIs<DataResult.Failure>(this).error

    // --- Mapping (recorded response) ---

    @Test
    fun `maps the recorded response to 77 stations`() = runTest {
        val stations = dataSource(FakeHttpEngine { jsonResponse(200, STATIONS_RESPONSE_JSON) })
            .stations(FetchMode.NetworkOnly)
            .stations()

        assertEquals(77, stations.size)
        assertEquals(6, stations.count { it.kind == StationKind.NATIONAL })
        assertEquals(26, stations.count { it.kind == StationKind.WEBRADIO })
        assertEquals(45, stations.count { it.kind == StationKind.LOCAL })
    }

    @Test
    fun `a brand without a stream is not a station but its radios are`() = runTest {
        val stations = dataSource(FakeHttpEngine { jsonResponse(200, STATIONS_RESPONSE_JSON) })
            .stations(FetchMode.NetworkOnly)
            .stations()

        assertTrue(stations.none { it.id == "FRANCEBLEU" })
        val alsace = stations.single { it.id == "FRANCEBLEU_ALSACE" }
        assertEquals(StationKind.LOCAL, alsace.kind)
        assertEquals("FRANCEBLEU", alsace.brandId)
        assertEquals("ICI", alsace.brandName)
    }

    @Test
    fun `keeps API order with each brand before its webradios`() = runTest {
        val stations = dataSource(FakeHttpEngine { jsonResponse(200, STATIONS_RESPONSE_JSON) })
            .stations(FetchMode.NetworkOnly)
            .stations()

        assertEquals("FRANCEINTER", stations[0].id)
        assertEquals("FRANCEINTER_LA_MUSIQUE_INTER", stations[1].id)
        assertEquals(StationKind.WEBRADIO, stations[1].kind)
        assertEquals("France Inter", stations[1].brandName)
    }

    @Test
    fun `stream URLs are kept as given including the France Culture lofi one`() = runTest {
        val stations = dataSource(FakeHttpEngine { jsonResponse(200, STATIONS_RESPONSE_JSON) })
            .stations(FetchMode.NetworkOnly)
            .stations()

        assertEquals(
            "https://icecast.radiofrance.fr/franceculture-lofi.mp3?id=openapi",
            stations.single { it.id == "FRANCECULTURE" }.streamUrl
        )
    }

    @Test
    fun `descriptions are trimmed and blank ones become null`() = runTest {
        val stations = dataSource(FakeHttpEngine { jsonResponse(200, STATIONS_RESPONSE_JSON) })
            .stations(FetchMode.NetworkOnly)
            .stations()

        // The API sends a trailing space here.
        val inter = stations.single { it.id == "FRANCEINTER" }.description
        assertEquals(inter?.trim(), inter)
        assertTrue(inter.orEmpty().startsWith("Joyeuse, savante et populaire"))
        // The API sends "" for most local radios.
        assertNull(stations.single { it.id == "FRANCEBLEU_ALSACE" }.description)
    }

    // --- Auth ---

    @Test
    fun `sends the key in the X-Token header and not in the URL`() = runTest {
        val engine = FakeHttpEngine { jsonResponse(200, STATIONS_RESPONSE_JSON) }

        dataSource(engine).stations(FetchMode.NetworkOnly)

        val request = engine.requests.single()
        assertEquals(apiKey, request.headers.single { it.name == "X-Token" }.value)
        assertFalse(request.url.contains(apiKey))
    }

    @Test
    fun `blank key gives MissingApiKey without any request`() = runTest {
        val engine = FakeHttpEngine { error("must not be called") }

        val result = dataSource(engine, key = " ").stations(FetchMode.CacheFirst)

        assertEquals(DataError.MissingApiKey, result.error())
        assertTrue(engine.requests.isEmpty())
    }

    // --- Errors ---

    @Test
    fun `403 with the recorded body gives InvalidApiKey in both modes`() = runTest {
        // CacheFirst also checks that the expected cache miss does not hide the 403.
        for (mode in FetchMode.entries) {
            val result = dataSource(
                FakeHttpEngine {
                    jsonResponse(403, INVALID_KEY_RESPONSE_JSON)
                }
            ).stations(mode)

            assertEquals(DataError.InvalidApiKey, result.error(), "mode $mode")
        }
    }

    @Test
    fun `429 gives QuotaExceeded`() = runTest {
        val result = dataSource(
            FakeHttpEngine {
                jsonResponse(429, """{"message":"Too Many Requests"}""")
            }
        )
            .stations(FetchMode.CacheFirst)

        assertEquals(DataError.QuotaExceeded, result.error())
    }

    @Test
    fun `other HTTP statuses give Server with the status`() = runTest {
        val result = dataSource(FakeHttpEngine { jsonResponse(500, """{"message":"boom"}""") })
            .stations(FetchMode.CacheFirst)

        assertEquals(DataError.Server(httpStatus = 500), result.error())
    }

    @Test
    fun `GraphQL errors without data give Server without status`() = runTest {
        // Shape from the GraphQL spec; not observed on this API.
        val result = dataSource(
            FakeHttpEngine {
                jsonResponse(200, """{"data":null,"errors":[{"message":"internal"}]}""")
            }
        ).stations(FetchMode.NetworkOnly)

        assertEquals(DataError.Server(httpStatus = null), result.error())
    }

    @Test
    fun `connection failure gives Network`() = runTest {
        val result = dataSource(
            FakeHttpEngine {
                throw ApolloNetworkException("Failed to connect")
            }
        )
            .stations(FetchMode.CacheFirst)

        assertEquals(DataError.Network, result.error())
    }

    // --- Cache ---

    @Test
    fun `CacheFirst hits the network once and NetworkOnly every time`() = runTest {
        val engine = FakeHttpEngine { jsonResponse(200, STATIONS_RESPONSE_JSON) }
        val dataSource = dataSource(engine)

        val first = dataSource.stations(FetchMode.CacheFirst).stations()
        val second = dataSource.stations(FetchMode.CacheFirst).stations()
        assertEquals(1, engine.requests.size)
        assertEquals(first, second)

        dataSource.stations(FetchMode.NetworkOnly)
        assertEquals(2, engine.requests.size)
    }
}
