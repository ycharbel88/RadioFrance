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
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            // feature:* depends on core:* only, never on another feature.
            implementation(projects.core.model)
            implementation(projects.core.designsystem)
            implementation(libs.compose.runtime)
        }
    }
}

ktlint {
    version = libs.versions.ktlint
    // Skip generated sources (BuildKonfig, Compose resources). We use
    // invariantSeparatorsPath because on Windows the raw path contains backslashes.
    filter { exclude { it.file.invariantSeparatorsPath.contains("/build/") } }
}
