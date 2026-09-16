# Анализ незавершённых задач по слоям (1.3, 1.2, 1.4)

**Дата:** 27 April 2026  
**Цель:** Детальный анализ того, что именно осталось не завершённым в указанных слоях перед завершением Фазы 1

---

## 📊 Краткая сводка

| Слой | Прогресс | Критичность | Остаток работ | Оценка |
|------|----------|-------------|---------------|--------|
| **Data Layer (1.3)** | 98% | Низкая | Release-level non-functional проверки | 1-2 дня |
| **Доменный слой (1.2)** | 55% | Средняя | AnalyticsService реализация | 5-7 дней |
| **Сетевой слой (1.4)** | 95% | Высокая | RTSP runtime stability, field validation | 3-5 дней + тесты |

**Общий остаток:** ~10-15 человеко-дней

---

## 1. Data Layer (1.3) - 98%

### ✅ Что завершено

| ID | Задача | Статус |
|----|--------|--------|
| 1.3.1 | SQLDelight репозитории для всех сущностей | ✅ 100% |
| 1.3.2 | Entity мапперы | ✅ 100% |
| 1.3.3 | DatabaseFactory (Android, iOS, Desktop) | ✅ 100% |
| 1.3.4 | LocalDataSource / RemoteDataSource (6/6) | ✅ 100% |
| 1.3.5 | Рефакторинг репозиториев V2 | ✅ 100% |
| 1.3.6 | Миграции БД (версионирование, MigrationManager, тесты) | ✅ 100% |

### ⚠️ Что осталось (2%)

**Остаток: release-level non-functional проверки**

| Задача | Описание | Приоритет | Оценка |
|--------|----------|-----------|--------|
| **1.3.6.A** | Production smoke тесты после миграции | Высокий | 0.5 дня |
| **1.3.6.B** | Performance тесты для bulk operations | Средний | 1 день |
| **1.3.6.C** | Documentation: миграционный guide для production | Низкий | 0.5 дня |

### Детализация остатка

#### 1.3.6.A - Production smoke тесты

**Что нужно:**
```kotlin
// Тесты для проверки:
// 1. Миграция fresh DB → target version
// 2. Миграция legacy v1 → v2 → target
// 3. Idempotency: повторная миграция не ломает
// 4. Downgrade guard: нельзя откатиться на старую версию
```

**Статус:** Частично выполнено (`MigrationManagerIntegrationTest` покрывает fresh/v1/v2/idempotent/downgrade-guard)

**Остаток:** Production-профильные тесты с реальными данными

#### 1.3.6.B - Performance тесты

**Что нужно:**
- Bulk insert/update/delete операции (>1000 записей)
- Миграция большой БД (100K+ записей)
- Memory usage during migration

**Статус:** Не начато

**Оценка:** 1 день на настройку + запуск

#### 1.3.6.C - Documentation

**Что нужно:**
- Migration guide для production
- Rollback procedure
- Best practices для migration в production

**Статус:** Частично выполнено (есть runbook'и)

**Остаток:** Единый consolidated guide

### Вывод по Data Layer

**Реальный остаток:** ~2 дня (в основном documentation и minor tests)  
**Блокирует ли MVP:** Нет  
**Рекомендация:** Выполнить параллельно с другими задачами, не критично

---

## 2. Доменный слой (1.2) - 55%

### ✅ Что завершено

| ID | Задача | Статус |
|----|--------|--------|
| 1.2.1 | Модели данных (Camera, Recording, Event, User, Settings, Notification) | ✅ 100% |
| 1.2.2 | Интерфейсы репозиториев | ✅ 100% |
| 1.2.3 | Use Cases (28 штук) | ✅ 100% |

### ⚠️ Что осталось (45%)

**Остаток: AnalyticsService реализация и интеграция**

| Задача | ID | Описание | Приоритет | Оценка |
|--------|-----|----------|-----------|--------|
| **AnalyticsService нативная реализация** | 1.2.5 | Полная реализация для всех платформ | Высокий | 5-7 дней |
| **Use Cases для аналитики** | 1.2.4 | DetectMotion, DetectObjects, TrackObjects, ANPR, DetectFaces | Средний | 3-5 дней |
| **Интеграция с VideoAnalyticsService** | 2.1.7 | Сквозная интеграция AI с видеопотоками | Высокий | 3-4 дня |

### Детализация остатка

#### 1.2.5 - AnalyticsService контракт

**Текущий статус:**
```kotlin
// shared/src/commonMain/kotlin/.../AnalyticsService.kt
interface AnalyticsService {
    suspend fun detectMotion(...): Result<MotionDetectionResult>
    suspend fun detectObjects(...): Result<ObjectDetectionResult>
    suspend fun detectFaces(...): Result<FaceDetectionResult>
    suspend fun recognizeLicensePlate(...): Result<LicensePlateRecognitionResult>
}
```

**Что реализовано:**
- ✅ Интерфейс определён
- ✅ JVM реализация (частично)
- ✅ Android реализация (через JNI)
- ⚠️ iOS реализация (не завершена)
- ⚠️ Desktop реализация (заглушка)

**Остаток:**
- Полноценная iOS реализация через KMM/Native
- Desktop нативная интеграция
- Единый API для всех платформ

#### 1.2.4 - Use Cases для аналитики

**Статус по Use Cases:**

| Use Case | Статус | Примечание |
|----------|--------|------------|
| DetectMotionUseCase | 🟡 Реализован | Требуется field calibration |
| DetectObjectsUseCase | 🟡 Реализован | Зависит от модели (TFLite/YOLO) |
| TrackObjectsUseCase | 🟡 Реализован | Не связан полностью с UI |
| RecognizeLicensePlateUseCase | ✅ Готов | Полный цикл реализован |
| DetectFacesUseCase | ✅ Готов | Полный цикл реализован |

**Остаток:**
- Полевая калибровка детекторов
- Интеграция с UI
- Policy событий

#### 2.1.7 - Сквозная интеграция AI с видеопотоками

**Текущий статус:**
```kotlin
// VideoAnalyticsService.kt
fun startAnalytics(
    cameraId: String,
    camera: Camera,
    frameInput: AnalyticsFrameInput  // RTSP_DECODED или HLS_DERIVED
)
```

**Что реализовано:**
- ✅ `AnalyticsFrameInput` / `AnalyticsFrameSourceKind`
- ✅ Метрики `/analytics/metrics` (`frameSource`, `lastError`, `lastErrorAt`)
- ✅ WebSocket события аналитики
- ✅ Матрица приёмки `PHASE2_1_7_ACCEPTANCE_MATRIX.md`

**Остаток:**
- ❌ HLS-only аналитика (без RTSP frame source)
- ❌ Сквозная стабильность тестирование
- ❌ Production monitoring и alerting

**Сценарии приёмки (из матрицы):**
- **S-RTSP-1:** RTSP-decoded frames → аналитика → события (требуется)
- **S-RTSP-2:** Ошибка потока → корректная обработка (требуется)
- **S-HLS-1:** HLS-only → аналитика (бэклог)

### Вывод по Доменному слою

**Реальный остаток:** ~10-15 дней  
**Блокирует ли MVP:** Частично (базовый MVP работает без полной аналитики)  
**Рекомендация:** 
- Для MVP: достаточно базовой реализации AnalyticsService
- Полную реализацию перенести в Фаза 2
- Критично закрыть 2.1.7 (сквозная интеграция)

---

## 3. Сетевой слой (1.4) - 95%

### ✅ Что завершено

| ID | Задача | Статус |
|----|--------|--------|
| 1.4.1 | ApiClient (HTTP) | ✅ 100% |
| 1.4.2 | API сервисы и DTO | ✅ 100% |
| 1.4.3 | OnvifClient (Discovery, Device, Media, PTZ, Digest Auth) | 🟢 92% |
| 1.4.4 | WebSocketClient | 🟢 92% |
| 1.4.6 | ONVIF Event service (PullPoint, маппинг) | 🟢 92% |
| 1.4.7 | ONVIF Digest Authentication | 🟢 95% |

### ⚠️ Что осталось (5%)

**Остаток: RTSP runtime stability и field validation**

| Задача | ID | Описание | Приоритет | Оценка |
|--------|-----|----------|-----------|--------|
| **RTSP runtime stability** | 1.4.5 | Long-run тесты, reconnect логика | **КРИТИЧНЫЙ** | 3-5 дней + 24-48ч тесты |
| **Field validation ONVIF** | 1.4.3 | Тестирование с реальными камерами | Высокий | 2-3 дня |
| **Audio decoding** | 1.4.5 | Аудио декодирование FFmpeg | Средний | 2-3 дня |

### Детализация остатка

#### 1.4.5 - RtspClient runtime stability

**Текущий статус:**
- ✅ Нативная C++ реализация (rtsp_client.cpp) - 85%
- ✅ Структура Kotlin обертки (RtspClient.kt)
- ✅ cinterop + Kotlin/Native компилируется
- ✅ Базовые тесты созданы
- ⚠️ Длительные интеграционные тесты (не проведены)
- ⚠️ Обработка ошибок и reconnect (требует validation)
- ❌ Аудио декодирование (отключено, FFmpeg 8.0 API)

**Что реализовано:**
```kotlin
// RtspClient.kt
class RtspClient(
    url: String,
    onFrame: (RtspFrame) -> Unit,
    onError: (RealtimeErrorType, Throwable) -> Unit
) {
    suspend fun start()
    suspend fun stop()
    suspend fun disconnect()
    fun close()
}
```

**Runtime diagnostics:**
```kotlin
data class RtspRuntimeDiagnostics(
    val isConnected: Boolean,
    val framesPerSecond: Double,
    val lastFrameTimestamp: Long,
    val reconnectCount: Int,
    val errors: List<ErrorLog>
)
```

**Остаток:**
- **Long-run тесты:** 24-48 часов стабильной работы
- **Reconnect логика:** Автоматическое восстановление при обрывах
- **Error handling:** Корректная обработка всех классов ошибок
- **Audio decoding:** Известное ограничение (можно документировать as-known)

**Созданные артефакты:**
- ✅ `RtspLongRunStabilityTest.kt` - Тестовый runner
- ✅ `run-rtsp-test-with-checks.ps1` - Скрипт запуска
- ✅ Документация по тестированию

#### 1.4.3 - Field validation ONVIF

**Текущий статус:**
- Проверено на 6 камерах: ONVIF 200/6
- Media+Events совместимость: 4/6 камер
- WS-Discovery улучшен
- UPnP fallback добавлен

**Остаток:**
- Финальная ручная валидация с реальными камерами
- Тестирование в различных сетевых конфигурациях
- Оптимизация для больших сетей (100+ устройств)

**Созданные скрипты:**
- ✅ `onvif-events-api-verification.ps1` - Автоматизированная проверка
- ✅ `MVP_ONVIF_EVENTS_VERIFICATION.md` - Документация

#### Аудио декодирование

**Статус:** Известное ограничение

**Причина:** FFmpeg 8.0 API изменения

**Влияние на MVP:** Минимальное (видео без звука приемлемо для MVP)

**Рекомендация:** Документировать как known limitation

### Автоматизация сетевого слоя (завершено)

**A1-A8 автоматизация:** ✅ Закрыто

| ID | Задача | Статус |
|----|--------|--------|
| A1 | WebSocket live integration тест | ✅ |
| A2 | PowerShell orchestration скрипт | ✅ |
| A3 | CI job для live-checks | ✅ |
| A4 | Machine-readable сводка | ✅ |
| A5 | WebSocket сценарий reconnect | ✅ |
| A6 | RTSP soak автоматизация | ✅ |
| A7 | Авто-консолидация статусов | ✅ |
| A8 | CI aggregate gate | ✅ |

**Скрипты:**
- ✅ `run-network-layer-automation.ps1`
- ✅ `sync-network-layer-1-4-status.ps1`
- ✅ `check-network-layer-ci-prerequisites.ps1`
- ✅ `run-network-layer-1-4-local-smoke.ps1`

### Вывод по Сетевому слою

**Реальный остаток:** ~5-8 дней + тестирование  
**Блокирует ли MVP:** **ДА** - RTSP stability критичен для MVP  
**Рекомендация:** 
- Приоритет 1: RTSP long-run тесты (критично)
- Приоритет 2: Field validation ONVIF
- Аудио декодирование - known limitation (не блокирует MVP)

---

## 📊 Итоговая сводка по всем трём слоям

### Приоритизация остатка

| Приоритет | Слой | Задача | Оценка | Блокирует MVP |
|-----------|------|--------|--------|---------------|
| **P0** | 1.4 | RTSP runtime stability | 3-5 дней + тесты | **ДА** |
| **P1** | 1.4 | Field validation ONVIF | 2-3 дня | Нет |
| **P1** | 1.2 | AnalyticsService сквозная интеграция (2.1.7) | 3-4 дня | Частично |
| **P2** | 1.2 | Полная нативная реализация AnalyticsService | 5-7 дней | Нет |
| **P3** | 1.3 | Production smoke тесты | 1-2 дня | Нет |
| **P3** | 1.4 | Audio decoding (optional) | 2-3 дня | Нет |

### План выполнения (по приоритетам)

#### Неделя 1: Критичное

**День 1-3:** RTSP long-run тесты
- Запустить: `.\scripts\run-rtsp-test-with-checks.ps1 -DurationMinutes 180`
- Анализ результатов
- Исправление проблем

**День 4-5:** Field validation ONVIF
- Прогон: `onvif-events-api-verification.ps1`
- Ручное тестирование с камерами

#### Неделя 2: Высокий приоритет

**День 1-3:** AnalyticsService integration (2.1.7)
- Закрыть сценарий S-RTSP-1 из матрицы приёмки
- Production monitoring

**День 4-5:** Production smoke тесты (1.3)
- Миграция с реальными данными
- Performance тесты

### Критерии завершения каждого слоя

#### Data Layer (1.3) → 100%

- [ ] Production smoke тесты успешны
- [ ] Performance benchmark документирован
- [ ] Migration guide завершён

**Оценка:** 1-2 дня

#### Доменный слой (1.2) → 80% (для MVP)

- [ ] AnalyticsService базовая реализация для MVP платформ
- [ ] Сценарий 2.1.7 S-RTSP-1 закрыт
- [ ] Known limitations задокументированы

**Оценка:** 5-7 дней (можно отложить на Фаза 2)

#### Сетевой слой (1.4) → 100%

- [ ] RTSP long-run тесты 24+ часа успешны
- [ ] Field validation ONVIF завершена
- [ ] Known limitations (audio) задокументированы

**Оценка:** 5-8 дней + тестирование

---

## 🎯 Рекомендации

### Для завершения Фазы 1 (MVP)

**Критично (обязательно):**
1. ✅ RTSP runtime stability (P0) - **блокирует MVP**
2. ⚠️ Field validation ONVIF (P1) - рекомендуется

**Оптимально (если есть ресурс):**
3. AnalyticsService сквозная интеграция 2.1.7 (P1)
4. Production smoke тесты Data Layer (P3)

**Отложить на Фаза 2:**
5. Полная нативная реализация AnalyticsService для всех платформ
6. Audio decoding RTSP
7. Performance тесты Data Layer

### Итоговая оценка

**Остаток работ для MVP:** ~10-15 человеко-дней  
**Время:** 2-3 недели при фокусированной разработке  
**Критичные блокеры:** 1 (RTSP stability)

---

**Дата анализа:** 27 April 2026  
**Актуально для:** Фазы 1 (MVP)  
**Следующий шаг:** Запуск RTSP long-run тестов
