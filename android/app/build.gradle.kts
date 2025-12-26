plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.0"
    }
}

dependencies {
    // Shared module
    implementation(project(":shared"))
    implementation(project(":core:network"))

    // Compose
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.uiToolingPreview)
    implementation(compose.activityCompose)

    // AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Koin
    implementation(libs.bundles.koin)
    implementation("io.insert-koin:koin-androidx-compose:3.6.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Logging
    implementation(libs.kotlin.logging)
    implementation("ch.qos.logback:logback-classic:1.5.9")
}

