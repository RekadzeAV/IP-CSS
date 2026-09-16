# Интеграция видео декодера H.264/H.265

## Обзор

Реализована интеграция нативного декодера H.264/H.265 для декодирования видеокадров из RTSP потоков.

## Архитектура

### Структура модулей

```
core/network/src/
├── commonMain/kotlin/.../video/
│   └── VideoDecoder.kt          # Expect класс и утилиты
├── jvmMain/kotlin/.../video/
│   └── VideoDecoder.jvm.kt      # JVM реализация (Desktop)
└── nativeMain/kotlin/.../video/
    └── VideoDecoder.native.kt   # Native реализация (Linux, macOS, Windows)
```

### Основные компоненты

1. **VideoDecoder** (expect/actual)
   - Единый интерфейс для всех платформ
   - Поддержка H.264, H.265, MJPEG
   - Callback для получения декодированных кадров

2. **DecodedVideoFrame**
   - Структура декодированного кадра
   - Поддержка RGB24 и YUV420 форматов
   - Утилита `toBufferedImage()` для конвертации

3. **Интеграция в VideoPlayer**
   - Автоматическое определение кодека
   - Создание декодера при первом кадре
   - Fallback на MJPEG через ImageIO

## Использование

### Базовое использование

```kotlin
import com.company.ipcamera.core.network.video.*

// Создание декодера
val decoder = VideoDecoder(
    codec = VideoCodec.H264,
    width = 1920,
    height = 1080
)

// Установка callback
decoder.setCallback { decodedFrame ->
    val image = decodedFrame.toBufferedImage()
    // Использование изображения
}

// Декодирование кадра
val success = decoder.decode(rtspFrame)

// Освобождение ресурсов
decoder.release()
```

### Определение кодека

```kotlin
// Из строки
val codec = "H.264".toVideoCodec()

// Из информации о потоке RTSP
val streams = rtspClient.getStreams()
val videoStream = streams.firstOrNull { it.type == RtspStreamType.VIDEO }
val codec = videoStream?.codec?.toVideoCodec() ?: VideoCodec.UNKNOWN
```

## Реализации платформ

### JVM (Desktop)

- **Текущая реализация**: Поддержка MJPEG через ImageIO
- **H.264/H.265**: Требуется JNI интеграция или JavaCV
- **Fallback**: Автоматическое определение MJPEG по сигнатуре (FF D8)

### Native (Linux, macOS, Windows)

- **Текущая реализация**: Заглушка (требуется полная интеграция cinterop)
- **Требуется**:
  1. Настройка cinterop в build.gradle.kts
  2. Сборка нативной библиотеки video_processing
  3. Интеграция функций из video_decoder.h

## Интеграция в VideoPlayer

VideoPlayer автоматически:
1. Определяет кодек из информации о потоке RTSP
2. Создает декодер при первом кадре
3. Декодирует кадры через декодер
4. Конвертирует декодированные кадры в BufferedImage
5. Отображает через Compose ImageBitmap

### Пример потока данных

```
RTSP Frame (ByteArray)
    ↓
VideoDecoder.decode()
    ↓
DecodedVideoFrame (RGB24 ByteArray)
    ↓
toBufferedImage()
    ↓
BufferedImage
    ↓
toComposeImageBitmap()
    ↓
Compose Image
```

## Требования для полной интеграции

### Для JVM (Desktop)

1. **Вариант 1: JNI обертка**
   - Создать JNI методы в native/video-processing/src/jni/
   - Загрузить библиотеку через System.loadLibrary()
   - Реализовать все методы декодера

2. **Вариант 2: JavaCV**
   - Добавить зависимость org.bytedeco:javacv
   - Использовать FFmpegFrameGrabber для декодирования
   - Конвертировать в BufferedImage

3. **Вариант 3: JavaCPP**
   - Использовать org.bytedeco:ffmpeg
   - Прямая интеграция с FFmpeg через JavaCPP

### Для Native платформ

1. Убедиться, что cinterop правильно настроен в build.gradle.kts
2. Собрать нативную библиотеку video_processing
3. Обновить VideoDecoder.native.kt с реальными вызовами cinterop

## Текущий статус

- ✅ Структура VideoDecoder (expect/actual)
- ✅ JVM реализация для MJPEG
- ✅ Интеграция в VideoPlayer
- ✅ Определение кодека из RTSP потока
- ✅ Конвертация RGB24 в BufferedImage
- ⚠️ Native реализация (заглушка)
- ⚠️ H.264/H.265 для JVM (требуется JNI/JavaCV)

## Следующие шаги

1. Реализовать JNI обертку для desktop (JVM)
2. Завершить native реализацию через cinterop
3. Добавить поддержку YUV420 конвертации
4. Оптимизировать производительность декодирования
5. Добавить кэширование декодеров

