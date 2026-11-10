// core:player: the `AudioPlayer` interface and its implementations. The default
// binding is `UnavailableAudioPlayer` (commonMain), which iOS keeps for now;
// Android overrides it with Media3.
// Why a module: playback outlives the screen (background, notification, lock
// screen) and has to be testable with a fake. The UI only ever sees the interface.
// Rule: knows nothing about UI or Compose. Depends on core:model only.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Same KMP + Android-library setup in every module, duplicated on purpose.
// Next step when modules multiply: build-logic convention plugins.
// No Compose plugin here: the player must not know about UI.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.ktlint)
}

kotlin {
    android {
        namespace = "com.chy.radiofrance.core.player"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        // Runs commonTest on the JVM: `:core:player:testAndroidHostTest`.
        withHostTest { }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            // `api`: AudioPlayer and PlayerState expose Station and StateFlow.
            api(projects.core.model)
            api(libs.kotlinx.coroutines.core)
        }
        // Android player: Media3 runs ExoPlayer inside a MediaSessionService, and the
        // app talks to it through a MediaController. None of these types leave the module.
        androidMain.dependencies {
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.session)
            implementation(libs.kotlinx.coroutines.guava)
            // androidContext() in the Android Koin module.
            implementation(libs.koin.android)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

ktlint {
    version = libs.versions.ktlint
    // Skip generated sources (BuildKonfig, Compose resources). We use
    // invariantSeparatorsPath because on Windows the raw path contains backslashes.
    filter { exclude { it.file.invariantSeparatorsPath.contains("/build/") } }
}
