package com.chy.radiofrance.di

import com.chy.radiofrance.core.network.networkModule
import com.chy.radiofrance.core.player.playerModule
import com.chy.radiofrance.feature.stations.stationsModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/** Every Koin module of the app. The shell is the only place that knows all of them. */
val appModules = listOf(networkModule, playerModule, stationsModule)

/**
 * Starts Koin once per process. Called from the Android `Application` and from the
 * iOS `MainViewController()`. [config] lets each platform add its own setup
 * (Android context, logger, platform modules).
 */
fun initKoin(config: KoinAppDeclaration = {}) {
    startKoin {
        config()
        modules(appModules)
    }
}
