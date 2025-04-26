// ---------------------------------------------
// 🧩 Plugin Configuration
// ---------------------------------------------
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
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
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }


        buildTypes {
            debug {
                // 🟡 Removed buildConfigField for PICOVOICE_API_KEY
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
            buildConfig = true // Keep this if you're using BuildConfig elsewhere
        }

        composeOptions {
            kotlinCompilerExtensionVersion = "1.5.1"
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

        // ✅ Android Media
        implementation("androidx.media:media:1.6.0")


        // 🧪 Compose UI Testing
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
        implementation(kotlin("script-runtime"))
    }
}
