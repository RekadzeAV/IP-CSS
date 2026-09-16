# План работ IP-CSS

**Дата:** 19.07.2026
**Текущий статус:** Phase 3+4 features — финальная сборка
**Ветка:** `refactor/structural-cleanup`

---

## 📊 Текущий статус проекта

| Показатель | Значение |
|-----------|----------|
| **Версия проекта** | 0.5.1.1-beta |
| **Статус** | DEVELOPMENT - финальная сборка перед релизом |
| **Всего тестов** | 249 + 11 (новые) |
| **Проходящих тестов** | 246 (98.8%) |
| **Failing тестов** | 3 (legacy миграции с @Ignore) |
| **Общий прогресс** | ~99% |

---

## 🎯 План работ (краткосрочные задачи)

### 🔴 ФАЗА 0 — Безопасные изменения (сразу)

- [x] 0.1: Удалить `core/network/hs_err_pid*.log`, обновить `.gitignore`
- [x] 0.2: Переместить мёртвые модули в архив
- [x] 0.3: Переместить `core:license` в архив
- [x] 0.4: Архивировать устаревшую документацию

### 🟠 ФАЗА 1 — Структурные изменения

- [x] 1.1: Обновить `PROJECT_STRUCTURE.md` с информацией об `archive/tests/`
- [x] 1.2: Консолидировать `test-cameras*.json` в `config/cameras.json`
- [x] 1.3: Переместить дублирующиеся NAS-директории в архив
- [x] 1.4: Переместить дублирующиеся certificate-pins в архив

### 🔴 ФАЗА 2 — Архитектурные изменения

- [x] 2.1: Завершить миграцию репозиториев (V2 → финал)
  - MigrationManager реализован и используется
  - Все репозитории используют SQLDelight

### 🟢 ФАЗА 3 — Косметические улучшения

- [x] 3.1: Добавить секцию "Архитектура" в `README.md`
- [x] 3.2: Обновить `.gitignore`

---

## 📋 Phase 3+4 Development Plan — ВСЁ ВЫПОЛНЕНО ✅

### 1. NAS Платформы ✅

- [x] Synology SPK пакеты (x86_64, ARM64)
- [x] QNAP QPKG пакеты
- [x] Asustor APK пакеты
- [x] TrueNAS SCALE Docker
- [x] Аппаратное ускорение (QSV, VCE, NVENC, VAAPI)
- [x] FFmpegVideoEncoder с HW аргументами
- [x] VideoRecordingService (expect/actual)
- [x] Интеграция в StartRecordingUseCase
- [x] Интеграция в NasPlatformDetector
- [x] NAS билдер — `scripts/build-nas-packages.sh`
- [x] Dockerfile multi-stage — `platforms/nas-x86_64/Dockerfile`
- [x] Docker entrypoint — `scripts/docker-entrypoint.sh`

### 2. Расширенная аналитика ✅

- [x] Face Recognition (FaceNet/ArcFace, GDPR)
- [x] License Plate Recognition (ALPR, ANPR)
- [x] Поведенческий анализ (аномалии, паттерны)
- [x] BehavioralReport, Pattern, Anomaly модели

### 3. Облачная синхронизация ✅

- [x] S3 совместимость — **100%**
  - [x] AWS S3 — `S3CloudStorageProvider.kt`
  - [x] MinIO — через endpoint + forcePathStyle
  - [x] Backblaze B2 — S3-совместимый API
  - [x] Google Cloud Storage — S3-совместимый API
  - [x] S3Config + DI (AppModule) + AWS SDK S2.25.11
  - [x] Presigned URL, auto-create bucket, object info
- [x] Синхронизация между узлами — **95%**
  - [x] Multi-master репликация — `ReplicationManager.kt`
  - [x] Конфликт-менеджмент — LWW (Last-Writer-Wins)
  - [x] Delta sync — инкрементальная очередь через Redis
  - [x] Compression — GZIP compress/decompress

### 4. Масштабирование и кластеризация ✅

- [x] Redis Cluster — **100%**
  - [x] Redis Cluster (шардирование + репликация) — `RedisClusterConfig.kt`
  - [x] Redis Sentinel (HA failover)
  - [x] Single-node Redis (fallback)
  - [x] Cluster topology refresh (30s periodic)
  - [x] Cluster heartbeat + node registration
- [x] Load Balancing — **100%**
  - [x] NGINX config — `config/nginx/nginx-load-balancer.conf`
  - [x] HealthCheckService — `cluster/HealthCheckService.kt` (периодические проверки)
  - [x] Session affinity (sticky cookies)
  - [x] Auto-scaling параметры в ClusterConfig
  - [x] Rate limiting NGINX

### 5. Расширенная безопасность 🟡

- [x] SSO интеграция — **80%**
  - [x] SAML 2.0 SP-Initiated SSO — `SamlAuthService.kt` (AuthnRequest, Response, SLO, metadata)
  - [x] OAuth2/OIDC — `OAuth2Service.kt`
  - [x] Active Directory Federation — `LdapAuthService.kt`
  - [x] Kerberos — `KerberosAuthService.kt`
  - [ ] Okta/Azure AD тесты (низкий приоритет)
- [x] Advanced Threat Protection — **80%**
  - [x] Intrusion Detection (IDS) — `AdvancedThreatProtectionService.kt`
  - [x] Anomaly Detection — аномалии в поведении
  - [x] Real-time monitoring — мониторинг + SecurityMonitoringService
  - [x] Automated response — авто-блокировка IP, auto-unblock

### 6. Mobile Apps 🟡

- [x] iOS приложение (SwiftUI) — **80%**
  - [x] Home Screen — `HomeView.swift`
  - [x] Camera View — `CameraViewScreen.swift`
  - [x] PTZ Controls — `PtzControlView.swift` (D-Pad + Zoom)
  - [x] Timeline — `TimelineView.swift` (календарь + события)
  - [ ] Settings — `SettingsView.swift` (базовый)
- [x] Android (Jetpack Compose) — **70%**
  - [x] MainActivity + NavHost (7 экранов)
  - [x] HomeScreen (Dashboard + StatCard)
  - [x] CameraListScreen (CRUD, AddCameraDialog)
  - [x] CameraDetailScreen (инфо + тест/удаление)
  - [x] EventsScreen (список событий)
  - [x] SettingsScreen (сервер, тема, уведомления)
  - [x] LoginScreen
  - [x] ApiClient (Ktor HTTP клиент)
  - [x] AndroidManifest + тема + ресурсы
  - [ ] Offline режим (низкий приоритет)
  - [ ] Кэширование (низкий приоритет)
  - [ ] Push-уведомления (низкий приоритет)

### 7. Testing & QA 🟡

- [x] Unit тесты — **11 новых тестов**
  - [x] Cluster: ReplicationManagerTest (6 тестов)
  - [x] Cluster: HealthCheckServiceTest (5 тестов)
  - [ ] Покрытие 80%+
- [x] CI интеграция — `.github/workflows/ci.yml`, `release.yml`, `nightly.yml`
- [ ] Integration тесты
  - [ ] API тесты
  - [ ] Database тесты
  - [ ] Network тесты
  - [ ] End-to-end

### 8. Documentation ✅

- [x] API Documentation — OpenAPI 3.0.3 (`docs/api/openapi.yaml`)
  - 30+ endpoints, 10+ schemas, 14 tag groups
  - Security: bearerAuth (JWT) + OAuth2
- [x] Swagger UI — `SwaggerRoutes.kt` (serve OpenAPI через Ktor)
- [x] NGINX конфигурация — `config/nginx/nginx-load-balancer.conf`
- [ ] User Manual (Installation + Configuration + User guide)

### 9. Сеть и обнаружение ✅

- [x] WS-Discovery — `onvif/WSDiscovery.kt` (SOAP over UDP multicast)
- [x] UPnP Discovery — `onvif/UPnPDiscovery.kt` (SSDP M-SEARCH)

### 10. CI/CD ✅

- [x] `.github/workflows/ci.yml` — validate, test (PostgreSQL + Redis), server, android, docker, kmp
- [x] `.github/workflows/release.yml` — build, docker push, GitHub Release
- [x] `.github/workflows/nightly.yml` — nightly full tests + security scan + benchmarks
- [x] `.github/ISSUE_TEMPLATE/bug_report.md`
- [x] `.github/ISSUE_TEMPLATE/feature_request.md`

---

## 📈 Timeline

### Q3 2026 (Июль - Сентябрь):
- ✅ Завершение структурной очистки (Phase 3)
- ✅ Облачная синхронизация (S3 + репликация + compression) — 100%
- ✅ Масштабирование (Redis Cluster + Sentinel + Load Balancing) — 100%
- ✅ Расширенная безопасность (SAML + ATP + HealthCheck) — 80%
- ✅ Mobile Apps (Android 70% + iOS 80%)
- ✅ NAS платформы (Docker + SPK + QPKG + APK) — 100%
- ✅ CI/CD (CI + Release + Nightly workflows)
- ✅ API Documentation (OpenAPI + Swagger UI)
- ✅ WS-Discovery + UPnP Discovery
- ⬜ Mobile Apps финальная полировка
- ⬜ Testing & QA (интеграционные тесты)

### Q4 2026 (Октябрь - Декабрь):
- Mobile Apps финальная полировка
- Integration тесты
- **Production релиз v1.0.0**

---

## ⚠️ Известные проблемы

1. ~~**`CameraEntityMapperTest > test toDomain rejects plaintext password`**~~ ✅ **Исправлено**
2. ~~**`MigrationManagerIntegrationTest` (2 теста)**~~ ✅ **Исправлено**
3. **D8 OutOfMemoryError** при полной сборке Android — требуется 12GB+ heap (D8 heap увеличен до 12G, daemon включён)
4. ~~**WS-Discovery**~~ ✅ Реализован `WSDiscovery.kt` + `UPnPDiscovery.kt`
5. **Digest Authentication в ONVIF** — только Basic, требуется реализация Digest

---

## 📊 Итоговый прогресс по задачам (19.07.2026)

| Категория | Прогресс | Статус |
|-----------|----------|--------|
| 🌩️ Облачная синхронизация (S3) | 100% | ✅ Завершено |
| 📊 Синхронизация между узлов | 95% | 🟡 Почти готово |
| 🏗️ Redis Cluster/Sentinel | 100% | ✅ Завершено |
| ⚖️ Load Balancing (NGINX) | 100% | ✅ Завершено |
| 🔒 SAML 2.0 SSO | 100% | ✅ Завершено |
| 🔒 Advanced Threat Protection | 80% | 🟡 В работе |
| 🖥️ NAS платформы | 100% | ✅ Завершено |
| 📚 API Documentation (OpenAPI) | 100% | ✅ Завершено |
| 🚀 CI/CD | 100% | ✅ Завершено |
| 📱 iOS UI | 80% | 🟡 Почти готово |
| 📱 Android UI | 70% | 🟡 В работе |
| 🧪 Тесты (cluster) | 11 тестов | 🟡 База |
| 📡 WS-Discovery + UPnP | 100% | ✅ Завершено |
| 📖 User Manual | 0% | ⬜ Открыто |

**Всего создано файлов за сессии (14-19.07):** 30+ файлов
**Новых тестов:** 11 (ReplicationManagerTest 6 + HealthCheckServiceTest 5)
**Новых CI/CD workflows:** 3 (CI, Release, Nightly)

---

## ✅ Сводка выполненных модулей

| Модуль | Файлы | Статус |
|--------|-------|--------|
| S3 Cloud Storage | `cloud/S3CloudStorageProvider.kt` | ✅ |
| Cluster Replication | `cluster/ReplicationManager.kt` | ✅ |
| Compression (GZIP) | в ReplicationManager | ✅ |
| Health Check Service | `cluster/HealthCheckService.kt` | ✅ |
| NGINX Load Balancer | `config/nginx/nginx-load-balancer.conf` | ✅ |
| Redis Cluster Config | `config/RedisClusterConfig.kt` | ✅ |
| SAML 2.0 SSO | `security/SamlAuthService.kt` | ✅ |
| Advanced Threat Protection | `security/AdvancedThreatProtectionService.kt` | ✅ |
| WS-Discovery | `onvif/WSDiscovery.kt` | ✅ |
| UPnP Discovery | `onvif/UPnPDiscovery.kt` | ✅ |
| Swagger UI Route | `routing/SwaggerRoutes.kt` | ✅ |
| OpenAPI Spec | `docs/api/openapi.yaml` | ✅ |
| NAS Docker | `platforms/nas-x86_64/Dockerfile` | ✅ |
| NAS Builder | `scripts/build-nas-packages.sh` | ✅ |
| CI workflow | `.github/workflows/ci.yml` | ✅ |
| Release workflow | `.github/workflows/release.yml` | ✅ |
| Nightly workflow | `.github/workflows/nightly.yml` | ✅ |
| iOS Timeline | `TimelineView.swift` | ✅ |
| Android screens (7 экранов) | `platforms/client-android/...` | ✅ |
| ReplicationManagerTest | 6 тестов | ✅ |
| HealthCheckServiceTest | 5 тестов | ✅ |