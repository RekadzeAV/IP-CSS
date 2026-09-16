# Настройка тестовой среды для FFmpeg декодирования в RTSP клиенте

**Дата создания:** 27 January 2026
**Этап:** 2.1 - Подготовка тестовой среды
**Приоритет:** 🔴 Критично для MVP

---

## 📋 Обзор

Этот документ описывает настройку тестовой среды для тестирования FFmpeg декодирования в RTSP клиенте.

## 🎯 Цели Этапа 2.1

1. ✅ Настроить FFmpeg в окружении разработки
2. ✅ Собрать нативную библиотеку с `ENABLE_FFMPEG=ON`
3. ✅ Проверить, что FFmpeg библиотеки линкуются правильно
4. ✅ Создать документацию по настройке

---

## 📦 Шаг 1: Установка FFmpeg

### Windows

#### Вариант 1: Через Chocolatey (рекомендуется)
```powershell
choco install ffmpeg
```

#### Вариант 2: Ручная установка
1. Скачайте FFmpeg с https://ffmpeg.org/download.html
2. Распакуйте в `C:\ffmpeg`
3. Добавьте `C:\ffmpeg\bin` в PATH
4. Установите переменную окружения:
   ```powershell
   $env:FFMPEG_DIR = "C:\ffmpeg"
   ```

#### Проверка установки
```powershell
ffmpeg -version
ffmpeg -codecs | Select-String -Pattern "h264|h265|aac"
```

### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install -y \
    libavformat-dev \
    libavcodec-dev \
    libavutil-dev \
    libswscale-dev \
    libswresample-dev \
    pkg-config
```

### Linux (Fedora/RHEL)
```bash
sudo dnf install -y ffmpeg-devel pkgconfig
```

### macOS
```bash
brew install ffmpeg pkg-config
```

---

## 🔧 Шаг 2: Настройка переменных окружения

### Windows
```powershell
# Если FFmpeg установлен в C:\ffmpeg
$env:FFMPEG_DIR = "C:\ffmpeg"

# Проверка
echo $env:FFMPEG_DIR
```

### Linux/macOS
```bash
# Если FFmpeg установлен в нестандартном месте
export FFMPEG_DIR=/path/to/ffmpeg
export PKG_CONFIG_PATH=/path/to/ffmpeg/lib/pkgconfig:$PKG_CONFIG_PATH
```

---

## 🏗️ Шаг 3: Сборка нативной библиотеки с FFmpeg

### Windows (PowerShell)
```powershell
cd native\video-processing
mkdir -p build\windows-x64
cd build\windows-x64

cmake ..\.. `
    -DCMAKE_BUILD_TYPE=Release `
    -DENABLE_FFMPEG=ON `
    -DENABLE_OPENCV=OFF `
    -DCMAKE_GENERATOR_PLATFORM=x64

cmake --build . --config Release
```

### Linux
```bash
cd native/video-processing
mkdir -p build/linux-x64
cd build/linux-x64

cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=OFF

cmake --build . --config Release -j$(nproc)
```

### macOS
```bash
cd native/video-processing
mkdir -p build/macos-x64
cd build/macos-x64

cmake ../.. \
    -DCMAKE_BUILD_TYPE=Release \
    -DENABLE_FFMPEG=ON \
    -DENABLE_OPENCV=OFF

cmake --build . --config Release
```

---

## ✅ Шаг 4: Проверка сборки

### Проверка наличия библиотеки
**Windows:**
```powershell
Test-Path build\windows-x64\Release\video_processing.dll
```

**Linux:**
```bash
ls -lh build/linux-x64/libvideo_processing.so
```

**macOS:**
```bash
ls -lh build/macos-x64/libvideo_processing.dylib
```

### Проверка линковки FFmpeg
**Windows:**
```powershell
dumpbin /DEPENDENTS build\windows-x64\Release\video_processing.dll | Select-String -Pattern "avformat|avcodec|avutil"
```

**Linux:**
```bash
ldd build/linux-x64/libvideo_processing.so | grep -E "avformat|avcodec|avutil"
```

**macOS:**
```bash
otool -L build/macos-x64/libvideo_processing.dylib | grep -E "avformat|avcodec|avutil"
```

### Проверка экспорта символов
**Windows:**
```powershell
dumpbin /EXPORTS build\windows-x64\Release\video_processing.dll | Select-String -Pattern "rtsp_client|video_decoder"
```

**Linux:**
```bash
nm -D build/linux-x64/libvideo_processing.so | grep -E "rtsp_client|video_decoder"
```

**macOS:**
```bash
nm -gU build/macos-x64/libvideo_processing.dylib | grep -E "rtsp_client|video_decoder"
```

---

## 🧪 Шаг 5: Проверка FFmpeg кодеков

Убедитесь, что следующие кодеки доступны:

### Видео кодеки
- ✅ H.264 (h264)
- ✅ H.265/HEVC (hevc)

### Аудио кодеки
- ✅ AAC (aac)
- ✅ G.711 PCMU (pcm_mulaw)
- ✅ G.711 PCMA (pcm_alaw)

### Команда проверки
```bash
ffmpeg -codecs | grep -E "h264|hevc|aac|pcm"
```

---

## 📝 Шаг 6: Настройка тестовых камер (опционально)

Для интеграционных тестов потребуются тестовые RTSP камеры:

### Вариант 1: Реальные камеры
- Настройте доступ к реальным RTSP камерам
- Задокументируйте URL, учетные данные, параметры потоков

### Вариант 2: Эмуляторы RTSP
- **MediaMTX** (ранее rtsp-simple-server): https://github.com/aler9/mediamtx
- **FFmpeg RTSP сервер**: Используйте FFmpeg для создания тестового потока

### Пример запуска MediaMTX
```bash
# Скачать с https://github.com/aler9/mediamtx/releases
./mediamtx
# Тестовый поток будет доступен на rtsp://localhost:8554/test
```

---

## 🐛 Решение проблем

### Проблема: FFmpeg не найден CMake

**Решение:**
1. Установите переменную окружения `FFMPEG_DIR`
2. Убедитесь, что `pkg-config` может найти FFmpeg (Linux/macOS)
3. Проверьте пути в CMakeLists.txt

### Проблема: Ошибки линковки

**Решение:**
1. Убедитесь, что все FFmpeg библиотеки установлены
2. Проверьте версию FFmpeg (рекомендуется 6.0+)
3. Убедитесь, что библиотеки собраны для той же архитектуры

### Проблема: Символы не экспортируются

**Решение:**
1. Проверьте настройки экспорта в CMakeLists.txt
2. Убедитесь, что функции помечены как `extern "C"` в C++ коде
3. Проверьте `.def` файл для Windows (если используется)

---

## ✅ Критерии приемки Этапа 2.1

- [x] FFmpeg установлен и доступен
- [x] Нативная библиотека собирается с `ENABLE_FFMPEG=ON`
- [x] FFmpeg библиотеки успешно линкуются
- [x] Символы RTSP клиента экспортируются
- [x] Документация создана

---

## 📚 Следующие шаги

После завершения Этапа 2.1:
1. **Этап 2.2:** Unit тесты для декодирования
2. **Этап 2.3:** Интеграционные тесты с реальными камерами

---

**Последнее обновление:** 27 January 2026
