// core:player: the `AudioPlayer` interface and its platform implementations
// (Media3 on Android; for now an `UnavailableAudioPlayer` stub on iOS).
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
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(projects.core.model)
        }
    }
}

ktlint {
    version = libs.versions.ktlint
    // Skip generated sources (BuildKonfig, Compose resources). We use
    // invariantSeparatorsPath because on Windows the raw path contains backslashes.
    filter { exclude { it.file.invariantSeparatorsPath.contains("/build/") } }
}
