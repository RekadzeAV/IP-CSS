# Структура проекта IP Camera Surveillance System

**Версия документации:** 5.0
**Версия проекта:** 0.5.1.1-beta
**Дата последнего обновления:** 08 August 2026

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)

## Обзор

Проект организован по модульному принципу с четким разделением ответственности между компонентами. Используется Kotlin Multiplatform для кроссплатформенной бизнес-логики и нативные библиотеки C++ для обработки видео и AI-аналитики.

## Корневая структура

```
IP-CSS/
├── .github/                    # GitHub Actions workflows
│   ├── workflows/
│   │   ├── ci.yml             # CI pipeline (validate, test, build, docker)
│   │   ├── release.yml        # Release pipeline (build, docker push, GitHub Release)
│   │   └── nightly.yml        # Nightly pipeline (full tests, security scan, benchmarks)
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md      # Шаблон баг-репорта
│       └── feature_request.md # Шаблон запроса фичи
├── archive/                   # Архив устаревших модулей, конфигов и документации
├── core/                      # Общие кроссплатформенные модули
│   ├── common/                # Базовые типы (Resolution, CameraStatus)
│   ├── network/               # Сетевое взаимодействие (Ktor, ONVIF, WebSocket, RTSP)
│   │   └── onvif/             # ONVIF: WSDiscovery, UPnPDiscovery, OnvifDigestAuth
│   ├── security/              # Безопасность и криптография
│   ├── ui-bridge/             # UI-мост для кроссплатформенного UI
│   └── test-jvm/              # JVM-тесты для core-модулей
├── docs/                      # Документация
│   ├── api/                   # API документация
│   │   └── openapi.yaml       # OpenAPI 3.0.3 спецификация (30+ endpoints)
│   ├── USER_MANUAL.md         # Полное руководство пользователя (14 разделов)
│   └── ...
├── native/                    # Нативные C++ библиотеки
│   ├── video-processing/      # Обработка видео
│   ├── analytics/             # AI аналитика
│   ├── codecs/                # Кодеки
│   └── CMakeLists.txt         # Корневой CMake файл
├── scripts/                   # Скрипты сборки и развертывания
│   ├── build-nas-packages.sh  # Сборка NAS пакетов (SPK, QPKG, APK)
│   ├── docker-entrypoint.sh   # Docker entrypoint
│   └── ...
├── android/                   # Android приложение
│   └── app/                   # Android app модуль
├── server/                    # Серверная часть
│   ├── api/                   # REST API сервер (Ktor)
│   │   ├── cloud/             # S3 Cloud Storage Provider
│   │   ├── cluster/           # ReplicationManager, HealthCheckService
│   │   ├── config/            # RedisClusterConfig, ClusterConfig
│   │   ├── security/          # SamlAuthService, AdvancedThreatProtection
│   │   └── routing/           # Routing + SwaggerRoutes
│   └── web/                   # Веб-приложение (Next.js)
├── platforms/                 # Платформо-специфичные реализации
│   ├── nas-x86_64/            # NAS x86-x64 (Dockerfile)
│   ├── client-android/        # Метаданные платформы Android (исходники: android/app)
│   ├── client-ios/            # Клиенты iOS/macOS (SwiftUI, 5 экранов)
│   └── ...
├── shared/                    # Kotlin Multiplatform модуль
├── config/                    # Конфигурационные файлы
│   ├── cameras.json           # Определения камер
│   ├── nginx/                 # NGINX Load Balancer конфигурация
│   │   └── nginx-load-balancer.conf
│   ├── mediamtx/              # MediaMTX конфигурация
│   └── ...
├── ai-agent/                  # AI агент (Python)
├── .gitignore                 # Git ignore файл
├── build.gradle.kts           # Корневой build файл
├── CHANGELOG.md               # История изменений
├── docker-compose.yml         # Docker Compose конфигурация
├── gradle.properties          # Настройки Gradle
├── LICENSE                    # Лицензия проекта
├── PROJECT_STRUCTURE.md       # Этот файл
├── README.md                  # Основной README
├── PLAN_EXECUTION_MASTER.md   # Мастер-план выполнения (этапы 0–10)
├── REMAINING_TASKS.md         # Список незавершённых задач
└── settings.gradle.kts        # Настройки Gradle проекта
```

## Модули Gradle

| Модуль | Описание | Статус |
|--------|----------|:------:|
| `:shared` | KMM модуль с общей бизнес-логикой | ✅ Активен |
| `:core:common` | Базовые типы (Resolution, CameraStatus) | ✅ Активен |
| `:core:network` | Сетевое взаимодействие (Ktor, ONVIF, WebSocket, RTSP) | ✅ Активен |
| `:core:security` | Безопасность и криптография | ✅ Активен |
| `:core:ui-bridge` | UI-мост для кроссплатформенного UI | ✅ Активен |
| `:core:test-jvm` | JVM-тесты для core-модулей | ✅ Активен |
| `:android:app` | Android приложение | ✅ Активен |
| `:server:api` | REST API сервер (Ktor) | ✅ Активен |
| `:platforms:client-desktop-x86_64:app` | Desktop клиент x86-x64 | ✅ Активен |
| `:platforms:client-desktop-arm:app` | Desktop клиент ARM | ✅ Активен |
| `:platforms:nas-x86_64:build` | NAS сборка x86-x64 | ✅ Активен |
| `:platforms:nas-arm:build` | NAS сборка ARM | ✅ Активен |
| `:android:app` | Android клиент (Jetpack Compose) — единственный модуль | ✅ Активен |
| `platforms/client-android/` | Метаданные платформы (README); исходников нет | ℹ️ Метаданные |
| `:platforms:client-ios` | iOS клиент (SwiftUI) | ✅ Активен |
| ~~`:core:license`~~ | ~~Лицензирование~~ | ⏸️ В архиве |

## Новые модули (Phase 4, 14-19.07.2026)

| Модуль | Файл | Описание |
|--------|------|----------|
| **S3 Cloud Storage** | `server/api/.../cloud/S3CloudStorageProvider.kt` | AWS S3, MinIO, B2, GCS |
| **Cluster Replication** | `server/api/.../cluster/ReplicationManager.kt` | Multi-master + GZIP compression |
| **Health Check Service** | `server/api/.../cluster/HealthCheckService.kt` | Периодические health checks |
| **Redis Cluster Config** | `server/api/.../config/RedisClusterConfig.kt` | Cluster + Sentinel + Single |
| **NGINX Load Balancer** | `config/nginx/nginx-load-balancer.conf` | SSL, WebSocket, HLS, session affinity |
| **SAML 2.0 SSO** | `server/api/.../security/SamlAuthService.kt` | SP-Initiated SSO |
| **Advanced Threat Protection** | `server/api/.../security/AdvancedThreatProtectionService.kt` | IDS, anomaly detection |
| **ONVIF Digest Auth** | `core/network/.../onvif/OnvifDigestAuth.kt` | RFC 2617 Digest auth |
| **WS-Discovery** | `core/network/.../onvif/WSDiscovery.kt` | SOAP over UDP multicast |
| **UPnP Discovery** | `core/network/.../onvif/UPnPDiscovery.kt` | SSDP M-SEARCH |
| **Swagger UI** | `server/api/.../routes/SwaggerRoutes.kt` | OpenAPI serve via Ktor |
| **OpenAPI Spec** | `docs/api/openapi.yaml` | 30+ endpoints, 10+ schemas |
| **User Manual** | `docs/USER_MANUAL.md` | 14 разделов |
| **NAS Docker** | `platforms/nas-x86_64/Dockerfile` | Multi-stage Dockerfile |
| **NAS Builder** | `scripts/build-nas-packages.sh` | SPK, QPKG, APK сборка |
| **iOS UI (5 экранов)** | `platforms/client-ios/...` | Home, Camera, PTZ, Timeline, Settings |
| **Android UI** | `android/app/src/main/java/...` | навигация, 7+ экранов, Room DB + FCM (история: дубль `platforms/client-android/app` → `archive/client-android-dup-2026-09-04/`) |

## Тесты (44 новых теста)

| Файл | Количество | Описание |
|------|:----------:|----------|
| `ReplicationManagerTest.kt` | 6 | LWW, disabled mode, malformed data |
| `HealthCheckServiceTest.kt` | 5 | Unhealthy, timeout, all nodes, reset |
| `OnvifDigestAuthTest.kt` | 10 | Digest challenge, auth header, MD5 |
| `OidcAuthTest.kt` | 10 | Okta, Azure AD, Google JWT validation |
| `ApiIntegrationTest.kt` | 9 | Health, login, cameras, OpenAPI, CORS |
| `DatabaseIntegrationTest.kt` | 4 | PostgreSQL, Redis, schema, KV ops |

## CI/CD Pipelines

| Workflow | Триггер | Действия |
|----------|---------|----------|
| `ci.yml` | push/PR | Validate, test (PG+Redis), build server/android/docker, Trivy scan |
| `release.yml` | tag v* | Build artifacts, Docker push, GitHub Release |
| `nightly.yml` | ежедневно 02:00 UTC | Full tests, security scan, performance benchmarks |
