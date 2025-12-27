# Настройка видео декодера H.264/H.265

## Обзор

Документ описывает настройку и активацию декодера H.264/H.265 для разных платформ.

## Desktop (JVM) - JavaCV/FFmpeg

### Текущий статус

- ✅ Зависимости JavaCV добавлены в `core/network/build.gradle.kts`
- ⚠️ Реализация требует доработки для декодирования отдельных кадров
- ⚠️ FFmpegFrameGrabber работает с потоками, не с отдельными NAL units

### Требования

1. **JavaCV зависимости** (уже добавлены):
   ```kotlin
   implementation("org.bytedeco:javacv:1.5.9")
   implementation("org.bytedeco:ffmpeg-platform:6.0-1.5.9")
   ```

2. **FFmpeg нативные библиотеки**:
   - JavaCV автоматически загружает нативные библиотеки FFmpeg
   - Для Windows: `ffmpeg-6.0-1.5.9-windows-x86_64.jar`
   - Для Linux: `ffmpeg-6.0-1.5.9-linux-x86_64.jar`
   - Для macOS: `ffmpeg-6.0-1.5.9-macosx-x86_64.jar` или `-arm64.jar`

### Реализация через AVCodecContext

Для декодирования отдельных кадров из RTSP потока нужно использовать низкоуровневый API FFmpeg:

```kotlin
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.avcodec.*
import org.bytedeco.ffmpeg.avutil.*
import org.bytedeco.ffmpeg.swscale.*
import org.bytedeco.javacpp.BytePointer

// 1. Найти кодек
val codecId = when (codec) {
    VideoCodec.H264 -> avcodec.AV_CODEC_ID_H264
    VideoCodec.H265 -> avcodec.AV_CODEC_ID_H265
    else -> return false
}
val avCodec = avcodec_find_decoder(codecId)

// 2. Создать контекст кодера
val codecContext = avcodec_alloc_context3(avCodec)
codecContext.width(width)
codecContext.height(height)
codecContext.pix_fmt(avutil.AV_PIX_FMT_YUV420P)

// 3. Открыть кодек
avcodec_open2(codecContext, avCodec, null as Pointer?)

// 4. Декодировать кадр
val packet = AVPacket()
av_init_packet(packet)
packet.data(BytePointer(frame.data))
packet.size(frame.data.size)

avcodec_send_packet(codecContext, packet)
val frame = AVFrame()
avcodec_receive_frame(codecContext, frame)

// 5. Конвертировать YUV в RGB
val swsContext = sws_getContext(
    width, height, avutil.AV_PIX_FMT_YUV420P,
    width, height, avutil.AV_PIX_FMT_RGB24,
    swscale.SWS_BILINEAR, null, null, null
)

val rgbFrame = AVFrame()
val rgbBufferSize = av_image_get_buffer_size(avutil.AV_PIX_FMT_RGB24, width, height, 1)
val rgbBuffer = BytePointer(malloc(rgbBufferSize.toLong()))
av_image_fill_arrays(rgbFrame.data(), rgbFrame.linesize(), rgbBuffer, avutil.AV_PIX_FMT_RGB24, width, height, 1)

sws_scale(swsContext, frame.data(), frame.linesize(), 0, height, rgbFrame.data(), rgbFrame.linesize())

// 6. Конвертировать в BufferedImage
// ...
```

### Следующие шаги

1. Реализовать полный код декодирования через AVCodecContext
2. Добавить обработку SPS/PPS для H.264
3. Добавить обработку VPS/SPS/PPS для H.265
4. Оптимизировать производительность

## Native платформы (Linux, macOS, Windows)

### Текущий статус

- ✅ Структура cinterop настроена в `core/network/build.gradle.kts`
- ✅ `.def` файл создан: `core/network/src/nativeInterop/cinterop/video_processing.def`
- ⚠️ Код в `VideoDecoder.native.kt` закомментирован (требует активации)
- ⚠️ Нативная библиотека должна быть собрана

### Требования

1. **Нативная библиотека video_processing**:
   - Должна быть собрана через CMake
   - Должна быть доступна в `native/video-processing/lib/<platform>/`
   - Должна экспортировать функции из `video_decoder.h`

2. **Cinterop конфигурация**:
   - Уже настроена в `build.gradle.kts` для всех native платформ
   - Использует `video_processing.def` для генерации биндингов

### Активация интеграции

1. **Собрать нативную библиотеку**:
   ```bash
   cd native/video-processing
   mkdir -p build
   cd build
   cmake ..
   cmake --build .
   ```

2. **Проверить наличие библиотеки**:
   ```bash
   # Linux
   ls native/video-processing/lib/linux/x64/libvideo_processing.so

   # macOS
   ls native/video-processing/lib/macos/x64/libvideo_processing.dylib
   ls native/video-processing/lib/macos/arm64/libvideo_processing.dylib

   # Windows
   ls native/video-processing/lib/windows/x64/video_processing.dll
   ```

3. **Раскомментировать код в VideoDecoder.native.kt**:
   - Раскомментировать импорты cinterop
   - Раскомментировать код создания декодера
   - Раскомментировать код декодирования
   - Раскомментировать структуру DecodedFrameStruct

4. **Собрать проект**:
   ```bash
   ./gradlew :core:network:build
   ```

### Проверка интеграции

После активации проверьте:

1. **Компиляция проходит успешно**:
   ```bash
   ./gradlew :core:network:compileKotlinNative
   ```

2. **Биндинги сгенерированы**:
   ```bash
   ls core/network/build/classes/kotlin/native/main/cinterop/
   ```

3. **Тестирование**:
   ```kotlin
   val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)
   decoder.setCallback { frame ->
       println("Decoded frame: ${frame.width}x${frame.height}")
   }
   ```

## Структура файлов

```
core/network/src/
├── commonMain/kotlin/.../video/
│   └── VideoDecoder.kt              # Expect класс
├── jvmMain/kotlin/.../video/
│   └── VideoDecoder.jvm.kt          # JVM реализация (JavaCV)
└── nativeMain/kotlin/.../video/
    └── VideoDecoder.native.kt       # Native реализация (cinterop)

core/network/src/nativeInterop/cinterop/
└── video_processing.def             # Cinterop конфигурация

native/video-processing/
├── include/
│   └── video_decoder.h              # Нативный заголовок
├── src/
│   └── video_decoder.cpp            # Нативная реализация
└── lib/
    ├── linux/x64/
    ├── macos/x64/
    ├── macos/arm64/
    └── windows/x64/
```

## Отладка

### Проблемы с JavaCV

1. **Библиотеки не загружаются**:
   - Проверьте, что `ffmpeg-platform` зависимость включена
   - Проверьте логи загрузки библиотек

2. **Ошибки декодирования**:
   - Убедитесь, что данные кадра содержат полный NAL unit
   - Проверьте наличие SPS/PPS для H.264

### Проблемы с cinterop

1. **Биндинги не генерируются**:
   - Проверьте путь к заголовкам в `.def` файле
   - Проверьте, что библиотека собрана

2. **Ошибки линковки**:
   - Проверьте путь к библиотеке в `linkerOpts`
   - Убедитесь, что библиотека доступна в runtime

3. **Ошибки выполнения**:
   - Проверьте, что библиотека загружается правильно
   - Проверьте логи нативного кода

## Следующие шаги

1. ✅ Добавлены зависимости JavaCV
2. ✅ Создана структура для native интеграции
3. ⏳ Реализовать полное декодирование через AVCodecContext (JVM)
4. ⏳ Активировать cinterop интеграцию (Native)
5. ⏳ Добавить тесты для декодера
6. ⏳ Оптимизировать производительность

