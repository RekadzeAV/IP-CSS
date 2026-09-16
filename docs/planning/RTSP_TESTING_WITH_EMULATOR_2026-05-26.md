# Тестирование RTSP с аудио эмулятором

**Дата:** 26 May 2026  
**Задача:** 1.2 — Тестирование аудио декодирования  
**Статус:** 🟡 Готово к тестированию  
**Альтернатива:** RTSP эмулятор вместо реальных камер

---

## 🎯 Цель

Провести тестирование аудио декодирования (AAC, PCMU, PCMA) без реальных IP камер, используя RTSP эмулятор.

---

## 📋 Варианты тестирования

### Вариант 1: RTSP эмулятор (рекомендуемый)

**Преимущества:**
- ✅ Не требует реальных камер
- ✅ Полный контроль над параметрами потока
- ✅ Поддержка всех кодеков: AAC, PCMU, PCMA
- ✅ Мгновенный запуск
- ✅ Повторяемые тесты

**Недостатки:**
- ⚠️ Упрощённая реализация RTP
- ⚠️ Может отличаться от реальных камер

### Вариант 2: Реальные камеры

**Преимущества:**
- ✅ Реальные условия тестирования
- ✅ Проверка совместимости с производителями

**Недостатки:**
- ⚠️ Требуется оборудование
- ⚠️ Зависит от доступности камер

---

## 🚀 Быстрый старт (RTSP эмулятор)

### Шаг 1: Запуск эмулятора

```powershell
# Запуск с AAC аудио (по умолчанию)
python scripts/rtsp-audio-test-server.py

# Запуск с PCMU аудио
python scripts/rtsp-audio-test-server.py --audio-codec pcmu

# Запуск на другом порту
python scripts/rtsp-audio-test-server.py --port 8555

# Показать帮助
python scripts/rtsp-audio-test-server.py --help
```

**Ожидаемый вывод:**
```
Starting RTSP test server on port 8554...
Audio codec: aac
Sample rate: 48000 Hz
Channels: 2
Video: 640x480 @ 25fps
SDP:
v=0
o=- 1234567890 1234567890 IN IP4 127.0.0.1
...
RTSP server listening on port 8554
Press Ctrl+C to stop
```

### Шаг 2: Подключение с помощью ffprobe

```powershell
# Проверка SDP
ffprobe rtsp://127.0.0.1:8554/stream/

# Проверка потоков
ffprobe -i rtsp://127.0.0.1:8554/stream/ -show_streams

# Просмотр с аудио
ffplay rtsp://127.0.0.1:8554/stream/
```

**Ожидаемый вывод (ffprobe -show_streams):**
```
[STREAM]
index=0
codec_name=h264
codec_type=video
width=640
height=480
r_frame_rate=25/1
[/STREAM]
[STREAM]
index=1
codec_name=aac
codec_type=audio
sample_rate=48000
channels=2
[/STREAM]
```

### Шаг 3: Тестирование с нашим RTSP клиентом

**Файл:** `config/test-cameras.rtsp.json`

Добавить тестовую камеру:
```json
{
  "name": "Emulator_AAC",
  "url": "rtsp://127.0.0.1:8554/stream/",
  "type": "emulator",
  "audio": true,
  "video_codec": "H.264",
  "audio_codec": "AAC",
  "rtsp_transport": "udp",
  "timeout_ms": 10000
}
```

**Запуск тестирования:**
```powershell
.\scripts\test-rtsp-real-cameras.ps1 -CameraName "Emulator_AAC" -FullTest
```

---

## 🧪 Сценарии тестирования

### Сценарий 1: AAC аудио

**Команда:**
```powershell
# Запуск эмулятора
python scripts/rtsp-audio-test-server.py --audio-codec aac --port 8554

# Тестирование подключения
.\scripts\test-rtsp-real-cameras.ps1 -CameraName "Emulator_AAC"
```

**Проверяем:**
- [ ] SDP парсится корректно
- [ ] AAC декодер инициализируется
- [ ] Аудио фреймы декодируются
- [ ] Sample rate = 48000 Hz
- [ ] Channels = 2

### Сценарий 2: PCMU аудио

**Команда:**
```powershell
# Запуск эмулятора
python scripts/rtsp-audio-test-server.py --audio-codec pcmu --port 8554

# Тестирование
.\scripts\test-rtsp-real-cameras.ps1 -CameraName "Emulator_PCMU"
```

**Проверяем:**
- [ ] PCMU декодер инициализируется
- [ ] Декодирование в PCM S16
- [ ] Sample rate = 8000 Hz
- [ ] Channels = 1

### Сценарий 3: PCMA аудио

**Команда:**
```powershell
# Запуск эмулятора
python scripts/rtsp-audio-test-server.py --audio-codec pcma --port 8554

# Тестирование
.\scripts\test-rtsp-real-cameras.ps1 -CameraName "Emulator_PCMA"
```

**Проверяем:**
- [ ] PCMA декодер инициализируется
- [ ] Декодирование в PCM S16
- [ ] Sample rate = 8000 Hz
- [ ] Channels = 1

### Сценарий 4: Видео-только (без аудио)

**Команда:**
```powershell
# Запуск эмулятора без аудио (модифицировать код)
python scripts/rtsp-audio-test-server.py --port 8554

# Тестирование с SkipAudio
.\scripts\test-rtsp-real-cameras.ps1 -CameraName "Emulator_VideoOnly" -SkipAudio
```

**Проверяем:**
- [ ] Видео работает корректно
- [ ] Нет ошибок из-за отсутствия аудио

---

## 📊 Ожидаемые результаты

### Метрики успеха

| Метрика | Цель | Статус |
|---------|------|--------|
| Подключение к эмулятору | 100% | ⏳ Планируется |
| SDP парсинг | 100% | ⏳ Планируется |
| AAC декодирование | 100% | ⏳ Планируется |
| PCMU декодирование | 100% | ⏳ Планируется |
| PCMA декодирование | 100% | ⏳ Планируется |
| Нет crash'ов | 100% | ⏳ Планируется |

### Логи для проверки

**Успешное декодирование AAC:**
```
[INFO] Audio codec: AAC
[INFO] Sample rate: 48000 Hz
[INFO] Channels: 2
[INFO] AAC decoder initialized
[INFO] Audio frame decoded: 2048 samples
[INFO] Audio callback invoked
```

**Успешное декодирование PCMU:**
```
[INFO] Audio codec: PCMU
[INFO] Sample rate: 8000 Hz
[INFO] Channels: 1
[INFO] G.711 decoder initialized
[INFO] Audio frame decoded: 200 samples
[INFO] Audio callback invoked
```

---

## 🔧 Диагностика

### Проверка запуска эмулятора

```powershell
# Проверка порта
Test-NetConnection -ComputerName 127.0.0.1 -Port 8554

# Проверка процесса
Get-Process python | Where-Object { $_.CommandLine -like "*rtsp-audio-test-server*" }
```

### Отладка RTSP подключения

```powershell
# Включить verbose логи в RTSP клиенте
# (добавить флаг --verbose если поддерживается)

# Перехват RTP пакетов с Wireshark
# Фильтр: rtp && ip.addr == 127.0.0.1
```

### Проверка декодирования

```cpp
// Добавить логи в rtsp_client.cpp (строки 2059-2122)
#ifdef ENABLE_FFMPEG
    if (stream.codec == "AAC" && !stream.aacDecoder) {
        printf("[DEBUG] Initializing AAC decoder\n");
        // ...
    }
#endif
```

---

## 📝 План тестирования

### День 1 (26 May 2026)

**Утро:**
1. [ ] Запустить эмулятор с AAC
2. [ ] Проверить подключение через ffprobe
3. [ ] Запустить базовое тестирование

**День:**
4. [ ] Запустить эмулятор с PCMU
5. [ ] Проверить PCMU декодирование
6. [ ] Запустить эмулятор с PCMA
7. [ ] Проверить PCMA декодирование

**Вечер:**
8. [ ] Собрать результаты
9. [ ] Зафиксировать найденные проблемы
10. [ ] Обновить отчёт

### День 2 (27 May 2026)

**Утро:**
1. [ ] Повторное тестирование с исправлениями
2. [ ] Тестирование reconnect
3. [ ] Long-run тест (5-10 минут)

**День:**
4. [ ] Тестирование с реальными камерами (если доступны)
5. [ ] Сравнение эмулятор vs реальные камеры
6. [ ] Финальный отчёт

---

## 🎯 Критерии завершения задачи 1.2

### MVP Ready (обязательные)

- [ ] Эмулятор работает стабильно
- [ ] AAC декодирование с эмулятором подтверждено
- [ ] PCMU декодирование с эмулятором подтверждено
- [ ] PCMA декодирование с эмулятором подтверждено
- [ ] Отчёт с результатами тестирования создан
- [ ] Выявленные проблемы задокументированы

### Production Ready (желательные)

- [ ] Тестирование с 3+ реальными камерами
- [ ] Reconnect тестирование пройдено
- [ ] Long-run тест (30+ минут) без проблем
- [ ] Нет memory leaks подтверждено

---

## 📚 Связанные документы

- [scripts/rtsp-audio-test-server.py](../../scripts/rtsp-audio-test-server.py) — RTSP эмулятор
- [RTSP_CAMERA_TESTING_PLAN_2026-05-26.md](RTSP_CAMERA_TESTING_PLAN_2026-05-26.md) — Общий план тестирования
- [test-rtsp-real-cameras.ps1](../../scripts/test-rtsp-real-cameras.ps1) — Скрипт тестирования
- [AUDIO_DECODER_FIX_COMPLETION_2026-05-26.md](../reports/AUDIO_DECODER_FIX_COMPLETION_2026-05-26.md) — Отчёт задачи 1.1

---

**Статус:** Готово к тестированию  
**Следующий шаг:** Запустить эмулятор и проверить подключение  
**Ответственный:** AI Assistant
