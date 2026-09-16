# Рекомендации по оптимизации VideoDecoder

## На основе типичных результатов профилирования

### 1. Оптимизации для высоких разрешений (1080p+)

#### Проблема: Высокое время декодирования (>50ms)
**Симптомы:**
- Average decode time > 50ms для 1080p
- CPU usage > 80%
- FPS < 20

**Решения:**
1. **Включить аппаратное ускорение** (если доступно)
   - NVIDIA: Проверить наличие h264_cuvid/hevc_cuvid
   - Intel: Проверить наличие h264_qsv/hevc_qsv
   - macOS: VideoToolbox должен работать автоматически

2. **Оптимизировать sws_scale**
   - Использовать более быстрый алгоритм: `SWS_FAST_BILINEAR` вместо `SWS_BILINEAR`
   - Кэшировать SwsContext вместо пересоздания

3. **Уменьшить разрешение вывода**
   - Если UI не требует полного разрешения, масштабировать после декодирования

#### Реализация:
```kotlin
// В VideoDecoderImpl.kt
swsContext = sws_getContext(
    actualWidth, actualHeight, avutil.AV_PIX_FMT_YUV420P,
    actualWidth, actualHeight, avutil.AV_PIX_FMT_RGB24,
    swscale.SWS_FAST_BILINEAR, // Быстрее чем SWS_BILINEAR
    null, null, null
)
```

### 2. Оптимизации для множественных потоков

#### Проблема: Высокое использование CPU при нескольких потоках
**Симптомы:**
- CPU usage > 90% при 4+ потоках
- FPS падает при добавлении потоков
- Memory usage растет линейно

**Решения:**
1. **Ограничить количество одновременных декодеров**
   - Использовать пул декодеров
   - Ограничить до 4-6 одновременных потоков

2. **Использовать аппаратное ускорение для всех потоков**
   - GPU может обрабатывать несколько потоков параллельно

3. **Оптимизировать память**
   - Переиспользовать буферы
   - Очищать неиспользуемые декодеры

#### Реализация:
```kotlin
class VideoDecoderPool(private val maxDecoders: Int = 4) {
    private val decoders = ConcurrentLinkedQueue<VideoDecoder>()

    fun acquire(): VideoDecoder? {
        return decoders.poll() ?: if (decoders.size < maxDecoders) {
            VideoDecoder(...)
        } else null
    }

    fun release(decoder: VideoDecoder) {
        decoders.offer(decoder)
    }
}
```

### 3. Оптимизации для низкой задержки

#### Проблема: Высокая задержка (>100ms)
**Симптомы:**
- Latency > 100ms от получения кадра до отображения
- Задержка накапливается со временем

**Решения:**
1. **Уменьшить размер буфера**
   - Обрабатывать кадры сразу, без буферизации
   - Использовать ring buffer вместо queue

2. **Приоритизировать IDR кадры**
   - Обрабатывать IDR кадры в первую очередь
   - Пропускать старые P-кадры при переполнении

3. **Асинхронная обработка**
   - Декодирование в отдельном потоке
   - Отображение в UI потоке

#### Реализация:
```kotlin
// Асинхронное декодирование
private val decodeDispatcher = Dispatchers.Default.limitedParallelism(2)

suspend fun decodeAsync(frame: RtspFrame): DecodedVideoFrame? = withContext(decodeDispatcher) {
    decoder.decode(frame)
}
```

### 4. Оптимизации памяти

#### Проблема: Утечки памяти или высокое использование
**Симптомы:**
- Memory usage растет со временем
- OutOfMemoryError при длительной работе
- Memory usage > 500MB для одного потока

**Решения:**
1. **Правильное освобождение ресурсов**
   - Убедиться, что все FFmpeg ресурсы освобождаются
   - Использовать try-finally для гарантии освобождения

2. **Переиспользование буферов**
   - Кэшировать RGB буферы
   - Переиспользовать AVPacket и AVFrame

3. **Ограничить размер очереди кадров**
   - Не накапливать слишком много кадров
   - Пропускать старые кадры при переполнении

#### Реализация:
```kotlin
// Переиспользование буферов
private val rgbBufferPool = ArrayDeque<ByteArray>(10)

private fun getRgbBuffer(size: Int): ByteArray {
    return rgbBufferPool.removeFirstOrNull { it.size >= size }
        ?: ByteArray(size)
}

private fun returnRgbBuffer(buffer: ByteArray) {
    if (rgbBufferPool.size < 10) {
        rgbBufferPool.addLast(buffer)
    }
}
```

### 5. Оптимизации для слабых систем

#### Проблема: Низкая производительность на слабых CPU
**Симптомы:**
- CPU usage = 100%
- FPS < 15
- Decode time > 80ms

**Решения:**
1. **Снизить разрешение**
   - Декодировать в более низком разрешении
   - Масштабировать для отображения

2. **Уменьшить FPS**
   - Пропускать кадры (decode every 2nd frame)
   - Ограничить FPS до 15-20

3. **Использовать более легкий алгоритм масштабирования**
   - SWS_POINT вместо SWS_BILINEAR (быстрее, но хуже качество)

#### Реализация:
```kotlin
// Пропуск кадров для слабых систем
private var frameSkipCounter = 0
private val frameSkipInterval = 2 // Декодировать каждый 2-й кадр

fun decode(frame: RtspFrame): Boolean {
    frameSkipCounter++
    if (frameSkipCounter % frameSkipInterval != 0) {
        return false // Пропускаем кадр
    }
    // ... декодирование
}
```

### 6. Оптимизации для стабильности

#### Проблема: Падения или нестабильная работа
**Симптомы:**
- Периодические ошибки декодирования
- Переинициализация кодера
- Потеря кадров

**Решения:**
1. **Улучшить обработку ошибок**
   - Автоматическое восстановление после ошибок
   - Логирование всех ошибок для анализа

2. **Валидация данных перед декодированием**
   - Проверка размера кадра
   - Проверка наличия SPS/PPS

3. **Graceful degradation**
   - Fallback на MJPEG при ошибках H.264/H.265
   - Fallback на более низкое разрешение

#### Реализация:
```kotlin
private fun validateFrame(frame: RtspFrame): Boolean {
    if (frame.data.isEmpty()) {
        logger.warn { "Empty frame data" }
        return false
    }

    if (frame.width <= 0 || frame.height <= 0) {
        logger.warn { "Invalid frame dimensions: ${frame.width}x${frame.height}" }
        return false
    }

    return true
}
```

## Приоритизация оптимизаций

### Высокий приоритет
1. ✅ Аппаратное ускорение (уже реализовано)
2. ✅ Многопоточность (уже реализовано)
3. ⚠️ Оптимизация sws_scale алгоритма
4. ⚠️ Переиспользование буферов

### Средний приоритет
1. Пул декодеров для множественных потоков
2. Асинхронная обработка
3. Пропуск кадров для слабых систем

### Низкий приоритет
1. Кэширование SwsContext
2. Приоритизация IDR кадров
3. Ring buffer для кадров

## Метрики для мониторинга

Следите за следующими метриками:
- **Average decode time**: Должно быть < 40ms для 1080p
- **P95 decode time**: Должно быть < 60ms для 1080p
- **CPU usage**: Должно быть < 50% для одного потока
- **Memory usage**: Должно быть стабильным (без роста)
- **FPS**: Должно быть >= 25 для 1080p

## Заключение

Большинство критических оптимизаций уже реализованы:
- ✅ Аппаратное ускорение
- ✅ Многопоточность
- ✅ Метрики производительности

Дополнительные оптимизации можно применять по мере необходимости на основе реальных результатов тестирования.
