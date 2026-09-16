pluginManagement {
    repositories {
        maven { url = uri("https://plugins.gradle.org/m2/") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ip-camera-surveillance-system"

// =============================================================================
// CORE MODULES
// =============================================================================
include(":shared")
include(":core:common")
include(":core:network")
include(":core:ui-bridge")
include(":core:security")
include(":core:test-jvm")
// include(":core:license") // Отложено: лицензирование вынесено за рамки проекта

// =============================================================================
// APPLICATION MODULES
// =============================================================================
include(":android:app")
include(":server:api")

// =============================================================================
// PLATFORM MODULES
// =============================================================================
include(":platforms:client-desktop-x86_64:app")
project(":platforms:client-desktop-x86_64:app").projectDir = file("platforms/client-desktop-x86_64/app")

include(":platforms:client-desktop-arm:app")
project(":platforms:client-desktop-arm:app").projectDir = file("platforms/client-desktop-arm/app")

include(":platforms:nas-x86_64:build")
project(":platforms:nas-x86_64:build").projectDir = file("platforms/nas-x86_64/nas-build")

include(":platforms:nas-arm:build")
project(":platforms:nas-arm:build").projectDir = file("platforms/nas-arm/nas-build")
