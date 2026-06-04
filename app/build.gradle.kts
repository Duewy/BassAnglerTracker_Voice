import java.util.Properties
// ---------------------------------------------
// 🧩 Plugin Configuration
// ---------------------------------------------
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.plugin.compose)
}

// ---------------------------------------------
// ⚙️ Android Configuration Block
// ---------------------------------------------
android {
    namespace = "com.bramestorm.bassanglertracker"
    compileSdk = 35
                // Suppress the setText lint warnings project-wide
    lint {
        disable += "SetTextI18n"
    }

    defaultConfig {
        applicationId = "com.bramestorm.bassanglertracker"
        minSdk = 23
        targetSdk = 35
        versionCode = 4         // 04June2026
        versionName = "1.6.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders.putAll(
            mapOf(
                "MAPS_API_KEY" to (project.findProperty("MAPS_API_KEY") as String? ?: "AIzaSyDk_AhWI1MnCwFWAVfowN_KlwdV592LtPc"),
                "ADMOB_APP_ID" to (project.findProperty("ADMOB_APP_ID") as String? ?: "MISSING_ADMOB_APP_ID")
            )
        )
    }
    // --- ADD THIS (Phase A) ---
    flavorDimensions += "edition"

    productFlavors {
        create("free") {
            dimension = "edition"

            // Side-by-side install support (recommended)
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"

            // Displayed app name
            resValue("string", "app_name", "Catch and Call Free")

            // Flags for later (Phase B barriers)
            buildConfigField("Boolean", "FEATURE_VOICE_COMMANDS", "false")
            buildConfigField("Boolean", "FEATURE_GPS_LOGGING", "false")
            buildConfigField("Boolean", "FEATURE_DAILY_AD", "true")
            buildConfigField("Boolean", "FEATURE_CATCHENTRY_BANNER_ADS", "true")
        }

        create("tracker") {
            dimension = "edition"

            applicationIdSuffix = ".tracker"
            versionNameSuffix = "-tracker"

            resValue("string", "app_name", "Catch and Call Tracker")

            buildConfigField("Boolean", "FEATURE_VOICE_COMMANDS", "false")
            buildConfigField("Boolean", "FEATURE_GPS_LOGGING", "true")
            buildConfigField("Boolean", "FEATURE_DAILY_AD", "true")
            buildConfigField("Boolean", "FEATURE_CATCHENTRY_BANNER_ADS", "false")
        }

        create("provc") {
            dimension = "edition"

            applicationIdSuffix = ".provc"
            versionNameSuffix = "-provc"

            resValue("string", "app_name", "Catch and Call ProVC")

            buildConfigField("Boolean", "FEATURE_VOICE_COMMANDS", "true")
            buildConfigField("Boolean", "FEATURE_GPS_LOGGING", "true")
            buildConfigField("Boolean", "FEATURE_DAILY_AD", "true")
            buildConfigField("Boolean", "FEATURE_CATCHENTRY_BANNER_ADS", "false")
        }
    }
    // --- END ADD ---

    // --- SIGNING CONFIG ---
    val localProps = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localProps.load(localPropsFile.inputStream())
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("RELEASE_STORE_FILE", ""))
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }
    // --- END SIGNING CONFIG ---

                //TODO: release APK currently uses the Google test AdMob ID, which means ads won't generate revenue.
                     //  Same story for MAPS_API_KEY — your fallback is a real-looking key, so verify it's not restricted to debug SHA-1 only

    buildTypes {
        debug {
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            signingConfig = signingConfigs.getByName("release")
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

    // 🔧 Annotations + Material
    implementation(libs.androidx.annotation)
    implementation(libs.material)

    // 🔄 JSON Parsing
    implementation(libs.google.gson)
    implementation(libs.afs.native)

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

    // 🪧 Google Update Service
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // 💸💳🪙 Google Billing
    implementation("com.android.billingclient:billing-ktx:7.1.1")
}
