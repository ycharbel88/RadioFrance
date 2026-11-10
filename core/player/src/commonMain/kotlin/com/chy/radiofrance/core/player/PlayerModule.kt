package com.chy.radiofrance.core.player

import org.koin.dsl.module

/**
 * Koin bindings of core:player shared by every platform.
 *
 * [UnavailableAudioPlayer] is the default, so the graph is complete on every
 * platform at every step. A platform with a real player overrides the binding
 * from its own module, loaded after this one (Android: `androidPlayerModule`, Media3). iOS
 * keeps the default for now.
 */
val playerModule = module {
    single<AudioPlayer> { UnavailableAudioPlayer() }
}
