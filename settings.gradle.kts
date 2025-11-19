pluginManagement {
    repositories {
        google()          // 👈 this is required for Google plugins
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.1.2"
        id("com.google.gms.google-services") version "4.4.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://github.com/jitsi/jitsi-maven-repository/raw/master/releases") }
    }
}

rootProject.name = "project"
include(":app")
