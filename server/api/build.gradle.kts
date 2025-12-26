plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.company.ipcamera.server.ApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Shared module
    implementation(project(":shared"))
    implementation(project(":core:network"))

    // Ktor Server
    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.server.websockets)
    
    // Authentication
    implementation("io.ktor:ktor-server-auth:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-auth-jwt:${libs.versions.ktor.get()}")
    
    // JWT
    implementation("com.auth0:java-jwt:4.4.0")

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.kotlin.logging)
    implementation("ch.qos.logback:logback-classic:1.5.9")

    // Dependency Injection
    implementation(libs.bundles.koin)
    implementation("io.insert-koin:koin-ktor:3.6.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testing)
    testImplementation("io.ktor:ktor-server-test-host:${libs.versions.ktor.get()}")
}

tasks.test {
    useJUnitPlatform()
}

