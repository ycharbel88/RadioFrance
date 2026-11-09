package com.chy.radiofrance

import androidx.compose.ui.window.ComposeUIViewController

// PascalCase on purpose: Swift calls MainViewControllerKt.MainViewController(),
// and the name reads as "the main view controller" on the iOS side.
@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App() }
