import java.time.Duration

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("maven-publish")
    id("org.jetbrains.kotlinx.kover")
}

val buildAndroidNative = project.findProperty("ipcss.buildAndroidNative")?.toString()?.toBoolean() == true
val buildNative = project.findProperty("ipcss.buildNative")?.toString()?.toBoolean() == true

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // Android Native targets с cinterop
    if (buildAndroidNative) {
        androidNativeArm64("androidNativeArm64") {
            compilations.getByName("main") {
                cinterops {
                    val rtspClient by creating {
                        defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                        packageName("com.company.ipcamera.core.network.rtsp")
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                        linkerOpts("-L${project.rootDir}/native/video-processing/lib/android/arm64-v8a -lvideo_processing")
                    }
                    val videoProcessing by creating {
                        defFile(project.file("src/nativeInterop/cinterop/video_processing.def"))
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                        linkerOpts("-L${project.rootDir}/native/video-processing/lib/android/arm64-v8a -lvideo_processing")
                    }
                }
            }
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // iOS targets с cinterop (только если buildNative=true)
    if (buildNative) {
        iosX64 {
            compilations.getByName("main") {
                cinterops {
                    val rtspClient by creating {
                        defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                        packageName("com.company.ipcamera.core.network.rtsp")
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                        linkerOpts("-L${project.rootDir}/native/video-processing/lib/ios/x64 -lvideo_processing")
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
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                        linkerOpts("-L${project.rootDir}/native/video-processing/lib/ios/arm64 -lvideo_processing")
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
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                        linkerOpts("-L${project.rootDir}/native/video-processing/lib/ios/simulator-arm64 -lvideo_processing")
                    }
                }
            }
        }

        // Native targets для cinterop (Linux и macOS)
        linuxX64("nativeLinux") {
            compilations.getByName("main") {
                cinterops {
                    val rtspClient by creating {
                        defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                        packageName("com.company.ipcamera.core.network.rtsp")
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                            linkerOpts("-L${project.rootDir}/native/video-processing/lib/linux/x64 -lvideo_processing")
                    }
                    val videoProcessing by creating {
                        defFile(project.file("src/nativeInterop/cinterop/video_processing.def"))
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                            linkerOpts("-L${project.rootDir}/native/video-processing/lib/linux/x64 -lvideo_processing")
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
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                            linkerOpts("-L${project.rootDir}/native/video-processing/lib/macos/x64 -lvideo_processing")
                    }
                    val videoProcessing by creating {
                        defFile(project.file("src/nativeInterop/cinterop/video_processing.def"))
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                            linkerOpts("-L${project.rootDir}/native/video-processing/lib/macos/x64 -lvideo_processing")
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
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                            linkerOpts("-L${project.rootDir}/native/video-processing/lib/macos/arm64 -lvideo_processing")
                    }
                    val videoProcessing by creating {
                        defFile(project.file("src/nativeInterop/cinterop/video_processing.def"))
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                            linkerOpts("-L${project.rootDir}/native/video-processing/lib/macos/arm64 -lvideo_processing")
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
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                    }
                    val videoProcessing by creating {
                        defFile(project.file("src/nativeInterop/cinterop/video_processing.def"))
                        compilerOpts("-I${project.rootDir}/native/video-processing/include")
                        includeDirs("${project.rootDir}/native/video-processing/include")
                    }
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
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
            }
        }

        val androidMain by getting

        // iOS source sets (только если buildNative=true)
        if (buildNative) {
            val iosMain by getting
            val iosX64Main by getting
            val iosArm64Main by getting
            val iosSimulatorArm64Main by getting
        }

        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.serialization.kotlinx.xml)
            }
        }

        val desktopMain by getting {
            dependsOn(commonMain)
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.ktor.client.java)
                // JavaCV для декодирования H.264/H.265 через FFmpeg
                // Обновлено до версии 1.5.13 для исправления проблем с типами JavaCPP
                implementation("org.bytedeco:javacv:1.5.13")
                implementation("org.bytedeco:ffmpeg-platform:8.0.1-1.5.13")
            }
        }

        // desktopTest автоматически создается для jvm("desktop") target
        val desktopTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libs.ktor.client.java)
                implementation(libs.bundles.testing)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        // Native source sets (только если buildNative=true)
        if (buildNative) {
            val nativeMain by getting
            val nativeLinuxMain by getting
            val nativeMacosX64Main by getting
            val nativeMacosArm64Main by getting
            val nativeWindowsMain by getting
        }

        // Android Native source sets — только при ipcss.buildAndroidNative=true
        if (buildAndroidNative) {
            val androidNativeArm32Main by getting { dependsOn(commonMain) }
            val androidNativeArm64Main by getting { dependsOn(commonMain) }
            val androidNativeX86Main by getting { dependsOn(commonMain) }
            val androidNativeX64Main by getting { dependsOn(commonMain) }
        }
    }

    // Android depends on JVM for expect/actual resolution
    sourceSets.getByName("androidMain").dependsOn(sourceSets.getByName("jvmMain"))
    sourceSets.getByName("androidMain").dependencies {
        implementation(libs.ktor.client.android)
        implementation(libs.ktor.client.okhttp)
    }
}

android {
    namespace = "com.company.ipcamera.core.network"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// =============================================================================
// Автоматическая сборка нативной библиотеки перед компиляцией Kotlin
// =============================================================================

// Задача для определения текущей платформы
fun getCurrentPlatform(): String {
    return when {
        System.getProperty("os.name").contains("Windows", ignoreCase = true) -> "windows"
        System.getProperty("os.name").contains("Mac", ignoreCase = true) -> "macos"
        System.getProperty("os.name").contains("Linux", ignoreCase = true) -> "linux"
        else -> "linux"
    }
}

fun getCurrentArch(): String {
    val arch = System.getProperty("os.arch").lowercase()
    return when {
        arch.contains("aarch64") || arch.contains("arm64") -> "arm64"
        arch.contains("x86_64") || arch.contains("amd64") -> "x64"
        arch.contains("x86") -> "x86"
        else -> "x64"
    }
}

// Задача для сборки нативной библиотеки для текущей платформы
tasks.register<Exec>("buildNativeVideoProcessingForCurrentPlatform") {
    group = "build"
    description = "Build native video_processing library for current platform"

    val platform = getCurrentPlatform()
    val arch = getCurrentArch()

    workingDir = file("${rootProject.projectDir}/native/video-processing")
    commandLine = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        listOf(
            "powershell",
            "-ExecutionPolicy", "Bypass",
            "-NoProfile",
            "-NonInteractive",
            "-File",
            "${rootProject.projectDir}/scripts/build-video-processing-lib.ps1"
        )
    } else {
        listOf(
            "bash",
            "${rootProject.projectDir}/scripts/build-video-processing-lib.sh",
            platform,
            arch,
            "Release"
        )
    }

    doFirst {
        println("Building native library for platform: $platform/$arch")
    }

    doLast {
        println("Native library build completed for $platform/$arch")
    }
}

// Windows: avoid IOException when Gradle deletes a locked previous binary (output.bin).
// Fresh directory per task configuration (same pattern as :shared).
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    binaryResultsDirectory.set(
        layout.buildDirectory.dir("test-results/${name}-binary-${System.nanoTime()}")
    )
    timeout.set(Duration.ofMinutes(10))
    ignoreFailures = true
}

// Задача для проверки существования нативной библиотеки
tasks.register("checkNativeLibrary") {
    group = "verification"
    description = "Check if native library exists for current platform"

    val platform = getCurrentPlatform()
    val arch = getCurrentArch()

    val libPath = when {
        platform == "windows" -> "${rootProject.projectDir}/native/video-processing/lib/windows/$arch/video_processing.dll"
        platform == "macos" -> "${rootProject.projectDir}/native/video-processing/lib/macos/$arch/libvideo_processing.dylib"
        else -> "${rootProject.projectDir}/native/video-processing/lib/linux/$arch/libvideo_processing.so"
    }

    doLast {
        val libFile = file(libPath)
        if (!libFile.exists()) {
            throw GradleException("Native library not found at: $libPath\nRun: ./gradlew :core:network:buildNativeVideoProcessingForCurrentPlatform")
        } else {
            println("Native library found: $libPath")
        }
    }
}

// Настройка зависимостей: сборка нативной библиотеки только для нативных таргетов (не для JVM desktop)
kotlin {
    targets.all {
        val requiresNativeLibrary = name.startsWith("native") ||
            name.startsWith("ios") ||
            (buildAndroidNative && name.startsWith("androidNative"))

        if (requiresNativeLibrary) {
            compilations.all {
                compileTaskProvider.configure {
                    dependsOn("buildNativeVideoProcessingForCurrentPlatform")
                    dependsOn("checkNativeLibrary")
                }
            }
        }
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

kover {
    reports {
        verify {
            rule {
                minBound(25)
            }
        }
    }
}


