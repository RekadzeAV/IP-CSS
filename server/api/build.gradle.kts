plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.kover")
    id("io.gitlab.arturbosch.detekt")
    application
}

application {
    mainClass.set("com.company.ipcamera.server.ApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

// Detekt configuration — prevent stale detekt.yml from breaking the build
tasks.withType<io.gitlab.arturbosch.detekt.Detekt> {
    // Don't fail the Gradle build on detekt issues — CI runs detekt
    // as a separate validation step where the threshold applies.
    ignoreFailures = true
}

dependencies {
    // Shared module (core:common нужен для CameraStatus/Resolution из shared)
    implementation(project(":shared"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))

    // Ktor Server
    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.server.websockets)
    implementation("io.ktor:ktor-server-call-logging:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-cors:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-status-pages:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-default-headers:${libs.versions.ktor.get()}")

    // Authentication
    implementation("io.ktor:ktor-server-auth:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-auth-jwt:${libs.versions.ktor.get()}")

    // Cookies support
    implementation("io.ktor:ktor-server-sessions:${libs.versions.ktor.get()}")

    // JWT
    implementation("com.auth0:java-jwt:4.4.0")

    // BCrypt для хеширования паролей
    implementation("org.mindrot:jbcrypt:0.4")

    // Enterprise: LDAP/AD
    implementation("com.unboundid:unboundid-ldapsdk:7.0.0")

    // Enterprise: TOTP для 2FA
    implementation("dev.samstevens.totp:totp:1.7.1")

    // Ktor Client (OAuth2 token/userinfo requests)
    implementation("io.ktor:ktor-client-core:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.kotlin.logging)
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // Dependency Injection (koin-ktor: версия синхронизирована с catalog libs.versions.koin)
    implementation(libs.bundles.koin)
    implementation("io.insert-koin:koin-ktor:${libs.versions.koin.get()}")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactive)

    // Redis client (Lettuce - асинхронный клиент)
    implementation("io.lettuce:lettuce-core:6.3.2.RELEASE")

    // JSON (org.json для ClusterService и др.)
    implementation("org.json:json:20231013")

    // Jackson BOM для управления версиями зависимостей
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.17.2"))
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // PostgreSQL driver for SQLDelight (jdbc-driver 2.0.3 не существует, используем 2.0.2)
    implementation("org.postgresql:postgresql:42.7.5")
    implementation("app.cash.sqldelight:jdbc-driver:2.0.2")

    // Connection pooling
    implementation("com.zaxxer:HikariCP:5.1.0")

    // AWS S3 SDK для облачного хранилища
    implementation("software.amazon.awssdk:s3:2.25.11")

    // Email (SMTP) для уведомлений (B.1)
    implementation("com.sun.mail:jakarta.mail:2.0.2")

    // Database migrations (Flyway)
    implementation("org.flywaydb:flyway-core:10.20.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.20.1")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testing)
    testImplementation("io.ktor:ktor-server-test-host:${libs.versions.ktor.get()}")
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
    testImplementation(libs.kotlinx.coroutines.test)
    // In-memory SQLite for SQLDelight repository tests (ServerSettingsRepositorySqlDelightTest etc.)
    testImplementation("app.cash.sqldelight:sqlite-driver:2.0.2")
}

tasks.test {
    useJUnitPlatform()
    // Глобальный Redis rate limit не должен влиять на интеграционные тесты (много запросов с одного IP).
    environment("API_GLOBAL_RATE_LIMIT_ENABLED", "false")
}

// Integration test source set (requires real services: PostgreSQL + Redis or testcontainers)
val integrationTest by sourceSets.creating {
    compileClasspath += sourceSets.test.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath + configurations.testRuntimeClasspath.get()
}

configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}
configurations.named("integrationTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
    add("integrationTestImplementation", "io.ktor:ktor-server-test-host:${libs.versions.ktor.get()}")
    add("integrationTestImplementation", "io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")
    add("integrationTestImplementation", "redis.clients:jedis:5.1.0")
    add("integrationTestImplementation", "org.junit.vintage:junit-vintage-engine:5.10.0")
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    useJUnitPlatform()
    environment("API_GLOBAL_RATE_LIMIT_ENABLED", "false")
    environment("DATABASE_URL", System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/ipcss_test")
    environment("DATABASE_USER", System.getenv("DATABASE_USER") ?: "ipcss")
    environment("DATABASE_PASSWORD", System.getenv("DATABASE_PASSWORD") ?: "ipcss_test")
    environment("REDIS_HOST", System.getenv("REDIS_HOST") ?: "localhost")
    environment("REDIS_PORT", (System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379).toString())
    shouldRunAfter(tasks.test)
}

kover {
    reports {
        verify {
            rule {
                minBound(30)
            }
        }
    }
}

