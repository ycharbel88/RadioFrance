package com.chy.radiofrance.di

import com.chy.radiofrance.core.network.networkModule
import com.chy.radiofrance.core.player.playerModule
import com.chy.radiofrance.feature.stations.stationsModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/** Every Koin module of the app. The shell is the only place that knows all of them. */
val appModules = listOf(networkModule, playerModule, stationsModule)

/**
 * Starts Koin once per process. Called from the Android `Application` and from the
 * iOS `MainViewController()`.
 *
 * @param platformModules loaded **after** [appModules], so they replace the common
 *   defaults (Koin keeps the last definition). Android passes its Media3 player
 *   here; iOS passes nothing and keeps the default player.
 * @param config each platform's own setup (Android context, logger).
 */
fun initKoin(platformModules: List<Module> = emptyList(), config: KoinAppDeclaration = {}) {
    startKoin {
        config()
        modules(appModules + platformModules)
    }
}
