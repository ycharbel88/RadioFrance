package com.chy.radiofrance.di

import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind
import com.chy.radiofrance.core.network.FetchMode
import com.chy.radiofrance.core.network.StationsRemoteDataSource
import com.chy.radiofrance.core.player.AudioPlayer
import com.chy.radiofrance.core.player.PlaybackError
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.feature.stations.presentation.StationsViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * The real app graph ([appModules]) resolves the stations screen. Only the
 * network data source is replaced, so the test needs no key and sends no
 * request. Overriding a binding from a module loaded later is also how a
 * platform replaces the default player (Android, step 5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppGraphTest {
    private class FakeStationsRemoteDataSource : StationsRemoteDataSource {
        val modes = mutableListOf<FetchMode>()

        override suspend fun stations(mode: FetchMode): DataResult<List<Station>> {
            modes += mode
            return DataResult.Success(emptyList())
        }
    }

    private val remote = FakeStationsRemoteDataSource()
    private lateinit var app: KoinApplication

    // The ViewModel starts loading in viewModelScope (Dispatchers.Main) as soon as
    // it is created.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        app = koinApplication {
            modules(appModules + module { single<StationsRemoteDataSource> { remote } })
        }
    }

    @AfterTest
    fun tearDown() {
        app.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `the app graph resolves the stations view model`() {
        app.koin.get<StationsViewModel>()

        // Its first load went through the real use case and repository to the
        // (overridden) data source, cache first.
        assertEquals(listOf(FetchMode.CacheFirst), remote.modes)
    }

    @Test
    fun `the default player is the honest unavailable one`() {
        val player = app.koin.get<AudioPlayer>()
        val fip = Station(
            id = "FIP",
            name = "FIP",
            description = null,
            streamUrl = "https://icecast.radiofrance.fr/fip-hifi.aac",
            kind = StationKind.NATIONAL,
            brandId = "FIP",
            brandName = "FIP"
        )

        player.play(fip)

        assertEquals(PlayerState.Error(fip, PlaybackError.NotSupported), player.state.value)
    }
}
