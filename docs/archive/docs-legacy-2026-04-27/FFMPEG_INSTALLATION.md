# Руководство по установке и настройке FFmpeg

**Версия проекта:** Alfa-0.0.1
**Последнее обновление:** 26 January 2026

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

## Обзор

FFmpeg является критически важным компонентом для полной поддержки записи аудио в системе IP-CSS. Без FFmpeg аудио записываться не будет (только видео в fallback режиме).

## Требования

- **Минимальная версия FFmpeg:** 4.0+
- **Рекомендуемая версия:** 5.0+ (последняя стабильная)
- **Необходимые кодеки:**
  - Видео: H.264 (libx264), H.265 (опционально)
  - Аудио: AAC, MP3 (libmp3lame), G.711 (PCMU/PCMA), PCM

## Быстрая установка

### Автоматическая установка

#### Linux/macOS
```bash
# Перейти в директорию проекта
cd IP-CSS

# Запустить скрипт установки
chmod +x scripts/install-ffmpeg.sh
./scripts/install-ffmpeg.sh
```

#### Windows
```powershell
# Запустить скрипт установки
.\scripts\install-ffmpeg.ps1
```

## Ручная установка по платформам

### Linux

#### Ubuntu/Debian
```bash
sudo apt-get update
sudo apt-get install -y \
    ffmpeg \
    libavformat-dev \
    libavcodec-dev \
    libavutil-dev \
    libswscale-dev \
    libswresample-dev \
    libavfilter-dev \
    libavdevice-dev
```

#### RHEL/Fedora/CentOS
```bash
# Fedora/RHEL 8+
sudo dnf install -y \
    ffmpeg \
    ffmpeg-devel \
    libavformat \
    libavcodec \
    libavutil \
    libswscale \
    libswresample

# CentOS/RHEL 7 (требуется EPEL)
sudo yum install -y epel-release
sudo yum install -y ffmpeg ffmpeg-devel
```

#### Alpine Linux
```bash
sudo apk add --no-cache \
    ffmpeg \
    ffmpeg-dev \
    libavformat \
    libavcodec \
    libavutil \
    libswscale \
    libswresample
```

### macOS

#### Через Homebrew (рекомендуется)
```bash
brew install ffmpeg
```

#### Проверка установки
```bash
ffmpeg -version
```

### Windows

#### Способ 1: Chocolatey (рекомендуется)
```powershell
# Установить Chocolatey (если не установлен)
# https://chocolatey.org/install

# Установить FFmpeg
choco install ffmpeg -y
```

#### Способ 2: Scoop
```powershell
# Установить Scoop (если не установлен)
# https://scoop.sh

# Установить FFmpeg
scoop install ffmpeg
```

#### Способ 3: Ручная установка
1. Скачать FFmpeg с https://www.gyan.dev/ffmpeg/builds/
2. Распаковать в `C:\ffmpeg`
3. Добавить `C:\ffmpeg\bin` в PATH:
   - Открыть "Переменные среды"
   - Добавить `C:\ffmpeg\bin` в переменную `Path`

## Проверка установки

### Базовая проверка
```bash
ffmpeg -version
```

### Проверка поддержки кодеков
```bash
# Проверить аудио кодеки
ffmpeg -codecs | grep -E "DEA.*(aac|mp3|pcm|g711)"

# Проверить видео кодеки
ffmpeg -codecs | grep -E "DEV.*(h264|h265|hevc)"
```

### Проверка hardware acceleration
```bash
# Проверить доступные hardware энкодеры
ffmpeg -encoders | grep -E "(nvenc|qsv|videotoolbox|vaapi)"
```

## Оптимизация производительности

### Hardware Acceleration

Система автоматически определяет и использует hardware acceleration если доступно:

- **NVIDIA NVENC** (h264_nvenc) - для NVIDIA GPU
- **Intel Quick Sync** (h264_qsv) - для Intel процессоров с iGPU
- **VideoToolbox** (h264_videotoolbox) - для macOS
- **VAAPI** (h264_vaapi) - для Linux с поддержкой VAAPI

### Настройки производительности

Система использует следующие оптимизации:

1. **Многопоточность:** Автоматическое использование всех доступных CPU ядер
2. **Минимальная буферизация:** Снижение задержки при записи
3. **Оптимизированные пресеты:** Использование `fast` пресета вместо `medium` для снижения нагрузки
4. **Zerolatency tune:** Минимальная задержка для реального времени

### Ручная настройка (опционально)

Если требуется дополнительная оптимизация, можно изменить параметры в `FfmpegService.kt`:

```kotlin
// Изменить пресет кодирования
args.add("-preset")
args.add("ultrafast") // Самый быстрый, но больше размер файла

// Изменить количество потоков вручную
args.add("-threads")
args.add("4") // Использовать 4 потока вместо всех доступных
```

## Поддерживаемые аудио форматы

### Входные форматы (автоматически определяются)
- **AAC** - Advanced Audio Coding
- **MP3** - MPEG Audio Layer 3
- **G.711 PCMU** - μ-law (телефонный стандарт)
- **G.711 PCMA** - A-law (телефонный стандарт)
- **PCM** - Raw Pulse Code Modulation

### Выходные форматы (конвертируются в)
- **AAC** - для MP4, MKV, MOV
- **MP3** - для AVI, FLV

## Тестирование

### Запуск тестов записи с аудио
```bash
# Перейти в директорию проекта
cd IP-CSS

# Запустить тесты
./gradlew :server:api:test --tests "AudioRecordingTest"
```

### Тестирование с реальным RTSP потоком
```bash
# Установить переменную окружения с тестовым RTSP URL
export TEST_RTSP_URL="rtsp://test-camera.example.com/stream"

# Запустить тесты
./gradlew :server:api:test --tests "AudioRecordingTest.testRecordingWithAacAudio"
```

## Устранение проблем

### FFmpeg не найден

**Проблема:** `FFmpeg not available`

**Решение:**
1. Проверить установку: `ffmpeg -version`
2. Проверить PATH: `echo $PATH` (Linux/macOS) или `$env:Path` (Windows)
3. Перезапустить приложение после установки FFmpeg

### Hardware acceleration не работает

**Проблема:** Используется программное кодирование вместо hardware

**Решение:**
1. Проверить поддержку: `ffmpeg -encoders | grep nvenc`
2. Установить драйверы GPU (для NVIDIA)
3. Проверить что система видит GPU: `nvidia-smi` (NVIDIA)

### Аудио не записывается

**Проблема:** Записывается только видео, без аудио

**Решение:**
1. Проверить что FFmpeg установлен: `ffmpeg -version`
2. Проверить что камера поддерживает аудио: поле `audio: true` в конфигурации камеры
3. Проверить логи приложения на наличие ошибок FFmpeg

### Высокая нагрузка на CPU

**Проблема:** FFmpeg использует 100% CPU

**Решение:**
1. Включить hardware acceleration (см. выше)
2. Снизить качество записи (LOW или MEDIUM вместо HIGH/ULTRA)
3. Уменьшить количество одновременных записей
4. Использовать более мощный процессор

## Производительность

### Рекомендуемые системные требования

- **Минимальные:** 2 CPU ядра, 2GB RAM
- **Рекомендуемые:** 4+ CPU ядра, 4GB+ RAM
- **Оптимальные:** 8+ CPU ядер, 8GB+ RAM, GPU с hardware acceleration

### Ожидаемая нагрузка

- **1 запись (HIGH качество):** ~15-25% CPU (без hardware acceleration)
- **1 запись (HIGH качество):** ~5-10% CPU (с hardware acceleration)
- **10 записей (HIGH качество):** ~150-250% CPU (без hardware acceleration)
- **10 записей (HIGH качество):** ~50-100% CPU (с hardware acceleration)

## Дополнительные ресурсы

- [Официальный сайт FFmpeg](https://ffmpeg.org/)
- [Документация FFmpeg](https://ffmpeg.org/documentation.html)
- [FFmpeg Wiki](https://trac.ffmpeg.org/)

## Связанные документы

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

### Основные документы
- **[DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)** - Полный индекс документации
- **[README.md](../README.md)** - Обзор проекта

### Техническая документация
- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Руководство по развертыванию
- **[RTSP_CLIENT.md](RTSP_CLIENT.md)** - Документация RTSP клиента
- **[API.md](API.md)** - API документация

---

**Последнее обновление:** 26 January 2026

