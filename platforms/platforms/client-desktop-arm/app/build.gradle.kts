plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
}

dependencies {
    // Shared KMP module
    implementation(project(":shared"))

    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    // implementation(project(":core:license")) // Отложено: лицензирование вынесено за рамки проекта

    // Compose Desktop
    implementation(compose.desktop.currentOs)

    // Koin for Dependency Injection
    implementation(libs.koin.core)

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

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg
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

