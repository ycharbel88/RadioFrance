package com.chy.radiofrance.feature.stations

import com.chy.radiofrance.core.network.StationsRemoteDataSource
import com.chy.radiofrance.core.player.playerModule
import kotlin.test.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Checks, without creating anything, that every constructor parameter in the
 * stations module has a binding. Lives in androidHostTest, not commonTest:
 * `verify()` reads constructors by JVM reflection, which doesn't exist on iOS.
 */
class StationsModuleVerifyTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `stations module is complete with the player module`() {
        // playerModule is included, not listed as external: AudioPlayer must
        // really be bound. Only the network data source comes from outside.
        module { includes(stationsModule, playerModule) }
            .verify(extraTypes = listOf(StationsRemoteDataSource::class))
    }
}
