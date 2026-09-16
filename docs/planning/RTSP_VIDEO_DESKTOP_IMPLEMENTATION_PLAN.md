# 🎯 План реализации RTSP/видео интеграции для Desktop (JVM)

**Версия:** 1.0  
**Дата:** 26 May 2026  
**Статус:** 🟡 ~85% → 100%

## Текущее состояние

### ✅ Уже реализовано
- `RtspClient.kt` — обёртка с StateFlow, reconnect, diagnostics, callbacks
- `NativeRtspClient.kt` — expect-класс для нативного RTSP (C/C++)
- `VideoDecoder.kt` — expect-класс декодера
- `VideoFrameConversion.desktop.kt` — JavaCV конвертация кадров
- `RtspBenchmarkConfig.kt`, `SimpleRtspBenchmarkRunner.kt` — бенчмарки
- `RtspClientConfig` с reconnect, backoff, jitter
- `RtspRuntimeDiagnostics` — полная диагностика

### ⚠️ Проблемы
1. **Нет actual реализации NativeRtspClient для desktop (JVM)** — cinterop только для K/Native
2. **NativeRtspClient.create() всегда возвращает 0** → на desktop всегда fallback
3. **Simulated fallback** генерирует пустые кадры (100 байт) — не пригодно для прода
4. **JavaCV/FFmpeg** уже есть в зависимостях, но не используется для RTSP
5. **HLS pipeline** требует оптимизации задержки

## План работ

### Этап 1: JavaCV-based RTSP клиент для Desktop (неделя 1)

Создать `JvmRtspClient` — actual реализацию RTSP через JavaCV FFmpeg.

**Файлы:**
- `core/network/src/desktopMain/.../rtsp/JvmRtspClient.kt` — новый
- `core/network/src/desktopMain/.../rtsp/JvmRtspFrameConverter.kt` — новый
- `core/network/src/commonMain/.../rtsp/NativeRtspClient.kt` — модифицировать (добавить desktop expected)

### Этап 2: Интеграция VideoDecoder с JavaCV (неделя 1)

**Файлы:**
- `core/network/src/desktopMain/.../video/VideoDecoder.desktop.kt` — actual реализация
- `core/network/src/desktopMain/.../video/JvmVideoFrameProcessor.kt` — новый

### Этап 3: Оптимизация HLS pipeline (неделя 2)

**Файлы:**
- `core/network/src/commonMain/.../video/HlsStreamManager.kt` — новый или доработать
- `core/network/src/desktopMain/.../video/HlsSegmenter.desktop.kt` — новый

### Этап 4: Интеграционные тесты (неделя 2)

- Тесты с mock RTSP сервером
- Long-run стабильность
- Покрытие реконнекта и diagnostics

---

## Детальная реализация Этапа 1

### JvmRtspClient

```kotlin
class JvmRtspClient : NativeRtspClient() {
    private val ffmpeg = FFmpegFrameGrabber(url)
    // Обёртка над FFmpegFrameGrabber с coroutines поддержкой
    // getFrameCallback, reconnect, diagnostics
}
```

### Зависимости (уже есть)
- `org.bytedeco:javacv:1.5.13`
- `org.bytedeco:ffmpeg-platform:8.0.1-1.5.13`

---

## Проверка готовности

- [ ] JavaCV RTSP подключение — открытие потока
- [ ] Получение H.264/H.265 кадров
- [ ] Конвертация в RtspFrame
- [ ] Callback для видеокадров
- [ ] Callback для аудиокадров
- [ ] Reconnect при разрыве
- [ ] Diagnostics
- [ ] Интеграция с RtspClient
- [ ] Desktop long-run тесты
- [ ] HLS оптимизация (low latency)