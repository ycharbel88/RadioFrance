// feature:stations: the station list screen and its ViewModel.
// Why a module: a feature is a vertical slice that can grow, or be removed, on its
// own. The `shared` shell only wires it in.
// Rule: depends on core:* only, never on another feature.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Same KMP + Android-library setup in every module, duplicated on purpose.
// Next step when modules multiply: build-logic convention plugins.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ktlint)
}

kotlin {
    android {
        namespace = "com.chy.radiofrance.feature.stations"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        // Runs commonTest on the JVM: `:feature:stations:testAndroidHostTest`.
        withHostTest { }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            // feature:* depends on core:* only, never on another feature.
            implementation(projects.core.model)
            implementation(projects.core.network)
            // `api`: StationsUiState exposes PlayerState to the shell's mini-player.
            api(projects.core.player)
            implementation(projects.core.designsystem)
            implementation(libs.compose.runtime)
            // Multiplatform androidx ViewModel (viewModelScope = main thread on iOS too).
            implementation(libs.androidx.lifecycle.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(projects.core.testing)
        }
        // JVM-only tests: Koin's `verify()` uses reflection, which iOS doesn't have.
        getByName("androidHostTest").dependencies {
            implementation(libs.koin.test)
        }
    }
}

ktlint {
    version = libs.versions.ktlint
    // Skip generated sources (BuildKonfig, Compose resources). We use
    // invariantSeparatorsPath because on Windows the raw path contains backslashes.
    filter { exclude { it.file.invariantSeparatorsPath.contains("/build/") } }
}
