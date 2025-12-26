plugins {
    kotlin("multiplatform") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    id("com.android.application") version "8.7.0" apply false
    id("com.android.library") version "8.7.0" apply false
    id("org.jetbrains.compose") version "1.7.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("app.cash.sqldelight") version "2.0.3" apply false
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
}

tasks.register("buildAll") {
    dependsOn(
        ":core:common:build",
        ":shared:build",
        ":core:license:build",
        ":core:network:build"
    )
    group = "build"
    description = "Build all available modules"
}

tasks.register("testAll") {
    dependsOn(
        ":core:common:test",
        ":shared:test",
        ":core:license:test",
        ":core:network:test"
    )
    group = "verification"
    description = "Run all tests"
}
