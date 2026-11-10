# Radio France (Kotlin Multiplatform)

A list of Radio France stations from the Radio France Open API (GraphQL). Tapping a station
starts playback. Android and iOS share the UI through Compose Multiplatform.

## Modules

```
androidApp ──► shared ──► feature:stations ──► core:designsystem
 (Android        │                        ├──► core:network
  app shell)     │                        ├──► core:player
                 │                        └──► core:model
                 ├──► core:network ──► core:model
                 ├──► core:player  ──► core:model
                 └──► core:designsystem, core:model
iosApp (Xcode) ──► Shared.framework (built from `shared`)

core:testing ──► core:player   (test doubles; only ever a commonTest dependency)
```

| Module | Role |
|---|---|
| `androidApp` | Android application: `Application` (starts Koin), `MainActivity`, later the Media3 service. |
| `shared` | App shell: wires every module, holds the root composable, and is **the only iOS framework**. |
| `feature:stations` | Station list: `domain/` (repository interface, `GetStationSectionsUseCase`), `data/` (repository), `presentation/` (`StationsViewModel`, `StationsUiState`, `StationsEvent`). |
| `core:model` | Domain types (`Station`, `DataError`, `DataResult`), no dependencies. |
| `core:network` | Apollo client, API key, `StationsRemoteDataSource`. No Apollo type leaves this module. |
| `core:player` | `AudioPlayer` interface and `PlayerState`. No UI. Default binding: `UnavailableAudioPlayer` (see below). |
| `core:designsystem` | Theme and shared composables. |
| `core:testing` | Test doubles shared across modules (`FakeAudioPlayer`). Only ever a `commonTest` dependency: KMP has no `testFixtures`. |

Rules: `feature:*` depends only on `core:*`. `core:*` never depends on `feature:*`, `shared`
or `androidApp`. Why each module exists is written at the top of its `build.gradle.kts`.

## Presentation and player

- `StationsViewModel` (multiplatform androidx `ViewModel`) reads data only through
  `GetStationSectionsUseCase`, and the player state live from `AudioPlayer.state`. It exposes
  one immutable `StationsUiState` (`stateIn(WhileSubscribed(5_000))`) and takes
  `StationsEvent`s (`StationClicked`, `Retry`, `Refresh`, `TogglePlayPause`).
- Only one load runs at a time: a second pull-to-refresh sends no second request.
- `playerModule` binds `UnavailableAudioPlayer` by default, so the Koin graph is complete on
  every platform at every step. It answers every `play` with
  `PlayerState.Error(station, PlaybackError.NotSupported)`. A platform with a real player
  overrides the binding from a module loaded after it (Android: Media3). iOS keeps the
  default for now.
- `AppGraphTest` (in `shared`) resolves `StationsViewModel` from the real `appModules`, with
  only the network data source faked.

## API key

Add the key to `local.properties`, which is never committed:

```properties
RADIOFRANCE_API_KEY=12345678-1234-1234-1234-123456789abc
```

No quotes: `local.properties` keeps them as part of the value, and the API answers 403.
The build strips them anyway, then fails with a clear message if the value is not a UUID.
The build works without a key (that's how CI runs). The app then reports
`DataError.MissingApiKey` at runtime, without calling the API.

The key is sent as the `X-Token` header. The API allows 1000 requests per day.

## GraphQL schema

`core/network/src/commonMain/graphql/schema.graphqls` is committed. Refresh it by hand only
when needed (it costs 1 request):

```sh
./gradlew :core:network:downloadRadiofranceApolloSchemaFromIntrospection
```

Queries may use only fields present in that file.

## Build and checks

```sh
./gradlew :androidApp:assembleDebug        # Android app
./gradlew :shared:testAndroidHostTest :core:player:testAndroidHostTest \
          :core:network:testAndroidHostTest \
          :feature:stations:testAndroidHostTest   # unit tests (JVM, no network)
./gradlew ktlintCheck                      # lint (ktlintFormat to fix)
./gradlew :shared:compileKotlinIosSimulatorArm64   # iOS Kotlin compile, works on Windows/Linux too
```

To build the iOS app you need a Mac: open `iosApp/iosApp.xcodeproj` in Xcode 26.4.

## CI (GitHub Actions)

- **android** (ubuntu): `:androidApp:assembleDebug`, the four `testAndroidHostTest` tasks, `ktlintCheck`
- **ios** (macos-26, Xcode 26.4 pinned): `:shared:linkDebugFrameworkIosSimulatorArm64`, then
  `xcodebuild` of `iosApp` for the arm64 simulator, without code signing.

## Next steps (known debt)

- **build-logic convention plugins.** Every module repeats the same KMP and Android setup
  and the ktlint block. That's acceptable with 8 modules; past that, move it into plugins.
- **Module graph check.** The dependency rules are currently enforced only by review. A
  Gradle check (or a module-graph assertion plugin) would make CI enforce them.
