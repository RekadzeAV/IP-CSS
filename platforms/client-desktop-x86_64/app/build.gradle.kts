plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

// E2E Test configurations
val e2eTestImplementation by configurations.creating
val e2eTestCompileOnly by configurations.creating
val e2eTestRuntimeOnly by configurations.creating

// E2E Test sourceSet
val e2eTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    compileClasspath += sourceSets.test.get().output
    runtimeClasspath += output
    runtimeClasspath += sourceSets.test.get().output
}

dependencies {
    // Shared KMP module
    implementation(project(":shared"))

    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:network"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.materialIconsExtended)

    // Koin for Dependency Injection
    implementation(libs.koin.core)
    implementation("io.insert-koin:koin-compose:1.1.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation(libs.kotlin.logging)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("junit:junit:4.13.2")
    
    // E2E Testing dependencies
    e2eTestImplementation(kotlin("test"))
    e2eTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    e2eTestImplementation("org.seleniumhq.selenium:selenium-java:4.15.0")
    e2eTestImplementation("org.seleniumhq.selenium:selenium-chrome-driver:4.15.0")
    e2eTestImplementation("io.mockk:mockk:1.13.8")
    e2eTestImplementation("junit:junit:4.13.2")
    
    // E2E Test dependencies on shared and core modules
    e2eTestImplementation(project(":shared"))
    e2eTestImplementation(project(":core:common"))
    e2eTestImplementation(libs.kotlinx.coroutines.core)
    e2eTestImplementation("io.ktor:ktor-client-core:${libs.versions.ktor.get()}")
    e2eTestImplementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")
    e2eTestImplementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")
    e2eTestImplementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")
    
    // Compose Runtime для e2eTest (требуется Compose plugin)
    e2eTestImplementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.company.ipcamera.desktop.MainKt"
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )

            packageName = "IP-CSS Desktop"
            packageVersion = "1.0.0"
            description = "IP Camera Surveillance System - Desktop Client"
            vendor = "Company"

            windows {
                msiPackageVersion = "1.0.0"
                upgradeUuid = "18159995-d967-4cd2-8885-77BFA97CFA9F"
            }

            macOS {
                bundleID = "com.company.ipcamera.desktop"
                signing {
                    sign.set(false)
                }
            }

            linux {
                debMaintainer = "company@example.com"
            }
        }
    }
}

tasks.register<Test>("e2eTest") {
    description = "Runs E2E tests"
    group = "verification"
    
    testClassesDirs = e2eTest.output.classesDirs
    classpath = e2eTest.runtimeClasspath
    
    useJUnitPlatform()
    
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    
    outputs.upToDateWhen { false }
    mustRunAfter(tasks["test"])
}

