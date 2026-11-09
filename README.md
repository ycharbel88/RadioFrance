# Radio France (Kotlin Multiplatform)

A list of Radio France stations from the Radio France Open API (GraphQL). Tapping a station
starts playback. Android and iOS share the UI through Compose Multiplatform.

## Modules

```
androidApp ──► shared ──► feature:stations ──► core:designsystem
 (Android        │                        └──► core:model
  app shell)     ├──► core:network ──► core:model
                 ├──► core:player  ──► core:model
                 └──► core:designsystem, core:model
iosApp (Xcode) ──► Shared.framework (built from `shared`)
```

| Module | Role |
|---|---|
| `androidApp` | Android application: `Application` (starts Koin), `MainActivity`, later the Media3 service. |
| `shared` | App shell: wires every module, holds the root composable, and is **the only iOS framework**. |
| `feature:stations` | Station list screen and its ViewModel. |
| `core:model` | Domain types, no dependencies. |
| `core:network` | GraphQL client and API key. No Apollo/Ktor type leaves this module. |
| `core:player` | `AudioPlayer` interface plus platform implementations. No UI. |
| `core:designsystem` | Theme and shared composables. |

Rules: `feature:*` depends only on `core:*`. `core:*` never depends on `feature:*`, `shared`
or `androidApp`. Why each module exists is written at the top of its `build.gradle.kts`.

## API key

Add the key to `local.properties`, which is never committed:

```properties
RADIOFRANCE_API_KEY=your-key
```

The build works without it (that's how CI runs). The key then defaults to an empty string,
and the app reports the missing key at runtime.

## Build and checks

```sh
./gradlew :androidApp:assembleDebug        # Android app
./gradlew :shared:testAndroidHostTest      # unit tests (JVM)
./gradlew ktlintCheck                      # lint (ktlintFormat to fix)
./gradlew :shared:compileKotlinIosSimulatorArm64   # iOS Kotlin compile, works on Windows/Linux too
```

To build the iOS app you need a Mac: open `iosApp/iosApp.xcodeproj` in Xcode 26.4.

## CI (GitHub Actions)

- **android** (ubuntu): `:androidApp:assembleDebug :shared:testAndroidHostTest ktlintCheck`
- **ios** (macos-26, Xcode 26.4 pinned): `:shared:linkDebugFrameworkIosSimulatorArm64`, then
  `xcodebuild` of `iosApp` for the arm64 simulator, without code signing.

## Next steps (known debt)

- **build-logic convention plugins.** Every module repeats the same KMP and Android setup
  and the ktlint block. That's acceptable with 7 modules; past that, move it into plugins.
- **Module graph check.** The dependency rules are currently enforced only by review. A
  Gradle check (or a module-graph assertion plugin) would make CI enforce them.
