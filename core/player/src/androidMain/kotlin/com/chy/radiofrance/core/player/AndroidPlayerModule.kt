package com.chy.radiofrance.core.player

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * The Android player binding. Must be loaded **after** [playerModule]: it replaces
 * its `UnavailableAudioPlayer` default (Koin keeps the last definition loaded).
 *
 * One instance per process, like the service it talks to. androidContext() is the
 * Application, never an Activity.
 */
val androidPlayerModule = module {
    single<AudioPlayer> { MediaControllerAudioPlayer(androidContext()) } onClose {
        (it as? MediaControllerAudioPlayer)?.release()
    }
}
