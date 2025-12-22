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

    defaultConfig {
        applicationId = "com.bramestorm.bassanglertracker"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders.putAll(
            mapOf(
                "MAPS_API_KEY" to (project.findProperty("MAPS_API_KEY") as String? ?: "AIzaSyDk_AhWI1MnCwFWAVfowN_KlwdV592LtPc"),
                "ADMOB_APP_ID" to (project.findProperty("ADMOB_APP_ID") as String? ?: "ca-app-pub-3940256099942544~3347511713")
            )
        )
    }

    buildTypes {
        debug {
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
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
