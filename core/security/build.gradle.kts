plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    // Desktop (JVM)
    jvm("desktop") {
        compilations.getByName("main") {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }

    // Android
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }

    // Native targets - only if enabled globally
    val buildNative = project.findProperty("ipcss.buildNative")?.toString()?.toBoolean() == true
    
    if (buildNative) {
        // iOS
        iosX64()
        iosArm64()
        iosSimulatorArm64()

        // macOS
        macosX64()
        macosArm64()

        // Linux
        linuxX64("nativeLinux")

        // Windows
        mingwX64("nativeWindows")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core modules
                implementation(project(":core:common"))
                implementation(project(":core:network"))
                
                // Serialization
                implementation(libs.kotlinx.serialization.json)
                
                // Ktor
                implementation(libs.ktor.client.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                // JBCrypt removed - not available in Maven Central
                // TODO: Add alternative bcrypt implementation if needed
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.security.crypto)
            }
        }

        val desktopTest by getting
        
        // Native source sets - only if buildNative=true
        if (buildNative) {
            val nativeMain by getting
        }
    }
}

android {
    namespace = "com.company.ipcamera.core.security"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}