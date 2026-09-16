plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
}

// Условный google-services: приложение собирается и без android/app/google-services.json
// (FCM активируется автоматически при появлении конфигурации — задача 2.2)
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.company.ipcamera.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.company.ipcamera.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // NDK фильтры (опционально, если нужны только определенные архитектуры)
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    // Debug: ./gradlew :android:app:assembleDebug -Pipcss.localFrameAnalytics=true
    buildTypes {
        debug {
            val enableLocalFrameAnalytics =
                (project.findProperty("ipcss.localFrameAnalytics") as? String)
                    .equals("true", ignoreCase = true) == true
            buildConfigField("boolean", "LOCAL_FRAME_ANALYTICS", enableLocalFrameAnalytics.toString())
        }
        release {
            buildConfigField("boolean", "LOCAL_FRAME_ANALYTICS", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Конфигурация для нативных библиотек (JNI)
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs", "../native/video-processing/lib/android")
        }
    }

    // D8/R8 memory configuration
    // AGP 8.0+ uses separate process for D8 - configure via gradle.properties
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // Shared module
    implementation(project(":shared"))
    // Direct dependencies are required for Android app compile-time symbols.
    implementation(project(":core:network"))
    implementation(project(":core:common"))

    // Compose
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
    implementation(compose.uiTooling)
    implementation("androidx.activity:activity-compose:1.9.3")

    // AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Koin
    implementation(libs.bundles.koin)
    implementation("io.insert-koin:koin-androidx-compose:3.5.6")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)

    // Logging
    implementation(libs.kotlin.logging)

    // ExoPlayer (Media3) for video playback
    val media3Version = "1.2.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    // HLS через OkHttp с теми же cookies, что и Ktor ApiClient (JWT httpOnly)
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")

    // FCM push (каналы/сервис: service/PushNotificationService; активация — при наличии google-services.json)
    implementation("com.google.firebase:firebase-messaging:24.0.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
}

