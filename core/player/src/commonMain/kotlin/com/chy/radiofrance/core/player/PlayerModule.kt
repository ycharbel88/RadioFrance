package com.chy.radiofrance.core.player

import org.koin.dsl.module

/**
 * Koin bindings of core:player that are shared by every platform. Empty for now.
 * The platform `AudioPlayer` (Media3 on Android, a stub on iOS) will be bound in
 * a platform Koin module, not with expect/actual classes.
 */
val playerModule = module { }
