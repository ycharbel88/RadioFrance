package com.chy.radiofrance

import androidx.compose.ui.window.ComposeUIViewController
import com.chy.radiofrance.di.initKoin
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

// PascalCase on purpose: Swift calls MainViewControllerKt.MainViewController(),
// and the name reads as "the main view controller" on the iOS side.
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    // iOS has no Application class like Android. This is the first Kotlin code
    // Swift calls, so DI starts here. SwiftUI can create the controller more
    // than once, and startKoin throws if Koin is already started.
    if (KoinPlatform.getKoinOrNull() == null) initKoin()
    return ComposeUIViewController { App() }
}
