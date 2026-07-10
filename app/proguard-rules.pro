# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line numbers for production stack traces; hide original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Kotlin / Kotlin reflection
# ---------------------------------------------------------------------------
# Koin uses kotlin-reflect-style metadata (KClass) to resolve dependencies and
# Room/Compose tooling reads class-level annotations. Keep the metadata to
# avoid "kotlin.reflect.jvm.internal.KClassImpl: ..." failures at runtime.
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ---------------------------------------------------------------------------
# Room (androidx.room)
# Per Room's official guidance:
#   https://developer.android.com/jetpack/androidx/releases/room#proguard
# Room generates `<DatabaseName>_Impl` and `<DaoName>_Impl` classes through
# its annotation processor. Keep all generated Room implementations and the
# entities/DAOs/database classes themselves so reflection-based lookups in
# Room's runtime continue to work after minification.
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep class **_Impl { *; }
-dontwarn androidx.room.paging.**

# Keep our concrete database/entity/repository types explicitly. Belt and
# suspenders alongside the annotation-based rules above.
-keep class com.happypuppy.daycounter.data.database.** { *; }
-keep class com.happypuppy.daycounter.data.repository.DateConverters { *; }

# ---------------------------------------------------------------------------
# Koin 3.x
# Koin resolves dependencies via Kotlin reflection on constructor references
# (e.g. `viewModelOf(::EventViewModel)` and `single { ... }`). Keep our DI
# module + ViewModels + classes that participate in DI so their constructors
# and Kotlin metadata survive R8.
# ---------------------------------------------------------------------------
-keep class com.happypuppy.daycounter.di.** { *; }
-keep class com.happypuppy.daycounter.ui.screens.EventViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Koin internals occasionally reference optional integrations that we don't
# pull in (Ktor, Android Worker, etc.). Silence the warnings rather than
# pulling in unused deps.
-dontwarn org.koin.**

# ---------------------------------------------------------------------------
# Kotlin Coroutines
# Coroutines uses `ServiceLoader` for the Main dispatcher; the consumer rules
# bundled with the library handle most of it but the volatile/atomic
# field-updater patterns produce noisy warnings on R8.
# ---------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.debug.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---------------------------------------------------------------------------
# AndroidX DataStore (Preferences)
# Preferences DataStore does not use Proto/serialization, so only the
# generic dontwarn is needed.
# ---------------------------------------------------------------------------
-dontwarn androidx.datastore.**

# ---------------------------------------------------------------------------
# AndroidX Navigation Compose
# Type-safe @Serializable routes (P2-7) are encoded via kotlinx.serialization;
# keep route types and Navigation internals so R8 does not strip serializers.
# ---------------------------------------------------------------------------
-keepnames class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**
-keep class com.happypuppy.daycounter.navigation.** { *; }
-keepclassmembers class com.happypuppy.daycounter.navigation.** {
    *** Companion;
}
-keepclasseswithmembers class com.happypuppy.daycounter.navigation.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Jetpack Compose
# Compose ships with its own consumer rules; suppress noisy warnings from
# tooling-only classes.
# ---------------------------------------------------------------------------
-dontwarn androidx.compose.**
