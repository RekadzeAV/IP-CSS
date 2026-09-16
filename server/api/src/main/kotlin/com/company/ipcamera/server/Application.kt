package com.company.ipcamera.server

import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.config.ServerConfig
import com.company.ipcamera.server.config.DatabaseConfig
import com.company.ipcamera.server.di.appModule
import com.company.ipcamera.server.middleware.configureCookieAuth
import com.company.ipcamera.server.middleware.configureCsrfProtection
import com.company.ipcamera.server.middleware.configureExceptionHandling
import com.company.ipcamera.server.middleware.configureFileUploadValidation
import com.company.ipcamera.server.middleware.configureRequestLogging
import com.company.ipcamera.server.middleware.configureGlobalApiRateLimit
import com.company.ipcamera.server.middleware.configureSecurityHeaders
import com.company.ipcamera.server.middleware.getJwtToken
import com.company.ipcamera.server.middleware.installHttpsRedirect
import com.company.ipcamera.server.middleware.installHsts
import com.company.ipcamera.server.config.EnterpriseAuthConfig
import com.company.ipcamera.server.security.AuditLogRepository
import com.company.ipcamera.server.security.SecurityLogger
import com.company.ipcamera.server.service.CameraService
import com.company.ipcamera.server.service.OnvifEventSubscriptionService
import com.company.ipcamera.server.routing.configureRouting
import com.company.ipcamera.server.websocket.configureWebSocket
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import java.io.File
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    val config = ServerConfig.fromEnvironment()

    // Инициализация NAS конфигурации (если запущено на NAS)
    com.company.ipcamera.server.config.NasConfig.initialize()

    // В production рекомендуется использовать nginx для HTTPS терминирования
    // Ktor сервер работает на HTTP, nginx проксирует HTTPS запросы
    embeddedServer(Netty, port = config.httpPort, host = config.host, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Fail-fast preflight: в production сервер требует явную PostgreSQL конфигурацию.
    DatabaseConfig.validateDatabaseRequirementsForServerStartup()
    EnterpriseAuthConfig.validateSecurityRequirementsForServerStartup()

    // LDAP validation (non-blocking)
    EnterpriseAuthConfig.validateLdapRequirementsForServerStartup()

    // Koin DI
    install(Koin) {
        modules(appModule)
    }

    // Content Negotiation
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // CORS - ИСПРАВЛЕНО: убран anyHost(), добавлен whitelist доменов
    install(CORS) {
        // Получаем разрешенные домены из переменной окружения или используем дефолтные
        val allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS")
            ?.split(",")
            ?.map { it.trim() }
            ?: listOf("http://localhost:3000", "http://localhost:8080")

        allowedOrigins.forEach { origin ->
            val normalizedHost = origin
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
            if (normalizedHost.isNotBlank()) {
                allowHost(normalizedHost, schemes = listOf("http", "https"))
            }
        }

        allowHeader("Content-Type")
        allowHeader("Authorization")
        allowHeader("X-CSRF-Token")
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowCredentials = true
    }

    // Exception Handling - должен быть установлен до других middleware
    configureExceptionHandling()

    // Request Logging - логирование всех запросов
    configureRequestLogging()

    // Security Headers
    configureSecurityHeaders()

    // Глобальный rate limit (Redis) для /api/v1, кроме HLS/health/ws и служебных путей
    configureGlobalApiRateLimit()

    // File Upload Validation - проверка размера и типа загружаемых файлов
    configureFileUploadValidation()

    // HTTPS Redirect: HTTP → HTTPS (порт 80 → 443), опция «только HTTPS» из конфига FORCE_HTTPS
    val serverConfig = ServerConfig.fromEnvironment()
    installHttpsRedirect(enabled = serverConfig.forceHttps, httpsPort = serverConfig.httpsPort)

    // HSTS (HTTP Strict Transport Security) - только при включённом «только HTTPS»
    installHsts(
        maxAge = 31536000, // 1 год
        includeSubDomains = true,
        preload = false, // Не включаем preload по умолчанию (требует ручной регистрации)
        enabled = serverConfig.forceHttps
    )

    // Cookie Auth Middleware - читает токен из cookie и устанавливает в заголовок Authorization
    configureCookieAuth()

    // JWT Authentication
    install(Authentication) {
        jwt("jwt-auth") {
            realm = JwtConfig.realm
            verifier(JwtConfig.createVerifier())
            // Поддерживаем токен как из Authorization header, так и из cookie через getJwtToken().
            authHeader { call ->
                val token = call.getJwtToken() ?: return@authHeader null
                io.ktor.http.auth.parseAuthorizationHeader("Bearer $token")
            }
            challenge { defaultScheme, realm ->
                call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Token is invalid or expired")
            }
            validate { credential ->
                if (credential.payload.issuer == "ip-camera-server" &&
                    credential.payload.audience.contains("ip-camera-client")) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    // Required for websocket routes in configureWebSocket()
    install(WebSockets)

    // Call Logging (плагин ktor-server-call-logging: пакет callloging/calllogging в зависимости от версии)
    // install(CallLogging) { level = Level.INFO }

    // Routing
    configureRouting()

    // Enterprise: при включённом аудите — дублировать события в репозиторий; мониторинг — всегда для алертов (4.3.3)
    if (EnterpriseAuthConfig.auditPersistEnabled) {
        val auditRepo by inject<AuditLogRepository>()
        SecurityLogger.setAuditRepository(auditRepo)
    }
    val monitoringService by inject<com.company.ipcamera.server.security.SecurityMonitoringService>()
    SecurityLogger.setMonitoringService(monitoringService)

    // WebSocket
    configureWebSocket()

    // Start Camera Service monitoring (non-fatal in local smoke mode)
    runCatching {
        getKoin().get<CameraService>().startMonitoring()
    }.onFailure { e ->
        logger.warn(e) { "Camera monitoring startup skipped due to initialization error" }
    }

    // 4.1.3 Backup Scheduler (автоматическое резервное копирование)
    runCatching {
        getKoin().get<com.company.ipcamera.server.service.BackupSchedulerService>().start()
    }.onFailure { e ->
        logger.warn(e) { "Backup scheduler startup skipped due to initialization error" }
    }

    // 4.2.1 Cluster: heartbeat узла в Redis (если CLUSTER_ENABLED=true)
    runCatching {
        getKoin().get<com.company.ipcamera.server.cluster.ClusterService>()
            .startHeartbeat(CoroutineScope(Dispatchers.Default))
    }.onFailure { e ->
        logger.warn(e) { "Cluster heartbeat startup skipped due to initialization error" }
    }

    // ONVIF: при старте API подписать все подходящие камеры на события (если включено в конфиге)
    runCatching {
        val onvifSubscriptionService = getKoin().get<OnvifEventSubscriptionService>()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                onvifSubscriptionService.subscribeToAllCamerasAtStartup()
            } catch (e: Exception) {
                logger.error(e) { "ONVIF startup subscription failed" }
            }
        }
    }.onFailure { e ->
        logger.warn(e) { "ONVIF startup subscription skipped due to initialization error" }
    }

    // Database Performance Service: мониторинг производительности PostgreSQL
    runCatching {
        val dbPerformanceService = getKoin().getOrNull<com.company.ipcamera.server.service.DatabasePerformanceService>()
        dbPerformanceService?.start()
    }.onFailure { e ->
        logger.warn(e) { "Database Performance Service startup skipped due to initialization error" }
    }
}

