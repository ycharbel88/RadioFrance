// core:model: the domain types every layer shares (for example `Station`).
// Why a module: network, player and features all talk about the same concepts.
// Putting them here lets those modules share the vocabulary without depending
// on each other, and without pulling in Apollo.
// Rule: no dependencies at all. No Apollo, no Compose, no Koin.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Same KMP + Android-library setup in every module, duplicated on purpose.
// Next step when modules multiply: build-logic convention plugins.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.ktlint)
}

kotlin {
    android {
        namespace = "com.chy.radiofrance.core.model"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()
}

ktlint {
    version = libs.versions.ktlint
    // Skip generated sources (BuildKonfig, Compose resources). We use
    // invariantSeparatorsPath because on Windows the raw path contains backslashes.
    filter { exclude { it.file.invariantSeparatorsPath.contains("/build/") } }
}
