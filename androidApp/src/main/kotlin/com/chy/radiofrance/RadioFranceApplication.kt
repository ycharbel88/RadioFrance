package com.chy.radiofrance

import android.app.Application
import com.chy.radiofrance.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/**
 * Starts DI before any Activity or (later) the Media3 playback service, since
 * Android can start the service without starting the UI.
 */
class RadioFranceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@RadioFranceApplication)
        }
    }
}
