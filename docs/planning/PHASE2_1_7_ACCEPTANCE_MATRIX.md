# Матрица приёмки 2.1.7 — AI ↔ видеопоток (RTSP/HLS → события)

**Назначение:** фиксировать сквозные сценарии без смены API; каждый сценарий = отдельный прогон/отчёт.

## Обозначения

| Статус в прогоне | Значение |
|------------------|----------|
| PASS | критерий выполнен |
| FAIL | дефект / отложено |
| N/A | не применимо в текущем окружении |

## Сценарии

### S-RTSP-1: Сервер, RTSP-декодер доступен, аналитика включена

- Предусловия: камера в БД, `startStream` / HLS+RTSP, нативный `RtspClient` в состоянии PLAYING, в настройках аналитики включён минимум один детектор.  
- Ожидание: `GET .../analytics/status` — `isRunning: true` при активной сессии; `GET .../analytics/metrics` — `frameSource: RTSP_DECODED`, счётчики `processedFrames` растут; в WebSocket `stream_started` — `hasRtspFrameSource: true`, `analyticsPipelineActive: true`.  
- DoD: события детекции при срабатывании (или корректное отсутствие при пустом кадре), без дублирования сверх кулдауна.

### S-RTSP-2: Ошибка потока / обрыв

- Ожидание: в логе зафиксирована причина; при возможности `lastError` / `lastErrorAt` в metrics на время сессии; корректная остановка без утечек задач.

### S-HLS-1: HLS-only (RTSP-клиент недоступен на JVM)

- Предусловия: как в [VideoStreamService](../../server/api/src/main/kotlin/com/company/ipcamera/server/service/VideoStreamService.kt) — FFmpeg HLS работает, `RtspClient` не поднялся.  
- Текущее ожидаемое поведение: аналитика **не** стартует; WebSocket: `analyticsPipelineActive: false`, `hasRtspFrameSource: false`.  
- **Будущий DoD (бэклог):** `AnalyticsFrameSourceKind.HLS_DERIVED` и ненулевой поток кадров из FFmpeg → те же критерии, что S-RTSP-1.

### S-API-1: Смена настроек аналитики без остановки стрима

- `POST .../analytics` (merge) не ломает существующие поля; `updateAnalyticsConfig` подхватывается активной сессией.

## Связь с env

- `VIDEO_STREAM_FRAME_BUFFER_CAPACITY` (1..500) — размер буфера `SharedFlow` кадров; по умолчанию 50.

---

**Источник статуса по фазе:** [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) § 2.1.7.
