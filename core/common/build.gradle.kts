import java.time.Duration

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("maven-publish")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64("nativeLinux")
    macosX64("nativeMacosX64")
    macosArm64("nativeMacosArm64")
    mingwX64("nativeWindows")

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Serialization
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.security.crypto)
                implementation(libs.kotlin.logging)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
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

        val nativeMain by creating {
            dependsOn(commonMain)
        }

        val nativeLinuxMain by getting {
            dependsOn(nativeMain)
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

        // desktopMain автоматически создается для jvm("desktop") target
        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                // Logging для desktop
                implementation(libs.kotlin.logging)
            }
        }

        val desktopTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

android {
    namespace = "com.company.ipcamera.core.common"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Windows: unique binary output per Test task (see MVP_PHASE1_AUTOMATED_ACCEPTANCE.md §7).
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    binaryResultsDirectory.set(
        layout.buildDirectory.dir("test-results/${name}-binary-${System.nanoTime()}")
    )
    timeout.set(Duration.ofMinutes(10))
}

// Публикация в локальный Maven репозиторий
// Для Kotlin Multiplatform Gradle автоматически создает публикации для каждой платформы
// Настраиваем общие параметры для всех публикаций
afterEvaluate {
    publishing {
        publications {
            all {
                if (this is MavenPublication) {
                    groupId = "com.company.ipcamera"
                    version = project.version.toString()

                    // Настройка POM для правильной публикации
                    pom {
                        name.set("IP Camera Core Common")
                        description.set("Core common module for IP Camera Surveillance System")
                        url.set("https://github.com/company/ip-camera-surveillance-system")
                    }
                }
            }
        }
    }
}

