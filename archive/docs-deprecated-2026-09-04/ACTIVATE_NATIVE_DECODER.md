# Активация нативного декодера

## Обзор

Этот документ описывает шаги по активации нативного декодера H.264/H.265 для Native платформ (Linux, macOS, Windows).

## Шаги активации

### 1. Сборка нативной библиотеки

#### Linux/macOS

```bash
# Сборка для текущей платформы
./scripts/build-video-processing-lib.sh

# Или для конкретной платформы
./scripts/build-video-processing-lib.sh linux x64
./scripts/build-video-processing-lib.sh macos arm64
```

#### Windows

```powershell
# В PowerShell
.\scripts\build-video-processing-lib.ps1
```

**Требования:**
- CMake 3.15+
- C++ компилятор (g++, clang++ или MSVC)
- FFmpeg development libraries
- OpenCV (опционально)

**Проверка результата:**

```bash
# Linux
ls -lh native/video-processing/lib/linux/x64/libvideo_processing.so

# macOS
ls -lh native/video-processing/lib/macos/x64/libvideo_processing.dylib
ls -lh native/video-processing/lib/macos/arm64/libvideo_processing.dylib

# Windows
Test-Path native\video-processing\lib\windows\x64\video_processing.dll
```

### 2. Генерация cinterop биндингов

После сборки библиотеки нужно сгенерировать Kotlin биндинги:

```bash
# Генерация биндингов для всех native платформ
./gradlew :core:network:generateCInteropVideoProcessingNativeLinuxX64
./gradlew :core:network:generateCInteropVideoProcessingNativeMacosX64
./gradlew :core:network:generateCInteropVideoProcessingNativeMacosArm64

# Или для всех сразу (если настроено)
./gradlew :core:network:generateCInteropVideoProcessingNative
```

**Проверка результата:**

```bash
ls core/network/build/classes/kotlin/native/main/cinterop/videoProcessing/
```

Должны появиться файлы:
- `videoProcessing.klib`
- Сгенерированные Kotlin файлы с типами

### 3. Раскомментирование кода

После генерации биндингов нужно раскомментировать код в `VideoDecoder.native.kt`:

1. **Раскомментировать импорты:**
   ```kotlin
   import com.company.ipcamera.core.network.native.videoprocessing.*
   ```

2. **Раскомментировать код в `init` блоке:**
   - Создание нативного декодера
   - Установка callback
   - Обработка декодированных кадров

3. **Раскомментировать код в методе `decode()`:**
   - Вызов `video_decoder_decode()`

4. **Раскомментировать код в методе `getInfo()`:**
   - Вызов `video_decoder_get_info()`

5. **Раскомментировать код в методе `release()`:**
   - Вызов `video_decoder_destroy()`

### 4. Проверка компиляции

```bash
# Компиляция для проверки
./gradlew :core:network:compileKotlinNativeLinuxX64
./gradlew :core:network:compileKotlinNativeMacosX64
./gradlew :core:network:compileKotlinNativeMacosArm64
```

### 5. Тестирование

Создайте простой тест для проверки работы декодера:

```kotlin
@Test
fun testNativeDecoder() {
    val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)
    var decodedFrame: DecodedVideoFrame? = null

    decoder.setCallback { frame ->
        decodedFrame = frame
        println("Decoded frame: ${frame.width}x${frame.height}")
    }

    // Тестовые данные (H.264 NAL unit)
    val testFrame = RtspFrame(
        data = byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x67, ...), // H.264 SPS
        timestamp = System.currentTimeMillis(),
        streamType = RtspStreamType.VIDEO,
        width = 1920,
        height = 1080
    )

    val success = decoder.decode(testFrame)
    // Проверка результата

    decoder.release()
}
```

## Решение проблем

### Ошибка: "Unresolved reference: video_decoder_create"

**Причина:** Cinterop биндинги не сгенерированы или импорт не раскомментирован.

**Решение:**
1. Проверьте что библиотека собрана
2. Выполните генерацию cinterop биндингов
3. Проверьте что импорт раскомментирован

### Ошибка: "Library not found: libvideo_processing.so"

**Причина:** Библиотека не найдена в runtime.

**Решение:**
1. Проверьте что библиотека находится в правильной директории
2. Добавьте путь к библиотеке в `LD_LIBRARY_PATH` (Linux) или `DYLD_LIBRARY_PATH` (macOS)
3. Или скопируйте библиотеку в системную директорию

### Ошибка компиляции cinterop

**Причина:** Неправильная структура `.def` файла или библиотека не собрана.

**Решение:**
1. Проверьте структуру `video_processing.def`
2. Убедитесь что все заголовочные файлы доступны
3. Проверьте пути в `build.gradle.kts`

### Ошибка линковки

**Причина:** Зависимости библиотеки (FFmpeg, OpenCV) не найдены.

**Решение:**
1. Установите все зависимости
2. Проверьте что они доступны в системе
3. Установите переменные окружения для поиска библиотек

## Проверочный список

- [ ] Нативная библиотека собрана для нужных платформ
- [ ] Библиотека находится в `native/video-processing/lib/<platform>/<arch>/`
- [ ] Cinterop биндинги сгенерированы
- [ ] Импорты раскомментированы в `VideoDecoder.native.kt`
- [ ] Код в методах раскомментирован
- [ ] Проект компилируется без ошибок
- [ ] Тесты проходят успешно

## Дополнительная информация

См. также:
- `docs/VIDEO_DECODER_SETUP.md` - Общая документация по настройке
- `docs/VIDEO_DECODER_COMPLETE.md` - Полное описание реализации
- `native/video-processing/README.md` - Документация нативной библиотеки

