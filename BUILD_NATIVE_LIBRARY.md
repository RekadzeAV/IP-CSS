# Инструкция по сборке нативной библиотеки и активации декодера

## Краткая инструкция

### Шаг 1: Сборка нативной библиотеки

#### Linux/macOS:
```bash
./scripts/build-video-processing-lib.sh
```

#### Windows:
```powershell
.\scripts\build-video-processing-lib.ps1
```

### Шаг 2: Проверка результата

```bash
# Linux
ls native/video-processing/lib/linux/x64/libvideo_processing.so

# macOS
ls native/video-processing/lib/macos/x64/libvideo_processing.dylib

# Windows
Test-Path native\video-processing\lib\windows\x64\video_processing.dll
```

### Шаг 3: Генерация cinterop биндингов

```bash
./gradlew :core:network:generateCInteropVideoProcessingNativeLinuxX64
# или для macOS:
./gradlew :core:network:generateCInteropVideoProcessingNativeMacosX64
```

### Шаг 4: Раскомментирование кода

После успешной сборки библиотеки и генерации биндингов, откройте файл:

`core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/video/VideoDecoder.native.kt`

И раскомментируйте:

1. **Импорт cinterop** (строка ~9):
   ```kotlin
   import com.company.ipcamera.core.network.native.videoprocessing.*
   ```

2. **Код в init блоке** (строки ~56-132):
   - Раскомментировать весь блок с созданием декодера и callback

3. **Код в методе decode()** (строки ~157-169):
   - Раскомментировать вызов `video_decoder_decode()`

4. **Код в методе getInfo()** (строки ~205-229):
   - Раскомментировать вызов `video_decoder_get_info()`

5. **Код в методе release()** (строки ~251-256):
   - Раскомментировать вызов `video_decoder_destroy()`

### Шаг 5: Проверка компиляции

```bash
./gradlew :core:network:compileKotlinNativeLinuxX64
```

## Подробная документация

Для более детальной информации см.:
- `docs/ACTIVATE_NATIVE_DECODER.md` - Подробная инструкция по активации
- `native/video-processing/BUILD.md` - Детали сборки библиотеки
- `docs/VIDEO_DECODER_SETUP.md` - Общая документация по настройке

