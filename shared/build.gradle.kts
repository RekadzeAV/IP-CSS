plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    android {
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
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                
                // Ktor Client
                implementation("io.ktor:ktor-client-core:2.3.5")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
                
                // SQLDelight
                implementation("app.cash.sqldelight:runtime:2.0.0")
                
                // Logging
                implementation("io.github.microutils:kotlin-logging:3.0.5")
                
                // Date/Time
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-android:2.3.5")
                implementation("app.cash.sqldelight:android-driver:2.0.0")
            }
        }
        
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.5")
                implementation("app.cash.sqldelight:native-driver:2.0.0")
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
}

android {
    namespace = "com.company.ipcamera.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 26
        targetSdk = 34
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

