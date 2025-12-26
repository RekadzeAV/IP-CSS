pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "ip-camera-surveillance-system"

// Shared Kotlin Multiplatform modules
include(":shared")
include(":shared:common")
include(":shared:domain")
include(":shared:data")

// Platform-specific modules
include(":android")
include(":android:app")
include(":android:ui")
include(":android:platform")

include(":ios")

include(":desktop")
include(":desktop:common")
include(":desktop:windows")
include(":desktop:linux")
include(":desktop:macos")

// Server modules
include(":server")
include(":server:nas")
include(":server:web")
include(":server:api")

// Native C++ libraries
include(":native")
include(":native:video-processing")
include(":native:analytics")
include(":native:codecs")

// Core cross-platform modules
include(":core")
include(":core:license")
include(":core:network")
include(":core:database")
include(":core:utils")

