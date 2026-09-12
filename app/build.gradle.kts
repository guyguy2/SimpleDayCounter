import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.room

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.happypuppy.daycounter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.happypuppy.daycounter"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 full mode: code shrinking, optimization, and resource shrinking.
            // Keep rules for Room/Koin/Coroutines/Navigation live in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        // BuildConfig.DEBUG is referenced by Log.d guards across the app to
        // strip debug logging from release builds.
        buildConfig = true
    }

    lint {
        // `Instantiatable` is a known false positive for `MainActivity` which
        // correctly extends `androidx.activity.ComponentActivity` (a subclass
        // of `Activity`). It would otherwise fail `lintVitalRelease` and block
        // `assembleRelease` even though the class is valid at runtime.
        disable += "Instantiatable"
    }

    testOptions {
        unitTests {
            // Return default values (0 / null / false) from Android framework
            // stubs instead of throwing `Method ... not mocked`. The ViewModel
            // calls `android.util.Log.d` from inside coroutines launched on
            // viewModelScope; the unmocked exception silently aborted the
            // coroutine before any state update could be observed.
            isReturnDefaultValues = true
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.room.runtime)
    implementation(libs.androidx.room.ktx)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    ksp(libs.room.compiler)
//    implementation(libs.koin.compose.viewmodel)
    implementation (libs.androidx.material.icons.extended)



    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    // Required by Navigation Compose type-safe @Serializable routes (P2-7).
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}