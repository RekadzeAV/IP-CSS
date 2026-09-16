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

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Удалена зависимость от :shared для устранения циклической зависимости
                // Если нужны общие типы, они должны быть в :core:common
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlin.logging)
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
                implementation(libs.androidx.security.crypto)
                implementation(libs.bouncycastle.provider)
                implementation(libs.bouncycastle.pkix)
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

        val desktopMain by getting {
            dependsOn(commonMain)
        }
    }
}

android {
    namespace = "com.company.ipcamera.core.license"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
                        name.set("IP Camera Core License")
                        description.set("Core license module for IP Camera Surveillance System")
                        url.set("https://github.com/company/ip-camera-surveillance-system")
                    }
                }
            }
        }
    }
}

