# Детальный план доработки 2.4 Сетевой слой (1.4) до 100%

**Текущий статус:** ~94% (PHASE1_MVP_ANALYSIS_AND_100_PLAN.md)
**Целевой статус:** 100% (release hardening complete)
**Дата плана:** 28 April 2026

---

## Остаточные задачи по компонентам

### 1. RtspClient — доведение с ~76% до 100% (P0, критический блокер)

| Подзадача | Описание | Файлы |
|-----------|----------|-------|
| 1.1 | Реализовать полноценный `nativeMain` actual для `NativeRtspClient` (сейчас заглушка) | `core/network/src/nativeMain/.../NativeRtspClient.native.kt` |
| 1.2 | Использовать cinterop `rtsp_client` для вызовов C API (аналогично iOS) | тот же |
| 1.3 | Реализовать `StableRef` callbacks (frame + status) с корректным lifecycle | тот же |
| 1.4 | Добавить `RtspClientLongRunTest` — симуляция 30+ минут работы, reconnect, cleanup | `core/network/src/commonTest/.../RtspClientLongRunTest.kt` |
| 1.5 | Добавить `RtspClientNativeMockTest` — тесты Kotlin-обёртки без реальной нативной библиотеки | `core/network/src/commonTest/.../RtspClientNativeMockTest.kt` |
| 1.6 | Улучшить graceful degradation при отсутствии нативной библиотеки | `RtspClient.kt` |

### 2. OnvifClient — доведение с ~92% до 100% (P1)

| Подзадача | Описание | Файлы |
|-----------|----------|-------|
| 2.1 | Добавить `DigestAuthHelperTest` — unit-тесты для Digest аутентификации (nonce, stale, realm, qop) | `core/network/src/commonTest/.../auth/DigestAuthHelperTest.kt` |
| 2.2 | Добавить `OnvifXmlParserTest` — тесты парсинга XML ответов (probe matches, capabilities, profiles) | `core/network/src/commonTest/.../onvif/OnvifXmlParserTest.kt` |
| 2.3 | Добавить `OnvifClientMockEngineTest` — интеграционные тесты с mock Ktor engine | `core/network/src/commonTest/.../OnvifClientMockEngineTest.kt` |
| 2.4 | Улучшить обработку edge cases (пустые ответы, невалидный XML, timeout) | `OnvifClient.kt`, `OnvifEventServiceImpl.kt` |

### 3. WebSocketClient — доведение с ~92% до 100% (P1)

| Подзадача | Описание | Файлы |
|-----------|----------|-------|
| 3.1 | Добавить `WebSocketReconnectTest` — тесты логики переподключения с экспоненциальным backoff | `core/network/src/commonTest/.../WebSocketReconnectTest.kt` |
| 3.2 | Добавить `WebSocketLongSessionTest` — тесты стабильности при длительных сессиях | тот же |
| 3.3 | Дополнить `WebSocketClientIntegrationTest` тестами queue overflow под нагрузкой | `WebSocketClientIntegrationTest.kt` |

### 4. ONVIF Event service — доведение с ~92% до 100% (P1)

| Подзадача | Описание | Файлы |
|-----------|----------|-------|
| 4.1 | Добавить `OnvifEventServicePullPointTest` — lifecycle подписки/renew/pull/unsubscribe | `core/network/src/commonTest/.../onvif/OnvifEventServicePullPointTest.kt` |
| 4.2 | Добавить `OnvifEventMapperTest` — тесты маппинга нативных ONVIF событий в доменные `Event` | `core/network/src/commonTest/.../onvif/OnvifEventMapperTest.kt` |
| 4.3 | Добавить `OnvifEventSubscriptionTest` — тесты управления подписками (timeout, renew, cleanup) | `core/network/src/commonTest/.../onvif/OnvifEventSubscriptionTest.kt` |

### 5. ONVIF Digest Authentication — доведение с ~95% до 100% (P2)

| Подзадача | Описание | Файлы |
|-----------|----------|-------|
| 5.1 | Добавить тесты `stale nonce` — повторный запрос с новым nonce | `DigestAuthHelperTest.kt` |
| 5.2 | Добавить тесты `realm caching` — кэш параметров по URL | `DigestAuthHelperTest.kt` |
| 5.3 | Добавить тесты `qop=auth` и `qop=auth-int` | `DigestAuthHelperTest.kt` |

### 6. Общее тестовое покрытие — доведение с ~40% до 100% по сетевому слою (P1)

| Подзадача | Описание | Файлы |
|-----------|----------|-------|
| 6.1 | Создать `MockEngineFactory` для тестов Ktor клиента | `core/network/src/commonTest/.../test/MockEngineFactory.kt` |
| 6.2 | Добавить `ApiClientIntegrationTest` с mock engine | `core/network/src/commonTest/.../integration/ApiClientIntegrationTest.kt` |
| 6.3 | Добавить `CertificatePinningTest` для всех платформ (минимум unit) | `core/network/src/commonTest/.../security/CertificatePinningTest.kt` |
| 6.4 | Добавить baseline performance тесты (rate limiter, message queue) | `core/network/src/commonTest/.../performance/NetworkPerformanceTest.kt` |

---

## Порядок реализации (execution order)

1. **Шаг 1:** ✅ Реализация `NativeRtspClient.native.kt` (cinterop + StableRef) — снимает главный блокер 1.8.4
2. **Шаг 2:** ✅ Создание `MockEngineFactory` и `OnvifClientMockEngineTest` — закрывает 2.3, 6.1
3. **Шаг 3:** ✅ `DigestAuthHelperTest` + `OnvifXmlParserTest` — закрывает 2.1, 2.2, 5.1–5.3
4. **Шаг 4:** ✅ `OnvifEventServicePullPointTest` + `OnvifEventParserTest` — закрывает 4.1–4.3
5. **Шаг 5:** ✅ `WebSocketReconnectTest` + `WebSocketLongSessionTest` — закрывает 3.1–3.3
6. **Шаг 6:** ✅ `RtspClientLongRunTest` + `RtspClientNativeMockTest` — закрывает 1.4–1.6
7. **Шаг 7:** ✅ `ApiClientIntegrationTest` + `CertificatePinningTest` + performance baseline — закрывает 6.2–6.4

---

## Критерий готовности 100%

- [x] `NativeRtspClient.native.kt` не является заглушкой — использует cinterop
- [ ] Все новые тесты проходят (`./gradlew :core:network:allTests` или аналог)
- [ ] Покрытие сетевого слоя по строкам ≥ 60% (kover)
- [ ] Нет `TODO`/`FIXME` в коде сетевого слоя (кроме отложенных в Phase 2+)
- [ ] `scripts/run-network-layer-1-4-local-smoke.ps1` проходит без ошибок
