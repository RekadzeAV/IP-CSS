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

// Shared Kotlin Multiplatform module
include(":shared")

// Core cross-platform modules
include(":core:license")
include(":core:network")

// Native C++ libraries
// Note: Native modules are built with CMake, not Gradle
// They are included here for reference but won't be built by Gradle
// include(":native")
// include(":native:video-processing")
// include(":native:analytics")

// Server modules
// Note: server:web is a Node.js project, not a Gradle module
// It should be built separately using npm/yarn

