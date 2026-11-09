// core:network: the Radio France Open API client (Apollo GraphQL, from step 2).
// Why a module: it is the only place that knows about GraphQL, Apollo, Ktor and the
// API key. Generated Apollo code stays `internal`, and only core:model types leave
// this module, so the transport can change without touching features.
// Rule: depends on core:model only.

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.io.StringReader
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Same KMP + Android-library setup in every module, duplicated on purpose.
// Next step when modules multiply: build-logic convention plugins.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.ktlint)
}

kotlin {
    android {
        namespace = "com.chy.radiofrance.core.network"
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
            implementation(projects.core.model)
        }
    }
}

// The API key comes from local.properties, which is never committed.
// We read it through `providers` so the configuration cache tracks the file:
// editing the key invalidates the cache. A missing file or key gives "",
// so the build still passes in CI. The app reports a missing key at runtime.
val apiKey: String =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText
        .map { text -> Properties().apply { load(StringReader(text)) } }
        .map { props -> props.getProperty("RADIOFRANCE_API_KEY").orEmpty() }
        .getOrElse("")

buildkonfig {
    packageName = "com.chy.radiofrance.core.network"
    // The generated object is `internal` by default, so the key cannot leave this module.
    defaultConfigs {
        buildConfigField(STRING, "RADIOFRANCE_API_KEY", apiKey)
    }
}

ktlint {
    version = libs.versions.ktlint
    // Skip generated sources (BuildKonfig, Compose resources). We use
    // invariantSeparatorsPath because on Windows the raw path contains backslashes.
    filter { exclude { it.file.invariantSeparatorsPath.contains("/build/") } }
}
