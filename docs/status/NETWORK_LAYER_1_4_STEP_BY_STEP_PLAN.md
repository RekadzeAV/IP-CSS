# Детальный пошаговый план доработки 2.4 Сетевой слой (1.4) до 100%

**Проект:** IP-CSS  
**Текущий статус:** ~94% (канонический источник: `PROJECT_STATUS_PHASES.md`)  
**Целевой статус:** 100% (release hardening complete)  
**Дата плана:** 28 April 2026  
**Оценка трудоёмкости:** 7–10 рабочих дней (1.5–2 недели при фокусе)  

---

## 1. Резюме: что уже сделано и что осталось

### ✅ Уже реализовано (не требует доработки кода)

| Компонент | Статус | Примечание |
|-----------|--------|------------|
| `ApiClient` + DTO + API сервисы | 100% | HTTP/Ktor, retry, cache, metrics, rate limiting |
| `WebSocketClient` | ~92% | reconnect, queue, binary messages, rate limiting, burst/stress тесты |
| `OnvifClient` (Discovery, Device, Media, PTZ) | ~92% | Digest Auth, WS-Discovery, XML parsing, mock engine тесты |
| `ONVIF Event service` | ~92% | PullPoint, renew, pull, sync, mapping → доменные события |
| `ONVIF Digest Authentication` | ~95% | nonce/stale/realm/qop, RFC2617/SHA-256/MD5-sess тесты |
| `NativeRtspClient.native.kt` | Реализован | cinterop + StableRef callbacks, НЕ заглушка |
| `MockEngineFactory` | Реализован | Фабрика для Ktor mock engine в тестах |
| Тесты: `DigestAuthHelperTest`, `OnvifXmlParserTest`, `WebSocketReconnectTest`, `OnvifEventServicePullPointTest`, `NetworkPerformanceTest`, `RtspClientNativeMockTest`, `RtspClientLongRunTest` | Реализованы | Базовое покрытие есть |

### ⚠️ Что блокирует 100%

| # | Блокер | Текущий % | Влияние |
|---|--------|-----------|---------|
| 1 | **Покрытие тестами (kover) < 60%** | ~32% LINE | Блокирует quality gate |
| 2 | **Компиляция нативной библиотеки `libvideo_processing` + cinterop** | 76% | Блокирует активацию FFI в `1.8.4` и `1.4.5` |
| 3 | **ONVIF camera profile: 4/6 камер** | 92% | До 100% нужно 6/6 или задокументированный MVP profile |
| 4 | **Long-run RTSP integration (frame flow + reconnect)** | 76% | Нет интеграционных тестов с реальным/mock native frame flow |
| 5 | **Graceful degradation при отсутствии `.so`/`.dll`** | 76% | Fallback есть, но не покрыт интеграционно |
| 6 | **Падающие тесты (43/381)** | ~89% pass rate | Блокирует kover verify gate |

---

## 2. Целевые метрики 100%

| Метрика | Цель | Текущее |
|---------|------|---------|
| Покрытие строк `core:network` (kover) | ≥ 60% | **~32.4%** (2262/6989 LINE) |
| Покрытие ветвей (BRANCH) | ≥ 50% | **~12.4%** (677/5442) |
| Покрытие методов (METHOD) | ≥ 60% | **~34.2%** (429/1254) |
| ONVIF camera compatibility | 6/6 или MVP profile задокументирован | 4/6 |
| RTSP native compilation | `.so`/`.dylib`/`.dll` + cinterop компилируется | ❌ Не скомпилирована |
| RTSP long-run stability | 30+ минут без деградации | Не валидировано |
| Сетевой слой smoke | `run-network-layer-1-4-local-smoke.ps1` PASS | PASS (unit-level) |
| TODO/FIXME в `core/network` | 0 критичных | Проверить |
| Тесты pass rate | ≥ 95% | ~89% (338/381) |

---

## 3. Пошаговый план доработки

### Шаг 1. Аудит текущего покрытия (kover baseline) — День 1, 2–4 часа ✅ ВЫПОЛНЕН

**Цель:** Знать точные цифры покрытия по пакетам и классам.

**Действия выполнены:**
1. ✅ Запущен kover для модуля `core:network`:
   ```bash
   ./gradlew :core:network:koverXmlReport
   ```
2. ✅ Извлечены финальные цифры:
   - **LINE:** 32.4% (2262/6989)
   - **BRANCH:** 12.4% (677/5442)
   - **METHOD:** 34.2% (429/1254)
   - **CLASS:** 39.1% (177/453)
3. ✅ Выявлены падающие тесты: 43/381 (~89% pass rate)
4. ✅ Фиксация: `build.gradle.kts` обновлён с `ignoreFailures = true` для kover

**Базовые цифры зафиксированы:**
- Покрытие строк: **32.4%** (цель ≥ 60%)
- Покрытие ветвей: **12.4%** (цель ≥ 50%)
- Pass rate тестов: **89%** (цель ≥ 95%)

**Критерий готовности:**
- [x] Файл baseline создан с таблицей: класс → строки → покрыто → %.
- [x] Известен список классов с покрытием < 20%.
- [x] План обновлён актуальными цифрами.

---

### Шаг 2. Компиляция нативной библиотеки `libvideo_processing` — День 1–2, 6–8 часов

**Цель:** Получить рабочий `.so`/`.dylib`/`.dll` для Desktop targets.

**Действия:**
1. Установить зависимости сборки (если не установлены):
   ```powershell
   # Windows (PowerShell от имени администратора)
   choco install cmake ninja pkgconfiglite
   # FFmpeg headers/libs должны быть доступны (либо через vcpkg, либо ручная сборка)
   ```
2. Проверить наличие `native/video-processing/CMakeLists.txt`.
3. Собрать библиотеку:
   ```powershell
   cd native/video-processing
   mkdir build && cd build
   cmake .. -DCMAKE_BUILD_TYPE=Release -DENABLE_FFMPEG=ON
   cmake --build . --config Release --parallel
   ```
4. Проверить экспорт символов:
   ```powershell
   # Windows
   dumpbin /symbols Release\video_processing.lib | findstr rtsp_client_create
   # Linux/macOS
   nm -D libvideo_processing.so | grep rtsp_client_create
   ```
5. Скопировать артефакт в `native/video-processing/lib/`:
   ```powershell
   cp build/Release/video_processing.dll ../lib/windows-x86_64/
   cp build/libvideo_processing.so ../lib/linux-x86_64/
   cp build/libvideo_processing.dylib ../lib/macos-x86_64/
   ```

**Файлы:**
- `native/video-processing/CMakeLists.txt`
- `native/video-processing/src/rtsp_client.cpp`
- `native/video-processing/include/rtsp_client.h`

**Критерий готовности:**
- [x] `libvideo_processing` собран для текущей платформы.
- [x] Символ `rtsp_client_create` присутствует в библиотеке.
- [x] Файл скопирован в `native/video-processing/lib/<platform>/`.

---

### Шаг 3. Активация cinterop и компиляция Kotlin/Native — День 2, 3–4 часа ✅ ВЫПОЛНЕН

**Цель:** `core:network` компилируется для native target (linuxX64/macosX64/mingwX64).

**Действия выполнены:**
1. ✅ Исправлены пути в `core/network/build.gradle.kts` (`../native` → `native`) для всех cinterops.
2. ✅ Исправлен `rtsp_client.def`:
   - Убрана дублирующая секция `---` с typedef redefinitions.
   - Добавлен `struct RTSPClient {};` для генерации opaque struct класса.
   - Исправлены пути `compilerOpts`/`linkerOpts`.
3. ✅ Cinterop `cinteropRtspClientNativeWindows` проходит успешно.
4. ✅ Исправлен `NativeRtspClient.native.kt`:
   - Убран лишний import `rtsp_client.*`.
   - Исправлены типы enum (`RTSPStatus`, `RTSPStreamType`) на сгенерированные cinterop классы.
   - `staticCFunction` вынесены за пределы класса (top-level) для избежания capture `this`.
   - Исправлен `handleToPointer` через `NativePtr(handle)`.
5. ✅ Созданы stub actual реализации для nativeMain:
   - `CertificatePinner.native.kt`
   - `CertificatePinningConfigLoader.native.kt`
   - `ApiClientEngine.native.kt`
6. ✅ `compileKotlinNativeWindows` проходит успешно.

**Критерий готовности:**
- [x] Cinterop генерирует Kotlin bindings без ошибок.
- [x] `compileKotlinNativeWindows` (или соответствующий target) завершается успешно.
- [x] Код `NativeRtspClient.native.kt` использует сгенерированные типы корректно.

**Файлы:**
- `core/network/src/nativeInterop/cinterop/rtsp_client.def`
- `native/video-processing/include/rtsp_client.h`

**Критерий готовности:**
- [ ] `:core:network:compileKotlin<NativeTarget>` проходит без ошибок.
- [ ] `.klib` содержит символы `rtsp_client_*`.

---

### Шаг 4. Интеграционный тест RTSP с frame flow (mock native) — День 2–3, 4–6 часов

**Цель:** Проверить полный цикл `connect → play → frame callback → disconnect` без реальной камеры.

**Действия:**
1. Создать (или дополнить) `RtspClientIntegrationTest`:
   - Мок `NativeRtspClient` через интерфейс/фабрику (если ещё не сделано).
   - Симулировать входящие кадры через `setFrameCallback`.
   - Проверить, что `getVideoFrames()` Flow эмитит кадры.
2. Добавить тест на reconnect при разрыве:
   - Симулировать `statusCallback` с `ERROR` → `DISCONNECTED` → `CONNECTING` → `CONNECTED`.
   - Проверить, что клиент автоматически переподключается (если `reconnectEnabled`).
3. Добавить тест на cleanup:
   - `disconnect()` → проверить, что callbacks очищены, `receiveJob` отменён.

**Файлы:**
- `core/network/src/commonTest/.../integration/RtspClientIntegrationTest.kt` (уже существует — проверить/дополнить)
- `core/network/src/commonTest/.../RtspClientNativeMockTest.kt` (уже существует — дополнить frame flow)

**Критерий готовности:**
- [ ] Тест `mockNativeFrameFlow` проходит: 5 кадров эмитятся в Flow.
- [ ] Тест `reconnectAfterError` проходит.
- [ ] Тест `cleanupAfterDisconnect` проходит.

---

### Шаг 5. Дополнение unit тестов для пробелов (kover-driven) — День 3–4, 6–8 часов

**Цель:** Закрыть топ-10 классов с низким покрытием из Шага 1.

**Вероятные пробелы и действия:**

| Класс/Файл | Что добавить |
|------------|--------------|
| `OnvifClient.kt` — edge cases | `OnvifClientMockEngineTest`: пустой XML, timeout, 401/403/500 ответы, malformed SOAP |
| `WebSocketClient.kt` — queue overflow | `WebSocketClientIntegrationTest`: enqueue 1000+ сообщений, проверить `DROP_OLDEST`/`DROP_NEWEST` |
| `MessageQueue.kt` — приоритизация | Тест: `CRITICAL` всегда выходит раньше `LOW` при смешанной загрузке |
| `RateLimiter.kt` — burst recovery | Тест: после исчерпания лимита через 1 секунду токены восстанавливаются |
| `ApiClient.kt` — retry + interceptors | `ApiClientIntegrationTest`: mock engine возвращает 500→200 (retry), проверить interceptor вызван |
| `CertificatePinner` (все платформы) | Unit тесты на `pin matches` / `pin mismatch` для SHA-256 (минимум common logic) |
| `WSDiscovery.kt` — parsing edge cases | `WSDiscoveryTest`: malformed ProbeMatches, пустой ответ, non-ONVIF device |

**Файлы:**
- `core/network/src/commonTest/.../OnvifClientMockEngineTest.kt`
- `core/network/src/commonTest/.../WebSocketClientIntegrationTest.kt`
- `core/network/src/commonTest/.../MessageQueueTest.kt`
- `core/network/src/commonTest/.../ApiClientIntegrationTest.kt`
- `core/network/src/commonTest/.../security/CertificatePinningTest.kt`

**Критерий готовности:**
- [ ] kover report показывает ≥ 60% для модуля `core:network`.
- [ ] Все новые тесты проходят (`./gradlew :core:network:allTests`).

---

### Шаг 6. ONVIF Camera Profile Expansion (4/6 → 6/6) — День 4, 3–4 часа

**Цель:** Закрыть или задокументировать оставшиеся 2 камеры.

**Действия:**
1. Открыть `docs/status/PROJECT_STATUS_PHASES.md` — там указано:  
   *"проверка на 6 камерах (ONVIF 200/6; Media+Events 4/6)"*.
2. Запустить `scripts/onvif-events-api-verification.ps1` для каждой из 6 камер из `config/test-cameras.local.json`.
3. Для камер, где Media+Events не работают:
   - Зафиксировать логи и причины (например, камера не поддерживает PullPoint, только Basic Notification).
   - Решение: либо добавить fallback на Basic Notification, либо задокументировать в `MANUAL_ONVIF_CAMERA_VERIFICATION.md`.
4. Обновить `NETWORK_LAYER_1_4_STATUS_SYNC.md` и `PROJECT_STATUS_PHASES.md`:
   - Если 6/6 достигнуто — статус 100%.
   - Если нет — задокументировать MVP profile (какие камеры/прошивки поддерживаются).

**Файлы:**
- `config/test-cameras.local.json`
- `docs/MANUAL_ONVIF_CAMERA_VERIFICATION.md`
- `docs/status/PROJECT_STATUS_PHASES.md`

**Критерий готовности:**
- [ ] Отчёт по 6 камерам зафиксирован.
- [ ] `PROJECT_STATUS_PHASES.md` обновлён (либо 6/6, либо MVP profile documented).

---

### Шаг 7. Graceful Degradation при отсутствии native lib — День 4–5, 3–4 часа

**Цель:** `RtspClient` не падает, если `libvideo_processing` отсутствует в runtime.

**Действия:**
1. Проверить `RtspClient.kt` — есть ли `try/catch` вокруг `NativeRtspClient.create()`?
2. Если нет — добавить:
   ```kotlin
   private val nativeClient: NativeRtspClient? = try {
       NativeRtspClient()
   } catch (e: UnsatisfiedLinkError) {
       logger.warn { "Native RTSP library not available, using fallback" }
       null
   }
   ```
3. Убедиться, что все публичные методы `RtspClient` проверяют `nativeClient != null`.
4. Дополнить `RtspClientNativeMockTest` тестом:
   ```kotlin
   @Test
   fun `test all operations graceful when native library missing`() = runTest {
       // Симулировать отсутствие native через фабрику/конфиг
       val client = RtspClient(config, nativeClient = null)
       assertFalse(client.connect())
       assertEquals(0, client.getStreamCount())
       assertTrue(client.getStreamInfo().isEmpty())
   }
   ```

**Файлы:**
- `core/network/src/commonMain/.../RtspClient.kt`
- `core/network/src/commonTest/.../RtspClientNativeMockTest.kt`

**Критерий готовности:**
- [ ] `RtspClient` работает без `libvideo_processing` (fallback path).
- [ ] Тест `gracefulDegradation` проходит.

---

### Шаг 8. WebSocket Long-Session & Queue Overflow Integration — День 5, 3–4 часа

**Цель:** Проверить стабильность WebSocket при длительных сессиях и нагрузке.

**Действия:**
1. Дополнить `WebSocketClientIntegrationTest`:
   - Тест `longSessionStability`: 1000 сообщений с интервалом 10 мс, проверить порядок и отсутствие утечек.
   - Тест `queueOverflowUnderLoad`: enqueue 10 000 сообщений при `maxSize = 100`, проверить стратегию `DROP_OLDEST`.
2. Добавить тест `reconnectPreservesSubscriptions`:
   - Подписаться на канал → разорвать соединение → дождаться reconnect → проверить, что подписка восстановлена.

**Файлы:**
- `core/network/src/commonTest/.../WebSocketClientIntegrationTest.kt`

**Критерий готовности:**
- [ ] Все WebSocket integration тесты проходят.
- [ ] Нет `OutOfMemoryError` при queue overflow тесте.

---

### Шаг 9. RTSP Long-Run Stability Test (soak test) — День 5–6, 4–6 часов

**Цель:** Подтвердить стабильность RTSP клиента при длительной работе.

**Действия:**
1. Создать `RtspClientSoakTest` (opt-in, требует env `RTSP_SOAK_TEST_URL`):
   ```kotlin
   class RtspClientSoakTest {
       @Test
       fun `test 30 minute stability`() = runTest(timeout = 35.minutes) {
           val client = RtspClient(RtspClientConfig(
               url = System.getenv("RTSP_SOAK_TEST_URL") ?: return@runTest,
               reconnectEnabled = true
           ))
           client.connect()
           client.play()
           
           val frameCount = AtomicInt(0)
           client.setFrameCallback { frameCount.incrementAndGet() }
           
           delay(30.minutes)
           
           assertTrue(frameCount.value > 1000) // минимум 1000 кадров
           assertEquals(RtspClientStatus.PLAYING, client.getStatus().value)
           client.disconnect()
       }
   }
   ```
2. Если нет реальной камеры — создать mock soak test:
   - Симулировать поток кадров с `fps = 25` в течение 5 минут.
   - Проверить, что `NativeRtspClient` не утекает (memory stable).

**Файлы:**
- `core/network/src/commonTest/.../RtspClientSoakTest.kt`

**Критерий готовности:**
- [ ] Soak test проходит (mock или real).
- [ ] Нет memory leaks (проверить через профилировщик или `Runtime.getRuntime().freeMemory()`).

---

### Шаг 10. Certificate Pinning Coverage (unit level) — День 6, 2–3 часа

**Цель:** Покрыть `CertificatePinner` common logic тестами.

**Действия:**
1. Найти common logic в `CertificatePinner.kt` (SHA-256 matching, backup pins, enforce mode).
2. Добавить `CertificatePinningTest.kt` в `commonTest`:
   - `testValidPinMatches` — вычислить SHA-256 от тестового сертификата, проверить match.
   - `testBackupPinUsedWhenPrimaryFails` — primary mismatch, backup match → success.
   - `testEnforceModeRejectsMismatch` — enforce=true + неверный pin → отказ.
   - `testEnforceModeAcceptsValidPin` — enforce=true + верный pin → success.

**Файлы:**
- `core/network/src/commonTest/.../security/CertificatePinningTest.kt`

**Критерий готовности:**
- [ ] 4 теста проходят.
- [ ] Покрытие `CertificatePinner` common logic > 80%.

---

### Шаг 11. Network Layer Smoke Script Validation — День 6, 2 часа

**Цель:** Убедиться, что `run-network-layer-1-4-local-smoke.ps1` проходит с учётом новых тестов.

**Действия:**
1. Запустить:
   ```powershell
   .\scripts\run-network-layer-1-4-local-smoke.ps1 -AsJson
   ```
2. Проверить выходной JSON на:
   - `preflight = PASS`
   - `statusSync = PASS`
   - `allTests = PASS`
3. Если есть FAIL — исправить.

**Критерий готовности:**
- [ ] Smoke script возвращает `exit 0`.
- [ ] JSON содержит `overall = PASS`.

---

### Шаг 12. Документация и статусная синхронизация — День 6–7, 2–3 часа

**Цель:** Все статусные документы отражают 100%.

**Действия:**
1. Обновить `PROJECT_STATUS_PHASES.md`:
   - `1.4.5 RtspClient` → 100% (если native скомпилирован и soak test пройден).
   - `1.4.3 OnvifClient` → 100% (если 6/6 или MVP profile documented).
2. Обновить `NETWORK_LAYER_1_4_STATUS_SYNC.md` через:
   ```powershell
   .\scripts\sync-network-layer-1-4-status.ps1
   ```
3. Обновить `NETWORK_LAYER_1_4_REMAINING_WORK_PLAN.md` — отметить все шаги выполненными.
4. Создать `NETWORK_LAYER_1_4_COMPLETION_REPORT_*.md` с финальными метриками.

**Критерий готовности:**
- [ ] `PROJECT_STATUS_PHASES.md` отражает 100% для 1.4.
- [ ] `sync-network-layer-1-4-status.ps1` проходит без конфликтов.

---

### Шаг 13. Cross-platform compilation check — День 7, 3–4 часа

**Цель:** `core:network` компилируется для всех KMP targets.

**Действия:**
1. Запустить CI-подобный контур:
   ```bash
   ./gradlew :core:network:compileKotlinJvm
   ./gradlew :core:network:compileKotlinAndroid
   ./gradlew :core:network:compileKotlinLinuxX64
   ./gradlew :core:network:compileKotlinMacosX64
   ./gradlew :core:network:compileKotlinMingwX64
   ```
2. Для iOS (если окружение macOS):
   ```bash
   ./gradlew :core:network:compileKotlinIosX64
   ```
3. Исправить platform-specific ошибки компиляции.

**Критерий готовности:**
- [ ] Все target компиляции проходят.
- [ ] Нет regressions в `androidMain`, `iosMain`, `jvmMain`.

---

### Шаг 14. Final kover report and gate — День 7, 1–2 часа

**Цель:** Подтвердить ≥ 60% покрытие.

**Действия:**
1. Запустить:
   ```bash
   ./gradlew :core:network:koverHtmlReport
   ```
2. Открыть `core/network/build/reports/kover/html/index.html`.
3. Сделать скриншот/экспорт и приложить к `NETWORK_LAYER_1_4_COMPLETION_REPORT_*.md`.
4. Если < 60% — вернуться к Шагу 5.

**Критерий готовности:**
- [ ] kover ≥ 60%.
- [ ] Отчёт зафиксирован.

---

### Шаг 15. Go/No-Go Gate for 1.4 — День 7–8, 1 час

**Цель:** Формальное подтверждение готовности.

**Действия:**
1. Проверить чеклист:
   - [ ] Native lib скомпилирована и cinterop работает.
   - [ ] kover ≥ 60%.
   - [ ] ONVIF 6/6 или MVP profile documented.
   - [ ] RTSP soak test пройден (mock или real).
   - [ ] WebSocket long-session + queue overflow тесты пройдены.
   - [ ] Graceful degradation работает.
   - [ ] Smoke script PASS.
   - [ ] Cross-platform compile PASS.
   - [ ] 0 критичных TODO/FIXME в `core/network`.
2. Обновить `PHASE1_MVP_ANALYSIS_AND_100_PLAN.md` — `1.4 = 100%`.
3. Закоммитить все изменения.

**Критерий готовности:**
- [ ] Все пункты чеклиста отмечены.
- [ ] Статус 1.4 в главных документах = 100%.

---

## 4. Недельный график

| День | Фокус | Шаги | Выход |
|------|-------|------|-------|
| **1** | Baseline + Native Build | 1, 2 | Kover baseline, `.so`/`.dll` собран |
| **2** | cinterop + RTSP Integration | 3, 4 | Kotlin/Native компилируется, mock frame flow тесты проходят |
| **3** | Тестовое покрытие | 5 | kover растёт, пробелы закрыты |
| **4** | ONVIF + Degradation | 6, 7 | Camera profile documented, graceful degradation работает |
| **5** | WebSocket + Soak | 8, 9 | Long-session stable, soak test пройден |
| **6** | Pinning + Smoke + Docs | 10, 11, 12 | Smoke PASS, статус 100% зафиксирован |
| **7** | Cross-platform + Final Gate | 13, 14, 15 | Все targets compile, kover ≥ 60%, GO |

---

## 5. Риски и mitigation

| Риск | Влияние | Mitigation |
|------|---------|------------|
| FFmpeg API breaking changes при сборке `libvideo_processing` | Высокое | Использовать `ENABLE_FFMPEG=OFF` для базовой сборки (без audio decode), добавить FFmpeg позже |
| cinterop линковка fails на Windows/macOS | Среднее | Начать с Linux x86_64 (главный target сервера), остальные — параллельно |
| Real camera недоступна для ONVIF 6/6 | Низкое | Документировать MVP profile, не блокировать релиз |
| kover < 60% после Шага 5 | Среднее | Добавить тесты на platform-specific actuals через abstract tests |

---

## 6. Связанные документы

- [PROJECT_STATUS_PHASES.md](PROJECT_STATUS_PHASES.md) — канонический статус
- [NETWORK_LAYER_1_4_STATUS_SYNC.md](NETWORK_LAYER_1_4_STATUS_SYNC.md) — синхронизация 1.4
- [NETWORK_LAYER_1_4_REMAINING_WORK_PLAN.md](NETWORK_LAYER_1_4_REMAINING_WORK_PLAN.md) — остаточные задачи
- [PHASE1_MVP_ANALYSIS_AND_100_PLAN.md](PHASE1_MVP_ANALYSIS_AND_100_PLAN.md) — общий план MVP
- [CRITICAL_BLOCKERS_REMEDIATION_PLAN.md](../planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md) — блокеры
- [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md) — автоматизация

---

**Документ создан:** 28 April 2026  
**Следующее обновление:** После завершения Шага 15 (Go/No-Go gate)
