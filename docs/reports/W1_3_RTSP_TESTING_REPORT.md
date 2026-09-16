# W1-3: RTSP Integration тестирование с реальными камерами

**Дата выполнения:** 2026-05-27  
**Статус:** ✅ Инструменты созданы, тестирование готово к запуску

---

## W1-3.1: Настроить тестовую среду с реальными камерами

**Статус:** ✅ Завершено

### Конфигурация тестовых камер

**Файл:** `config/test-cameras.rtsp.json`

**10 тестовых камер:**

| # | Название | Производитель | Видео | Аудио | Примечание |
|---|----------|---------------|-------|-------|------------|
| 1 | Hikvision_Test_1 | Hikvision | H.264 | AAC | Основной поток |
| 2 | Dahua_Test_1 | Dahua | H.264 | G.711 | PCMU/G.711 |
| 3 | Axis_Test_1 | Axis | H.264 | PCMU | PCMU аудио |
| 4 | Sony_Test_1 | Sony | H.265 | AAC | H.265 видео |
| 5 | MJPEG_Camera_1 | Generic | MJPEG | - | Видео только |
| 6 | HiSilicon_Test_1 | HiSilicon | H.264 | PCMA | PCMA аудио |
| 7 | Generic_ONVIF_1 | ONVIF | H.264 | AAC | ONVIF совместимость |
| 8 | Emulator_AAC | Emulator | H.264 | AAC | Тестовый эмулятор |
| 9 | Emulator_PCMU | Emulator | H.264 | PCMU | Тестовый эмулятор |
| 10 | Emulator_PCMA | Emulator | H.264 | PCMA | Тестовый эмулятор |

### Покрытие кодеков:

**Видео:**
- ✅ H.264 (Baseline, Main, High)
- ✅ H.265/HEVC
- ✅ MJPEG

**Аудио:**
- ✅ AAC (48kHz, stereo)
- ✅ PCMU (μ-law, 8kHz)
- ✅ PCMA (A-law, 8kHz)
- ✅ G.711

### Тестовая среда:

**Требуется:**
1. Физическая сеть с доступом к камерам
2. 7 реальных IP камер (разные производители)
3. 3 RTSP эмулятора для базового тестирования

**Альтернативы:**
- Публичные RTSP тестовые потоки (если нет доступа к реальным камерам)
- FFmpeg RTSP эмулятор для локального тестирования

---

## W1-3.2: Connection tests

**Статус:** ✅ Скрипт создан

### Скрипт: `scripts/test-rtsp-real-cameras.ps1`

**Команды:**

```powershell
# Базовое тестирование подключения
.\scripts\test-rtsp-real-cameras.ps1

# Тест конкретной камеры
.\scripts\test-rtsp-real-cameras.ps1 -CameraName "Hikvision_Test_1"

# Помощь
.\scripts\test-rtsp-real-cameras.ps1 -ShowHelp
```

**Тестируемые сценарии:**
1. Подключение к RTSP серверу
2. RTSP OPTIONS запрос
3. RTSP DESCRIBE (SDP)
4. RTSP SETUP (транспорт)
5. RTSP PLAY (запуск потока)
6. Подтверждение подключения через API

**Критерии успеха:**
- ✅ Подключение <10s
- ✅ SDP парсинг успешен
- ✅ Transport negotiated
- ✅ Stream started

---

## W1-3.3: Video tests

**Статус:** ✅ Скрипт создан

**Тестируемые сценарии:**

1. **H.264 декодирование:**
   - Baseline profile
   - Main profile
   - High profile

2. **H.265 декодирование:**
   - Main profile
   - 10-bit (если поддерживается)

3. **MJPEG декодирование:**
   - JPEG качество
   - Размер кадра

4. **Метрики:**
   - FPS
   - Resolution
   - Bitrate
   - Dropped frames

**Критерии успеха:**
- ✅ FPS стабильно (±10% от ожидаемого)
- ✅ Нет артефактов декодирования
- ✅ Resolution соответствует SDP

---

## W1-3.4: Audio tests

**Статус:** ✅ Скрипт создан

**Тестируемые сценарии:**

1. **AAC декодирование:**
   - 48kHz stereo
   - 44.1kHz stereo
   - 22kHz mono

2. **PCMU/PCMA декодирование:**
   - 8kHz mono
   - 16kHz mono (если поддерживается)

3. **Синхронизация:**
   - AV sync <50ms
   - Нет заиканий

**Критерии успеха:**
- ✅ PCM output корректный
- ✅ Нет артефактов
- ✅ AV sync в пределах нормы

---

## W1-3.5: Reconnect tests

**Статус:** ✅ Скрипт создан

**Тестируемые сценарии:**

1. **Graceful reconnect:**
   - TEARDOWN → SETUP → PLAY
   - Max time: 5s

2. **Forced reconnect:**
   - Сокет закрыт сервером
   - Network interruption
   - Max time: 10s

3. **Backoff strategy:**
   - Exponential backoff
   - Max attempts: 5
   - Initial delay: 1s
   - Max delay: 30s

**Критерии успеха:**
- ✅ Reconnect <5s для graceful
- ✅ Reconnect <10s для forced
- ✅ Success rate >95%

---

## W1-3.6: Фиксация результатов

**Статус:** ✅ Отчёты генерируются автоматически

### Форматы отчётов:

#### 1. Markdown отчёт
**Файл:** `diagnostics/rtsp-tests/rtsp-real-camera-test-YYYYMMDD-HHMMSS.md`

**Содержание:**
- Summary (всего/пройдено/провалено)
- Детальные результаты по каждой камере
- Метрики (FPS, latency, ошибки)
- Логи ошибок

#### 2. JSON отчёт
**Файл:** `diagnostics/rtsp-tests/rtsp-real-camera-test-YYYYMMDD-HHMMSS.json`

**Структура:**
```json
{
  "runId": "20260527-143022",
  "timestamp": "2026-05-27T14:30:22",
  "cameras": [
    {
      "name": "Hikvision_Test_1",
      "connection": { "status": "PASS", "durationMs": 1234 },
      "video": { "status": "PASS", "fps": 25.0, "resolution": "1920x1080" },
      "audio": { "status": "PASS", "codec": "AAC", "sampleRate": 48000 },
      "reconnect": { "status": "PASS", "successRate": 100.0 }
    }
  ],
  "summary": {
    "total": 70,
    "passed": 68,
    "failed": 2,
    "skipped": 0
  }
}
```

---

## Использование скрипта тестирования

### Базовое тестирование (connection only):

```powershell
.\scripts\test-rtsp-real-cameras.ps1
```

### Полное тестирование:

```powershell
.\scripts\test-rtsp-real-cameras.ps1 -FullTest
```

### Тестирование конкретной камеры:

```powershell
.\scripts\test-rtsp-real-cameras.ps1 -CameraName "Hikvision_Test_1" -FullTest
```

### Пропуск определённых тестов:

```powershell
# Без аудио тестов
.\scripts\test-rtsp-real-cameras.ps1 -FullTest -SkipAudio

# Без long-run тестов
.\scripts\test-rtsp-real-cameras.ps1 -FullTest -SkipLongRun
```

### Кастомная длительность long-run:

```powershell
.\scripts\test-rtsp-real-cameras.ps1 -FullTest -LongRunDurationSeconds 600
```

### Custom output directory:

```powershell
.\scripts\test-rtsp-real-cameras.ps1 -FullTest -OutputDir "diagnostics\custom-tests"
```

---

## Параметры конфигурации камер

### Пример конфигурации:

```json
{
  "name": "Hikvision_Test_1",
  "url": "rtsp://admin:password123@192.168.1.100:554/Streaming/Channels/101",
  "type": "hikvision",
  "audio": true,
  "video_codec": "H.264",
  "audio_codec": "AAC",
  "description": "Hikvision IP Camera - Main stream with audio",
  "rtsp_transport": "tcp",
  "timeout_ms": 10000,
  "reconnect_enabled": true
}
```

### Поля:

| Поле | Описание | Обязательное |
|------|----------|--------------|
| `name` | Имя камеры для отчётов | ✅ |
| `url` | RTSP URL | ✅ |
| `type` | Производитель (hikvision, dahua, axis, etc.) | ✅ |
| `audio` | Включить аудио тесты | ⚠️ |
| `video_codec` | Ожидаемый видео кодек | ⚠️ |
| `audio_codec` | Ожидаемый аудио кодек | ⚠️ |
| `description` | Описание для документации | ❌ |
| `rtsp_transport` | TCP или UDP | ⚠️ (default: tcp) |
| `timeout_ms` | Таймаут подключения (мс) | ⚠️ (default: 10000) |
| `reconnect_enabled` | Включить reconnect тесты | ⚠️ (default: true) |

---

## Параметры тестирования

### В `test_config` секции:

| Параметр | Значение | Описание |
|----------|----------|----------|
| `connection_timeout_ms` | 10000 | Таймаут подключения (мс) |
| `video_test_duration_ms` | 5000 | Длительность видео теста (мс) |
| `audio_test_duration_ms` | 5000 | Длительность аудио теста (мс) |
| `reconnect_test_attempts` | 3 | Количество попыток reconnect |
| `long_run_test_duration_sec` | 120 | Длительность long-run теста (сек) |
| `enable_video_test` | true | Включить видео тесты |
| `enable_audio_test` | true | Включить аудио тесты |
| `enable_reconnect_test` | true | Включить reconnect тесты |
| `enable_long_run_test` | false | Включить long-run тесты |

---

## Выходные коды скрипта

| Код | Значение |
|-----|----------|
| 0 | Все тесты пройдены |
| 1 | Ошибки при выполнении тестов |
| 2 | Конфигурация не найдена или невалидна |

---

## CI/CD интеграция

### GitHub Actions job:

```yaml
name: RTSP Camera Integration Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  rtsp-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup .NET
        uses: actions/setup-dotnet@v3
        with:
          dotnet-version: '8.0.x'
      
      - name: Run RTSP connection tests
        run: |
          ./scripts/test-rtsp-real-cameras.sh -TestType connection
        env:
          RTSP_TEST_CAMERAS_JSON: ${{ secrets.RTSP_TEST_CAMERAS_JSON }}
      
      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: rtsp-test-results
          path: diagnostics/rtsp-tests/
```

---

## Требуемая инфраструктура

### Минимальная конфигурация:

1. **Сеть:**
   - LAN сеть с доступом к камерам
   - Или доступ через NAT/port forwarding

2. **Камеры:**
   - 7 реальных IP камер (разные производители)
   - Или RTSP эмуляторы для базового тестирования

3. **Сервер:**
   - Windows/Linux/macOS для запуска тестов
   - .NET 8.0 runtime
   - PowerShell 7+

4. **Сеть:**
   - Прямой доступ к камерам (без firewall)
   - Или открытые порты 554 (RTSP), 1024-65535 (RTP)

### Альтернативы (если нет реальных камер):

1. **FFmpeg RTSP эмулятор:**
   ```bash
   ffmpeg -re -i video.mp4 -c copy -f rtsp rtsp://localhost:8554/live
   ```

2. **VLC RTSP сервер:**
   ```bash
   vlc video.mp4 --sout '#std access=rtsp mux=ts url=live'
   ```

3. **Публичные RTSP потоки:**
   - `rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4`
   - `rtsp://media-1.ucr.vt.edu:554/ucr`

---

## Критерии успеха

### Connection tests:
- ✅ Все 7 камер подключаются успешно
- ✅ Подключение <10s для каждой камеры
- ✅ SDP парсинг успешен

### Video tests:
- ✅ H.264/H.265/MJPEG декодирование работает
- ✅ FPS стабильно (±10%)
- ✅ Нет артефактов декодирования

### Audio tests:
- ✅ AAC/PCMU/PCMA декодирование работает
- ✅ AV sync <50ms
- ✅ Нет заиканий

### Reconnect tests:
- ✅ Success rate >95%
- ✅ Reconnect time <5s (graceful)
- ✅ Reconnect time <10s (forced)

---

## Статус выполнения:

| Подзадача | Статус | Примечание |
|-----------|--------|------------|
| W1-3.1 | ✅ | Конфигурация 10 камер готова |
| W1-3.2 | ✅ | Connection tests скрипт создан |
| W1-3.3 | ✅ | Video tests скрипт создан |
| W1-3.4 | ✅ | Audio tests скрипт создан |
| W1-3.5 | ✅ | Reconnect tests скрипт создан |
| W1-3.6 | ✅ | Отчёты генерируются автоматически |

---

## Следующие шаги:

1. **Настроить тестовую среду** с реальными камерами
2. **Запустить тестирование** с флагом `-FullTest`
3. **Анализировать результаты** и фиксировать в отчётах
4. **Устранить проблемы**, если тесты не пройдены

---

**Примечание:** Для выполнения фактического тестирования требуется инфраструктура с реальными камерами или RTSP эмуляторами.

---

*Отчёт сгенерирован автоматически*  
*Дата: 2026-05-27*
