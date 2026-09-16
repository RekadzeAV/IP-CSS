import java.time.Duration

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("app.cash.sqldelight")
    id("maven-publish")
    id("org.jetbrains.kotlinx.kover")
    // Development tools (optional, can be applied at root level)
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    // id("org.jetbrains.dokka")
}

kover {
    reports {
        verify {
            rule {
                minBound(20)
            }
        }
    }
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

    // Native targets - only if enabled globally
    val buildNative = project.findProperty("ipcss.buildNative")?.toString()?.toBoolean() == true
    
    if (buildNative) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

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
                api(libs.sqldelight.async.extensions)

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
                // kotlin("test") резолвится по платформам автоматически: junit4-вариант
                // для JVM (desktopTest) и для Android unit-test компиляций.
                // Пара test-common + test-annotations-common без платформенной реализации
                // давала "Unresolved reference 'Test'" в compileDebugUnitTestKotlinAndroid.
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                // Koin testing
                implementation(libs.koin.test)
            }
        }

        val desktopTest by getting {
            dependsOn(commonTest)
            dependencies {
                // SQLDelight JDBC driver for in-memory testing (JVM-only)
                implementation(libs.sqldelight.sqlite.driver)
                // Testing libraries (JVM-only)
                implementation(libs.bundles.testing)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.sqldelight.android.driver)
                implementation(project(":core:network"))
                implementation(libs.androidx.work.runtime.ktx)
                implementation("androidx.core:core-ktx:1.13.1")
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
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        // iOS source sets - only if buildNative=true
        if (buildNative) {
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
        }

        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.sqldelight.sqlite.driver)
                // JdbcDriver / JdbcSqliteDriver: типы для PostgresParityDriverSupport.jvm
                implementation("app.cash.sqldelight:jdbc-driver:2.0.2")
            }
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
        }
    }
}

// Windows: avoid IOException when Gradle deletes a locked previous binary (output.bin).
// Fresh directory per task configuration so AbstractTestTask does not need to wipe an in-use path.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    binaryResultsDirectory.set(
        layout.buildDirectory.dir("test-results/$name-binary-${System.nanoTime()}"),
    )
    // Guard against indefinite hangs in desktop/common test runs on Windows CI/local.
    timeout.set(Duration.ofMinutes(10))
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
            // Используем SQLite 3.35+ для поддержки ON CONFLICT DO UPDATE
            // Диалект указывается через зависимость в dependencies блоках
            // Настройка миграций
            // SQLDelight автоматически ищет исходные .sqm в поддиректории migrations рядом с .sq.
            // migrationOutputDirectory — только для задачи generate*Migrations (генерирует .sql).
            // Нельзя указывать ту же папку, что и у .sqm: Gradle очистит каталог и удалит ручные миграции.
            migrationOutputDirectory.set(layout.buildDirectory.dir("generated/sqldelight/migration-output"))
            // Версия базы данных
            schemaOutputDirectory.set(file("src/commonMain/sqldelight"))
        }
    }
}

// SQLDelight диалект SQLite 3.35+ для поддержки ON CONFLICT DO UPDATE
// В SQLDelight 2.1.0 диалект указывается через зависимость в dependencies блоках source sets
// Добавляем зависимость на диалект в commonMain

// SQLDelight диалект SQLite 3.35+ для поддержки ON CONFLICT DO UPDATE
// Диалект указывается через зависимость в sqldelight блоке

// Exclude SQLDelight generated code and comments from ktlint
ktlint {
    android = true
    ignoreFailures = true
    filter {
        exclude("**/generated/**")
    }
    disabledRules.set(setOf("discouraged-comment-location", "standard:indent", "standard:wrapping"))
}
afterEvaluate {
    publishing {
        publications {
            all {
                if (this is MavenPublication) {
                    groupId = "com.company.ipcamera"
                    version = project.version.toString()

                    pom {
                        name.set("IP Camera Shared")
                        description.set("Shared Kotlin Multiplatform module for IP Camera Surveillance System")
                        url.set("https://github.com/company/ip-camera-surveillance-system")
                    }
                }
            }
        }
    }
}