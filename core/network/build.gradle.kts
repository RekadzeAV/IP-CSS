plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("maven-publish")
}

kotlin {
    android()

    // Android Native targets для cinterop (если нужна нативная интеграция)
    // Примечание: Для Android обычно используется JNI через androidMain,
    // но можно также использовать androidNative* targets для Kotlin/Native
    androidNativeArm32("androidNativeArm32") {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/android/armeabi-v7a -lvideo_processing")
                }
            }
        }
    }

    androidNativeArm64("androidNativeArm64") {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/android/arm64-v8a -lvideo_processing")
                }
            }
        }
    }

    androidNativeX86("androidNativeX86") {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/android/x86 -lvideo_processing")
                }
            }
        }
    }

    androidNativeX64("androidNativeX64") {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/android/x86_64 -lvideo_processing")
                }
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

    // iOS targets с cinterop
    iosX64 {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/ios/x64 -lvideo_processing")
                }
            }
        }
    }

    iosArm64 {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/ios/arm64 -lvideo_processing")
                }
            }
        }
    }

    iosSimulatorArm64 {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/ios/simulator-arm64 -lvideo_processing")
                }
            }
        }
    }

    // Native targets для cinterop (Linux и macOS)
    linuxX64("native") {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/linux/x64 -lvideo_processing")
                }
            }
        }
    }

    macosX64("nativeMacosX64") {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/macos/x64 -lvideo_processing")
                }
            }
        }
    }

    macosArm64("nativeMacosArm64") {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/macos/arm64 -lvideo_processing")
                }
            }
        }
    }

    // Windows (MinGW)
    mingwX64("nativeWindows") {
        compilations.getByName("main") {
            cinterops {
                val rtspClient by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                    packageName("com.company.ipcamera.core.network.rtsp")
                    compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                    includeDirs("${project.rootDir}/../native/video-processing/include")
                    linkerOpts("-L${project.rootDir}/../native/video-processing/lib/windows/x64 -lvideo_processing")
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core common module
                implementation(project(":core:common"))
                // Ktor Client
                implementation(libs.bundles.ktor)
                implementation(libs.ktor.serialization.kotlinx.xml)
                // Serialization
                implementation(libs.kotlinx.serialization.json)
                // Coroutines
                implementation(libs.kotlinx.coroutines.core)
                // Logging
                implementation(libs.kotlin.logging)
                // DateTime
                implementation(libs.kotlinx.datetime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.bundles.testing)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
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

        val jvmMain by creating {
            dependsOn(commonMain)
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }

        val nativeMacosX64Main by getting {
            dependsOn(nativeMain)
        }

        val nativeMacosArm64Main by getting {
            dependsOn(nativeMain)
        }

        val nativeWindowsMain by getting {
            dependsOn(nativeMain)
        }

        // Android Native source sets
        val androidNativeArm32Main by creating {
            dependsOn(commonMain)
        }

        val androidNativeArm64Main by creating {
            dependsOn(commonMain)
        }

        val androidNativeX86Main by creating {
            dependsOn(commonMain)
        }

        val androidNativeX64Main by creating {
            dependsOn(commonMain)
        }
    }
}

android {
    namespace = "com.company.ipcamera.core.network"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
}

// Публикация в локальный Maven репозиторий
afterEvaluate {
    publishing {
        publications {
            all {
                if (this is MavenPublication) {
                    groupId = "com.company.ipcamera"
                    version = project.version.toString()

                    pom {
                        name.set("IP Camera Core Network")
                        description.set("Core network module for IP Camera Surveillance System")
                        url.set("https://github.com/company/ip-camera-surveillance-system")
                    }
                }
            }
        }
    }
}

