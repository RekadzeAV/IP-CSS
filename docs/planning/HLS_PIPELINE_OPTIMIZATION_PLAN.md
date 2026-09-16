# 🎯 FEATURE: HLS pipeline оптимизация для Desktop
# 📋 Задача: MVP Фаза 1 (1.1.4)
# ⏱ Оценка: 7-9 часов

## Контекст

Текущий HLS pipeline имеет задержку ~5-10 секунд. Требуется оптимизация до <2 секунд для Desktop.

**Связанные файлы:**
- `core/network/src/commonMain/.../video/` — возможные HLS компоненты
- `core/network/src/desktopMain/.../rtsp/JvmRtspClient.kt` — JavaCV RTSP клиент
- `core/network/src/jvmMain/.../video/VideoDecoder.jvm.kt` — JavaCV декодер
- `platforms/client-desktop-x86_64/` — Desktop плеер

**Блокер для:** MVP Фазы 1 (1.1.4 — оптимизация HLS pipeline для низкой задержки)

## Декомпозиция

- [ ] 1ч Исследовать текущий HLS pipeline и точки задержки
  - Критерий: документ с анализом latency (5-10с) и определением bottleneck
  - Файлы: `docs/research/HLS_LATENCY_ANALYSIS.md`
  
- [ ] 2ч Реализовать low-latency HLS сегментер (fMP4)
  - Критерий: HLS сегменты генерируются с задержкой <1с
  - Файлы: `core/network/.../video/HlsSegmenter.kt`, `HlsSegmenter.desktop.kt`
  - Зависимости: от исследования

- [ ] 1ч Настроить FFmpeg параметры для низкой задержки
  - Критерий: `-g`, `-sc_threshold`, `-tune zerolatency`, `-preset ultrafast`
  - Файлы: `JvmRtspClient.kt` (обновить JvmRtspConfig)
  
- [ ] 2ч Интегрировать оптимизированный HLS в Desktop плеер
  - Критерий: Desktop видео имеет задержку <2с
  - Файлы: `platforms/client-desktop-x86_64/.../VideoPlayer.kt`
  - Зависимости: от сегментера и FFmpeg настроек

- [ ] 1ч Написать long-run тест стабильности (1 час)
  - Критерий: тест проходит без потери кадров
  - Файлы: `core/network/.../desktopTest/.../HlsLongRunTest.kt`

- [ ] 1ч Написать интеграционный тест HLS pipeline
  - Критерий: тест проверяет полный цикл RTSP→HLS→плеер
  - Файлы: `core/network/.../desktopTest/.../HlsPipelineIntegrationTest.kt`

## Оценка

| Подзадача | Часы | Риски |
|-----------|------|-------|
| Исследование | 1ч | Низкие |
| Low-latency сегментер | 2ч | Средние — сложность FFmpeg API |
| FFmpeg параметры | 1ч | Низкие |
| Интеграция в плеер | 2ч | Средние — Desktop плеер может требовать доработки |
| Long-run тест | 1ч | Низкие |
| Интеграционный тест | 1ч | Средние — требуется mock RTSP сервер |
| **Итого** | **7-9ч** | |

## Definition of Done

- [ ] Задержка Desktop видео <2 секунды
- [ ] HLS сегменты генерируются с задержкой <1с
- [ ] Desktop плеер отображает видео с HLS pipeline
- [ ] Все тесты (unit + integration + long-run) проходят
- [ ] Long-run тест 1 час без потери кадров
- [ ] Код задокументирован