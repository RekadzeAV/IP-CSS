# Canonical Breakdown: 1.8 Видео и запись

**Статус:** Active  
**Версия:** 1.0  
**Дата:** 2026-03-27  
**Владелец секции:** Video/Streaming track  
**Источник истины для отчетов:** этот документ

---

## Назначение

Этот документ фиксирует единую декомпозицию этапа `1.8`, KPI и Definition of Done (DoD), чтобы:

- исключить расхождения между `docs/status/PROJECT_STATUS_PHASES.md`, `docs/TODO.md` (и при необходимости архивом `docs/archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md`) и сводными отчетами;
- считать готовность `1.8` по одинаковой формуле;
- иметь проверяемые критерии завершения, а не только экспертную оценку.

---

## Единая декомпозиция 1.8

| ID | Подэтап | Вес в 1.8 | Текущий статус | Текущая оценка |
|----|---------|-----------|----------------|----------------|
| 1.8.1 | Recording Core (VideoRecordingService + API + Use Cases) | 20% | 🟡 | 82% |
| 1.8.2 | HLS Pipeline (live + recordings) | 15% | 🟡 | 88% |
| 1.8.3 | Screenshot Pipeline | 5% | 🟡 | 54% |
| 1.8.4 | RTSP Native Integration (без симуляционного пути по умолчанию) | 20% | ⚠️ | 58% |
| 1.8.5 | Web Video Player Stability | 12% | 🟡 | 75% |
| 1.8.6 | Desktop Video Player Stability | 10% | 🟡 | 62% |
| 1.8.7 | Android Video + Background Recording | 12% | ⚠️ | 35% |
| 1.8.8 | iOS Video Playback | 6% | ❌ | 0% |

**Итог 1.8 (взвешенно):** **~63%**

Формула:

`Progress_1_8 = Σ(Weight_i * Progress_i) / 100`

---

## KPI и Definition of Done по подпунктам

### 1.8.1 Recording Core

**KPI**
- Start/Stop/Pause/Resume API доступны и работают на реальном RTSP-источнике.
- Не менее 95% операций start/stop завершаются без ошибок в smoke-наборе (N>=100 операций).
- WebSocket события `recording_started/stopped/paused/resumed` приходят в клиент.

**DoD**
- Есть unit + integration тесты на сервис/роуты записи.
- Ошибки валидации и недоступность камеры обрабатываются предсказуемыми кодами.
- Для всех поддерживаемых форматов подтверждена запись и чтение метаданных.

---

### 1.8.2 HLS Pipeline

**KPI**
- Генерация `playlist.m3u8` и `.ts` сегментов для live и recordings.
- Время до первого кадра (TTFF) в web-плеере <= 5с в локальной сети.
- Доля нефатальных восстановлений при сетевых ошибках >= 90%.

**DoD**
- Маршруты HLS покрыты интеграционными тестами (playlist/segment/404/permissions). **JVM:** `HlsStreamRoutesIntegrationTest` (JWT stream), `HlsPublicRoutesIntegrationTest` (публичные `hlsRoutes`), `HlsRecordingRoutesIntegrationTest` (JWT recordings + моки). Исправлен разбор пути сегмента записи (`/{segmentFile}` + regex вместо `segment_{segmentNumber}` в Ktor 2.3).
- Поддержан минимум один стабильный профиль качества + adaptive master playlist.
- Логи и метрики FFmpeg-процессов доступны для диагностики.
- **Срез 2026-04:** настраиваемые таймауты RTSP (`FFMPEG_RTSP_STIMEOUT_MICROS`, `FFMPEG_RTSP_RW_TIMEOUT_MICROS`) и стабилизация записи→HLS (`FFMPEG_RECORDING_HLS_GENPTS`) — см. [HLS_PIPELINE_1_8_2_LIVE_RECORDINGS_PLAN.md](../planning/HLS_PIPELINE_1_8_2_LIVE_RECORDINGS_PLAN.md), [config/ffmpeg-hls-pipeline.example.env](../../config/ffmpeg-hls-pipeline.example.env).

---

### 1.8.3 Screenshot Pipeline

**KPI**
- Успешный снимок кадра (RTSP или frame-based) >= 95% в smoke-наборе.
- Среднее время получения снимка <= 3с.

**DoD**
- Реализован `captureFrame(...)` с реальным декодированием кадра.
- Реализован fallback через FFmpeg при недоступном frame decoder.
- Добавлены тесты на успешный путь и на ошибочные кейсы.
- **JVM (частично):** [ScreenshotServiceTest.kt](../../server/api/src/test/kotlin/com/company/ipcamera/server/service/ScreenshotServiceTest.kt) — пустой/non-video кадр, `getScreenshotUrl`, `cleanupOldScreenshots`; успешный `captureFrame` с FFmpeg — вручную / в gate с `FFMPEG_PATH`.

---

### 1.8.4 RTSP Native Integration

**KPI**
- В production-пути используются native callbacks и реальные кадры.
- Непрерывное воспроизведение >= 30 минут без деградации статуса и утечек.
- Ошибки подключения/декодирования корректно поднимаются в status/error flow.

**DoD**
- Симуляционный генератор кадров не используется как основной путь.
- Подтверждена работоспособность минимум на 2 типах камер.
- Есть интеграционные тесты native bridge + длительный smoke.

---

### 1.8.5 Web Video Player Stability

**KPI**
- Успешный старт live playback >= 95% при N>=100 запусков.
- Автовосстановление после network/media error с ограниченным retry.
- Воспроизведение recordings через HLS без фатальных ошибок.

**DoD**
- Покрыты E2E сценарии: start stream, reconnect, switch quality, error state.
- Обновлены UX-статусы: loading/playing/error/reconnecting.
- Документированы ограничения браузеров и fallback-поведение.

---

### 1.8.6 Desktop Video Player Stability

**KPI**
- Успешный старт и удержание live playback >= 95% в smoke-наборе.
- Корректная работа play/pause/stop/reconnect.
- Снимок экрана сохраняется без падения UI.

**DoD**
- Подтверждена декодировка для целевых кодеков (минимум H.264 + fallback).
- Есть тест-кейсы на reconnect и error handling.
- Нет критичных утечек/зависаний при 30+ минутном просмотре.

---

### 1.8.7 Android Video + Background Recording

**KPI**
- Live playback и playback recordings доступны из приложения.
- Background recording service стабильно стартует/останавливается.
- Service recovery после процесса/restart подтвержден.

**DoD**
- Интегрирован shared `VideoRecordingService` или эквивалентный production-путь.
- Закрыты TODO в `RecordingService`/monitoring-сценариях.
- Добавлены integration/UI тесты для ключевых сценариев.

---

### 1.8.8 iOS Video Playback

**KPI**
- Запуск live playback (HLS, при необходимости RTSP через proxy).
- Запуск playback recordings (HLS).
- Базовая обработка ошибок сети/stream.

**DoD**
- Создан iOS клиентский слой для видеоплеера.
- AVPlayer интегрирован в UI flow камер и записей.
- Есть smoke-тесты и базовое руководство по отладке.

---

## Единые правила обновления статуса

- Любое изменение процента в разделе `1.8` в любых отчетах должно сначала обновляться здесь.
- В отчетах допускается только ссылка на этот файл и краткий агрегированный процент.
- Статусы считать так:
  - ✅: >= 90%
  - 🟡: 50-89%
  - ⚠️: 20-49% или есть критический блокер
  - ❌: < 20%

---

## Критические блокеры (на сейчас)

1. `1.8.4` RTSP native integration не завершена как production-путь.
2. `1.8.3` frame-based screenshot декодирование не завершено.
3. `1.8.7` Android background recording не доведена до DoD.
4. Недостаточное интеграционное покрытие сервиса записи/HLS (смежный риск для `1.8.1` и `1.8.2`).
