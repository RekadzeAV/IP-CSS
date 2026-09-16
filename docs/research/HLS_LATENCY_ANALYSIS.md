# HLS Pipeline — Анализ задержек

**Версия:** 1.0  
**Дата:** 27 May 2026  
**Задача:** MVP Фаза 1 (1.1.4)

---

## Текущая архитектура HLS

```
RTSP Camera → FFmpeg CLI (ProcessBuilder) → TS segments → HLS playlist → Web/Desktop плеер
```

**Ключевые компоненты:**
- `HlsGeneratorService.kt` — управление процессами FFmpeg для HLS
- `FfmpegService.kt` — обёртка над FFmpeg/FFprobe
- `HlsCleanupScheduler.kt` — очистка старых сегментов
- `routing/HlsRoutes.kt` — серверные эндпоинты для HLS

## Текущие параметры FFmpeg (live stream)

| Параметр | Значение | Влияние на задержку |
|----------|----------|-------------------|
| `-hls_time` | 2 сек | ⏱ Базовая задержка: 2-6 сек |
| `-hls_list_size` | 6 | 📦 Буферизация: ~12 сек |
| `-hls_flags` | `delete_segments+append_list` | ✅ Нормально |
| `-preset` | `fast`/`medium` | ⚠️ Медленнее кодирование |
| `-tune` | не указан | ❌ Нет zerolatency |
| `-g` (GOP) | auto | ❌ GOP может быть >1s |
| `-hls_segment_type` | `mpegts` (по умолч.) | ❌ TS медленнее fMP4 |
| `-probesize` | не указан | ❌ Медленный анализ |
| `-analyzeduration` | не указан | ❌ Медленный анализ |

## Анализ точек задержки

### 1. Запуск FFmpeg процесса (ProcessBuilder) — 500-1500ms
Каждый раз spawn нового процесса FFmpeg.

**Оптимизация:** Переиспользовать FFmpeg процесс, либо использовать JavaCV/FFmpeg API напрямую.

### 2. Анализ RTSP потока (probesize) — 1-3 сек
FFmpeg анализирует поток перед началом кодирования.

**Оптимизация:** `-probesize 32 -analyzeduration 0 -fflags nobuffer`

### 3. Кодирование/транскодирование — 0.5-2 сек
Зависит от `-preset` и аппаратного ускорения.

**Оптимизация:** `-preset ultrafast -tune zerolatency`

### 4. HLS сегментация — hls_time 2 сек
Каждый сегмент длится 2 секунды. Для live плееру нужно минимум 2-3 сегмента.

**Оптимизация:** `-hls_time 1` (сегменты по 1 сек). С fMP4 можно быстрее.

### 5. Сборка плейлиста — hls_list_size 6
Плеер загружает 6 сегментов = ~12 секунд буфера.

**Оптимизация:** `-hls_list_size 3` (только 3 сегмента в плейлисте)

## Целевые параметры (Low-Latency HLS)

| Параметр | Оптимизированное значение | Эффект |
|----------|--------------------------|--------|
| `-hls_time` | 1 | Сегменты по 1 сек |
| `-hls_list_size` | 3 | Только 3 сегмента |
| `-hls_flags` | `delete_segments+append_list+independent_segments` | Независимые сегменты |
| `-preset` | `ultrafast` | Максимальная скорость |
| `-tune` | `zerolatency` | Нулевая задержка |
| `-g` | 1 | GOP = 1 (каждый кадр keyframe) |
| `-probesize` | 32 | Минимальный анализ |
| `-analyzeduration` | 0 | Без анализа |
| `-fflags` | `nobuffer` | Без буферизации |
| `-hls_segment_type` | `fmp4` | Фрагментированный MP4 |

## Ожидаемые результаты

| Метрика | Текущая | Целевая |
|---------|---------|---------|
| Задержка live | 5-10 сек | <2 сек |
| Время первого кадра | 6-15 сек | <3 сек |
| Размер сегмента | ~500KB | ~250KB |
| Нагрузка CPU | Высокая | Средняя |

## Desktop vs Server HLS

**Серверный HLS** (через FFmpeg CLI):
- Используется для веб-плеера и стриминга на клиенты
- Оптимизации FFmpeg параметров (выше)
- Требует установленного FFmpeg на сервере

**Desktop HLS** (через JavaCV FFmpegFrameGrabber):
- Прямой доступ к RTSP через `JvmRtspClient`
- Не требует FFmpeg CLI — использует JavaCV библиотеку
- Может иметь собственную low-latency реализацию
- Потенциально быстрее — нет ProcessBuilder overhead

---

## Рекомендуемый план действий

1. ✅ **FFmpeg параметры** — обновить `HlsGeneratorService.kt` для live low-latency
2. ✅ **fMP4** — добавить поддержку фрагментированного MP4
3. ⏳ **Desktop HLS** — реализовать HLS сегментер через JavaCV (опционально)
4. ⏳ **Long-run тесты** — проверить стабильность при low-latency настройках