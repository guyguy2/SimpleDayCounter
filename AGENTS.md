# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Project

SimpleDayCounter is a single-module Android app (`:app`, package `com.happypuppy.daycounter`) that tracks named date events and shows days remaining or elapsed. Stack: Kotlin, Jetpack Compose + Material3, Room (single `Item` entity), DataStore Preferences, Koin, type-safe Navigation Compose. No network layer. `minSdk 31`, `compileSdk 37` (minor 1), `targetSdk 36`, JVM 17. Source root: `app/src/main/java/com/happypuppy/daycounter`.

## Commands

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug                            # what CI runs on PRs
./gradlew testDebugUnitTest                                                    # unit tests only (JVM, no device needed)
./gradlew testDebugUnitTest --tests "com.happypuppy.daycounter.ui.screens.EventViewModelTest"
./gradlew testDebugUnitTest --tests "*EventViewModelTest.methodName"           # single test
./gradlew assembleRelease                                                      # R8-minified APK (unsigned unless keystore configured)
./gradlew lintRelease bundleRelease -PversionCode=N -PversionName=X.Y.Z        # release AAB (needs signing config)
```

Instrumented tests (`app/src/androidTest`) run on an emulator only, never on a personal device: the Compose tests wipe the app database and reset DataStore. Never run a bare `connectedDebugAndroidTest` (it installs onto every attached device) and never a bare `adb`. Name the device every time:

```bash
./gradlew assembleDebug assembleDebugAndroidTest
adb -s emulator-5554 install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r com.happypuppy.daycounter.test/androidx.test.runner.AndroidJUnitRunner
```

When two emulators share one AVD, start every instance with `-read-only`.

Cold start and the first frame. Instrumented tests no longer need a clock workaround for startup (the old `advanceTimeBy(2000)` for the 1500 ms splash is gone); `StartupInstrumentedTest` checks that the main screen needs no timer. `ThemeMatrixInstrumentedTest` launches MainActivity for each system light/dark x app system/light/dark cell and checks the settled background color; it runs in a warm process and polls for the splash fade, so it is NOT a first-frame proof. The first-frame and flash check is manual, with a real process start:

```bash
adb -s emulator-5554 shell cmd uimode night yes            # or no; then set the app theme in Settings
adb -s emulator-5554 shell am force-stop com.happypuppy.daycounter
adb -s emulator-5554 shell "screenrecord --time-limit 6 --size 540x1212 /sdcard/cold.mp4" &
adb -s emulator-5554 shell am start -W -n com.happypuppy.daycounter/.MainActivity
adb -s emulator-5554 pull /sdcard/cold.mp4
# sample one background pixel per frame (ffmpeg crop+scale to 1x1, rawvideo rgb24) and look for a frame
# of the wrong theme or pure white after the content starts
adb -s emulator-5554 shell cmd uimode night no             # reset when done
```

screenrecord only records changed frames and is lossy, so it shows colors within a few units and cannot prove one exact frame. To check reduced motion run `adb -s emulator-5554 shell settings put global animator_duration_scale 0` and reset it to `1` afterwards.

Pseudolocales (debug builds only, `isPseudoLocalesEnabled` in `app/build.gradle.kts`; the release APK lists neither): `en-XA` expands and accents the text, `ar-XB` mirrors it for RTL. Switch the app to one with the per-app locale command, restart it, and reset the list when done:

```bash
adb -s emulator-5554 shell cmd locale set-app-locales com.happypuppy.daycounter --locales en-XA   # or ar-XB
adb -s emulator-5554 shell am force-stop com.happypuppy.daycounter
adb -s emulator-5554 shell cmd locale set-app-locales com.happypuppy.daycounter                   # reset to the device locale
```

The setting survives `install -r` and the instrumented tests assert English text, so always reset it before running them. The system language picker did not offer the pseudolocales on the emulator image used here, and `persist.sys.locale` cannot be set without root; the per-app command above worked without `android:localeConfig`.

CI (`.github/workflows/ci.yml`) runs on JDK 21 (Temurin). The Gradle daemon JVM is pinned to 21 in `gradle/gradle-daemon-jvm.properties` (written by `./gradlew updateDaemonJvm --jvm-version=21`, provisioned through the foojay toolchain resolver in `settings.gradle.kts`), and `app/build.gradle.kts` runs the unit tests on a JDK 21 launcher. The first run may download JDK 21.

The pin matters because `IsSingleGraphemeTest` needs `BreakIterator` grapheme-cluster rules from JDK 20 or later. It affects only the Gradle daemon and the test JVM: sources and bytecode still compile for JDK 17.

`JAVA_HOME` for the Gradle launcher (observed, not a documented minimum): the launcher JVM only has to run the wrapper, and the daemon is then started on JDK 21 (the daemon process was seen running from the JDK 21 toolchain). The unit tests and the full gate passed (87 tests) with the shell default Java 25 (Android Studio JBR) and with a JDK 17 `JAVA_HOME`. `./gradlew help` also worked with a JDK 11 `JAVA_HOME` and with `JAVA_HOME` unset (Homebrew Java 27), and `./gradlew --version` reported "Daemon JVM: Compatible with Java 21" in every case. No specific `JAVA_HOME` is needed any more.

Tests are mostly JVM unit tests (`app/src/test`, with `isReturnDefaultValues = true` so `android.util.Log` does not throw, `FakeItemDao`, `kotlinx-coroutines-test`, and Turbine). `app/src/androidTest` holds instrumented tests (in-memory Room DAO, DataStore, `isSingleGrapheme` on the Android runtime, and Compose main-flow tests); they are not run by CI.

Use the `android` CLI as needed for Android workflows (`android --help` / `android <cmd> --help`) — e.g. `android run` / `install`, `emulator` / `device`, `preview` (render Compose `@Preview`s without a device), `screen` / `layout` / `input`, `analyze` (APK/AAB inspection), `sdk` / `info`, and `docs`.

## Architecture

- **DI**: Koin, started in `MainApp`. All bindings live in `di/AppModule.kt` (`AppDatabase`, `ItemDao`, `ItemRepository`, `viewModelOf(::EventViewModel)`, `DataStoreManager`). `app/proguard-rules.pro` explicitly keeps `di.**`, `EventViewModel`, `data.database.**`, `DateConverters`, and `navigation.**` (including `kotlinx.serialization` serializers) for R8 — update it if you add new ViewModels, Room `TypeConverter`s, or `@Serializable` routes. `MainActivity`, `MainScreen`, `SettingsScreen`, and `CategoryManagementScreen` call `koinInject` / `koinViewModel` directly, so Compose previews of those need a Koin context.
- **Navigation**: `navigation/Navigation.kt` defines `@Serializable` route objects under `sealed interface Screen` (`Main`, `Settings`, `CategoryManagement`); `navigation/AppNavigation.kt` wires `NavHost` with `composable<Screen.X>()` and `Screen.Main` is the start destination. `AppNavigation` is the only file that touches the `NavController`: screens and sheets take lambdas (`MainScreen(onOpenSettings, onOpenCategories)`, `SettingsScreen(onBack, onOpenCategories)`, `CategoryManagementScreen(onBack)`, `onOpenCategories` on the Add/Edit sheets and `CategorySelector`). There is no in-app splash route. Transitions are Compose `tween` animations, so they follow the system animator duration scale (compose-ui reads `animator_duration_scale`; 0 means no animation). No `BackHandler` exists anywhere: back, predictive back (back-to-home, Settings and Category screens, bottom sheets) is the platform default and was checked on an emulator in W3.
- **Data flow & delete/undo**:
  - `ItemDao.getAllItems()` returns a Room `Flow<List<Item>>`. `EventViewModel` collects it once in `init` and exposes a single `StateFlow<MainScreenUiState>`. Mutations (`saveItem`, `updateItem`, `deleteItem`, `undoDelete`) only write to the repository and rely on the Flow to re-emit — do not re-fetch after writes.
  - `deleteItem` updates UI state immediately (snackbar + `lastDeletedItem`), then commits to Room after `DELETE_DELAY_MS` (200 ms) so the swipe-out animation finishes; the delayed job is tracked in `pendingDeleteJobs` and cancelled by `undoDelete`. `restoredItemKeys` (id to monotonic token) re-keys the `LazyColumn` row after undo so swipe state resets, and auto-expires after `RESTORATION_KEY_TTL_MS` (300 ms). Tests using `advanceUntilIdle()` drain these timers; existing tests use `runCurrent()` to inspect intermediate state.
- **Persistence & backup**:
  - Room: a single `items` table (`app_database`). `Item`, `ItemDao`, `AppDatabase`, and `ItemRepository` all live in `data/database/Database.kt`. `DateConverters` lives separately in `data/repository/DateConverters.kt`.
  - DataStore (`data/preferences/DataStoreManager.kt`, file `settings`): theme, vibrate (default on), date format, custom categories (`Set<String>`), dynamic color (`dynamic_color`, additive boolean, default OFF). `formatDate(date, format, locale)` is a top-level pure function in the same file, not a method.
  - `DataStoreManager(dataStore: DataStore<Preferences>)` takes the store as a constructor argument, so it runs in JVM tests on a temp file (`PreferenceDataStoreFactory`, see `DataStoreManagerTest`). Production injects the one `Context.dataStore` delegate (`preferencesDataStore(name = SETTINGS_DATASTORE_NAME)`, file `files/datastore/settings.preferences_pb`); keep it a single instance per file per process and do not open that file a second way.
  - `settingsFlow` emits `AppSettings(theme, dateFormat, dynamicColor)`: the first emission is the resolved stored value, never a placeholder. All flows of the manager read through one shared flow that catches `IOException` (including a corrupt file) and emits the defaults; other exceptions propagate. After such an error the flow emits the defaults and then COMPLETES, so the flows stop updating until the collector restarts (the root `StartupSettings` collector lives for the activity, so later changes in Settings reach the root again only after the activity is recreated). The release of the splash and the first frame still work on the defaults.
  - Date format default: nothing stored means `DateFormat.SYSTEM` (value `system`), which formats with the device locale (`DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)`). Only a tap on a Settings menu item writes the key; reading never does. Stored `default`, `us`, `eu`, `iso` and `full` are kept. The value strings are frozen (`PreferenceEnumsTest`); `system` was added in front. The old `default` option (`MMM d, yyyy`) is labelled "Short" in the UI.
  - `res/xml/data_extraction_rules.xml` explicitly includes `database/app_database` and `file/datastore/settings.preferences_pb` for cloud backup and device transfer — update it if either persistence filename changes.
- **Theme, categories & UI models**:
  - `SimpleDayCounterTheme` (`ui/theme/Theme.kt`) takes the resolved theme, date format and dynamic color flag (from `StartupSettings` in `MainActivity`) and provides them via `LocalIsDarkTheme` / `LocalDateFormat` / `LocalEventCardColors`. Leaf composables such as `EventCard` and `CategorySelector` read these locals instead of collecting DataStore flows; they throw if rendered outside the theme.
  - **Splash and first frame**: the app uses the system splash screen only (`androidx.core:core-splashscreen`). The activity theme is `Theme.App.Starting` (parent `Theme.SplashScreen`, `postSplashScreenTheme` = `Theme.SimpleDayCounter`; light and dark colors in `res/values/colors.xml` and `values-night/colors.xml`, icon `drawable/ic_splash.xml`). `MainActivity` calls `installSplashScreen()` before `super.onCreate`, collects `settingsFlow` into `StartupSettings` and keeps the splash up with `setKeepOnScreenCondition { !startup.isResolved }`; it draws nothing until the first resolved `AppSettings`. There is no artificial delay, but a first read that stalls for `StartupSettings.FIRST_READ_TIMEOUT_MS` (2 s) releases the splash with `AppSettings.Defaults` (a real value that arrives later still replaces them). `SplashColorsTest` ties `window_background` to the app's `LightColorScheme` background and `DarkBackground`, so change both together. The window background follows the system light/dark mode through the night colors, but the platform theme parent of `Theme.SimpleDayCounter` stays `android:Theme.Material.Light.NoActionBar` in both modes (a dark night parent would also turn platform widgets such as the text selection toolbar dark when the app is forced Light). Trade-off: with the system dark and the app dark those platform widgets stay light, as they were before W3; Compose draws everything else from the app theme. The system draws the splash from the system light/dark mode before the app can read its own theme setting, so with an app theme that differs from the system mode the splash fades into the other color (2 to 3 frames); that cannot be removed.
  - **Dynamic color**: Settings > Appearance has a "Dynamic color" switch, default OFF (the brand palette is the default). When on, `SimpleDayCounterTheme(dynamicColor = true)` uses the wallpaper scheme and EventCard states map to the tertiary (past), primary (today) and secondary (future) containers (`dynamicEventCardColors`); the splash cannot follow wallpaper colors. Brand schemes list every Material 3 role in `Theme.kt` and `BrandColorSchemeTest` pins them. `EventCard` holds no literal colors: it reads `LocalEventCardColors` (`ui/theme/EventCardColors.kt`: `brandEventCardColors` keeps the pink, purple and yellow tints). `EventCardColorsTest` asserts 4.5:1 for text on every state container and 3:1 for the badge digits in both brand schemes; the category icon and badge disc are decorative and sit at 1.3 to 2.9:1 against the light card containers, which is not asserted.
  - `MainActivity` declares `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|uiMode"` in `AndroidManifest.xml`, so rotation, resize, and dark-mode switches do not recreate the Activity.
  - `SortOption`, `FilterOption`, `FilterState`, `FilterStateSaver`, and `filterItems` are defined in `ui/screens/MainScreen.kt`, and `defaultCategories` in `ui/components/CategorySelector.kt` — look there rather than a dedicated model package.
  - Category validation (`normalizeCategoryName`, `isDuplicateCategory`) lives in `ui/components/AddCategoryInput.kt`; `CategoryColors` (`ui/theme/CategoryColors.kt`) assigns fixed light/dark colors to default categories and deterministic HSL colors (from `hashCode()`) to custom categories.
- **Text and strings**:
  - All UI text lives in `res/values/strings.xml` (English only, no `localeConfig`). Names are snake_case with a screen prefix (`main_`, `settings_`, `category_mgmt_`, `form_`, `filter_`, `event_card_`, ...) and `common_` for words shared across screens. Counts use `<plurals>` (`event_card_days_left`, `event_card_days_past`); the badge number itself stays `abs(daysLeft).toString()` on purpose so digits are not localized by accident. Typographic characters are written as XML entities (`&#8230;`, `&#169;`) so source files stay ASCII; do not use a spaced hyphen between digits or three dots in a string (lint `TypographyDashes` and `TypographyEllipsis` fire). `date_format_iso` ("ISO (2020-12-31)") is the one exception, see Known gaps.
  - `UiText` (`util/UiText.kt`) carries text out of non-UI code: `DynamicString`, `StringResource(id, args)`, `PluralResource(id, count, args)`, resolved with `asString()` (composable) or `asString(context)`. `EventError` (`ui/screens/EventError.kt`) is the sealed failure type of `EventViewModel` (load, save, update, delete, restore); `MainScreenUiState.error` holds it and the banner shows `error.message.asString()`. The ViewModel never surfaces exception messages.
  - Default categories keep their English strings as stored keys (colors, icons and saved events match on them). Display goes through `categoryDisplayName(key)` (`ui/components/CategoryNames.kt`), which maps the four defaults to `category_*` resources and shows custom categories as stored. The "Add Custom..." menu sentinel is the private key `ADD_CUSTOM_CATEGORY_KEY`; its visible text is `category_add_custom`.
  - Literal guard: `StringLiteralGuardTest` scans `app/src/main/java` and fails on a string literal with a letter in `Text("...")`, `text|title|description|placeholder|message|actionLabel|onClickLabel|contentDescription|label|supportingText|hint = "..."`, `onClick(label = "...")`, `CustomAccessibilityAction("...")`, and `Text(if (c) "A" else "B")` / `text = if (c) "A" else "B"`. Allow-list: the Compose animation labels `Alpha Animation` and `SwipeDeleteBackground`. Limits: it does not see a literal that reaches the UI through a variable, a `when` expression or a helper function, or a condition that has quotes of its own, so review those by eye.
- **Dates & utilities**:
  - `Item.targetDate` is a `java.util.Date` anchored at 12:00 local time. Day counts go through `calculateDaysLeft` (`java.time.LocalDate` + `ChronoUnit.DAYS`, DST-safe) in `util/Utils.kt`. Material3 `DatePicker` speaks UTC-midnight millis, so use `localDateToUtcPickerMillis` / `utcPickerMillisToLocalDate` in `util/Utils.kt` rather than converting by hand.
  - `util/Utils.kt` also holds `isSingleGrapheme` (emoji validation), `isColorBright`, `vibrate`, and `AppVersionInfo` (reads version info via `PackageManager`). `BuildConfig` is enabled (`buildConfig = true`) and `BuildConfig.DEBUG` guards debug `Log.d` calls and Koin logging.

## Database migrations

`AppDatabase` is at version 4 with `exportSchema = true`; schemas are written to `app/schemas/` and should be committed. `di/AppModule.kt` only allows destructive fallback **from versions 1–3** (`fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)`). Any schema change to v5+ must ship an explicit `Migration` and commit the new schema JSON, or users will crash on upgrade.

## Build, versioning and release

- `versionCode`/`versionName` default to `1` / `1.0` in `app/build.gradle.kts` and are overridden by `-PversionCode`/`-PversionName` or `VERSION_CODE`/`VERSION_NAME` env vars.
- `release-internal.yml` runs on pushes to `main`, `v*` tags, and manual `workflow_dispatch` (which supports a `publish=false` dry run): `versionCode = 10000 + run_number`, `versionName` = tag (without leading `v`), else `1.1.<run_number>`. It runs unit tests, `lintRelease`, `bundleRelease`, an offline `gplay preflight` check (`--max-size 100M --fail-on error`), and publishes to the Play **internal** track via `com.github.triplet.play`, authenticated keylessly through Workload Identity Federation.
- `promote.yml` (manual dispatch) handles `promote`, `update-rollout`, `complete`, and `halt` via `promoteArtifact`, using `PLAY_PROD_SA_EMAIL` when targeting `production`.
- Release builds enable R8, resource shrinking, and `ndk.debugSymbolLevel = "SYMBOL_TABLE"`, and fail the build if `targetSdk < 36` (Play policy gate in `app/build.gradle.kts`).
- Local signing reads `keystore.properties` (git-ignored) or `ANDROID_KEYSTORE_*` env vars; without them the release build is unsigned. Never commit keystores, `keystore.properties`, or service-account files.
- `app/src/main/play/release-notes/` is generated by CI from commit subjects (`wip`, `ci`, `chore`, `docs`, `build`, and `merge` commits are dropped; conventional prefixes are stripped; 500 char cap) and is git-ignored. Write commit subjects accordingly.
- Dependency versions live in `gradle/libs.versions.toml`. Dependabot is configured in `.github/dependabot.yml`; GitHub Actions are pinned by commit SHA.

## Release and CI/CD (linked repos)

This repo does not hold the release tooling. The shared CI/CD hub is a separate local git repo:

- CI/CD hub: `/Users/guy/dev/android/cicd` (GCP project `happypuppy-play-cicd`, service accounts `play-ci-testing` and `play-ci-production`, keyless WIF auth). Read before any release work:
  - `cicd/app-state.md`: living source of truth (per-app Play state, package names, keystore locations and aliases, what is live on which track).
  - `cicd/workflows/existing-app-onboard-and-release.md`: the repeatable release checklist.
  - `cicd/14-local-and-self-hosted-pipelines.md`: running the CI, release and promote workflows locally.
  - `cicd/release-log.md`: history and lessons. `cicd/AGENTS.md`: workspace rule, `versionName` is always clean semver, never a commit hash.
- Sibling app on the same pipeline: SpeechTimer_Jetpack (Simple Speech Timer, package com.happypuppy.toastmasterstimer) at /Users/guy/dev/android/SpeechTimer_Jetpack; same hub, same release workflows, its AGENTS.md points back here and to the hub
- This repo's own workflows are `.github/workflows/ci.yml`, `release-internal.yml` and `promote.yml` (SHA-pinned). Local mirrors live in the hub and take this repo via `--repo-dir`; run them from the hub directory:

```bash
cd /Users/guy/dev/android/cicd
scripts/local-ci.sh --repo-dir /Users/guy/dev/android/SimpleDayCounter
scripts/local-release-internal.sh --repo-dir /Users/guy/dev/android/SimpleDayCounter --keystore <path from app-state.md> --alias <alias from app-state.md> --version-code auto --dry-run
scripts/local-promote.sh --engine gplay --package com.happypuppy.daycounter --to production --action update-rollout --fraction 0.5
```

- Releasing from either app repo works the same way: the hub scripts take the app repo and package as arguments, so a release session can start in this repo or in the sibling and still find the hub and the other app through the paths above.
- Rules: `--dry-run` first. Uploading to Play, promoting, or changing a rollout is outward-facing: only with the owner's explicit yes. Release from a clean, merged `main` unless the owner says otherwise (the local script refuses a dirty tree). Never commit keystores, `keystore.properties` or service-account files; the hub documents where they live, with locations only. Commit subjects feed the Play release notes (see Build, versioning and release above).

## Known gaps

`date_format_iso` keeps hyphens because the example text must equal the real ISO output, so lint reports `TypographyDashes` for it (the only lint warning besides `OldTargetApi`). Remaining backlog items: extracting `FilterMenu`/`SortMenu` from `MainScreen` and moving filter/sort models out of UI files, and migrating `java.util.Date` to `java.time.LocalDate`. Screens other than the root still read settings with `collectAsState(initial = ...)` per screen (only the theme, date format and dynamic color are resolved once at the root).
