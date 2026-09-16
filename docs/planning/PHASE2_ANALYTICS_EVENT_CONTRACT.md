# Контракт событий аналитики (Фаза 2, § 2.1)

**Статус:** рабочий документ, согласован с серверным `VideoAnalyticsService` и WebSocket-каналом `ANALYTICS`.

## Доменное событие (сервер → WebSocket)

Поле `type` в широковещании соответствует результату детекции:

| `type` (пример) | Условие |
|------------------|---------|
| `motion_detected` | `motionDetection != null` |
| `object_detected` | `objectDetection != null` |
| `face_detected` | `faceDetection != null` |
| `license_plate_recognized` | `licensePlateRecognition != null` |
| `analytics_result` | иной состав |

Обязательные поля в JSON-данных:

- `cameraId` — string  
- `timestamp` — long, эпоха UTC в миллисекундах  
- `resultType` — string, дублирует семантику `type` (см. реализацию)  
- `detectors` — массив строк активных в этом сообщении детекторов (`motion`, `object`, `face`, `license_plate`)  
- `summary` — объект с агрегатами: `motionDetected`, `objectsCount`, `facesCount`, `platesCount`  

Опциональные вложенные объекты: `motionDetection`, `objectDetection`, `faceDetection`, `licensePlateRecognition` — только если сработал соответствующий детектор.

**Correlation:** для трассировки в будущем зарезервировано поле `correlationId` (пока не эмитится; добавление — без breaking change).

## События в БД (Event)

Создание записей `Event` выполняется `AnalyticsEventGenerator` и use case’ами с флагом `createEvent` по политике камеры; схема idempotency — через троттлинг/кулдауны в настройках (`motionEventCooldownMs` и т.д.).

## HTTP GET `/api/v1/cameras/{id}/analytics/metrics` (наблюдаемость)

Расширяемо additive-полями (defaults `null` для старых клиентов):

- `frameSource` — имя `AnalyticsFrameSourceKind` в `server/api/.../analytics/AnalyticsFrameInput.kt`  
- `lastError` / `lastErrorAt` — последняя ошибка пайплайна за время активной сессии аналитики  

---

**См. также:** [PHASE2_1_7_ACCEPTANCE_MATRIX.md](PHASE2_1_7_ACCEPTANCE_MATRIX.md), [docs/status/PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) § 2.1.
