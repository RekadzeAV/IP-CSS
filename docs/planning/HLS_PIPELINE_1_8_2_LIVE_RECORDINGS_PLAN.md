# План реализации 1.8.2 — HLS Pipeline (live + recordings)

**Версия проекта:** Alfa-0.1.1  
**Связь:** [CANONICAL_BREAKDOWN_1_8_VIDEO_AND_RECORDING.md](../status/CANONICAL_BREAKDOWN_1_8_VIDEO_AND_RECORDING.md) § 1.8.2 · [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) § 1.8 · [VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md)

## Цель

Стабильная генерация **live** HLS из RTSP и **VOD** HLS из файлов записи с предсказуемым поведением FFmpeg на сетевых сбоях и «рваных» PTS.

## Выполнено в коде (итерация)

| Шаг | Содержание | Файлы |
|-----|------------|--------|
| A | RTSP: `-stimeout` (мкс), по умолчанию 5s; `FFMPEG_RTSP_STIMEOUT_MICROS=0` — отключить | [FfmpegCli.kt](../../server/api/src/main/kotlin/com/company/ipcamera/server/service/FfmpegCli.kt) `rtspFfmpegInputArgs` |
| B | Recording→HLS: `-fflags +genpts` перед `-i` по умолчанию; `FFMPEG_RECORDING_HLS_GENPTS=0/false` — отключить | `ffmpegRecordingToHlsInputFlags()`, [HlsGeneratorService.kt](../../server/api/src/main/kotlin/com/company/ipcamera/server/service/HlsGeneratorService.kt) `startHlsFromRecording` |
| C | Адаптивный HLS: учёт процессов в `activeAdaptiveStreams` до старта + очистка в `monitorProcess` | `HlsGeneratorService` (см. историю коммитов) |
| D | Пример env | [config/ffmpeg-hls-pipeline.example.env](../../config/ffmpeg-hls-pipeline.example.env) |

## Следующие шаги (бэклог 1.8.2)

1. **Интеграционные тесты маршрутов** HLS — закрыто для среза HTTP: [HlsStreamRoutesIntegrationTest.kt](../../server/api/src/test/kotlin/com/company/ipcamera/server/integration/HlsStreamRoutesIntegrationTest.kt), [HlsPublicRoutesIntegrationTest.kt](../../server/api/src/test/kotlin/com/company/ipcamera/server/integration/HlsPublicRoutesIntegrationTest.kt), [HlsRecordingRoutesIntegrationTest.kt](../../server/api/src/test/kotlin/com/company/ipcamera/server/integration/HlsRecordingRoutesIntegrationTest.kt); исправлен разбор сегмента записи в [RecordingRoutes.kt](../../server/api/src/main/kotlin/com/company/ipcamera/server/routing/RecordingRoutes.kt). Остаётся: RBAC по ролям на HLS (если появится в продукте), полевой gate.
2. **Рестарт FFmpeg** при падении процесса live (политика: N попыток, backoff) — отдельная задача.
3. **Метрики/логи**: длительность сегмента, exit code, stderr tail в структурированный лог.
4. **Полевая проверка** TTFF и recovery по [VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md).

## Критерии готовности (DoD среза)

- [x] Единая точка аргументов RTSP для FFmpeg + документированные env.
- [x] Запись→HLS: опция стабилизации PTS через env.
- [x] Интеграционные тесты HTTP HLS (роуты защищённого `streamRoutes`: playlist / variant / segment / 401 / 404 / 503).
- [x] Публичные `hlsRoutes()` и JWT recordings HLS (integration); сегмент записи `/{segmentFile}` + regex (Ktor 2.3).
- [ ] Полевой прогон по профилю камер / gate `video-e2e-go-no-go.ps1`.
