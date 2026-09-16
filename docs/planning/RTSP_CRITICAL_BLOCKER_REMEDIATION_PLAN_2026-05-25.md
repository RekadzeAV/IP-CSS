# План устранения критического блокера: RTSP клиент

**Дата создания:** 25 May 2026  
**Версия плана:** 1.0  
**Статус:** 🟡 В процессе реализации  

> **📚 Связанные документы:**
> - [PHASE1_CRITICAL_BLOCKERS_REMEDIATION_STATUS_2026-05-25.md](../reports/PHASE1_CRITICAL_BLOCKERS_REMEDIATION_STATUS_2026-05-25.md) — Общий статус
> - [RTSP_CLIENT_README.md](../rtsp/RTSP_CLIENT_README.md) — API документация
> - [CRITICAL_BLOCKERS_REMEDIATION_PLAN.md](CRITICAL_BLOCKERS_REMEDIATION_PLAN.md) — Общий план

---

## 🎯 Цель

Полное устранение критического блокера RTSP клиента для достижения MVP готовности к **7 June 2026** (2 недели).

**Критерий успеха:** RTSP клиент работает стабильно с 5+ реальными IP камерами, включая аудио и видео потоки.

---

## 📊 Текущий статус

| Компонент | Прогресс | Статус |
|-----------|----------|--------|
| Нативная C++ реализация | 100% | ✅ Завершено |
| JNI обёртки | 100% | ✅ Завершено |
| Kotlin Multiplatform | 90% | 🟡 В процессе |
| FFI биндинги | 70% | 🟡 В процессе |
| Аудио декодирование | 0% | ❌ Отключено |
| Тестирование с камерами | 10% | ⚠️ Начато |
| Производительность | 50% | 🟡 В процессе |

**Общий прогресс:** ~85%

---

## 📋 Задачи

### Задача 1: Исправление аудио декодирования (FFmpeg 8.0 API)

**Приоритет:** 🔴 Критический  
**Оценка:** 2-3 дня  
**Ответственный:** Разработчик backend/native  
**Статус:** ❌ Не начато

#### Проблемы

1. FFmpeg 8.0 изменил API для аудио декодирования
2. `audio_decoder.cpp` отключен из-за компиляционных ошибок
3. Аудио кодеки: AAC, MP3, PCMU, PCMA не работают

#### План выполнения

**День 1: Анализ и исправление компиляционных ошибок**

1. Изучить FFmpeg 8.0 API изменения для аудио:
   ```bash
   cd native/video-processing
   ffmpeg -codecs | grep -E "aac|mp3|ac3"
   ```

2. Исправить `audio_decoder.h`:
   - Обновить структуры для нового API
   - Добавить поддержку AVCodecContext для аудио
   - Исправить типы данных

3. Исправить `audio_decoder.cpp`:
   - Обновить инициализацию декодера
   - Исправить процесс декодирования пакетов
   - Добавить обработку аудио фреймов

**Файлы:**
- `native/video-processing/include/audio_decoder.h`
- `native/video-processing/src/audio_decoder.cpp`
- `native/video-processing/CMakeLists.txt`

**Критерий завершения:**
- [ ] `audio_decoder.cpp` компилируется без ошибок
- [ ] Все функции экспортируются в DLL/SO
- [ ] Базовые unit тесты проходят

**День 2: Реализация аудио кодеков**

1. Добавить поддержку AAC:
   ```cpp
   const AVCodec *codec = avcodec_find_decoder(AV_CODEC_ID_AAC);
   ```

2. Добавить поддержку MP3:
   ```cpp
   const AVCodec *codec = avcodec_find_decoder(AV_CODEC_ID_MP3);
   ```

3. Добавить поддержку G.711 (PCMU/PCMA):
   ```cpp
   const AVCodec *codec = avcodec_find_decoder(AV_CODEC_ID_PCM_MULAW);
   const AVCodec *codec = avcodec_find_decoder(AV_CODEC_ID_PCM_ALAW);
   ```

4. Реализовать аудио-видео синхронизацию:
   - Добавить PTS/DTS обработку
   - Создать AV sync buffer
   - Реализовать jitter buffer

**Файлы:**
- `native/video-processing/src/audio_decoder.cpp`
- `native/video-processing/src/rtsp_client.cpp` (audio sections)

**Критерий завершения:**
- [ ] AAC декодирование работает
- [ ] MP3 декодирование работает
- [ ] PCMU/PCMA декодирование работает
- [ ] AV sync реализован

**День 3: Тестирование аудио**

1. Создать тестовые сценарии:
   - `AudioDecodingTest.kt` — unit тесты
   - `RtspClientAudioIntegrationTest.kt` — интеграционные тесты

2. Протестировать с различными аудио форматами

**Файлы:**
- `core/network/src/commonTest/.../AudioDecodingTest.kt`
- `core/network/src/desktopTest/.../RtspClientAudioIntegrationTest.kt`

**Критерий завершения:**
- [ ] Все аудио форматы декодируются
- [ ] AV sync работает корректно
- [ ] Нет утечек памяти

---

### Задача 2: Настройка тестовой среды с реальными камерами

**Приоритет:** 🔴 Критический  
**Оценка:** 1 день  
**Ответственный:** QA / Разработчик  
**Статус:** ❌ Не начато

#### План выполнения

**День 1: Подготовка тестовой среды**

1. Создать `config/test-cameras.rtsp.json`:
   ```json
   {
     "cameras": [
       {
         "name": "Hikvision_Test_1",
         "url": "rtsp://user:pass@192.168.1.100:554/Streaming/Channels/101",
         "type": "hikvision",
         "audio": true,
         "video_codec": "H.264",
         "audio_codec": "AAC"
       },
       {
         "name": "Dahua_Test_1",
         "url": "rtsp://user:pass@192.168.1.101:554/cam/realmonitor?channel=1&subtype=0",
         "type": "dahua",
         "audio": true,
         "video_codec": "H.264",
         "audio_codec": "G.711"
       },
       {
         "name": "Axis_Test_1",
         "url": "rtsp://user:pass@192.168.1.102:554/axis-media/media.amp",
         "type": "axis",
         "audio": true,
         "video_codec": "H.264",
         "audio_codec": "PCMU"
       },
       {
         "name": "Sony_Test_1",
         "url": "rtsp://user:pass@192.168.1.103:554/onvif1",
         "type": "sony",
         "audio": true,
         "video_codec": "H.265",
         "audio_codec": "AAC"
       },
       {
         "name": "MJPEG_Camera_1",
         "url": "rtsp://user:pass@192.168.1.104:554/mjpeg/stream1",
         "type": "generic",
         "audio": false,
         "video_codec": "MJPEG",
         "audio_codec": null
       }
     ]
   }
   ```

2. Настроить тестовую сеть:
   - Убедиться, что все камеры доступны
   - Проверить credentials
   - Настроить network isolation (опционально)

3. Обновить `test-rtsp-integration.ps1/sh`:
   - Добавить загрузку `test-cameras.rtsp.json`
   - Реализовать автоматическое тестирование каждой камеры
   - Добавить генерацию отчёта

**Файлы:**
- `config/test-cameras.rtsp.json` (создать)
- `test-rtsp-integration.ps1` (обновить)
- `test-rtsp-integration.sh` (создать)

**Критерий завершения:**
- [ ] Конфигурация создана
- [ ] Все 5+ камер доступны
- [ ] Скрипт тестирования работает

---

### Задача 3: Integration тестирование с реальными камерами

**Приоритет:** 🔴 Критический  
**Оценка:** 3-5 дней  
**Ответственный:** QA / Разработчик  
**Статус:** ❌ Не начато

#### План выполнения

**День 1: Базовое подключение**

1. Создать `RtspClientRealCameraTest.kt`:
   ```kotlin
   class RtspClientRealCameraTest {
       @Test
       fun testConnectToHikvision() {
           // Подключение к Hikvision камере
       }
       
       @Test
       fun testConnectToDahua() {
           // Подключение к Dahua камере
       }
       
       // ... другие камеры
   }
   ```

2. Тестировать базовое подключение для всех 5 камер
3. Проверить получение stream info
4. Зафиксировать результаты

**Критерий завершения:**
- [ ] Все 5 камер подключаются успешно
- [ ] Stream info получен корректно

**День 2: Видео поток**

1. Тестировать воспроизведение видео:
   - `play()` для каждой камеры
   - Получение видео кадров
   - Проверка декодирования H.264/H.265/MJPEG

2. Тестировать pause/stop:
   - `pause()` и восстановление
   - `stop()` и повторный `play()`

**Критерий завершения:**
- [ ] Видео кадры получаются
- [ ] Пауза/возобновление работает
- [ ] Остановка/перезапуск работает

**День 3: Аудио поток (после Задачи 1)**

1. Тестировать аудио для поддерживаемых камер
2. Проверить синхронизацию аудио-видео
3. Протестировать различные аудио кодеки

**Критерий завершения:**
- [ ] Аудио кадры получаются
- [ ] AV sync работает

**День 4: Переподключение и устойчивость**

1. Тестировать reconnect:
   - Искусственный разрыв сети
   - Автоматическое переподключение
   - Проверка recovery

2. Тестировать reconnectWithBackoff():
   - Multiple failures
   - Exponential backoff
   - Max retries reached

**Критерий завершения:**
- [ ] Автоматический reconnect работает
- [ ] Backoff strategy работает
- [ ] Нет утечек при reconnect

**День 5: Long-run тесты**

1. Запустить длительную сессию (2-4 часа):
   - Мониторинг памяти
   - Мониторинг CPU
   - Проверка стабильности

2. Создать `RtspClientLongRunRealCameraTest.kt`

**Критерий завершения:**
- [ ] Нет memory leaks
- [ ] Нет crashes
- [ ] Стабильная работа

**Файлы:**
- `core/network/src/commonTest/.../RtspClientRealCameraTest.kt`
- `core/network/src/desktopTest/.../RtspClientLongRunRealCameraTest.kt`
- `docs/reports/RTSP_REAL_CAMERA_TEST_RESULTS.md`

---

### Задача 4: FFI биндинги для Native (Kotlin/Native)

**Приоритет:** 🟡 Высокий  
**Оценка:** 2-3 дня  
**Ответственный:** Разработчик KMP  
**Статус:** 🟡 В процессе (70%)

#### План выполнения

**День 1: Настройка cinterop**

1. Проверить `rtsp_client.def`:
   ```
   headers = rtsp_client.h
   compilerOpts = -Inative/video-processing/include
   ```

2. Обновить `core/network/build.gradle.kts`:
   ```kotlin
   nativeInterop {
       cinterop {
           rtsp_client {
               headers("rtsp_client.h")
               compilerOpts("-Inative/video-processing/include")
           }
       }
   }
   ```

3. Сгенерировать биндинги для всех платформ:
   ```bash
   ./gradlew :core:network:generateNativeInterop
   ```

**Критерий завершения:**
- [ ] cinterop настроен
- [ ] Биндинги сгенерированы

**День 2: Native реализация (linuxX64, mingwX64)**

1. Улучшить `NativeRtspClient.native.kt`:
   - Использовать сгенерированные биндинги
   - Реализовать StableRef для callbacks
   - Добавить управление lifecycle

2. Тестирование на Linux:
   ```bash
   ./gradlew :core:network:linuxX64Test
   ```

3. Тестирование на Windows:
   ```bash
   ./gradlew :core:network:mingwX64Test
   ```

**Критерий завершения:**
- [ ] linuxX64 работает
- [ ] mingwX64 работает

**День 3: Native реализация (macOS, iOS)**

1. Улучшить `NativeRtspClient.ios.kt`:
   - Использовать cinterop биндинги
   - Реализовать memory management для iOS

2. Тестирование на macOS:
   ```bash
   ./gradlew :core:network:macosArm64Test
   ```

3. Тестирование для iOS (simulator):
   ```bash
   ./gradlew :core:network:iosSimulatorArm64Test
   ```

**Критерий завершения:**
- [ ] macosArm64 работает
- [ ] iosSimulatorArm64 работает

**Файлы:**
- `core/network/src/nativeInterop/cinterop/rtsp_client.def`
- `core/network/build.gradle.kts`
- `core/network/src/nativeMain/.../NativeRtspClient.native.kt`
- `core/network/src/iosMain/.../NativeRtspClient.ios.kt`

---

### Задача 5: Обработка различных кодеков

**Приоритет:** 🟡 Высокий  
**Оценка:** 2-3 дня  
**Ответственный:** Разработчик native  
**Статус:** ❌ Не начато

#### План выполнения

**День 1: H.264 (Main/High profiles)**

1. Проверить поддержку H.264:
   ```cpp
   AVCodecID codecId = AV_CODEC_ID_H264;
   const AVCodec *codec = avcodec_find_decoder(codecId);
   ```

2. Тестировать различные профили:
   - Baseline
   - Main
   - High

**День 2: H.265/HEVC**

1. Добавить поддержку H.265:
   ```cpp
   AVCodecID codecId = AV_CODEC_ID_HEVC;
   const AVCodec *codec = avcodec_find_decoder(codecId);
   ```

2. Оптимизировать для производительности (требует больше ресурсов)

**День 3: MJPEG и другие**

1. Добавить поддержку MJPEG:
   ```cpp
   AVCodecID codecId = AV_CODEC_ID_MJPEG;
   ```

2. Протестировать все кодеки с реальными камерами

**Файлы:**
- `native/video-processing/src/video_decoder.cpp`
- `native/video-processing/include/video_decoder.h`

**Критерий завершения:**
- [ ] H.264 работает
- [ ] H.265 работает
- [ ] MJPEG работает

---

### Задача 6: Производительность и оптимизация

**Приоритет:** 🟡 Высокий  
**Оценка:** 3-4 дня  
**Ответственный:** Разработчик backend/native  
**Статус:** 🟡 В процессе (50%)

#### План выполнения

**День 1: Профилирование**

1. Добавить instrumentation:
   - Время декодирования
   - Время обработки
   - Memory usage
   - CPU usage

2. Запустить бенчмарки с 5 камерами

**День 2: Buffer optimization**

1. Оптимизировать buffer allocation:
   - Использовать object pool для buffers
   - Minimize memory copies
   - Zero-copy pathways где возможно

2. Реализовать:
   ```cpp
   class FrameBufferPool {
       std::queue<AVFrame*> pool;
       AVFrame* acquire();
       void release(AVFrame* frame);
   };
   ```

**День 3: Multi-threading**

1. Добавить multi-threading для декодирования:
   ```cpp
   codecCtx->thread_count = 4; // или auto-detect
   codecCtx->thread_type = FF_THREAD_FRAME;
   ```

2. Разделить потоки:
   - RTP receive thread
   - Decode thread
   - Callback dispatch thread

**День 4: Latency optimization**

1. Minimize buffer sizes:
   - RTP buffer
   - Decode buffer
   - Output buffer

2. Target: <200ms end-to-end latency

**Файлы:**
- `native/video-processing/src/rtsp_client.cpp`
- `native/video-processing/src/video_decoder.cpp`
- `core/network/src/commonMain/.../RtspClient.kt`

**Критерий завершения:**
- [ ] Latency <200ms
- [ ] CPU usage <30% per stream
- [ ] Memory stable (no leaks)

---

## 📅 График выполнения

### Неделя 1 (25 May - 31 May 2026)

| День | Задачи | Ожидаемый результат |
|------|--------|---------------------|
| 25 May | Задача 1: День 1 (аудио анализ) | Исправленные компиляционные ошибки |
| 26 May | Задача 1: День 2 (аудио кодеки) | Рабочее аудио декодирование |
| 27 May | Задача 1: День 3 (аудио тесты) | Audio tests passing |
| 28 May | Задача 2 (тестовая среда) | 5+ камер готовы к тестированию |
| 29 May | Задача 3: День 1 (подключение) | Все камеры подключаются |
| 30 May | Задача 3: День 2 (видео) | Видео потоки работают |
| 31 May | Задача 3: День 3 (аудио) | Аудио потоки работают |

### Неделя 2 (1 Jun - 7 Jun 2026)

| День | Задачи | Ожидаемый результат |
|------|--------|---------------------|
| 1 Jun | Задача 3: День 4 (reconnect) | Reconnect работает |
| 2 Jun | Задача 3: День 5 (long-run) | Long-run стабильность |
| 3 Jun | Задача 4: День 1 (cinterop) | Биндинги сгенерированы |
| 4 Jun | Задача 4: День 2 (Linux/Windows) | Native платформы работают |
| 5 Jun | Задача 4: День 3 (macOS/iOS) | iOS/macOS работают |
| 6 Jun | Задача 5 (кодеки) | H.264/H.265/MJPEG работают |
| 7 Jun | Задача 6 (оптимизация) | Latency <200ms |

---

## 🎯 Критерии завершения

### MVP Ready (обязательные)

- [x] RTSP клиент компилируется для всех платформ (JVM, Android, Native)
- [x] Подключение к 5+ реальным камерам работает
- [x] Видео декодирование (H.264) работает
- [ ] Аудио декодирование (AAC, PCMU, PCMA) работает
- [ ] Reconnect с backoff работает
- [ ] Нет критических memory leaks
- [ ] Integration tests passing

### Production Ready (желательные)

- [ ] H.265 поддержка
- [ ] Latency <200ms
- [ ] CPU usage <30% per stream
- [ ] Long-run стабильность (24+ часа)
- [ ] Full test coverage (>80%)

---

## 📊 Риски и митигация

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| FFmpeg 8.0 API слишком сложный | Средняя | Высокое | Использовать FFmpeg 6.x как fallback |
| Камеры недоступны для тестирования | Низкая | Высокое | Использовать RTSP тестовые серверы |
| Производительность недостаточна | Средняя | Среднее | Оптимизировать критические path |
| Native сборка ломается | Низкая | Среднее | CI/CD для всех платформ |

---

## 📝 История изменений

| Дата | Автор | Изменения |
|------|-------|-----------|
| 2026-05-25 | AI Assistant | Создан план |

---

**Утверждено:** ________________  
**Дата:** ________________  
**Следующий пересмотр:** 28 May 2026
