plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Shared KMP module
    implementation(project(":shared"))

    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:network"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.ui)

    // Koin for Dependency Injection
    implementation(libs.koin.core)
    implementation("io.insert-koin:koin-compose:1.1.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation(libs.kotlin.logging)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
}

compose.desktop {
    application {
        mainClass = "com.company.ipcamera.desktop.MainKt"
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi
            )

            packageName = "IP-CSS Desktop"
            packageVersion = "1.0.0"
            description = "IP Camera Surveillance System - Desktop Client (ARM)"
            vendor = "Company"

            macOS {
                bundleID = "com.company.ipcamera.desktop"
                signing {
                    sign.set(false)
                }
            }

            linux {
                debMaintainer = "company@example.com"
            }
        }
    }
}

