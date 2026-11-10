// core:testing: test doubles shared by several modules' tests (FakeAudioPlayer).
// Why a module: KMP has no Gradle `testFixtures`, and a fake in core:player's
// commonMain would ship in the app. Here it is only ever added to `commonTest`.
// Rule: never a dependency of a main source set. Depends on core:* only.

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
        namespace = "com.chy.radiofrance.core.testing"
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
            // `api`: the fakes implement core:player interfaces, which tests use directly.
            api(projects.core.player)
            api(projects.core.model)
        }
    }
}

ktlint {
    version = libs.versions.ktlint
    // Skip generated sources. We use invariantSeparatorsPath because on Windows
    // the raw path contains backslashes.
    filter { exclude { it.file.invariantSeparatorsPath.contains("/build/") } }
}
