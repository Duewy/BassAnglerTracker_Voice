pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    // ← add this:
    plugins {
        id("com.android.application") version "8.6.0" apply false
        id("com.android.library")     version "8.6.0" apply false
        // If you use the Kotlin Gradle plugin here too, bump it as needed:
        id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BassAnglerTracker"
include(":app")
