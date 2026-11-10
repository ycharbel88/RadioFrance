// core:network: the Radio France Open API client (Apollo GraphQL).
// Why a module: it is the only place that knows about GraphQL, Apollo and the
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
    alias(libs.plugins.apollo)
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
        // Runs commonTest on the JVM: `:core:network:testAndroidHostTest`.
        withHostTest { }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            // `api`: StationsRemoteDataSource returns core:model types.
            api(projects.core.model)
            // Apollo picks its HTTP engine per platform: OkHttp on Android,
            // NSURLSession on iOS. No Ktor.
            implementation(libs.apollo.runtime)
            implementation(libs.apollo.normalizedCache)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
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
        // Properties keeps quotes as part of the value: `KEY="abc"` gives `"abc"`,
        // which the API rejects with a 403. Strip them, and stray spaces.
        .trim()
        .removeSurrounding("\"")
        .trim()
        .also { key ->
            // Radio France keys are UUIDs. Fail now, with a clear message, rather than
            // with a confusing 403 at runtime. An empty key is allowed (CI).
            val uuid = Regex("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")
            if (key.isNotEmpty() && !uuid.matches(key)) {
                throw GradleException(
                    "RADIOFRANCE_API_KEY in local.properties does not look like a Radio France " +
                        "API key (expected a UUID such as 12345678-1234-1234-1234-123456789abc, " +
                        "without quotes). Fix it, or remove the line to build without a key."
                )
            }
        }

buildkonfig {
    packageName = "com.chy.radiofrance.core.network"
    // The generated object is `internal` by default, so the key cannot leave this module.
    defaultConfigs {
        buildConfigField(STRING, "RADIOFRANCE_API_KEY", apiKey)
    }
}

apollo {
    service("radiofrance") {
        packageName = "com.chy.radiofrance.core.network.graphql"
        // Generated models are `internal`: no Apollo type leaves core:network.
        generateAsInternal = true

        // No normalized-cache compiler plugin on purpose: it only generates
        // @typePolicy/TTL metadata (empty for us), and as a `public object` that
        // ignores generateAsInternal. Add it when a @typePolicy is needed.

        // Run once by hand, then commit the schema (it costs 1 API request):
        // ./gradlew :core:network:downloadRadioFranceApolloSchemaFromIntrospection
        // It sends the key as the `X-Token` header, the same transport as the app.
        introspection {
            endpointUrl = "https://openapi.radiofrance.fr/v1/graphql"
            headers.put("X-Token", apiKey)
            schemaFile = file("src/commonMain/graphql/schema.graphqls")
        }
    }
}

ktlint {
    version = libs.versions.ktlint
    // Skip generated sources (BuildKonfig, Apollo). We use invariantSeparatorsPath
    // because on Windows the raw path contains backslashes.
    filter { exclude { it.file.invariantSeparatorsPath.contains("/build/") } }
}
