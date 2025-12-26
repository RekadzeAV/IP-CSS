plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    android()
    
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared"))
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
        
        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-java:2.3.5")
            }
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

