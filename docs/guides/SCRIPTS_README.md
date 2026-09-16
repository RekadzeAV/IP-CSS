# RTSP Client - Скрипты и утилиты

Полное руководство по использованию скриптов для сборки, тестирования и отладки.

---

## 📋 Содержание

1. [Скрипты сборки](#скрипты-сборки)
2. [Скрипты тестирования](#скрипты-тестирования)
3. [Утилиты отладки](#утилиты-отладки)
4. [Docker-контейнеры](#docker-контейнеры)

---

## Скрипты сборки

### 1. Windows Build Script

**Путь:** `native/video-processing/build-windows.ps1`

**Использование:**
```powershell
# Быстрая сборка Release
.\native\video-processing\build-windows.ps1

# Сборка Debug
.\native\video-processing\build-windows.ps1 Debug

# Очистка и пересборка
.\native\video-processing\build-windows.ps1 -Clean

# Показать справки
.\native\video-processing\build-windows.ps1 -Help

# Показать инструкции по установке зависимостей
.\native\video-processing\build-windows.ps1 -InstallDeps
```

**Проверки:**
- ✅ CMake 3.20+
- ✅ Visual Studio 2022
- ✅ FFmpeg (C:\ffmpeg)
- ✅ Java 17+ (для JNI)
- ✅ Visual C++ Redistributable

**Вывод:**
```
=========================================
RTSP Native Library Builder (Windows)
=========================================

🔹 Проверка требований...
✅ CMake 3.28.0
✅ Visual Studio 2022: C:\Program Files\Microsoft Visual Studio\2022\Community
✅ FFmpeg: C:\ffmpeg
✅ JAVA_HOME: C:\Program Files\Java\jdk-17

🔹 Конфигурация CMake...
✅ CMake сконфигурирован

🔹 Сборка...
✅ Сборка завершена

🔹 Установка библиотеки...
✅ lib/windows/x64/video_processing.dll (2.5 MB)

✅ Готово!
```

---

### 2. macOS Build Script

**Путь:** `native/video-processing/build-macos.sh`

**Использование:**
```bash
# Быстрая сборка (автодетект архитектуры)
./native/video-processing/build-macos.sh

# Сборка Release для Apple Silicon
./native/video-processing/build-macos.sh Release arm64

# Сборка для Intel
./native/video-processing/build-macos.sh Release x86_64

# Универсальная сборка
./native/video-processing/build-macos.sh Release universal

# Очистка и пересборка
./native/video-processing/build-macos.sh Release --clean

# Установка зависимостей
./native/video-processing/build-macos.sh --install-deps

# Показать справку
./native/video-processing/build-macos.sh --help
```

**Проверки:**
- ✅ CMake 3.20+
- ✅ Xcode Command Line Tools
- ✅ FFmpeg 6.0+
- ✅ Java 17+ (для JNI)
- ✅ pkg-config

**Вывод:**
```
=========================================
RTSP Native Library Builder (macOS)
=========================================

🔹 Проверка требований...
✅ CMake 3.28.0
✅ Xcode 15.2
✅ FFmpeg 6.1
✅ Homebrew установлен

🔹 Целевая архитектура: arm64
ℹ️  Сборка для Apple Silicon (M1/M2/M3)

🔹 Конфигурация CMake...
✅ CMake сконфигурирован

🔹 Сборка библиотеки...
✅ Сборка завершена

🔹 Установка библиотеки...
✅ lib/macos/libvideo_processing.dylib (3.2 MB)

✅ Готово!
```

---

## Скрипты тестирования

### 3. RTSP Connection Tester

**Путь:** `test-rtsp-connection.ps1`

**Назначение:** Быстрая проверка подключения к RTSP камере

**Использование:**
```powershell
# Интерактивный режим
.\test-rtsp-connection.ps1

# С указанием URL
.\test-rtsp-connection.ps1 -Url "rtsp://admin:pass@192.168.1.100:554/stream"

# С аутентификацией
.\test-rtsp-connection.ps1 -Url "rtsp://192.168.1.100:554/stream" -Username admin -Password pass

# С увеличенным таймаутом
.\test-rtsp-connection.ps1 -Url "rtsp://camera/stream" -Timeout 30
```

**Проверяет:**
1. ✅ Сетевая доступность (ping)
2. ✅ Открытие порта 554
3. ✅ Информация о потоке (ffprobe)
4. ✅ Задержка сети
5. ✅ Совместимость с VLC/ffplay

**Вывод:**
```
=========================================
RTSP Connection Tester
=========================================

🔹 1. Проверка сетевой доступности...
✅ Хост доступен (ping OK)
✅ Порт 554 открыт

🔹 2. Проверка кодеков (ffprobe)...
✅ Поток доступен
ℹ️  Информация о потоке:
  Видеокодек: h264
  Разрешение: 1920x1080
  FPS: 30/1

🔹 3. Проверка с VLC...
✅ VLC найден

🔹 4. Быстрый просмотр (ffplay)...
ℹ️  Запуск ffplay для просмотра потока...

🔹 5. Тест задержки...
ℹ️  Измерение 1/3... ✅ 45 ms
ℹ️  Измерение 2/3... ✅ 42 ms
ℹ️  Измерение 3/3... ✅ 48 ms

✅ Средняя задержка: 45 ms
✅ Отличная задержка (< 100ms)

=========================================
Результаты
=========================================
✅ RTSP камера доступна и готова к использованию!
```

---

### 4. Integration Test Runner

**Путь:** `test-rtsp-integration.ps1` / `test-rtsp-integration.sh`

**Назначение:** Автоматический запуск интеграционных тестов с RTSP сервером

**Использование (Windows):**
```powershell
# Автоматический запуск с mediamtx
.\test-rtsp-integration.ps1

# С внешним RTSP сервером
$env:TEST_RTSP_URL="rtsp://192.168.1.100:554/stream"
.\test-rtsp-integration.ps1
```

**Использование (Linux/macOS):**
```bash
# Автоматический запуск с mediamtx
./test-rtsp-integration.sh

# С внешним RTSP сервером
export TEST_RTSP_URL="rtsp://192.168.1.100:554/stream"
./test-rtsp-integration.sh
```

**Что делает:**
1. Запускает RTSP сервер (mediamtx) в Docker
2. Ждет готовности сервера
3. Запускает интеграционные тесты
4. Останавливает сервер
5. Показывает результаты

**Вывод:**
```
=========================================
RTSP Integration Test Runner
=========================================

ℹ️  TEST_RTSP_URL не установлен. Запуск с локальным RTSP сервером...

🔄 Остановка существующего RTSP сервера...
🚀 Запуск RTSP сервера (mediamtx)...
⏳ Ожидание запуска сервера (10 секунд)...
✅ RTSP сервер запущен

📡 RTSP URL: rtsp://localhost:8554/test

=========================================
🧪 Запуск интеграционных тестов
=========================================

> Task :desktopTest

RtspRealStreamTest > realStream_connection PASSED
RtspRealStreamTest > realStream_play PASSED
RtspRealStreamTest > realStream_frames PASSED

BUILD SUCCESSFUL

🔄 Остановка RTSP сервера...

=========================================
✅ Все тесты пройдены!
=========================================
```

---

### 5. Basic RTSP Test Client

**Путь:** `test-rtsp-client.ps1`

**Назначение:** Базовое тестирование RTSP клиента

**Использование:**
```powershell
# Запустить все тесты
.\test-rtsp-client.ps1

# Тест с конкретным URL
.\test-rtsp-client.ps1 -Url "rtsp://camera/stream"
```

**Тестирует:**
- Подключение
- Воспроизведение
- Получение кадров
- Переподключение

---

## Утилиты отладки

### 6. FFmpeg Tools

**Проверка потока:**
```bash
# Информация о потоке
ffprobe -rtsp_transport tcp -i "rtsp://admin:pass@ip:554/stream"

# Просмотр потока
ffplay -rtsp_transport tcp -fflags nobuffer -i "rtsp://admin:pass@ip:554/stream"

# Запись потока
ffmpeg -rtsp_transport tcp -i "rtsp://admin:pass@ip:554/stream" -c copy output.mp4
```

### 7. Проверка экспортируемых символов

**Windows:**
```powershell
dumpbin /exports lib/windows/x64/video_processing.dll | findstr rtsp_client
```

**Linux/macOS:**
```bash
nm -D lib/linux/x64/libvideo_processing.so | grep rtsp_client
```

---

## Docker-контейнеры

### 8. Linux Build Container

**Путь:** `native/video-processing/Dockerfile.linux`

**Использование:**
```bash
# Сборка контейнера
cd native/video-processing
docker build -f Dockerfile.linux -t rtsp-builder .

# Запуск сборки
docker run --rm -v ${PWD}:/build rtsp-builder

# Копирование библиотеки
docker create --name rtsp rtsp-builder
docker cp rtsp:/build/build/lib/linux/x64/libvideo_processing.so ./lib/linux/x64/
docker rm rtsp
```

---

## 📊 Сравнение скриптов

| Скрипт | Цель | Платформа | Автоматизация |
|--------|------|-----------|---------------|
| `build-windows.ps1` | Сборка | Windows | Высокая |
| `build-macos.sh` | Сборка | macOS | Высокая |
| `test-rtsp-connection.ps1` | Тест подключения | Windows | Средняя |
| `test-rtsp-integration.ps1` | Интеграционные тесты | Windows | Полная |
| `test-rtsp-integration.sh` | Интеграционные тесты | Linux/macOS | Полная |
| `test-rtsp-client.ps1` | Базовое тестирование | Windows | Средняя |

---

## 🎯 Рекомендации

### Для разработчиков

1. **Перед началом работы:**
   ```powershell
   # Windows
   .\native\video-processing\build-windows.ps1 -InstallDeps
   
   # macOS
   ./native/video-processing/build-macos.sh --install-deps
   ```

2. **После изменений кода:**
   ```powershell
   # Windows
   .\native\video-processing\build-windows.ps1 -Clean
   
   # macOS
   ./native/video-processing/build-macos.sh Release --clean
   ```

3. **Перед коммитом:**
   ```bash
   ./gradlew :core:network:desktopTest
   ```

### Для тестировщиков

1. **Проверка камеры:**
   ```powershell
   .\test-rtsp-connection.ps1 -Url "rtsp://camera/stream"
   ```

2. **Интеграционные тесты:**
   ```powershell
   .\test-rtsp-integration.ps1
   ```

### Для DevOps

1. **Автоматическая сборка:**
   - GitHub Actions уже настроен
   - См. `.github/workflows/rtsp-native-build.yml`

---

**Версия:** 1.0  
**Обновлено:** 24 мая 2026
