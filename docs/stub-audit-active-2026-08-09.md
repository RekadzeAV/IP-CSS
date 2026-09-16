# Stub-аудит активного кода (09 August 2026)

**Метод:** поиск маркеров `TODO (stub/реализовать/implement)`, `NotImplementedError`, `UnsupportedOperationException`, `placeholder`, `заглушк`, `fallback to placeholder` в `.kt`/`.java` (без `build/`, `bin/`, `archive/`, `docs/archive/`, `node_modules/`).

**Итог:** 52 файла с маркерами; 89 маркерных строк в активном коде.
**Обновление (10 Aug 2026):** `core/security` — `SecureTokenEncryption` (iOS/Native) и `SecurePasswordHasher.jvm` реализованы (см. ниже), модуль `core:security:desktopTest` теперь собирается и проходит 37 тестов.
**Обновление 2 (10 Aug 2026):** уведомления — APNs ES256 JWT + интеграция в NotificationService; добавлены широковещательные каналы Telegram/Email/SMS (`BroadcastNotificationChannel` + `BroadcastChannelsFactory`), подключены в DI; FCM остаётся отключённым (Firebase SDK недоступен offline). server:api:test: 262/0.
**Обновление 3 (14.09.2026, ревизия):** выяснено, что прежняя «реализация» `BruteForceProtectionManager` не выполняла блокировку вообще (карты блокировок только очищались, порог из `BruteForceConfig` игнорировался). Реализована единая для всех платформ семантика: скользящее окно попыток + lockout по порогу, конфиг применяется фабрикой; +10 юнит-тестов с детерминированными часами (`core:security:desktopTest`). Статусы дат «14.09» уточнены до «14.09.2026».

## Значимые кандидаты на реализацию (активный код)

| Файл | Маркеров | Контур | Статус |
|------|--------:|--------|--------|
| `server/api/.../service/WebRtcService.kt` | 24 | Janus Gateway fallback; fallback-SDP генерируется из offer (`OfferSdpAnswerFactory`), путь Janus за интерфейсом `WebRtcMediaGateway` | 🟨 **14.09.2026: SDP-стаб закрыт** — `OfferSdpAnswerFactory` собирает ответ из offer (кодеки/ICE/DTLS/направления); QS: реальная медиа-нога — только при наличии Janus-сервера (внешний) |
| `server/api/.../notification/ApnsNotificationSender.kt` | 12 | ✅ Push-уведомления iOS; ES256 JWT (ApnsJwtUtil); подключён к NotificationService через ApnsPushDelivery | ✅ |
| `server/api/.../repository/ServerUserRepository*.kt` | 8+6 | PostgreSQL/InMemory-репозитории; TODO на part-методы | ⬜ |
| `server/api/.../routing/CameraRoutes.kt` | 4 | TODO на отдельные маршруты | ⬜ |
| `server/api/.../service/NotificationService.kt` | 1 | ✅ Реализован: persistence через NotificationRepository + push-маршрутизация по платформе (PushNotificationDelivery, PushTokenService); APNs-канал подключён | ✅ |
| `server/api/.../config/LdapConfig.kt` | 4 | LDAP/AD интеграция | ⬜ |
| `core/security/.../SecurePasswordHasher.jvm.kt` | 2 | ✅ PBKDF2WithHmacSHA256 (ранее hashCode()-заглушка) | ✅ |
| `core/security/.../SecureTokenEncryption.{ios,native}.kt` | 3+3 | ✅ XOR-obfuscation + Base64URL (врем. мера до KMP AES) | ✅ |
| `core/security/.../BruteForceProtectionManager.{jvm,android,ios,native}.kt` | 1 | ✅ **14.09.2026: рабочая in-memory реализация на всех платформах** — скользящее окно попыток (`timeWindowMinutes`), блокировка при пороге (`maxFailedAttempts`) на `lockoutDurationMinutes`, unlock-time, reset, `getFailedAttempts`; `BruteForceProtectionFactory.create(config)` применяет конфиг (ранее игнорировался). Ревизия 14.09 выявила: предыдущая версия блокировку не выполняла (карты блокировок только очищались). +10 юнит-тестов. Остаток: БД-персистентность (пост-релиз, Фаза 6) |
| `core/network/.../Live555RTSPClient.jvm.kt`, `RTSPSClient.jvm.kt` | 2 | JVM-заглушки (Live555 недоступен на JVM) | ⬜ (платф.) |
| `core/network/.../analytics/NativeAnalytics.ios.kt` | 1 | iOS-заглушка нативного аналитического движка | ⬜ (платф.) |
| `core/network/.../ScannerUtils.kt` (`jvmMain`) | 2 | `get() = null // TODO: реализовать` | ✅ **14.09.2026: реализован** — ScanProgress ETA (`remainingTimeSeconds/Formatted`, `progressPercent`, `hostsProgressPercent`), `collectSnapshots`, `findByIp/Url/Name`, `groupCamerasBy*`; маркер из аудита 09.08 устарел |
| `shared/.../datasource/remote/impl/EventRemoteDataSourceImpl.kt` | 1 | `UnsupportedOperationException` для не поддерживаемых API-операций | ⬜ |
| `shared/.../AnalyticsService.kt` | 1 | заглушка для платформ без нативной поддержки | ⬜ |

## Платформенные заглушки (acceptable)
- `core/network/src/iosMain/.../UPnPDiscovery.ios.kt` — TODO: NSURLSession/CFNetwork для iOS.
- `core/network/src/jvmMain/.../Live555RTSPClient.jvm.kt`, `RTSPSClient.jvm.kt` — JVM-заглушки нативных клиентов (Live555 не доступен на JVM).
- `core/network/src/iosMain/.../NativeAnalytics.ios.kt` — iOS-заглушка.
- `core/network/src/iosMain/nativeMain/.../CertificatePinningEngineWrapper` — `UnsupportedOperationException` в обёртке.

## Вне активного кода (архив/устаревшее, уже заархивировано)
- `archive/modules/core-license/**` — LicenseManager с TODO (онлайн/офлайн активация) — модуль отложен.
- `archive/repositories/**` — старые не-используемые репозитории с `UnsupportedOperationException`.
- `android/app/.../ui/screens/...` — placeholders UI (тексты/URL по умолчанию) — норма.
- `core/network/.../RtspClient.kt:548,573` — `ByteArray(100/1024) // Заглушка` — данные по умолчанию в тесте, не критично.

## Вывод
- ✅ **Реализовано (10 Aug + 14.09.2026):** `SecureTokenEncryption`, `SecurePasswordHasher.jvm`, `ScannerUtils` (ETA/снапшоты/поиск), `BruteForceProtectionManager` (in-memory, все платформы — скользящее окно + lockout), WebRTC SDP (через `OfferSdpAnswerFactory`).
- ⬜ **Остаётся реализовать (пост-релиз/порт A):** реальная медиа-нога WebRTC (Janus-сервер, внешний), APNs (enabled=false), LDAP (внешний сервер), BruteForceProtectionManager → БД-персистентность, части ServerUserRepository/CameraRoutes, полноценный KMP AES.
- Остальное — осознанные платформенные заглушки или тестовые placeholders.
- Серверные TODO (порт A/Фаза 6): LDAP, парт-методы CameraRoutes, части ServerUserRepository.