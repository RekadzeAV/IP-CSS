# Полевая валидация с локальными камерами

**Дата:** 2026-05-28  
**Версия:** 1.0

---

## 🎯 Цель

Проведение полевой валидации RTSP, HLS и Screenshot pipeline с реальными камерами в локальной сети.

---

## 📋 Предварительные требования

### 1. Камеры в локальной сети

- Камеры должны быть доступны по сети (ping)
- RTSP поток должен быть доступен (порт 554 по умолчанию)
- Учётные данные для доступа к камерам

### 2. Сетевая доступность

```powershell
# Проверка доступности камеры
Test-NetConnection -ComputerName 192.168.1.100 -Port 554
```

### 3. API сервер запущен

```powershell
# Проверка сервера
curl http://localhost:8080/api/v1/health
```

---

## 🚀 Быстрый старт

### Шаг 1: Обнаружение камер

```powershell
# Сканирование локальной сети
.\scripts\discover-rtsp-cameras.ps1 -Subnet 192.168.1.0/24

# Результаты: diagnostics/discovered-cameras.json
```

### Шаг 2: Настройка конфигурации

1. Скопируйте шаблон:
```powershell
Copy-Item config/test-cameras-local-network.example.json config/test-cameras-local-network.json
```

2. Отредактируйте файл `config/test-cameras-local-network.json`:
   - Замените `CHANGE_ME` на реальные пароли
   - Обновите IP адреса камер
   - Укажите правильные RTSP URL

### Шаг 3: Запуск тестов

```powershell
# Полный цикл тестирования
.\scripts\field-validation.ps1 -RunAllTests

# Или по отдельности:
.\scripts\field-validation.ps1 -RunRtspTests
.\scripts\field-validation.ps1 -RunHlsTests
.\scripts\field-validation.ps1 -RunScreenshotTests
```

---

## 📊 Тестируемые сценарии

### RTSP тестирование

| Тест | Цель | Критерий успеха |
|------|------|-----------------|
| Connection | Проверка подключения | Подключение <5s |
| Video | Проверка видеопотока | Первый кадр <3s |
| Audio | Проверка аудио (если есть) | AAC/PCMU/PCMA декодирование |
| Reconnect | Восстановление после разрыва | Reconnect <5s |
| Long-run | Стабильность 2min+ | Нет сбоев |

### HLS тестирование

| Тест | Цель | Критерий успеха |
|------|------|-----------------|
| Startup | Запуск HLS потока | Плейлист создан |
| Long-run | Стабильность (120s+) | Нет падений процесса |
| Cleanup | Очистка при остановке | Процессы завершены |
| Reconnect | Перезапуск потока | Успешный reconnect |

### Screenshot тестирование

| Тест | Цель | Критерий успеха |
|------|------|-----------------|
| Basic | Базовый захват | JPEG/PNG создан |
| Timeout | Обработка таймаутов | Корректная ошибка |
| Sequential | Последовательные захваты | 3 кадра без сбоев |

---

## 🔧 Настройка конфигурации

### Пример конфигурации для одной камеры:

```json
{
  "cameras": [
    {
      "id": "my-camera-1",
      "name": "Living Room",
      "rtspUrl": "rtsp://192.168.1.100:554/stream1",
      "username": "admin",
      "password": "mypassword",
      "protocol": "TCP",
      "timeoutMs": 5000
    }
  ]
}
```

### Распространённые RTSP URL форматы:

#### HIKVISION:
```
rtsp://admin:password@192.168.1.100:554/h264_stream
```

#### Dahua:
```
rtsp://admin:password@192.168.1.101:554/cam/realmonitor?channel=1&subtype=0
```

#### Axis:
```
rtsp://admin:password@192.168.1.102:554/axis-media/media.amp
```

#### Generic:
```
rtsp://admin:password@192.168.1.103:554/stream1
rtsp://admin:password@192.168.1.104:554/live.sdp
```

---

## 📁 Структура отчётов

```
diagnostics/field-validation/
├── rtsp-tests/
│   ├── rtsp-test-YYYYMMDD-HHMMSS.md
│   └── rtsp-test-YYYYMMDD-HHMMSS.json
├── hls-tests/
│   ├── hls-runtime-stability-test-YYYYMMDD-HHMMSS.md
│   └── hls-runtime-stability-test-YYYYMMDD-HHMMSS.json
├── screenshot-tests/
│   ├── screenshot-pipeline-test-YYYYMMDD-HHMMSS.md
│   └── screenshot-pipeline-test-YYYYMMDD-HHMMSS.json
├── discovered-cameras.json
└── field-validation-report-YYYYMMDD-HHMMSS.md
```

---

## 🎯 Критерии успеха

### Обязательные (MVP):

- [ ] Подключение к ≥90% камер
- [ ] Видеопоток работает на всех камерах
- [ ] Long-run тест (120s) без сбоев
- [ ] Reconnect работает после разрыва
- [ ] Screenshot захват работает

### Рекомендуемые:

- [ ] Аудио декодирование (если камера поддерживает)
- [ ] HLS TTFF <500ms
- [ ] Reconnect time <5s
- [ ] Cleanup 100% процессов

---

## 🐛 Устранение проблем

### Проблема: "RTSP port not open"

**Решение:**
1. Проверьте IP адрес камеры
2. Проверьте порт RTSP (иногда 5540, 8554)
3. Проверьте брандмауэр

### Проблема: "Authentication failed"

**Решение:**
1. Проверьте логин/пароль в конфигурации
2. Убедитесь, что камера поддерживает RTSP auth
3. Проверьте права пользователя на камере

### Проблема: "No video stream"

**Решение:**
1. Проверьте поддерживаемые кодеки (H.264/H.265)
2. Попробуйте другой transport (TCP/UDP)
3. Проверьте разрешение потока

### Проблема: "HLS process crashed"

**Решение:**
1. Проверьте доступность FFmpeg
2. Проверьте логи FFmpeg в `data/logs/`
3. Попробуйте уменьшить битрейт

---

## 📈 Метрики успеха

| Метрика | Цель | Измерение |
|---------|------|-----------|
| Connection success rate | >95% | RTSP tests |
| First frame time | <3s | RTSP tests |
| Long-run stability | 24h+ | Long-run tests |
| Reconnect success rate | >95% | Reconnect tests |
| HLS TTFF | <500ms | HLS tests |
| Screenshot success rate | >95% | Screenshot tests |

---

## 🔄 Повторное тестирование

После изменений в коде:

```powershell
# Запустить все тесты снова
.\scripts\field-validation.ps1 -RunAllTests

# С кастомной длительностью HLS
.\scripts\field-validation.ps1 -RunAllTests -HlsDurationSeconds 600
```

---

## 📚 Связанная документация

- [RTSP Testing Report](../reports/W1_3_RTSP_TESTING_REPORT.md)
- [HLS Pipeline Report](../reports/W2_1_W2_2_HLS_SCREENSHOT_REPORT.md)
- [Audio Implementation](../reports/W1_2_AUDIO_IMPLEMENTATION_REPORT.md)

---

## ✅ Чеклист готовности

### Перед началом:

- [ ] Камеры доступны в сети
- [ ] RTSP URL и учётные данные известны
- [ ] Конфигурация обновлена
- [ ] API сервер запущен
- [ ] Директория diagnostics/ создана

### После тестирования:

- [ ] Все тесты прошли успешно
- [ ] Отчёты сгенерированы
- [ ] Проблемы задокументированы
- [ ] Метрики собраны
- [ ] Рекомендации зафиксированы

---

*Документ создан: 2026-05-28*  
*Версия: 1.0*
