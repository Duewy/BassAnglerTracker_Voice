// ---------------------------------------------
// 🧩 Plugin Configuration
// ---------------------------------------------
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

// ---------------------------------------------
// ⚙️ Android Configuration Block
// ---------------------------------------------
android {
    namespace = "com.bramestorm.bassanglertracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bramestorm.bassanglertracker" // make sure this is the app you want on Play
        minSdk = 23
        targetSdk = 35
        versionCode = 2             // ↑ must be higher than the last upload for THIS applicationId submission September 04 2025
        versionName = "1.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders.putAll(
            mapOf(
                // if not set, fall back to empty for Maps (so it won’t crash the build)
                "MAPS_API_KEY" to (project.findProperty("MAPS_API_KEY") as String? ?: "AIzaSyDk_AhWI1MnCwFWAVfowN_KlwdV592LtPc"),
                // default to Google’s TEST AdMob App ID if none provided
                "ADMOB_APP_ID" to (project.findProperty("ADMOB_APP_ID") as String?
                    ?: "ca-app-pub-3940256099942544~3347511713")
            )
        )
    }

    // Optional: use test AdMob App ID on debug no matter what
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
            // Add other debug-only flags here if needed
        }
        // release will use whatever is in local.properties
    }

    buildTypes {
        debug {
            // 🟡 Removed buildConfigField for PICOVOICE_API_KEY tried but dropped
        }
        release {
            // Tip: set to false for your first tester build to avoid R8/ProGuard surprises
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
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
        buildConfig = true
    }

    // Using the Kotlin Compose plugin (2.1.0). You can remove this block;
    // the plugin manages the compiler version. If you keep it, ensure it matches.
    // composeOptions { kotlinCompilerExtensionVersion = "1.5.1" }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ---------------------------------------------
// 🔗 Dependencies - Core, Compose, Google, Testing
// ---------------------------------------------
dependencies {
    // 🔧 AndroidX Core Components
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.symbol.processing.api)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui.test.android)

    // 🖼 Jetpack Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // 🧱 Classic Android UI Support
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    // 📍 Google Play Services (Location, Maps, Auth)
    implementation(libs.play.services.location)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.maps)

    // 🔧 Support + Material
    implementation(libs.support.annotations)
    implementation(libs.material)

    // 🔄 JSON Parsing
    implementation(libs.google.gson)

    // ✅ Unit Testing
    testImplementation(libs.junit)

    // ✅ Android Instrumented Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)

    // ✅ Android Media
    implementation(libs.androidx.media.v170)
    implementation(libs.androidx.localbroadcastmanager)

    // 🧪 Compose UI Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 📝 Google Ad Services
    implementation(libs.play.services.ads)
    implementation(libs.protolite.well.known.types)
}
