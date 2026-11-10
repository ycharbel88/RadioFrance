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
| `androidApp` | Android application: `Application` (starts Koin with the Android modules), `MainActivity`. |
| `shared` | App shell: wires every module, holds the root composable and the mini-player, and is **the only iOS framework**. |
| `feature:stations` | Station list: `domain/` (repository interface, `GetStationSectionsUseCase`), `data/` (repository), `presentation/` (`StationsViewModel`, `StationsUiState`, `StationsEvent`, `StationsScreen`, `StationItem`, strings and icons). |
| `core:model` | Domain types (`Station`, `DataError`, `DataResult`), no dependencies. |
| `core:network` | Apollo client, API key, `StationsRemoteDataSource`. No Apollo type leaves this module. |
| `core:player` | `AudioPlayer` interface and `PlayerState`. No UI. Default binding: `UnavailableAudioPlayer`; Android: Media3 `PlaybackService` + `MediaControllerAudioPlayer` (see below). |
| `core:designsystem` | `RadioFranceTheme` (Material 3 light/dark; brand colours come later), `RadioFranceIcons`. |
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
  overrides the binding from a module loaded after it: `initKoin(platformModules)` loads
  them after the common ones. Android passes `androidPlayerModule`; iOS keeps the default.
- `AppGraphTest` (in `shared`) resolves `StationsViewModel` from the real `appModules`, with
  only the network data source faked.

## Android player (Media3)

- `PlaybackService` (a `MediaSessionService`, in `core:player` androidMain) owns the
  `ExoPlayer` and the `MediaSession`. Media3 turns that into the media notification, lock
  screen controls, headset/Bluetooth buttons and playback that survives the Activity.
  ExoPlayer handles audio focus and "becoming noisy" (headphones unplugged) itself.
- Live radio: the session plays through `LivePlayer`, a `ForwardingPlayer` that decides on
  each resume between the buffer and the live broadcast. Each new connection starts with
  a pre-roll ad from the server, so short pauses resume the buffer; once the total lag
  behind live (all pauses since the last connection) passes `LIVE_REJOIN_THRESHOLD`
  (60 s), or after a stop or an error, the resume reconnects to live. Same rule from the
  app, the notification, the lock screen and headset buttons.
- `MediaControllerAudioPlayer` is the app side: a `MediaController` connected
  asynchronously (commands sent before the connection are queued in order), mapping
  the player's events to `PlayerState`. Application context only.
- Swiping the app away keeps the radio playing; if nothing is playing, the service stops.
- `POST_NOTIFICATIONS` is neither declared nor requested: media session notifications
  are exempt from it (Android 13+ notification permission docs).
- The service and the controller are checked on a device, not by unit tests. The
  state mapping, the live-edge rule and the lag accounting are pure, tested on the JVM.
- Debug builds log every player event, plus load starts/cancels, under the logcat tag
  `EventLogger`. A resume that reconnects shows `loadCanceled` then `loadStarted` with
  a new task id.

iOS: no player yet. Tapping a station shows "not supported" in the mini-player.

## UI (Compose Multiplatform, shared by Android and iOS)

- `App()` (in `shared`): theme + `Scaffold` with a top bar, the stations screen, and the
  mini-player at the bottom (hidden when nothing is loaded; play/pause, or retry after an
  error).
- `StationsScreen` is the only composable that knows Koin (`koinViewModel()`) and the
  ViewModel. It collects the state with `collectAsStateWithLifecycle()` and hands it to the
  stateless `StationsScreenContent`, which the previews call directly.
- States: loading, empty, error, and a sectioned list (sticky headers, keys = station ids)
  with pull-to-refresh. A failed refresh keeps the list and shows a banner above it.
- Each `DataError` has its own message. "Retry" appears only when it can help (network or
  server error), never for a missing or refused API key or the daily quota.
  `StationsTexts.kt` holds these rules and is unit-tested.
- The UI is in French (`composeResources/values/strings.xml`). Another language is one more
  `values-xx` folder, with no code change.
- Previews: one per state, light and dark, in `StationsPreviews.kt`. Open them in Android
  Studio from the commonMain file.

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
