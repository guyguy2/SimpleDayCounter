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

CI (`.github/workflows/ci.yml`) runs on JDK 21 (Temurin). Locally, the Gradle 9.6 wrapper needs a JDK 17+ `JAVA_HOME`. Tests are JVM unit tests only (`app/src/test`, with `isReturnDefaultValues = true` so `android.util.Log` does not throw, `FakeItemDao`, `kotlinx-coroutines-test`, and Turbine); there is no `app/src/androidTest` directory (the `androidTestImplementation` dependencies are declared but unused).

Use the `android` CLI as needed for Android workflows (`android --help` / `android <cmd> --help`) — e.g. `android run` / `install`, `emulator` / `device`, `preview` (render Compose `@Preview`s without a device), `screen` / `layout` / `input`, `analyze` (APK/AAB inspection), `sdk` / `info`, and `docs`.

## Architecture

- **DI**: Koin, started in `MainApp`. All bindings live in `di/AppModule.kt` (`AppDatabase`, `ItemDao`, `ItemRepository`, `viewModelOf(::EventViewModel)`, `DataStoreManager`). `app/proguard-rules.pro` explicitly keeps `di.**`, `EventViewModel`, `data.database.**`, `DateConverters`, and `navigation.**` (including `kotlinx.serialization` serializers) for R8 — update it if you add new ViewModels, Room `TypeConverter`s, or `@Serializable` routes. `MainActivity`, `MainScreen`, `SettingsScreen`, and `CategoryManagementScreen` call `koinInject` / `koinViewModel` directly, so Compose previews of those need a Koin context.
- **Navigation**: `navigation/Navigation.kt` defines `@Serializable` route objects under `sealed interface Screen` (`Splash`, `Main`, `Settings`, `CategoryManagement`); `navigation/AppNavigation.kt` wires `NavHost` with `composable<Screen.X>()`. `Screen.Splash` is the fixed start destination and pops itself after `SPLASH_DURATION_MS` (1500 ms).
- **Data flow & delete/undo**:
  - `ItemDao.getAllItems()` returns a Room `Flow<List<Item>>`. `EventViewModel` collects it once in `init` and exposes a single `StateFlow<MainScreenUiState>`. Mutations (`saveItem`, `updateItem`, `deleteItem`, `undoDelete`) only write to the repository and rely on the Flow to re-emit — do not re-fetch after writes.
  - `deleteItem` updates UI state immediately (snackbar + `lastDeletedItem`), then commits to Room after `DELETE_DELAY_MS` (200 ms) so the swipe-out animation finishes; the delayed job is tracked in `pendingDeleteJobs` and cancelled by `undoDelete`. `restoredItemKeys` (id to monotonic token) re-keys the `LazyColumn` row after undo so swipe state resets, and auto-expires after `RESTORATION_KEY_TTL_MS` (300 ms). Tests using `advanceUntilIdle()` drain these timers; existing tests use `runCurrent()` to inspect intermediate state.
- **Persistence & backup**:
  - Room: a single `items` table (`app_database`). `Item`, `ItemDao`, `AppDatabase`, and `ItemRepository` all live in `data/database/Database.kt`. `DateConverters` lives separately in `data/repository/DateConverters.kt`.
  - DataStore (`data/preferences/DataStoreManager.kt`, file `settings`): theme, vibrate (default on), date format, custom categories (`Set<String>`). `formatDate(date, format)` is a top-level pure function in the same file, not a method.
  - `res/xml/data_extraction_rules.xml` explicitly includes `database/app_database` and `file/datastore/settings.preferences_pb` for cloud backup and device transfer — update it if either persistence filename changes.
- **Theme, categories & UI models**:
  - `SimpleDayCounterTheme` (`ui/theme/Theme.kt`) resolves dark mode and date format once (collected in `MainActivity`) and provides them via `LocalIsDarkTheme` / `LocalDateFormat`. Leaf composables such as `EventCard` and `CategorySelector` read these locals instead of collecting DataStore flows; they throw if rendered outside the theme.
  - `MainActivity` declares `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|uiMode"` in `AndroidManifest.xml`, so rotation, resize, and dark-mode switches do not recreate the Activity.
  - `SortOption`, `FilterOption`, `FilterState`, `FilterStateSaver`, and `filterItems` are defined in `ui/screens/MainScreen.kt`, and `defaultCategories` in `ui/components/CategorySelector.kt` — look there rather than a dedicated model package.
  - Category validation (`normalizeCategoryName`, `isDuplicateCategory`) lives in `ui/components/AddCategoryInput.kt`; `CategoryColors` (`ui/theme/CategoryColors.kt`) assigns fixed light/dark colors to default categories and deterministic HSL colors (from `hashCode()`) to custom categories.
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

## Known gaps

Remaining backlog items: extracting `FilterMenu`/`SortMenu` from `MainScreen` and moving filter/sort models out of UI files, replacing `NavController` parameters with lambdas, and migrating `java.util.Date` to `java.time.LocalDate`.
