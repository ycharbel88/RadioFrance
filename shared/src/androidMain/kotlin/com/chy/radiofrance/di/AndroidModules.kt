package com.chy.radiofrance.di

import com.chy.radiofrance.core.player.androidPlayerModule

/**
 * The Android-only Koin modules, passed to [initKoin] by the Application. They live
 * here because androidApp only sees `shared`, not the core modules.
 */
val androidPlatformModules = listOf(androidPlayerModule)
