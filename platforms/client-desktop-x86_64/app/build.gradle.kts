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
    implementation(project(":core:license"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)

    // Koin for Dependency Injection
    implementation(libs.koin.core)
    implementation("io.insert-koin:koin-compose:1.1.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation(libs.kotlin.logging)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DateTime
    implementation(libs.kotlinx.datetime)
}

compose.desktop {
    application {
        mainClass = "com.company.ipcamera.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )

            packageName = "IP-CSS Desktop"
            packageVersion = "1.0.0"
            description = "IP Camera Surveillance System - Desktop Client"
            vendor = "Company"

            windows {
                msiPackageVersion = "1.0.0"
                upgradeUuid = "18159995-d967-4cd2-8885-77BFA97CFA9F"
            }

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

