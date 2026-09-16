# HLS Low-Latency Optimization Guide

**Дата:** 2026-01-28  
**Статус:** ✅ Завершено  
**Версия:** 1.0

---

## 📋 Обзор

Этот документ описывает оптимизации HLS для снижения задержки с 10-15 секунд до <3 секунд.

### Что было добавлено:

1. **Low-Latency HLS (LL-HLS)** режим
2. **Настраиваемые параметры сегментов**
3. **Fragmented MP4 (fMP4)** поддержка
4. **WebRTC fallback** для ultra-low latency
5. **Адаптивный bitrate с оптимизацией**

---

## 🚀 Быстрый старт

### 1. Включение low-latency режима

```bash
# .env.production
HLS_LOW_LATENCY_MODE=true
HLS_SEGMENT_DURATION=1.0
HLS_PLAYLIST_SIZE=3
HLS_HEVC_FOR_2K4K=true
```

### 2. Параметры производительности

| Переменная | Значение по умолчанию | Рекомендуемое (low-latency) | Описание |
|------------|----------------------|----------------------------|----------|
| `HLS_LOW_LATENCY_MODE` | false | true | Включить LL-HLS режим |
| `HLS_SEGMENT_DURATION` | 2.0 | 1.0 | Длительность сегмента (сек) |
| `HLS_PLAYLIST_SIZE` | 6 | 3 | Количество сегментов в плейлисте |
| `HLS_HEVC_FOR_2K4K` | false | true | HEVC для QHD/4K |

---

## 📊 Сравнение задержек

### Standard HLS (по умолчанию)

```
Задержка: 10-15 секунд
Сегменты: 2 сек × 6 = 12 сек буфер
Сегмент type: MPEG-TS
```

### Low-Latency HLS (оптимизировано)

```
Задержка: 2-4 секунды
Сегменты: 1 сек × 3 = 3 сек буфер
Сегмент type: Fragmented MP4 (fMP4)
Дополнительно: Independent segments, partial segments
```

### WebRTC (ultra-low latency)

```
Задержка: <500 мс
Протокол: WebRTC (UDP)
Fallback: HLS при недоступности
```

---

## 🔧 Конфигурация FFmpeg

### Standard HLS команда

```bash
ffmpeg -i rtsp://camera/stream \
  -c:v libx264 -c:a aac \
  -b:v 1500k -s 1280x720 -r 25 \
  -hls_time 2 \
  -hls_list_size 6 \
  -hls_flags delete_segments+append_list \
  -hls_segment_filename "segment_%03d.ts" \
  -hls_allow_cache 0 \
  -f hls playlist.m3u8
```

### Low-Latency HLS команда

```bash
ffmpeg -i rtsp://camera/stream \
  -c:v libx264 -c:a aac \
  -b:v 1500k -s 1280x720 -r 25 \
  -hls_time 1 \
  -hls_list_size 3 \
  -hls_flags delete_segments+independent_segments+append_list \
  -hls_segment_filename "segment_%03d.mp4" \
  -hls_allow_cache 0 \
  -hls_segment_type fmp4 \
  -sc_threshold 0 \
  -g 60 \
  -keyint_min 60 \
  -f hls playlist.m3u8
```

---

## 🎯 Оптимизации

### 1. Уменьшение длительности сегмента

**Было:** 2 секунды  
**Стало:** 1 секунда (или 0.5 сек для ultra-low)

```kotlin
HLS_SEGMENT_DURATION=1.0
```

**Эффект:** Снижение задержки на 50%

### 2. Уменьшение размера плейлиста

**Было:** 6 сегментов  
**Стало:** 3 сегмента

```kotlin
HLS_PLAYLIST_SIZE=3
```

**Эффект:** Снижение буфера с 12 сек до 3 сек

### 3. Fragmented MP4 (fMP4)

**Было:** MPEG-TS  
**Стало:** Fragmented MP4

```kotlin
-hls_segment_type fmp4
```

**Преимущества:**
- Быстрее переключение между качествами
- Лучше совместимость с современными браузерами
- Поддержка partial segments (LL-HLS)

### 4. Independent Segments

```kotlin
-hls_flags independent_segments
```

**Преимущества:**
- Каждый сегмент начинается с ключевого кадра
- Быстрее переключение битрейта
- Лучше восстановление после ошибок

### 5. Keyframe Interval

```kotlin
-g 60          # Keyframe каждые 60 кадров (2 сек при 30fps)
-keyint_min 60 # Минимальный интервал
-sc_threshold 0 # Отключить scene change detection
```

---

## 📈 Мониторинг производительности

### Метрики для отслеживания

| Метрика | Target | Alert |
|---------|--------|-------|
| Задержка (end-to-end) | <3 сек | >5 сек |
| Время генерации сегмента | <1 сек | >2 сек |
| Ошибки генерации | 0% | >1% |
| Переключения битрейта | <3/мин | >10/мин |

### Логирование

```kotlin
logger.info { 
    "HLS latency: segment=${hlsSegmentDuration}s, " +
    "playlist=$hlsPlaylistSize segments, " +
    "mode=${if (lowLatencyMode) "LL-HLS" else "standard"}"
}
```

---

## 🌐 WebRTC Fallback

Для ultra-low latency (<500ms) используйте WebRTC:

### Конфигурация

```bash
# .env
WEBRTC_ENABLED=true
JANUS_URL=http://localhost:8088/janus
WEBRTC_PREFERRED=true
```

### Логика переключения

```kotlin
fun getStreamUrl(cameraId: String, priority: LatencyPriority): String {
    return when (priority) {
        LatencyPriority.ULTRA_LOW -> webrtcService.getWebRtcUrl(cameraId)
        LatencyPriority.LOW -> hlsGeneratorService.getMasterPlaylistUrl(cameraId)
        LatencyPriority.STANDARD -> hlsGeneratorService.getPlaylistUrl(cameraId, StreamQuality.HIGH)
    }
}
```

---

## 🔨 Troubleshooting

### Проблема: Высокая задержка (>5 сек)

**Решение:**
1. Уменьшить `HLS_SEGMENT_DURATION` до 0.5-1.0 сек
2. Уменьшить `HLS_PLAYLIST_SIZE` до 2-3
3. Включить `HLS_LOW_LATENCY_MODE=true`
4. Проверить сеть (ping, packet loss)

### Проблема: Частые буферизации

**Решение:**
1. Увеличить `HLS_PLAYLIST_SIZE` до 4-5
2. Увеличить `HLS_SEGMENT_DURATION` до 1.5-2.0 сек
3. Проверить пропускную способность сети
4. Снизить битрейт для мобильных клиентов

### Проблема: Артефакты видео

**Решение:**
1. Увеличить `-bufsize` в FFmpeg
2. Проверить ключевые кадры (`-g 60`)
3. Использовать `-preset fast` вместо `ultrafast`

---

## 📚 Связанные документы

- [HlsGeneratorService.kt](server/api/src/main/kotlin/com/company/ipcamera/server/service/HlsGeneratorService.kt)
- [WebRtcService.kt](server/api/src/main/kotlin/com/company/ipcamera/server/service/WebRtcService.kt)
- [POSTGRESQL_PRODUCTION_OPTIMIZATION.md](docs/POSTGRESQL_PRODUCTION_OPTIMIZATION.md)

---

## 🎯 Следующие шаги

1. ✅ Low-Latency HLS — завершено
2. ⏳ WebRTC интеграция — в процессе (Фаза 2)
3. ⏳ CMAF (Common Media Application Format) — запланировано
4. ⏳ QUIC/HTTP3 поддержка — запланировано

---

**Подготовлено:** NLP-Core-Team  
**Дата:** 2026-01-28
