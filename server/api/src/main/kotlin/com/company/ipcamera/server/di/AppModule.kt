package com.company.ipcamera.server.di

import com.company.ipcamera.server.cloud.CloudStorageProvider
import com.company.ipcamera.server.cloud.CloudStorageService
import com.company.ipcamera.server.cloud.FileBasedCloudStorageProvider
import com.company.ipcamera.server.cloud.S3CloudStorageProvider
import com.company.ipcamera.server.config.CloudStorageConfig
import com.company.ipcamera.server.config.DatabaseConfig
import com.company.ipcamera.server.config.RedisConfig
import com.company.ipcamera.server.repository.*
import com.company.ipcamera.server.security.OAuth2Service
import com.company.ipcamera.server.security.PostgresSecurityAlertRepository
import com.company.ipcamera.server.security.SecurityAlertRepository
import com.company.ipcamera.server.security.AuditLogRepository
import com.company.ipcamera.server.security.PostgresAuditLogRepository
import com.company.ipcamera.server.security.SecurityMonitoringService
import com.company.ipcamera.server.cluster.ClusterService
import com.company.ipcamera.server.service.*
import com.company.ipcamera.server.service.analytics.AnalyticsProductionMonitor
import com.company.ipcamera.server.service.analytics.RtspFrameSource
import com.company.ipcamera.core.common.security.PasswordEncryptionFactory
import com.company.ipcamera.shared.database.CameraDatabase
import com.company.ipcamera.shared.domain.repository.*
import com.company.ipcamera.shared.domain.service.NotificationService
import com.zaxxer.hikari.HikariDataSource
import com.company.ipcamera.server.config.RedisClientWrapper
import com.company.ipcamera.server.config.RedisClusterConfig
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.dsl.module
import javax.sql.DataSource

val appModule = module {
    // =========================================================================
    // Database — SQLDelight CameraDatabase + HikariCP DataSource
    // =========================================================================
    
    // DataSource для прямых SQL-запросов (PostgresSecurityAlertRepository)
    single<DataSource> {
        DatabaseConfig.getDataSource() ?: error("DataSource not initialized. Call DatabaseConfig.createPostgresDriver() first")
    }
    
    // CameraDatabase для SQLDelight-запросов
    single<CameraDatabase> {
        val driver = DatabaseConfig.createPostgresDriver()
        runBlocking {
            // Идемпотентная инициализация: на живой БД (перезапуск сервера) таблицы
            // уже созданы, а Schema.create не идемпотентен ("already exists").
            // Инкрементальные изменения схемы накатывает Flyway (db/migration).
            val schemaExists = runCatching {
                DatabaseConfig.getDataSource()?.connection?.use { conn ->
                    conn.prepareStatement(
                        "SELECT 1 FROM information_schema.tables WHERE table_name = 'camera'"
                    ).use { ps ->
                        ps.executeQuery().use { rs -> rs.next() }
                    }
                } ?: false
            }.getOrDefault(false)
            if (!schemaExists) {
                CameraDatabase.Schema.create(driver).await()
            }
        }
        CameraDatabase(driver)
    }
    
    // Redis client wrapper
    single<RedisClientWrapper> {
        runBlocking { RedisClusterConfig.createConnection() }
    }

    // Redis coroutines-команды для DI (AuthRoutes: rate-limit/blacklist).
    // RedisConfig.getCommands() имеет graceful fallback — no-op команды,
    // если Redis недоступен (локальная разработка/смок без Redis-контейнера).
    single<io.lettuce.core.api.coroutines.RedisCoroutinesCommands<String, String>> {
        RedisConfig.getCommands()
    }
    
    // ClusterService — координация узлов кластера (health/ready). Требует Redis-команды + ClusterConfig.
    // Ремонт DI 14.09: отсутствовал бин → health возвращал 500 (NoBeanDefFound).
    single<ClusterService> {
        ClusterService(
            redis = get<io.lettuce.core.api.coroutines.RedisCoroutinesCommands<String, String>>(),
            config = com.company.ipcamera.server.config.ClusterConfig
        )
    }
    
    // RateLimitMiddleware — защита от брутфорса/DDoS (Redis). дефолт RedisConfig.getCommands().
    // Ремонт DI 14.09: отсутствовал бин → /api/v1/* возвращал 500 (NoBeanDefFound).
    single<com.company.ipcamera.server.middleware.RateLimitMiddleware> {
        com.company.ipcamera.server.middleware.RateLimitMiddleware()
    }
    
    // OAuth2 state store
    single<com.company.ipcamera.server.security.OAuth2StateStore> {
        com.company.ipcamera.server.security.OAuth2StateStore(redis = get())
    }
    
    // =========================================================================
    // Repository — Persistent implementations (replacing InMemory)
    // =========================================================================
    
    // Motion repositories — in-memory (TODO: migrate to SQLDelight)
    single<MotionConfigRepository> {
        SqlDelightMotionConfigRepository()
    }
    single<MotionEventRepository> {
        SqlDelightMotionEventRepository()
    }
    
    // Security alert repository — PostgreSQL (server-only, via DataSource)
    single<SecurityAlertRepository> {
        PostgresSecurityAlertRepository(dataSource = get<DataSource>())
    }
    
    // AuditLogRepository — PostgreSQL (SecurityLogger/audit; таблица audit_log, V4).
    // Ремонт DI 14.09: требовался SecurityMonitoringService для запуска (NoBeanDefFound).
    single<AuditLogRepository> {
        PostgresAuditLogRepository(dataSource = get<DataSource>())
    }
    
    // SecurityMonitoringService (4.3.3.2) — правила и алерты безопасности.
    // Ремонт DI 14.09: отсутствовал бин → сервер не стартовал (NoBeanDefFound).
    single<SecurityMonitoringService> {
        SecurityMonitoringService(
            auditRepository = get<AuditLogRepository>(),
            alertRepository = get<SecurityAlertRepository>(),
        )
    }
    
    // Other repositories
    // SettingsRepository — SQLDelight/PostgreSQL (интерфейс: требуется SettingsRoutes)
    single<com.company.ipcamera.shared.domain.repository.SettingsRepository> {
        ServerSettingsRepositorySqlDelight(
            database = get(),
            storageService = getOrNull()
        )
    }
    factory { PasswordService() }
    // Single: пользователи хранятся в памяти процесса; AuthRoutes и внешние провайдеры
    // (LDAP/SSO) должны работать с одним и тем же экземпляром.
    single<ServerUserRepository> { ServerUserRepositoryInMemory() }

    // =========================================================================
    // Внешние провайдеры аутентификации (4.3.1.1 LDAP/AD)
    // Цепочка логина в AuthRoutes: внешние провайдеры → локальная аутентификация.
    // Активируются при LDAP_ENABLED=true; пользователь создаётся/находится локально
    // без пароля (аутентификация только через LDAP).
    // =========================================================================
    single<List<com.company.ipcamera.server.security.ExternalAuthProvider>> {
        listOf(
            com.company.ipcamera.server.security.LdapAuthService(
                config = com.company.ipcamera.server.config.EnterpriseAuthConfig,
                createOrGetLocalUser = { username, email, fullName, role ->
                    get<ServerUserRepository>().getOrCreateUserByUsername(
                        username = username,
                        email = email,
                        fullName = fullName,
                        role = role
                    )
                }
            )
        )
    }

    // =========================================================================
    // Cloud Storage — S3 / MinIO / Backblaze B2 / FileSystem
    // =========================================================================

    /**
     * Провайдер облачного хранилища.
     * Приоритет: S3-совместимые (s3, minio, b2, gcs) → FileBased (локальная ФС) → null
     * Если CLOUD_STORAGE_ENABLED=false и CLOUD_STORAGE_PROVIDER не указан — null.
     */
    single<CloudStorageProvider?> {
        val providerName = CloudStorageConfig.provider
        when {
            providerName == "s3" || providerName == "minio" || providerName == "b2" ||
                providerName == "gcs" || providerName == "azure" -> {
                val s3Config = CloudStorageConfig.s3Config
                val bucket = s3Config.bucketName; val ep = s3Config.endpoint ?: "default"
                logInfo("Initializing S3-compatible cloud storage: $providerName (bucket=$bucket, endpoint=$ep)")
                S3CloudStorageProvider(s3Config)
            }
            CloudStorageConfig.enabled && providerName == null -> {
                logInfo("Cloud storage enabled without S3 provider. Using file-based storage.")
                FileBasedCloudStorageProvider(CloudStorageConfig.prefix)
            }
            else -> {
                logInfo("Cloud storage is disabled.")
                null
            }
        }
    }

    /**
     * Сервис облачного хранилища для записей (CloudStorageService 4.1.2).
     */
    single<CloudStorageService> {
        CloudStorageService(
            provider = get(),
            prefix = CloudStorageConfig.prefix
        )
    }

    // =========================================================================
    // Services
    // =========================================================================
    single<OAuth2Service> {
        OAuth2Service(
            config = com.company.ipcamera.server.config.EnterpriseAuthConfig,
            stateStore = get(),
            createOrGetUser = { _, _, _, _ ->
                com.company.ipcamera.shared.domain.model.User(
                    id = "oauth-user",
                    username = "oauth-user",
                    role = com.company.ipcamera.shared.domain.model.UserRole.VIEWER,
                    createdAt = System.currentTimeMillis()
                )
            }
        )
    }
    single<NotificationService> {
        NotificationService(
            notificationRepository = get(),
            pushTokenService = getOrNull(),
            deliveries = listOfNotNull(
                com.company.ipcamera.server.notification.ApnsPushDeliveryFactory.createFromEnvironment(),
                com.company.ipcamera.server.notification.FcmPushDeliveryFactory.createFromEnvironment(
                    tokenPurger = getOrNull<PushTokenService>()
                ),
                com.company.ipcamera.server.notification.WebPushDeliveryFactory.createFromEnvironment(
                    tokenPurger = getOrNull<PushTokenPurger>()
                )
            ),
            broadcastChannels = com.company.ipcamera.server.notification.BroadcastChannelsFactory.createFromEnvironment()
        )
    }
    single<PushTokenService> { PushTokenService(get()) }

    // =========================================================================
    // Серверная AI-аналитика (задача 1.1): RTSP-кадры (ffmpeg) → JNI C++ OpenCV.
    // RtspFrameSource — общий источник RGB24-кадров (graceful: isAvailable()=false
    // без ffmpeg → сервисы возвращают ошибку запуска, сервер работает дальше).
    // CameraRepository — общий SQLDelight-репозиторий камер (credentials для RTSP).
    // =========================================================================
    single<RtspFrameSource> { RtspFrameSource() }
    single<com.company.ipcamera.shared.domain.repository.CameraRepository> {
        ServerCameraRepository(
            database = get(),
            passwordEncryption = PasswordEncryptionFactory.create(),
        )
    }

    single<VideoAnalyticsService> {
        VideoAnalyticsService(
            frameSource = get(),
            cameraRepository = get(),
        )
    }
    single<FfmpegService> { FfmpegService() }
    single<CameraControlService> {
        CameraControlService(
            onvifClient = com.company.ipcamera.core.network.OnvifClient(
                engine = io.ktor.client.engine.cio.CIO.create()
            )
        )
    }
    single<MotionDetectorService> {
        MotionDetectorService(
            motionConfigRepository = get(),
            motionEventRepository = get(),
            notificationService = get(),
            snapshotService = get(),
            frameSource = get(),
            cameraRepository = get(),
        )
    }
    single<MotionNotificationService> { MotionNotificationService(get(), get(), getOrNull()) }
    single<MotionSnapshotService> { MotionSnapshotService(get()) }
    // RTSP benchmark (RTSP-роутеры: POST /rtsp/benchmark). Без DI-зависимостей.
    single<RtspBenchmarkService> { RtspBenchmarkService() }
    // StorageService — мониторинг дискового пространства (health/записи). Без DI-зависимостей.
    single<StorageService> { StorageService() }
    single<ObjectDetectionService> {
        ObjectDetectionService(
            configRepository = get(),
            eventRepository = get(),
            frameSource = get(),
            snapshotService = get(),
            notificationService = get(),
            cameraRepository = get(),
        )
    }
    single<EventGenerator> { EventGenerator() }
    single<AnalyticsProductionMonitor> { AnalyticsProductionMonitor() }
    
    // =========================================================================
    // EventRepository (shared) — требуется EventRoutes (/events), RecordingRoutes
    // и MotionNotificationService. Ранее НЕ был зарегистрирован в основном
    // приложении: Koin бросал InstanceCreationException при первом обращении
    // к /events. Мигрирован на SQLDelight/PostgreSQL (задача 1.4): общий
    // пул соединений CameraDatabase.
    // =========================================================================
    single<com.company.ipcamera.shared.domain.repository.EventRepository> {
        ServerEventRepositorySqlDelight(database = get())
    }

    // =========================================================================
    // RecordingRepository (shared) — требуется RecordingRoutes (/recordings).
    // Ранее НЕ был зарегистрирован: Koin бросал InstanceCreationException при
    // первом обращении к /recordings. Мигрирован на SQLDelight/PostgreSQL
    // (задача 1.4): общий пул соединений CameraDatabase.
    // =========================================================================
    single<com.company.ipcamera.shared.domain.repository.RecordingRepository> {
        ServerRecordingRepositorySqlDelight(database = get())
    }

    // ServerNotificationService (alias) — УДАЛЕНО (14.09, блокер запуска):
    // single<ServerNotificationService>{ get<NotificationService>() } был саморекурсивным:
    // typealias ServerNotificationService == NotificationService, а get() резолвил
    // тот же тип через себя → StackOverflowError при старте (AppModule.kt:449).
    // Потребители ServerNotificationService в коде отсутствуют — используйте NotificationService.
}

private fun logInfo(msg: String) {
    mu.KotlinLogging.logger {}.info(msg)
}

typealias ServerNotificationService = com.company.ipcamera.server.service.NotificationService
