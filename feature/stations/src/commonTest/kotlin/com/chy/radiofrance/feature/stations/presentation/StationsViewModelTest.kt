package com.chy.radiofrance.feature.stations.presentation

import app.cash.turbine.test
import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.model.DataResult
import com.chy.radiofrance.core.model.Station
import com.chy.radiofrance.core.model.StationKind.NATIONAL
import com.chy.radiofrance.core.player.PlaybackError
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.core.testing.FakeAudioPlayer
import com.chy.radiofrance.core.testing.FakeAudioPlayer.Call
import com.chy.radiofrance.feature.stations.FakeStationsRepository
import com.chy.radiofrance.feature.stations.domain.GetStationSectionsUseCase
import com.chy.radiofrance.feature.stations.domain.SectionHeader
import com.chy.radiofrance.feature.stations.domain.StationSection
import com.chy.radiofrance.feature.stations.station
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * The ViewModel gets the real [GetStationSectionsUseCase] on top of a fake
 * repository: the use case is its only data dependency, and its rule is tested
 * on its own. A [CompletableDeferred] gate holds a request "in flight" so each
 * intermediate state can be observed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StationsViewModelTest {
    private val fip = station("FIP", NATIONAL, "FIP")
    private val inter = station("FRANCEINTER", NATIONAL, "FRANCEINTER", "France Inter")

    private val repository = FakeStationsRepository(DataResult.Success(listOf(fip)))
    private val player = FakeAudioPlayer()

    // viewModelScope runs on Dispatchers.Main, which doesn't exist in a unit test.
    // runTest reuses this dispatcher's scheduler, so both share one virtual clock.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = StationsViewModel(GetStationSectionsUseCase(repository), player)

    private fun loaded(vararg stations: Station) =
        StationsContent.Loaded(listOf(StationSection(SectionHeader.Stations, stations.toList())))

    private fun holdNextRequest() = CompletableDeferred<Unit>().also { repository.gate = it }

    // --- Loading ---

    @Test
    fun `starts loading then shows the sections from the cache`() = runTest {
        val request = holdNextRequest()
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(StationsContent.Loading, awaitItem().content)
            request.complete(Unit)
            assertEquals(loaded(fip), awaitItem().content)
        }
        assertEquals(listOf(false), repository.forceRefreshCalls)
    }

    @Test
    fun `no station gives the empty state`() = runTest {
        repository.result = DataResult.Success(emptyList())

        viewModel().uiState.test {
            assertEquals(StationsContent.Empty, awaitItem().content)
        }
    }

    @Test
    fun `a failed load gives the error state`() = runTest {
        repository.result = DataResult.Failure(DataError.MissingApiKey)

        viewModel().uiState.test {
            assertEquals(StationsContent.Error(DataError.MissingApiKey), awaitItem().content)
        }
    }

    @Test
    fun `retry after an error goes through loading to the list`() = runTest {
        repository.result = DataResult.Failure(DataError.Network)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(StationsContent.Error(DataError.Network), awaitItem().content)

            repository.result = DataResult.Success(listOf(fip))
            val request = holdNextRequest()
            viewModel.onEvent(StationsEvent.Retry)
            assertEquals(StationsContent.Loading, awaitItem().content)

            request.complete(Unit)
            assertEquals(loaded(fip), awaitItem().content)
        }
        assertEquals(listOf(false, false), repository.forceRefreshCalls)
    }

    @Test
    fun `retry from the empty state loads again`() = runTest {
        repository.result = DataResult.Success(emptyList())
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(StationsContent.Empty, awaitItem().content)

            repository.result = DataResult.Success(listOf(fip))
            viewModel.onEvent(StationsEvent.Retry)

            assertEquals(loaded(fip), expectMostRecentItem().content)
        }
    }

    // --- Refresh ---

    @Test
    fun `refresh keeps the list while it asks the network then shows the new list`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(loaded(fip), awaitItem().content)

            repository.result = DataResult.Success(listOf(fip, inter))
            val request = holdNextRequest()
            viewModel.onEvent(StationsEvent.Refresh)
            val refreshing = awaitItem()
            assertEquals(true, refreshing.isRefreshing)
            assertEquals(loaded(fip), refreshing.content)

            request.complete(Unit)
            val refreshed = awaitItem()
            assertEquals(false, refreshed.isRefreshing)
            assertEquals(loaded(fip, inter), refreshed.content)
        }
        assertEquals(listOf(false, true), repository.forceRefreshCalls)
    }

    @Test
    fun `a failed refresh keeps the list and reports the error until the next refresh`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(1)

            repository.result = DataResult.Failure(DataError.Network)
            val failing = holdNextRequest()
            viewModel.onEvent(StationsEvent.Refresh)
            skipItems(1) // refreshing
            failing.complete(Unit)
            val failed = awaitItem()
            assertEquals(loaded(fip), failed.content)
            assertEquals(false, failed.isRefreshing)
            assertEquals(DataError.Network, failed.refreshError)

            repository.result = DataResult.Success(listOf(fip))
            val retrying = holdNextRequest()
            viewModel.onEvent(StationsEvent.Refresh)
            val refreshing = awaitItem()
            assertEquals(true, refreshing.isRefreshing)
            assertNull(refreshing.refreshError)

            retrying.complete(Unit)
            assertNull(awaitItem().refreshError)
        }
    }

    @Test
    fun `a refresh while one is running sends no second request`() = runTest {
        val viewModel = viewModel()
        val request = holdNextRequest()

        viewModel.onEvent(StationsEvent.Refresh)
        viewModel.onEvent(StationsEvent.Refresh)
        request.complete(Unit)

        assertEquals(listOf(false, true), repository.forceRefreshCalls)
    }

    @Test
    fun `retry and refresh during the first load send no second request`() = runTest {
        val request = holdNextRequest()
        val viewModel = viewModel()

        viewModel.onEvent(StationsEvent.Retry)
        viewModel.onEvent(StationsEvent.Refresh)
        request.complete(Unit)

        assertEquals(listOf(false), repository.forceRefreshCalls)
    }

    @Test
    fun `refresh does nothing in the error state where retry is the action`() = runTest {
        repository.result = DataResult.Failure(DataError.Network)
        val viewModel = viewModel()

        // uiState is WhileSubscribed: without a collector, `.value` never moves past
        // its initial value. Always read it through a collector (Turbine).
        viewModel.uiState.test {
            viewModel.onEvent(StationsEvent.Refresh)

            val state = expectMostRecentItem()
            assertEquals(StationsContent.Error(DataError.Network), state.content)
            assertEquals(false, state.isRefreshing)
        }
        assertEquals(listOf(false), repository.forceRefreshCalls)
    }

    // --- Player ---

    @Test
    fun `clicking a station plays it and the state follows the player`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(PlayerState.Idle, awaitItem().player)

            viewModel.onEvent(StationsEvent.StationClicked(fip))

            val playing = awaitItem()
            assertEquals(PlayerState.Playing(fip), playing.player)
            assertEquals("FIP", playing.currentStationId)
        }
        assertEquals(listOf<Call>(Call.Play(fip)), player.calls)
    }

    @Test
    fun `every player state reaches the ui state`() = runTest {
        val viewModel = viewModel()
        val states = listOf(
            PlayerState.Buffering(fip),
            PlayerState.Playing(fip),
            PlayerState.Paused(fip),
            PlayerState.Error(fip, PlaybackError.StreamUnavailable),
            PlayerState.Idle
        )

        viewModel.uiState.test {
            assertEquals(PlayerState.Idle, awaitItem().player)
            for (state in states) {
                player.emit(state)
                val ui = awaitItem()
                assertEquals(state, ui.player)
                assertEquals(if (state == PlayerState.Idle) null else "FIP", ui.currentStationId)
            }
        }
    }

    /**
     * Every player state x every player event -> the command sent to the player.
     * Rules: a tap on the station already on (playing or buffering) never restarts
     * it; another station always switches; toggle pauses what is on and resumes
     * (play = reconnect to live) what is paused or failed; nothing to toggle when idle.
     * A NotSupported error still sends play: the mini-player hides its button there.
     */
    @Test
    fun `each player state turns each player event into the right command`() = runTest {
        val clickSame = StationsEvent.StationClicked(fip)
        val clickOther = StationsEvent.StationClicked(inter)
        val toggle = StationsEvent.TogglePlayPause
        val switch = listOf<Call>(Call.Play(inter))
        val replay = listOf<Call>(Call.Play(fip))
        val pause = listOf<Call>(Call.Pause)
        val nothing = emptyList<Call>()

        val table: Map<PlayerState, Map<StationsEvent, List<Call>>> = mapOf(
            PlayerState.Idle to mapOf(clickSame to replay, clickOther to switch, toggle to nothing),
            PlayerState.Buffering(fip) to
                mapOf(clickSame to nothing, clickOther to switch, toggle to pause),
            PlayerState.Playing(fip) to
                mapOf(clickSame to nothing, clickOther to switch, toggle to pause),
            PlayerState.Paused(fip) to
                mapOf(clickSame to replay, clickOther to switch, toggle to replay),
            PlayerState.Error(fip, PlaybackError.Network) to
                mapOf(clickSame to replay, clickOther to switch, toggle to replay),
            PlayerState.Error(fip, PlaybackError.NotSupported) to
                mapOf(clickSame to replay, clickOther to switch, toggle to replay)
        )

        for ((state, cases) in table) {
            for ((event, expected) in cases) {
                // A fresh player per case, so calls from one case don't leak into the next.
                val fake = FakeAudioPlayer()
                val viewModel = StationsViewModel(GetStationSectionsUseCase(repository), fake)
                fake.emit(state)

                viewModel.onEvent(event)

                assertEquals(expected, fake.calls, "$state + $event")
            }
        }
    }
}
