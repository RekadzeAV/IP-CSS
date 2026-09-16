# Руководство по выполнению тестирования VideoDecoder

## Требования к окружению (для полной сборки)

- **Android SDK:** для сборки модулей с Android-таргетом (`:android:app`, `:shared`, `:core:common`) нужна переменная `ANDROID_HOME` или `sdk.dir` в `local.properties`.
- **Полная сборка:** `.\gradlew.bat build` на Windows может потребовать Android SDK; для проверки только JVM/сервера используйте сборку нужных подмодулей (см. корневой `build.gradle.kts`, задачи `buildAll` / `testAll`).

## Быстрый старт

### Вариант 1: Демо-тесты (без реальных камер)

Демо-тесты используют симулятор и могут быть запущены без доступа к реальным камерам:

```bash
# Linux/macOS
./scripts/run-demo-tests.sh

# Windows
.\scripts\run-demo-tests.ps1
```

Эти тесты демонстрируют:
- Базовое декодирование H.264
- Производительность для различных разрешений
- Сбор метрик
- Профилирование

### Вариант 2: Тесты с реальными камерами

#### Шаг 1: Настройка переменных окружения

```bash
# Linux/macOS
source ./scripts/setup-test-environment.sh

# Windows PowerShell
. .\scripts\setup-test-environment.ps1
```

Это создаст файл `.test-env` с шаблоном конфигурации. Отредактируйте его с вашими камерами:

```bash
export TEST_CAMERA_H264_URL="rtsp://192.168.1.100:554/stream"
export TEST_CAMERA_H265_URL="rtsp://192.168.1.101:554/stream"
export TEST_CAMERA_MJPEG_URL="rtsp://192.168.1.102:554/stream"
export TEST_CAMERA_USERNAME="admin"
export TEST_CAMERA_PASSWORD="password"
```

#### Шаг 2: Запуск тестов

```bash
# Linux/macOS
./scripts/test-video-decoder.sh

# Windows
.\scripts\test-video-decoder.ps1
```

#### Шаг 3: Анализ результатов

Результаты будут в:
- Логах: `build/test-results/`
- Метрики: `build/test-results/video-decoder-metrics.csv`
- Статистика: `build/test-results/video-decoder-stats.json`

## Сборка нативных библиотек

### Linux x64

**Требования:**
- Linux система (Ubuntu/Debian/Fedora)
- CMake 3.10+
- FFmpeg development libraries
- GCC/Clang

**Установка зависимостей (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install -y cmake build-essential pkg-config
sudo apt-get install -y libavcodec-dev libavformat-dev libavutil-dev libswscale-dev libswresample-dev
```

**Сборка:**
```bash
./scripts/build-video-processing-linux.sh
```

### macOS x64/arm64

**Требования:**
- macOS 10.15+
- Xcode Command Line Tools
- Homebrew
- FFmpeg

**Установка зависимостей:**
```bash
# Установка Homebrew (если не установлен)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка зависимостей
brew install cmake ffmpeg
```

**Сборка:**
```bash
# Для Apple Silicon (arm64)
./scripts/build-video-processing-macos.sh arm64

# Для Intel (x64)
./scripts/build-video-processing-macos.sh x64
```

## Профилирование

### Использование встроенного профилировщика

Профилировщик автоматически собирает метрики во время тестов. Результаты выводятся в логи.

### Использование внешних инструментов

#### VisualVM
```bash
# Запуск VisualVM
jvisualvm &

# Запуск тестов
./scripts/test-video-decoder.sh
```

Подключитесь к процессу Java в VisualVM для детального профилирования.

#### JProfiler
```bash
# Запуск JProfiler
jprofiler &

# Запуск тестов с JProfiler
jprofiler -offline -config=jprofiler.xml -jar app.jar
```

## Анализ результатов

### 1. Заполнение отчета

Используйте шаблон `docs/TESTING_RESULTS_TEMPLATE.md` для документирования результатов.

Пример заполненного отчета: `docs/TESTING_RESULTS_EXAMPLE.md`

### 2. Анализ метрик

Метрики экспортируются в CSV и JSON форматах:

```bash
# Просмотр CSV метрик
cat build/test-results/video-decoder-metrics.csv

# Просмотр JSON статистики
cat build/test-results/video-decoder-stats.json
```

### 3. Определение узких мест

Смотрите на следующие метрики:
- **Average decode time**: Если > 50ms для 1080p, требуется оптимизация
- **P95 decode time**: Если > 80ms, есть проблемы с производительностью
- **CPU usage**: Если > 80%, требуется аппаратное ускорение или оптимизация
- **Memory usage**: Если растет со временем, возможна утечка памяти

### 4. Применение оптимизаций

См. `docs/OPTIMIZATION_RECOMMENDATIONS.md` для рекомендаций по оптимизации на основе результатов.

## Устранение проблем

### Проблема: Тесты не запускаются

**Решение:**
1. Проверьте, что FFmpeg/JavaCV установлены
2. Проверьте, что переменные окружения настроены
3. Проверьте логи на ошибки

### Проблема: Низкая производительность

**Решение:**
1. Проверьте, используется ли аппаратное ускорение (см. логи)
2. Проверьте количество CPU ядер
3. См. рекомендации в `docs/OPTIMIZATION_RECOMMENDATIONS.md`

### Проблема: Ошибки декодирования

**Решение:**
1. Проверьте формат потока камеры
2. Проверьте наличие SPS/PPS в потоке
3. Проверьте логи на детали ошибок

## Следующие шаги

После выполнения тестирования:

1. **Заполните отчет** используя шаблон
2. **Проанализируйте результаты** и определите узкие места
3. **Примените оптимизации** из рекомендаций
4. **Повторите тестирование** для проверки улучшений

## Дополнительные ресурсы

- `docs/TESTING_PLAN_VIDEO_DECODER.md` - Детальный план тестирования
- `docs/HARDWARE_ACCELERATION_GUIDE.md` - Руководство по аппаратному ускорению
- `docs/MULTITHREADING_GUIDE.md` - Руководство по многопоточности
- `docs/OPTIMIZATION_RECOMMENDATIONS.md` - Рекомендации по оптимизации
