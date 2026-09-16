# План тестирования

**Версия проекта:** 0.5.1.1-beta
**Дата:** 08 August 2026 · **актуализировано 15.09.2026** (Kover-пороги: см. ниже)
**Примечание 15.09:** актуальные Kover-пороги — `server:api` **minBound(30)**, `core:network` **minBound(25)**, `shared` **minBound(20)**. Упоминания `minBound(10)` ниже — исторические (первичный порог), перевыполнены. Подробности — `PLAN_RECOMMENDATIONS.md` (Тестирование) и `PLAN_EXECUTION_MASTER.md` (Этап 1).

---

## Текущая база
- Фактические прогоны в main (offline):
  - `:server:api:test` — **474 теста, 0 failures / 0 errors / 0 skipped** (Kover-порции №6-8 + Web Push delivery с автоочисткой подписок + WebRTC `WebRtcMediaGateway`/динамический fallback-SDP). Покрытие `server:api` — **30.1% LINE** (порог Kover `minBound(30)`).
  - `:shared:desktopTest` — **245 тестов, 1 skipped, 0 failures / 0 errors**.
  - `:core:common:desktopTest` — **23 теста, 5 skipped, 0 failures / 0 errors**.
  - `:core:test-jvm:test` — **19 тестов, 0 failures / 0 errors / 0 skipped**.
  - `:core:network:desktopTest` — **544 теста, 0 failed, 38 skipped** (все 25 падений устранены; причины и фиксы в `docs/analysis-core-network-25-failures-2026-08-09.md`).
- Итого подтверждённых прошедших тестов в стабильных модулях: **540 + 506 (core:network) = 1046**.
- Блокер тестовой готовности устранён: `:core:network:desktopTest` — **544 tests, 0 failed, 38 skipped**.

## Статус тестовой сборки
| Модуль | Задача | Статус |
|---|---|---|
| `server:api` | Тесты | ✅ **474 passed / 0 failed / 0 skipped**; Kover LINE **30.1%**, порог `minBound(30)` (`server/api/build.gradle.kts:160`) |
| `shared` | `desktopTest` | ✅ |
| `core:common` | `desktopTest` | ✅ |
| `core:test-jvm` | `test` | ✅ |
| `core:network` | `desktopTest` | ✅ **544 tests, 0 failed, 38 skipped** |
| `core:ui-bridge` | `desktopTest` | ✅ **46 tests, 0 failed** (модуль восстановлен после рассинхронизации API) |
| `android:app` | `assembleDebug` | ✅ APK собран (33MB) |
| `server:api` | `integrationTest` | ✅ подключено; 10 skipped (ApiIntegrationTest — нет сервера), 4 passed (DatabaseIntegrationTest) |

## Файловая база проекта
- Всего файлов: **~210k** (с учётом артефактов `build/`, `.class`, `.map`, `.bin`, `node_modules`-подобных `.js/.ts`).
- `docs/`: **1,291** `.md`.
- Топ расширений среди исходов/артефактов: `.md`, `.kt`, `.java`, `.json`, `.xml`, `.kts`, `.c/.h`, `.cmake`, `.o/.d`, `.jar`.

## Шаги
1. **Исправить `:core:network:desktopTest`** — ✅ **ВСЕ 25 ПАДЕНИЙ УСТРАНЕНЫ**: **544 tests / 0 failed / 38 skipped** (было 25 failed). Причины и фиксы: `docs/analysis-core-network-25-failures-2026-08-09.md` (парсер XAddr как child-элемент, `trt:Profiles`, SOAPAction в MockEngine, WebSocketMessage discriminator, RTSP backoff/race, locale-независимый `%.2f`).
2. **Интеграционные тесты:** ✅ `:server:api:integrationTest` подключён, запускается; требуется окружение PostgreSQL+Redis (`docker-compose.integration.yml`).
3. **Native/JNI smoke:** ✅ `NativeRtspClientBridgeJvmTest`, `NativeRtspClientSmokeTest`, `NativeRtspClientLiveFrameTest`, `NativeLibraryLoadTest` — проходят на desktop.
4. **Stub-аудит:** ✅ выполнен; отчёт `docs/stub-audit-active-2026-08-09.md` (52 файла). Реализовано: `SecureTokenEncryption` (iOS/Native — детерм. XOR+Base64URL), `SecurePasswordHasher.jvm` (PBKDF2WithHmacSHA256; модуль `core:security:desktopTest` — 37/0). Осталось: WebRTC placeholder SDP, APNs (не подключён), LDAP, KMP AES.
5. **KMP-гейты (CONTRIBUTING):** ✅ **ПОЛНЫЙ прогон `verify-kmp-phase1.py` (python+gradle) — ПРОХОДИТ**: `check-commonmain-forbidden-imports`, `check-security-expect-actual-signatures`, `check-no-jvm-deps-in-native-source-sets`, `check-video-runtime-matrix-config`, `validate-video-e2e-profile`, `:core:common:compileKotlinMetadata`/`:core:network`/`:shared` (SKIPPED по конфигурации), `:core:common:desktopTest`, `:core:common:compileKotlinNativeWindows`. Примечание: `core/network` Android-таргет получает `actual DigestCrypto` из `jvmMain` (иерархия source sets), отдельный `DigestCrypto.android.kt` не нужен:
   ```text
   python scripts/ci/verify-kmp-phase1.py
   python scripts/ci/check-commonmain-forbidden-imports.py .
   python scripts/ci/check-security-expect-actual-signatures.py .
   python scripts/ci/check-no-jvm-deps-in-native-source-sets.py .
   ```
6. **Android:** ✅ полная сборка `:android:app:assembleDebug` успешна (APK ~33MB) + `:android:app:testDebugUnitTest` — **3 tests, 0 failures**; D8 OOM не проявился. Компиляция `:core:network:compileDebugKotlinAndroid` ранее падала из-за duplicate `DigestCrypto` actual — устранено удалением лишнего android-файла.
7. **iOS:** сборка только на macOS/Xcode (не выполнялось).
8. **Покрытие:** ✅ Kover для `server:api` — добавлены unit-тесты для ранее непокрытых классов; актуальные цифры: INSTRUCTION 22.2%, **LINE 23.1%**, BRANCH 17.4%, METHOD 21.8%, CLASS 23.8% (обновлено после прогона; порог `minBound(10)` выполняется с запасом). Цель — повышение до ≥30%.
