plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

group = "com.company.ipcamera"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    // Testing only
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Kover Code Coverage (simplified)
kover {
    reports {
        total {
            html {}
            xml {}
        }
    }
}
