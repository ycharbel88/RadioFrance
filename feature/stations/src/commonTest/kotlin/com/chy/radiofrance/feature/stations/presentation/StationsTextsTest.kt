package com.chy.radiofrance.feature.stations.presentation

import com.chy.radiofrance.core.model.DataError
import com.chy.radiofrance.core.model.StationKind.NATIONAL
import com.chy.radiofrance.core.player.PlaybackError
import com.chy.radiofrance.core.player.PlayerState
import com.chy.radiofrance.feature.stations.domain.SectionHeader
import com.chy.radiofrance.feature.stations.station
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StationsTextsTest {
    private val allErrors = listOf(
        DataError.MissingApiKey,
        DataError.InvalidApiKey,
        DataError.QuotaExceeded,
        DataError.Network,
        DataError.Server(httpStatus = 500)
    )

    @Test
    fun `retry is offered only when the problem is temporary`() {
        val retryable = allErrors.filter { errorUi(it).canRetry }

        assertEquals(listOf(DataError.Network, DataError.Server(500)), retryable)
    }

    @Test
    fun `a server error has the same text whatever the status`() {
        assertEquals(errorUi(DataError.Server(500)), errorUi(DataError.Server(null)))
    }

    @Test
    fun `each data error has its own message`() {
        assertEquals(allErrors.size, allErrors.map { errorUi(it).message }.distinct().size)
    }

    @Test
    fun `each section kind has its own title`() {
        val headers = listOf(
            SectionHeader.Stations,
            SectionHeader.Webradios("FIP", "FIP"),
            SectionHeader.Locals("FRANCEBLEU", "ICI")
        )

        assertEquals(3, headers.map { headerResource(it) }.distinct().size)
    }

    @Test
    fun `each playback error has its own message`() {
        val errors = PlaybackError.entries

        assertEquals(errors.size, errors.map { playbackErrorResource(it) }.distinct().size)
    }

    @Test
    fun `idle has no state description and every other state has its own`() {
        val fip = station("FIP", NATIONAL, "FIP")
        val states = listOf(
            PlayerState.Buffering(fip),
            PlayerState.Playing(fip),
            PlayerState.Paused(fip),
            PlayerState.Error(fip, PlaybackError.Network)
        )

        assertNull(playerStateResource(PlayerState.Idle))
        assertEquals(states.size, states.map { playerStateResource(it) }.distinct().size)
    }
}
