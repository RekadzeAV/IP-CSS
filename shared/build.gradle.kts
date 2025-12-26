plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("app.cash.sqldelight")
    // Development tools (optional, can be applied at root level)
    // id("io.gitlab.arturbosch.detekt")
    // id("org.jlleitschuh.gradle.ktlint")
    // id("org.jetbrains.dokka")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core common module
                implementation(project(":core:common"))

                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Serialization
                implementation(libs.kotlinx.serialization.json)

                // Ktor Client
                implementation(libs.bundles.ktor)

                // SQLDelight
                implementation(libs.sqldelight.runtime)

                // Logging
                implementation(libs.kotlin.logging)

                // Date/Time
                implementation(libs.kotlinx.datetime)

                // Dependency Injection
                implementation(libs.bundles.koin)

                // Core network module
                implementation(project(":core:network"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlinx.coroutines.test)
                // SQLDelight driver for testing
                implementation(libs.sqldelight.sqlite.driver)
                // Testing libraries
                implementation(libs.bundles.testing)
                // Koin testing
                implementation(libs.koin.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.sqldelight.android.driver)
                implementation(project(":core:network"))
                implementation(libs.androidx.work.runtime.ktx)
                // Optional: OpenCV for Android (uncomment if needed)
                // implementation(libs.opencv.android)
                // Optional: TensorFlow Lite for Android (uncomment if needed)
                // implementation(libs.tensorflow.lite)
                // implementation(libs.tensorflow.lite.gpu)
                // implementation(libs.tensorflow.lite.support)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("app.cash.sqldelight:sqlite-driver:2.0.3")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqldelight.native.driver)
                implementation(project(":core:network"))
            }
        }

        val iosX64Main by getting {
            dependsOn(iosMain)
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

android {
    namespace = "com.company.ipcamera.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("CameraDatabase") {
            packageName.set("com.company.ipcamera.shared.database")
            generateAsync.set(true)
        }
    }
}

