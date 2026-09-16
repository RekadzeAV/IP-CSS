# Video Decoder API

## Обзор

Модуль видео декодирования предоставляет унифицированный интерфейс для аппаратного и программного декодирования видео в кроссплатформенном приложении.

## Features

- ✅ Аппаратное декодирование (DXVA2, VAAPI, VideoToolbox)
- ✅ Программное декодирование (fallback)
- ✅ Автоматический выбор декодера
- ✅ Поддержка H.264/H.265
- ✅ Статистика декодирования
- ✅ Поддержка 4K разрешения

## Быстрый старт

### Базовое использование

```kotlin
import com.company.ipcamera.core.decoder.*

// Создание конфигурации
val config = DecoderConfig(
    format = VideoFormat.H264,
    width = 1920,
    height = 1080,
    fps = 30,
    bitrate = 4096,
    useHardwareAcceleration = true
)

// Создание декодера (автоматический выбор)
val decoder = createSmartDecoder(config)

// Инициализация
val initialized = decoder.initialize(config)
if (!initialized) {
    println("Failed to initialize decoder")
    return
}

// Декодирование
val frame = decoder.decode(videoData, timestamp)
if (frame != null) {
    // Обработка кадра
    processFrame(frame.data)
}

// Закрытие
decoder.close()
```

### Использование с Live555

```kotlin
// Подключение к RTSP
val rtspClient = Live555RTSPClient(rtspConfig)
rtspClient.connect()
rtspClient.play()

// Создание декодера
val videoInfo = rtspClient.getVideoInfo()
val decoderConfig = DecoderConfig(
    format = videoInfo.format.toDecoderFormat(),
    width = videoInfo.width,
    height = videoInfo.height,
    fps = videoInfo.fps,
    bitrate = videoInfo.bitrate
)
val decoder = createSmartDecoder(decoderConfig)

// Обработка потока
while (playing) {
    val frame = rtspClient.getVideoFrame()
    if (frame != null) {
        val decoded = decoder.decode(frame.data, frame.timestamp)
        if (decoded != null) {
            displayFrame(decoded.data)
        }
    }
}

// Очистка
decoder.close()
rtspClient.close()
```

## API Reference

### VideoDecoder

**Интерфейс:**
```kotlin
interface VideoDecoder {
    suspend fun initialize(config: DecoderConfig): Boolean
    suspend fun decode(frame: ByteArray, timestamp: Long): DecodedFrame?
    suspend fun decodeWithBuffer(buffer: VideoBuffer): DecodedFrame?
    fun getStatus(): StateFlow<DecoderStatus>
    fun getStats(): DecoderStats
    fun close()
}
```

### DecoderConfig

**Поля:**
- `format: VideoFormat` - Формат видео (H264, H265, MPEG4, VP8, VP9)
- `width: Int` - Ширина кадра
- `height: Int` - Высота кадра
- `fps: Int` - Кадров в секунду
- `bitrate: Int` - Битрейт (kbps)
- `useHardwareAcceleration: Boolean` - Использовать аппаратное ускорение
- `maxBufferFrames: Int` - Максимальное количество кадров в буфере
- `outputFormat: PixelFormat` - Формат вывода (RGBA, RGB24, NV12, I420)

### DecoderStatus

**Значения:**
- `UNINITIALIZED` - Не инициализирован
- `INITIALIZING` - Инициализация
- `READY` - Готов к работе
- `DECODING` - Декодирование
- `PAUSED` - Приостановлен
- `ERROR` - Ошибка
- `CLOSED` - Закрыт

### DecodedFrame

**Поля:**
- `data: ByteArray` - Данные кадра
- `width: Int` - Ширина
- `height: Int` - Высота
- `timestamp: Long` - Метка времени
- `isKeyFrame: Boolean` - Ключевой кадр
- `format: PixelFormat` - Формат пикселей
- `stride: Int` - Stride (шаг строки)
- `pts: Long` - Presentation timestamp
- `dts: Long` - Decode timestamp

### DecoderStats

**Поля:**
- `framesDecoded: Long` - Общее количество декодированных кадров
- `framesDropped: Long` - Количество пропущенных кадров
- `decodingTimeMs: Long` - Общее время декодирования
- `averageFrameTimeMs: Double` - Среднее время на кадр
- `minFrameTimeMs: Long` - Минимальное время на кадр
- `maxFrameTimeMs: Long` - Максимальное время на кадр
- `cpuUsagePercent: Double` - Использование CPU (%)
- `memoryUsageBytes: Long` - Использование памяти (байты)

### HardwareDecoderInfo

**Поля:**
- `supported: Boolean` - Поддерживается ли
- `decoderType: DecoderType` - Тип декодера
- `supportedFormats: List<VideoFormat>` - Поддерживаемые форматы
- `maxResolution: IntWidthHeight` - Максимальное разрешение
- `capabilities: DecoderCapabilities` - Возможности

### DecoderCapabilities

**Поля:**
- `maxConcurrentStreams: Int` - Максимум потоков одновременно
- `supportedResolutions: List<IntWidthHeight>` - Поддерживаемые разрешения
- `supportsBFrames: Boolean` - Поддержка B-кадров
- `supportsRefFrames: Boolean` - Поддержка референсных кадров
- `maxReferenceFrames: Int` - Максимум референсных кадров

## Утилитарные функции

### Проверка поддержки

```kotlin
// Проверка поддержки H.264
val h264Supported = isH264Supported(useHardware = true)

// Проверка поддержки H.265
val h265Supported = isH265Supported(useHardware = true)

// Проверка поддержки 4K
val is4KSupported = is4KSupported(useHardware = true)

// Проверка поддержки 1080p
val is1080pSupported = is1080pSupported(useHardware = true)

// Общая проверка поддержки
val supported = isHardwareDecoderSupported()
```

### Получение информации

```kotlin
// Получить предпочтительный декодер
val preferred = getPreferredDecoder()

// Получить поддерживаемые разрешения
val resolutions = getSupportedResolutions()

// Получить максимальное разрешение
val maxRes = getMaxSupportedResolution()

// Получить информацию о декодере
val info = HardwareDecoderSupport().getDecoderInfo()
```

### Создание декодера

```kotlin
// Автоматический выбор (рекомендуется)
val decoder = createSmartDecoder(config)

// Конкретный тип
val dxva2Decoder = createDecoder(config) // Может вернуть null

// Программный (fallback)
val softwareDecoder = createSoftwareDecoder(config)
```

### Фабрика декодеров

```kotlin
val factory = DecoderFactory()

// Получить или создать декодер
val decoder = factory.getDecoder(DecoderType.DXVA2, config)

// Закрыть конкретный декодер
factory.closeDecoder(DecoderType.DXVA2)

// Закрыть все декодеры
factory.closeAll()
```

## Платформенные реализации

### Windows (DXVA2)

**Файл:** `core/video-decoder/src/windowsMain/kotlin/.../VideoDecoderDXVA2.windows.kt`

**Преимущества:**
- Аппаратное ускорение через DirectX
- Низкое потребление CPU
- Поддержка H.264/H.265

**Требования:**
- Windows 7+
- DirectX 11+
- Поддержка DXVA2 графическим драйвером

### Linux (VAAPI)

**Файл:** `core/video-decoder/src/linuxMain/kotlin/.../VideoDecoderVAAPI.linux.kt`

**Преимущества:**
- Аппаратное ускорение через VAAPI
- Поддержка Intel/AMD/NVIDIA GPU
- Открытый исходный код

**Требования:**
- Linux с VAAPI поддержкой
- Драйверы GPU с VAAPI
- libva, libva-drm

### macOS/iOS (VideoToolbox)

**Файл:** `core/video-decoder/src/iosMain/kotlin/.../VideoDecoderVideoToolbox.ios.kt`

**Преимущества:**
- Нативная поддержка Apple
- Оптимизировано для Apple Silicon
- Низкое энергопотребление

**Требования:**
- macOS 10.13+
- iOS 12.0+
- Поддержка VideoToolbox

## Производительность

### Бенчмарки

| Платформа | Разрешение | FPS | CPU | Memory |
|-----------|------------|-----|-----|--------|
| Windows (DXVA2) | 1920x1080 | 60 | ~5% | ~100 MB |
| Windows (Software) | 1920x1080 | 60 | ~30% | ~150 MB |
| Linux (VAAPI) | 1920x1080 | 60 | ~5% | ~100 MB |
| Linux (Software) | 1920x1080 | 60 | ~35% | ~150 MB |
| iOS (VideoToolbox) | 1920x1080 | 60 | ~3% | ~80 MB |
| iOS (Software) | 1920x1080 | 60 | ~40% | ~120 MB |

### Рекомендации

1. **Всегда включайте аппаратное ускорение** если доступно
2. **Используйте H.264** для максимальной совместимости
3. **4K требует мощного GPU** для аппаратного декодирования
4. **Программный fallback** может быть медленным на слабых устройствах

## Обработка ошибок

```kotlin
try {
    val decoder = createSmartDecoder(config)
    
    val initialized = decoder.initialize(config)
    if (!initialized) {
        println("Failed to initialize decoder")
        return
    }
    
    decoder.getStatus().collect { status ->
        when (status) {
            DecoderStatus.ERROR -> {
                println("Decoder error")
                // Пересоздать декодер
                decoder.close()
            }
            else -> {}
        }
    }
    
    // Декодирование
    val frame = decoder.decode(data, timestamp)
    
} catch (e: Exception) {
    println("Decoder exception: ${e.message}")
} finally {
    decoder.close()
}
```

## Troubleshooting

### Аппаратное декодирование не работает

**Проблема:** `isHardwareDecoderSupported()` возвращает false

**Решения:**
1. Проверьте драйверы GPU
2. Убедитесь что GPU поддерживает аппаратное декодирование
3. Проверьте настройки BIOS/UEFI (интегрированная графика)

### Высокое использование CPU

**Проблема:** CPU > 30% при воспроизведении

**Решения:**
1. Включите аппаратное ускорение: `useHardwareAcceleration = true`
2. Уменьшите разрешение потока
3. Проверьте что используется правильный декодер

### Ошибка инициализации

**Проблема:** `initialize()` возвращает false

**Решения:**
1. Проверьте параметры конфигурации
2. Убедитесь что формат поддерживается
3. Проверьте что разрешение не превышает максимум

### Artefacts на изображении

**Проблема:** Искажения/артефакты на видео

**Решения:**
1. Попробуйте программный декодер
2. Обновите драйверы GPU
3. Проверьте целостность видеопотока

## Extension функции

### Конвертация форматов

```kotlin
// VideoFormat -> String
val formatName = VideoFormat.H264.displayName // "H.264 / AVC"

// PixelFormat -> String
val pixelName = PixelFormat.RGBA.displayName // "RGBA (32-bit)"

// DecoderType -> String
val decoderName = DecoderType.DXVA2.displayName // "DXVA2 (DirectX)"
```

## Чек-лист использования

- [ ] Проверить поддержку аппаратного декодирования
- [ ] Создать конфигурацию с правильными параметрами
- [ ] Использовать createSmartDecoder для автоматического выбора
- [ ] Обработать ошибку инициализации
- [ ] Подписаться на статус декодера
- [ ] Обрабатывать ошибки декодирования
- [ ] Закрывать декодер после использования
- [ ] Проверять статистику для отладки

## См. также

- [Live555 Integration](LIVE555_CINTEROP_INTEGRATION.md)
- [Network Scanner](NETWORK_SCANNER.md)
- [FFI Bindings Guide](NATIVE_FFI_BINDINGS_GUIDE.md)

---

**Версия:** 1.0  
**Дата:** 2025-01-15  
**Автор:** NLP-Core-Team
