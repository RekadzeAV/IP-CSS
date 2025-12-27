plugins {
    kotlin("multiplatform") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    id("com.android.application") version "8.7.0" apply false
    id("com.android.library") version "8.7.0" apply false
    id("org.jetbrains.compose") version "1.7.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("app.cash.sqldelight") version "2.0.3" apply false
    // Publishing
    id("maven-publish") apply false
    // Development tools
    id("io.gitlab.arturbosch.detekt") version "1.24.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.0.0" apply false
    id("org.jetbrains.dokka") version "1.9.20" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://jitpack.io") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
    dependsOn("cleanNativeLibraries")
}

tasks.register("buildAll") {
    dependsOn(
        "buildNativeLibraries",
        ":core:common:build",
        ":shared:build",
        // ":core:license:build", // Отложено: лицензирование вынесено за рамки проекта
        ":core:network:build"
    )
    group = "build"
    description = "Build all available modules"
}

tasks.register("testAll") {
    dependsOn(
        ":core:common:test",
        ":shared:test",
        // ":core:license:test", // Отложено: лицензирование вынесено за рамки проекта
        ":core:network:test"
    )
    group = "verification"
    description = "Run all tests"
}

tasks.register("publishToLocalMaven") {
    dependsOn(
        ":core:common:publishToMavenLocal",
        ":core:network:publishToMavenLocal",
        // ":core:license:publishToMavenLocal", // Отложено: лицензирование вынесено за рамки проекта
        ":shared:publishToMavenLocal"
    )
    group = "publishing"
    description = "Publish all modules to local Maven repository (~/.m2/repository)"
}

// =============================================================================
// NAS Packages Build Tasks
// =============================================================================

tasks.register("buildNasPackages") {
    group = "build"
    description = "Build all NAS packages (Synology, QNAP, Asustor, TrueNAS)"
    dependsOn(":server:api:build")

    doLast {
        println("Building NAS packages...")
        println("Run: ./scripts/build-nas-package.sh <type> <arch> [version]")
        println("  Types: synology, qnap, asustor, truenas")
        println("  Arch: x86_64, arm64")
        println("  Example: ./scripts/build-nas-package.sh synology x86_64 Alfa-0.0.1")
    }
}

tasks.register<Exec>("buildNasPackageSynologyX86") {
    group = "build"
    description = "Build Synology SPK package for x86_64"
    dependsOn(":server:api:build")
    commandLine("bash", "scripts/build-nas-package.sh", "synology", "x86_64", "Alfa-0.0.1")
}

tasks.register<Exec>("buildNasPackageSynologyArm") {
    group = "build"
    description = "Build Synology SPK package for ARM64"
    dependsOn(":server:api:build")
    commandLine("bash", "scripts/build-nas-package.sh", "synology", "arm64", "Alfa-0.0.1")
}

tasks.register<Exec>("buildNasPackageQnapX86") {
    group = "build"
    description = "Build QNAP QPKG package for x86_64"
    dependsOn(":server:api:build")
    commandLine("bash", "scripts/build-nas-package.sh", "qnap", "x86_64", "Alfa-0.0.1")
}

tasks.register<Exec>("buildNasPackageQnapArm") {
    group = "build"
    description = "Build QNAP QPKG package for ARM64"
    dependsOn(":server:api:build")
    commandLine("bash", "scripts/build-nas-package.sh", "qnap", "arm64", "Alfa-0.0.1")
}

tasks.register<Exec>("buildNasPackageAsustorX86") {
    group = "build"
    description = "Build Asustor APK package for x86_64"
    dependsOn(":server:api:build")
    commandLine("bash", "scripts/build-nas-package.sh", "asustor", "x86_64", "Alfa-0.0.1")
}

tasks.register<Exec>("buildNasPackageAsustorArm") {
    group = "build"
    description = "Build Asustor APK package for ARM64"
    dependsOn(":server:api:build")
    commandLine("bash", "scripts/build-nas-package.sh", "asustor", "arm64", "Alfa-0.0.1")
}

tasks.register<Exec>("buildNasPackageTruenas") {
    group = "build"
    description = "Build TrueNAS package (Docker/Kubernetes)"
    dependsOn(":server:api:build")
    commandLine("bash", "scripts/build-nas-package.sh", "truenas", "x86_64", "Alfa-0.0.1")
}

// =============================================================================
// Native Libraries Build Tasks
// =============================================================================

tasks.register("buildNativeLibraries") {
    group = "build"
    description = "Build all native libraries for current platform"
    dependsOn(
        "buildNativeVideoProcessing",
        "buildNativeAnalytics",
        "buildNativeCodecs"
    )
}

tasks.register<Exec>("buildNativeVideoProcessing") {
    group = "build"
    description = "Build video-processing native library"
    workingDir = file("native")
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()

    when {
        os.contains("windows") -> {
            commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "../scripts/build-all-native-libs.ps1")
        }
        os.contains("linux") || os.contains("mac") -> {
            commandLine("bash", "../scripts/build-all-native-libs.sh", "all")
        }
        else -> {
            doFirst {
                throw GradleException("Unsupported OS: $os")
            }
        }
    }
}

tasks.register<Exec>("buildNativeAnalytics") {
    group = "build"
    description = "Build analytics native library"
    dependsOn("buildNativeVideoProcessing")
    // Analytics собирается вместе с video-processing в одном скрипте
    doFirst {
        println("Analytics library will be built with video-processing")
    }
}

tasks.register<Exec>("buildNativeCodecs") {
    group = "build"
    description = "Build codecs native library"
    dependsOn("buildNativeVideoProcessing")
    // Codecs собирается вместе с video-processing в одном скрипте
    doFirst {
        println("Codecs library will be built with video-processing")
    }
}

tasks.register<Exec>("buildNativeForLinux") {
    group = "build"
    description = "Build native libraries for Linux x64"
    workingDir = file("native")
    commandLine("bash", "../scripts/build-all-native-libs.sh", "linux")
}

tasks.register<Exec>("buildNativeForMacOS") {
    group = "build"
    description = "Build native libraries for macOS (x64 and arm64)"
    workingDir = file("native")
    commandLine("bash", "../scripts/build-all-native-libs.sh", "macos")
}

tasks.register<Exec>("buildNativeForWindows") {
    group = "build"
    description = "Build native libraries for Windows x64"
    workingDir = file("native")
    commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "../scripts/build-all-native-libs.ps1")
}

tasks.register("cleanNativeLibraries") {
    group = "clean"
    description = "Clean native libraries build artifacts"
    doLast {
        delete(fileTree("native/build") { include("**/*") })
        delete(fileTree("native/video-processing/lib") { include("**/*") })
        delete(fileTree("native/analytics/lib") { include("**/*") })
        delete(fileTree("native/codecs/lib") { include("**/*") })
    }
}
