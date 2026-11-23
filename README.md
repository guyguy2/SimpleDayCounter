# Simple Day Counter

A minimalist Android app for tracking important dates and counting days left until upcoming events or days elapsed since past milestones.

Fully free, offline-first, no ads, no accounts, and no internet permission required.

[Get it on Google Play](https://play.google.com/store/apps/details?id=com.happypuppy.daycounter)

---

## Features

- **Countdown & count-up tracking** - Automatically displays days remaining (`days left`), `Today!`, or days elapsed (`days past`) with DST-safe local-date calculations and status-colored cards.
- **Categories & custom categories** - Includes `Personal`, `Work`, `Health`, and `Finance` categories out of the box, plus custom user-defined categories with deterministic color badges and a dedicated Category Management screen.
- **Custom emoji icons** - Personalize any event with a single emoji character, or fall back to its category icon.
- **Filtering & sorting** - Filter events by All, Future, Today, Past, or a specific category; sort ascending or descending by Name, Target Date, Date Added, or Category.
- **Swipe to delete with Undo** - Swipe an event card to delete it with optional haptic feedback and an Undo snackbar.
- **Customizable date formats** - Choose between Default (`Dec 31, 2020`), US (`12/31/2020`), EU (`31/12/2020`), ISO (`2020-12-31`), and Full (`December 31, 2020`) with live preview.
- **Light, Dark & System themes** - Material 3 theme support with edge-to-edge layout and automatic device backup rules.

## Privacy

All data stays on your device (with optional Android system backup via standard data extraction rules). No accounts, no analytics, no tracking, and no internet permission (only `VIBRATE` for optional haptic feedback).

## Requirements

- Android 12 (API 31) or higher (`targetSdk 36`)

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3
- Room (local database with exported schemas)
- DataStore Preferences
- Koin (dependency injection)
- Navigation Compose (type-safe `@Serializable` routes)
- Kotlin Coroutines & Flow

## Building & Testing

Requires JDK 17+ locally (CI runs on Temurin JDK 21).

```bash
# Run unit tests, lint, and assemble debug APK (same as PR CI)
./gradlew testDebugUnitTest lintDebug assembleDebug

# Unit tests only (JVM, no device required)
./gradlew testDebugUnitTest

# Release APK (R8-minified; unsigned unless keystore.properties or ANDROID_KEYSTORE_* env vars are set)
./gradlew assembleRelease

# Release App Bundle (signed when keystore.properties or ANDROID_KEYSTORE_* env vars are set)
./gradlew lintRelease bundleRelease -PversionCode=1 -PversionName=1.0.0
```

## Architecture

Single-module MVVM + Repository pattern.

```
app/src/main/java/com/happypuppy/daycounter/
├── MainActivity.kt    # Entry point, edge-to-edge setup, theme & date-format collection
├── MainApp.kt         # Application class initializing Koin
├── data/
│   ├── database/      # Room Item entity, DAO, AppDatabase, ItemRepository
│   ├── preferences/   # DataStoreManager (theme, vibration, date format, custom categories)
│   └── repository/    # Room DateConverters
├── di/                # Koin module bindings
├── navigation/        # Type-safe @Serializable routes and NavHost
├── ui/
│   ├── components/    # EventCard, BottomSheet (AddEventBottomSheet), CategorySelector, CategoryColorIndicator, AddCategoryInput, FilterChip
│   ├── screens/       # MainScreen, EditEventScreen, SettingsScreen, CategoryManagementScreen, SplashScreen, EventViewModel
│   └── theme/         # Material 3 Light/Dark themes and CategoryColors
└── util/              # DST-safe day count math, UTC DatePicker helpers, grapheme/emoji & color helpers
```
