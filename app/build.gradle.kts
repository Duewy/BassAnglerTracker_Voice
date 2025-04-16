// ---------------------------------------------
// 🧩 Plugin Configuration
// ---------------------------------------------
plugins {
    alias(libs.plugins.android.application) // Android app plugin (from libs.versions.toml)
    alias(libs.plugins.jetbrains.kotlin.android) // Kotlin plugin for Android (from libs.versions.toml)
}

// ---------------------------------------------
// 🔐 Load Picovoice Access Key from local.properties
// ---------------------------------------------
val picovoiceApiKey = "sk-TawEvd8BQnSwLVve851+KaCo7U1H7uOVEHsbWKZrz28flmwGzHTX3w=="


// ---------------------------------------------
// ⚙️ Android Configuration Block
// ---------------------------------------------
android {
    // Namespace used for R class and generated code
    namespace = "com.bramestorm.bassanglertracker"

    // Compile against this version of Android SDK
    compileSdk = 35

    // ✅ Native JNI Library Directory for Picovoice models (Porcupine)
    sourceSets {
        getByName("main").jniLibs.srcDirs("src/main/jniLibs")
    }

    // ---------------------------------------------
    // 📦 Default App Configuration
    // ---------------------------------------------
    defaultConfig {
        applicationId = "com.bramestorm.bassanglertracker"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Used for instrumentation testing
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Use backward-compatible vector drawables
        vectorDrawables {
            useSupportLibrary = true
        }

        // 📦 ABI Splits - generates APKs for different architectures
        splits {
            abi {
                isEnable = true
                reset()
                include("armeabi-v7a", "arm64-v8a", "x86", "x86_64") // Supported CPU ABIs
                isUniversalApk = true // Include a universal APK with all ABIs
            }
        }
    }

    // ---------------------------------------------
    // 🏗 Build Types - Debug & Release
    // ---------------------------------------------
    buildTypes {
        debug {
            // 👇 Inject API key into BuildConfig in debug builds
            buildConfigField("String", "PICOVOICE_API_KEY", "\"${picovoiceApiKey}\"")
        }

        release {
            isMinifyEnabled = true // Enables code shrinking and obfuscation
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 🐛 Symbol table for native crash reports (optional)
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }

            // 👇 Also inject API key in release builds
            buildConfigField("String", "PICOVOICE_API_KEY", "\"${picovoiceApiKey}\"")
        }
    }

    // ---------------------------------------------
    // ☕️ Java Compatibility
    // ---------------------------------------------
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 🧠 Kotlin Compatibility
    kotlinOptions {
        jvmTarget = "17"
    }

    // ---------------------------------------------
    // 🖌 Enable Jetpack Compose & BuildConfig
    // ---------------------------------------------
    buildFeatures {
        compose = true // Enable Jetpack Compose UI toolkit
        buildConfig = true // Needed for BuildConfig.PICOVOICE_API_KEY to compile
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    // ---------------------------------------------
    // 📦 Packaging Options
    // ---------------------------------------------
    packaging {
        resources {
            // Exclude duplicate or unnecessary licenses from bundled libs
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ---------------------------------------------
// 🔗 Dependencies - Core, Voice, Compose, Google, Testing
// ---------------------------------------------
dependencies {
    // 🎙️ Voice Wake Word Detection (Picovoice Porcupine)
    implementation("ai.picovoice:porcupine-android:3.0.0")

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

    // 🧪 Compose UI Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
// ------------------- END DEPENDENCIES ------------------------
