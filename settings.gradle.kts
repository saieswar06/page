// ✅ settings.gradle.kts — correct and complete

pluginManagement {
    repositories {
        google()                // Required for Google & Android Gradle plugins
        mavenCentral()
        gradlePluginPortal()
    }

    // ✅ Declare Google Services plugin version here
    plugins {
        id("com.google.gms.google-services") version "4.4.2"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()                // Required for com.google.android.gms libraries
        mavenCentral()
    }
}

rootProject.name = "page"
include(":app")
