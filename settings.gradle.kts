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

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "ip-camera-surveillance-system"

// =============================================================================
// ПЛАТФОРМЫ ПРОЕКТА
// =============================================================================
// Проект разделен на платформы. См. PLATFORM_STRUCTURE.md для подробностей:
//
// Серверные платформы с веб-интерфейсом:
// - platforms/sbc-arm/        - Микрокомпьютеры ARM (использует :server:api)
// - platforms/server-x86_64/  - Серверы x86-x64 (использует :server:api)
// - platforms/nas-arm/        - NAS ARM (использует :server:api)
// - platforms/nas-x86_64/     - NAS x86-x64 (использует :server:api)
//
// Клиентские платформы:
// - platforms/client-desktop-x86_64/ - Desktop x86-x64 (планируется)
// - platforms/client-desktop-arm/    - Desktop ARM (планируется)
// - platforms/client-android/        - Android (использует :android:app)
// - platforms/client-ios/            - iOS/macOS (планируется)
//
// Все платформы используют общие модули: :shared, :core:*, :native
// =============================================================================

// Shared Kotlin Multiplatform module
// Используется всеми платформами
include(":shared")

// Core cross-platform modules
// Используются всеми платформами
include(":core:common")
include(":core:license")
include(":core:network")

// Android app
// Используется платформой: platforms/client-android/
include(":android:app")

// Native C++ libraries
// Note: Native modules are built with CMake, not Gradle
// They are included here for reference but won't be built by Gradle
// Используются всеми платформами (сборка через CMake для каждой архитектуры)
// include(":native")
// include(":native:video-processing")
// include(":native:analytics")

// Server modules
// Используется платформами: platforms/sbc-arm/, platforms/server-x86_64/,
//                           platforms/nas-arm/, platforms/nas-x86_64/
include(":server:api")
// Note: server:web is a Node.js project, not a Gradle module
// It should be built separately using npm/yarn
// Используется всеми серверными платформами с веб-интерфейсом

