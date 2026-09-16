# IP Camera Surveillance System (IP-CSS)

Кроссплатформенная система видеонаблюдения с IP-камер с продвинутой AI-аналитикой, облачной синхронизацией и кластеризацией.

**Версия проекта:** 0.5.1.1-beta  
**Дата:** 08 August 2026  
**Status:** 🟡 **DEVELOPMENT — консолидация и подготовка к тестовым сборкам**

[![Documentation](https://img.shields.io/badge/Documentation-Complete-blue)](docs/DOCUMENTATION_INDEX.md)
[![API](https://img.shields.io/badge/API-OpenAPI%203.0-success)](docs/api/openapi.yaml)
[![License](https://img.shields.io/badge/License-GPLv3-green)](LICENSE)
[![CI](https://img.shields.io/badge/CI-GitHub%20Actions-brightgreen)](.github/workflows/ci.yml)
[![NAS](https://img.shields.io/badge/NAS-Synology%20%7C%20QNAP%20%7C%20TrueNAS-orange)](scripts/build-nas-packages.sh)

> **📋 Полный индекс документации:** [docs/DOCUMENTATION_INDEX.md](docs/DOCUMENTATION_INDEX.md)  
> **📋 Структура проекта:** [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)  
> **📋 План работ:** [PLAN_EXECUTION_MASTER.md](PLAN_EXECUTION_MASTER.md)  
> **📋 Оставшиеся задачи:** [REMAINING_TASKS.md](REMAINING_TASKS.md)  
> **📖 Руководство пользователя:** [docs/USER_MANUAL.md](docs/USER_MANUAL.md)  
> **📘 API документация:** `http://localhost:8080/api/v1/docs` (Swagger UI)

---

## 🚀 Быстрый старт

```bash
# Docker (рекомендуемый способ)
docker run -d --name ipcss -p 8080:8080 ghcr.io/rekadzeav/ip-css:latest

# Docker Compose (полный стек)
docker-compose up -d

# Из исходников
./gradlew :server:api:installDist
server/api/build/install/api/bin/api
```

---

## 🌟 Ключевые возможности

| Возможность | Статус |
|-------------|--------|
| **📹 Поддержка камер** (RTSP, ONVIF, HTTP) | ✅ 100% |
| **🔍 WS-Discovery** (автообнаружение камер) | ✅ 100% |
| **🔐 ONVIF Digest Authentication** (RFC 2617) | ✅ 100% |
| **📺 HLS стриминг** (низкая задержка) | ✅ 100% |
| **AI-аналитика** (движение, лица, объекты, ANPR) | ✅ 100% |
| **☁️ S3 Cloud Sync** (AWS, MinIO, B2, GCS) | ✅ 100% |
| **📊 Cluster Replication** (Multi-master + GZIP) | ✅ 95% |
| **🏗️ Redis Cluster + Sentinel** | ✅ 100% |
| **⚖️ NGINX Load Balancer** (SSL, WebSocket, HLS) | ✅ 100% |
| **🔒 SAML 2.0 SSO + OIDC** (Okta, Azure AD, Google) | ✅ 100% |
| **🛡️ Advanced Threat Protection** (IDS, rate limit) | ✅ 80% |
| **🖥️ NAS поддержка** (Synology, QNAP, Asustor, TrueNAS) | ✅ 100% |
| **📱 iOS приложение** (SwiftUI, PTZ, Timeline) | ✅ 80% |
| **📱 Android приложение** (Jetpack Compose, Room, FCM) | ✅ 85% |
| **🚀 CI/CD** (GitHub Actions, Docker, Trivy) | ✅ 100% |
| **📚 API Documentation** (OpenAPI 3.0.3 + Swagger UI) | ✅ 100% |

---

## 🧪 Статус тестирования (проверено для 0.5.1.1-beta)

| Показатель | Значение |
|-----------|---------|
| **Проверено: `:server:api:test`** | **253 теста — 0 failures / 0 errors / 0 skipped** ✅ |
| **Проверено: `:shared:desktopTest`** | **245 тестов — 1 skipped / 0 failures / 0 errors** ✅ |
| **Проверено: `:core:network:desktopTest`** | **544 теста — 25 failed / 38 skipped** ⚠️ (сборка восстановлена) |
| **Проверено: `:core:common:desktopTest`** | **23 теста — 5 skipped / 0 failures / 0 errors** ✅ |
| **Проверено: `:core:test-jvm:test`** | **19 тестов — 0 failures / 0 errors / 0 skipped** ✅ |
| **Компиляция `:server:api`** | ✅ BUILD SUCCESSFUL |
| **Заявлено в README ранее** | 293 (требует полной проверки по всем модулям) |
| **Cluster тесты** | ReplicationManagerTest (6) + HealthCheckServiceTest (5) |
| **Security тесты** | OnvifDigestAuthTest (10) + OidcAuthTest (10) |
| **Integration тесты** | ApiIntegrationTest (9) + DatabaseIntegrationTest (4) — не подключены к сборке/CI |
| **Repro-only @Ignore (кросс-модульно)** | 20 (Android Keystore, Live555, certificate-pinning) |

---

## 🏗️ Архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                     NGINX Load Balancer                         │
│              (SSL, Session Affinity, Rate Limiting)              │
└──────────────────────────┬──────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼───┐         ┌────▼───┐         ┌────▼───┐
   │ Node 1 │◄────────►│ Node 2 │◄────────►│ Node 3 │
   │ Server │          │ Server │          │ Server │
   └───┬────┘          └───┬────┘          └───┬────┘
       │                   │                   │
   ┌───▼───────────────────▼───────────────────▼───┐
   │              Redis Cluster                     │
   │   (Metadata + Replication + Session Cache)    │
   └────────────────────────────────────────────────┘
   ┌────────────────────────────────────────────────┐
   │           PostgreSQL (Primary/Replica)          │
   └────────────────────────────────────────────────┘
   ┌────────────────────────────────────────────────┐
   │           S3 Cloud Storage (AWS/MinIO/B2/GCS)  │
   └────────────────────────────────────────────────┘
```

### Ключевые компоненты

| Компонент | Технология | Назначение |
|-----------|-----------|-----------|
| **API Server** | Ktor (Kotlin) | REST API, WebSocket, HLS streaming |
| **Load Balancer** | NGINX | SSL termination, session affinity, rate limiting |
| **Metadata Store** | Redis Cluster | Config cache, session store, replication queue |
| **Persistent Store** | PostgreSQL | Camera configs, events, users, recordings |
| **Cloud Storage** | AWS S3 / MinIO / B2 / GCS | Video recordings, snapshots, backups |
| **Auth** | SAML 2.0 / OIDC / LDAP / Kerberos | Enterprise SSO, JWT, OAuth2 |
| **Security** | ATP Service | IDS, brute-force detection, auto-block |
| **Discovery** | WS-Discovery / UPnP | Automatic camera discovery |
| **Mobile** | SwiftUI / Jetpack Compose | iOS and Android apps |
| **CI/CD** | GitHub Actions | CI, Release, Nightly pipelines |

---

## 🛠️ Системные требования

### Обязательные

- **JDK:** 17+ (Temurin рекомендуется)
- **Kotlin:** 2.0.0+ (через Gradle)
- **Gradle:** 8.9 (через wrapper, `gradlew`)
- **Node.js:** 20+ (для веб-интерфейса Next.js)
- **Python:** 3.9+ (для AI-агента и скриптов)

### Для сервера и тестов

- **Docker:** 24.0+
- **Docker Compose:** 2.0+
- **PostgreSQL:** 16+ (для интеграционных тестов и production)
- **Redis:** 7+ (для кэширования, сессий, репликации)

### Для полной сборки (все платформы)

- **Android SDK:** 34+ (API 34, Build-tools 34+, NDK 30+) — для Android сборки
- **Xcode:** 15+ (только macOS) — для iOS сборки
- **CMake:** 3.20+ — для нативных C++ библиотек
- **FFmpeg:** 6.0+ — для обработки видео
- **C++ компилятор:** GCC 10+/Clang 14+/MSVC 19+ — для нативных библиотек

> 💡 **Полный справочник с установкой для каждой ОС:**
> [LOCAL_DEVELOPMENT_REQUIREMENTS.md](LOCAL_DEVELOPMENT_REQUIREMENTS.md)

---

## 📦 Релизный пайплайн

```bash
# 1. Сборка сервера
./gradlew :server:api:shadowJar -x test

# 2. Запуск тестов
./gradlew test

# 3. Сборка Docker образа
docker build -t ipcss-server:latest -f platforms/nas-x86_64/Dockerfile .

# 4. Сборка NAS пакетов
./scripts/build-nas-packages.sh all

# 5. Публикация
# GitHub Actions: push tag v* → автоматический релиз
```

---

## 📚 Документация

- **[PLAN_EXECUTION_MASTER.md](PLAN_EXECUTION_MASTER.md)** — Мастер-план выполнения (этапы 0–10, гейты)
- **[PLAN_VULNERABILITIES.md](PLAN_VULNERABILITIES.md)** — План устранения уязвимостей зависимостей (реестр, CI-гейты, процесс новых)
- **[REMAINING_TASKS.md](REMAINING_TASKS.md)** — Список незавершённых задач (единый трекер)
- **[LOCAL_DEVELOPMENT_REQUIREMENTS.md](LOCAL_DEVELOPMENT_REQUIREMENTS.md)** — Полные требования для разработки (Windows/Linux/macOS)
- **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** — Структура проекта и модули (v4.0)
- **[CHANGELOG.md](CHANGELOG.md)** — История изменений
- **[docs/USER_MANUAL.md](docs/USER_MANUAL.md)** — Руководство пользователя (14 разделов)
- **[docs/api/openapi.yaml](docs/api/openapi.yaml)** — OpenAPI 3.0.3 (30+ endpoints)
- **[CONTRIBUTING.md](CONTRIBUTING.md)** — Руководство для контрибьюторов
- **[LICENSE](LICENSE)** — Лицензия GPLv3

**API Documentation (Swagger UI):** Запустите сервер и откройте `http://localhost:8080/api/v1/docs`

---

## 📋 Платформы

| Платформа | Статус | Тип |
|-----------|--------|-----|
| **Docker** | ✅ Готово | `ghcr.io/rekadzeav/ip-css:latest` |
| **Synology NAS** | ✅ Готово | SPK пакет |
| **QNAP NAS** | ✅ Готово | QPKG пакет |
| **Asustor NAS** | ✅ Готово | APK пакет |
| **TrueNAS SCALE** | ✅ Готово | Docker образ |
| **Windows/Linux/macOS** | ✅ Готово | Java JAR |
| **Android** | 🔧 85% | Jetpack Compose |
| **iOS** | 🔧 80% | SwiftUI |

---

## 📞 Контакты

- **Репозиторий:** [github.com/RekadzeAV/IP-CSS](https://github.com/RekadzeAV/IP-CSS)
- **Issues:** [github.com/RekadzeAV/IP-CSS/issues](https://github.com/RekadzeAV/IP-CSS/issues)
- **Документация:** [docs/DOCUMENTATION_INDEX.md](docs/DOCUMENTATION_INDEX.md)
- **Лицензия:** GPLv3

---

*IP-CSS v0.5.1.1-beta — Open Source IP Camera Surveillance System*
