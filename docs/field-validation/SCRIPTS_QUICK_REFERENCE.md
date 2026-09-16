# Quick Reference: Скрипты Полевой Валидации

**Версия:** 1.0  
**Дата:** 2026-05-28

---

## 🚀 Быстрый Старт

### Полный цикл тестирования:
```powershell
.\scripts\field-validation.ps1 -RunAllTests
```

### Отдельные тесты:
```powershell
# Обнаружение камер
.\scripts\discover-rtsp-cameras.ps1

# RTSP тесты
.\scripts\test-rtsp-real-cameras.ps1

# HLS тесты
.\scripts\hls-runtime-stability-test.ps1 -FullTest

# Screenshot тесты
.\scripts\screenshot-pipeline-test.ps1
```

---

## 📋 Все Команды

### 1. Обнаружение камер

```powershell
.\scripts\discover-rtsp-cameras.ps1 [-Subnet <subnet>] [-RtspPort <port>] [-OutputFile <path>]

# Примеры:
.\scripts\discover-rtsp-cameras.ps1 -Subnet 192.168.1.0/24
.\scripts\discover-rtsp-cameras.ps1 -Subnet 10.0.0.0/24 -RtspPort 5540
.\scripts\discover-rtsp-cameras.ps1 -ShowHelp
```

**Параметры:**
- `-Subnet` — Подсеть для сканирования (default: 192.168.1.0/24)
- `-RtspPort` — Port RTSP (default: 554)
- `-OutputFile` — Файл для результатов (default: diagnostics/discovered-cameras.json)
- `-ShowHelp` — Показать справку

**Результаты:** `diagnostics/discovered-cameras.json`

---

### 2. Полевая валидация (единый запуск)

```powershell
.\scripts\field-validation.ps1 [-RunAllTests] [-ConfigPath <path>] [-BaseUrl <url>]

# Примеры:
.\scripts\field-validation.ps1 -RunAllTests
.\scripts\field-validation.ps1 -RunDiscovery
.\scripts\field-validation.ps1 -RunRtspTests
.\scripts\field-validation.ps1 -RunHlsTests -HlsDurationSeconds 300
.\scripts\field-validation.ps1 -RunScreenshotTests
.\scripts\field-validation.ps1 -ConfigPath config/my-cameras.json
.\scripts\field-validation.ps1 -ShowHelp
```

**Параметры:**
- `-ConfigPath` — Конфигурация камер (default: config/test-cameras-local-network.example.json)
- `-BaseUrl` — URL API сервера (default: http://localhost:8080)
- `-RunAllTests` — Запустить все тесты
- `-RunDiscovery` — Обнаружить камеры
- `-RunRtspTests` — Только RTSP тесты
- `-RunHlsTests` — Только HLS тесты
- `-RunScreenshotTests` — Только Screenshot тесты
- `-HlsDurationSeconds` — Длительность HLS теста (default: 120)
- `-OutputDir` — Директория для отчётов (default: diagnostics/field-validation)
- `-ShowHelp` — Показать справку

**Результаты:** `diagnostics/field-validation/field-validation-report-*.md`

---

### 3. RTSP Тестирование

```powershell
.\scripts\test-rtsp-real-cameras.ps1 [-ConfigPath <path>] [-FullTest] [-BaseUrl <url>]

# Примеры:
.\scripts\test-rtsp-real-cameras.ps1 -FullTest
.\scripts\test-rtsp-real-cameras.ps1 -ConfigPath config/test-cameras-local-network.example.json
.\scripts\test-rtsp-real-cameras.ps1 -ConnectionTestOnly
.\scripts\test-rtsp-real-cameras.ps1 -ShowHelp
```

**Параметры:**
- `-ConfigPath` — Конфигурация камер
- `-BaseUrl` — URL API сервера
- `-FullTest` — Полный цикл тестов (connection, video, audio, reconnect)
- `-ConnectionTestOnly` — Только проверка подключения
- `-OutputDir` — Директория для отчётов
- `-ShowHelp` — Показать справку

**Результаты:** `diagnostics/rtsp-tests/rtsp-test-*.md`

---

### 4. HLS Тестирование

```powershell
.\scripts\hls-runtime-stability-test.ps1 [-FullTest] [-LongRunDurationSeconds <sec>]

# Примеры:
.\scripts\hls-runtime-stability-test.ps1
.\scripts\hls-runtime-stability-test.ps1 -FullTest -LongRunDurationSeconds 600
.\scripts\hls-runtime-stability-test.ps1 -SkipCleanupTest
.\scripts\hls-runtime-stability-test.ps1 -SkipReconnectTest
.\scripts\hls-runtime-stability-test.ps1 -ShowHelp
```

**Параметры:**
- `-BaseUrl` — URL API сервера (default: http://localhost:8080)
- `-LongRunDurationSeconds` — Длительность long-run теста (default: 120)
- `-FullTest` — Полный цикл (включая cleanup и reconnect)
- `-SkipCleanupTest` — Пропустить тест cleanup
- `-SkipReconnectTest` — Пропустить тест reconnect
- `-OutputDir` — Директория для отчётов
- `-ShowHelp` — Показать справку

**Результаты:** `diagnostics/hls-tests/hls-runtime-stability-test-*.md`

---

### 5. Screenshot Тестирование

```powershell
.\scripts\screenshot-pipeline-test.ps1 [-BaseUrl <url>]

# Примеры:
.\scripts\screenshot-pipeline-test.ps1
.\scripts\screenshot-pipeline-test.ps1 -BaseUrl http://192.168.1.100:8080
.\scripts\screenshot-pipeline-test.ps1 -ShowHelp
```

**Параметры:**
- `-BaseUrl` — URL API сервера (default: http://localhost:8080)
- `-OutputDir` — Директория для отчётов
- `-ShowHelp` — Показать справку

**Результаты:** `diagnostics/screenshot-tests/screenshot-pipeline-test-*.md`

---

## 📊 Типовые Сценарии

### Сценарий 1: Быстрая проверка одной камеры

```powershell
# 1. Создать конфиг с одной камерой
@{
    cameras = @(
        @{
            id = "test-camera"
            name = "Test Camera"
            rtspUrl = "rtsp://192.168.1.100:554/stream1"
            username = "admin"
            password = "password"
        }
    )
} | ConvertTo-Json | Out-File config/quick-test.json

# 2. Запустить RTSP тесты
.\scripts\test-rtsp-real-cameras.ps1 -ConfigPath config/quick-test.json

# 3. Проверить результаты
Get-Content diagnostics/rtsp-tests/rtsp-test-*.md
```

---

### Сценарий 2: Полная валидация перед релизом

```powershell
# 1. Обнаружить все камеры в сети
.\scripts\discover-rtsp-cameras.ps1 -Subnet 192.168.1.0/24

# 2. Обновить конфигурацию на основе обнаруженных камер
# Отредактировать config/test-cameras-local-network.json

# 3. Запустить полный цикл тестов
.\scripts\field-validation.ps1 -RunAllTests -HlsDurationSeconds 600

# 4. Проверить итоговый отчёт
Get-Content diagnostics/field-validation/field-validation-report-*.md
```

---

### Сценарий 3: Тестирование HLS стабильности

```powershell
# Запустить HLS тесты на 1 час
.\scripts\hls-runtime-stability-test.ps1 -FullTest -LongRunDurationSeconds 3600

# Проверить результаты
Get-Content diagnostics/hls-tests/hls-runtime-stability-test-*.md
```

---

### Сценарий 4: Тестирование в CI/CD

```powershell
# Базовый smoke тест (без long-run)
.\scripts\test-rtsp-real-cameras.ps1 -ConnectionTestOnly
if ($LASTEXITCODE -eq 0) {
    Write-Host "RTSP tests passed"
} else {
    Write-Host "RTSP tests failed"
    exit 1
}
```

---

## 🐛 Устранение Проблем

### Проблема: "Camera not found"

**Решение:**
```powershell
# Проверить доступность
Test-NetConnection -ComputerName 192.168.1.100 -Port 554

# Обнаружить камеры
.\scripts\discover-rtsp-cameras.ps1 -Subnet 192.168.1.0/24
```

---

### Проблема: "Authentication failed"

**Решение:**
```powershell
# Проверить учётные данные в конфигурации
Get-Content config/test-cameras-local-network.json | ConvertFrom-Json

# Обновить пароль
# Отредактировать config/test-cameras-local-network.json
```

---

### Проблема: "FFmpeg not found"

**Решение:**
```powershell
# Проверить установку FFmpeg
ffmpeg -version

# Установить FFmpeg
choco install ffmpeg
```

---

## 📁 Структура Отчётов

```
diagnostics/
├── discovered-cameras.json
├── rtsp-tests/
│   ├── rtsp-test-YYYYMMDD-HHMMSS.md
│   └── rtsp-test-YYYYMMDD-HHMMSS.json
├── hls-tests/
│   ├── hls-runtime-stability-test-YYYYMMDD-HHMMSS.md
│   └── hls-runtime-stability-test-YYYYMMDD-HHMMSS.json
├── screenshot-tests/
│   ├── screenshot-pipeline-test-YYYYMMDD-HHMMSS.md
│   └── screenshot-pipeline-test-YYYYMMDD-HHMMSS.json
└── field-validation/
    └── field-validation-report-YYYYMMDD-HHMMSS.md
```

---

## 📊 Критерии Успеха

### RTSP Тесты:

| Тест | Критерий |
|------|----------|
| Connection | Подключение <5s |
| Video | Первый кадр <3s |
| Audio | Декодирование AAC/PCMU/PCMA |
| Reconnect | Восстановление <5s |

### HLS Тесты:

| Тест | Критерий |
|------|----------|
| Startup | Плейлист создан |
| Long-run | Нет сбоев (120s+) |
| Cleanup | Процессы завершены |
| Reconnect | Успешный перезапуск |

### Screenshot Тесты:

| Тест | Критерий |
|------|----------|
| Basic | JPEG/PNG создан |
| Timeout | Корректная ошибка |
| Sequential | 3 кадра без сбоев |

---

## 🔧 Дополнительные Команды

### Проверка здоровья сервера:
```powershell
curl http://localhost:8080/api/v1/health
```

### Просмотр логов FFmpeg:
```powershell
Get-Content data/logs/ffmpeg-*.log -Tail 50
```

### Остановка всех процессов FFmpeg:
```powershell
Get-Process ffmpeg | Stop-Process -Force
```

### Очистка временных файлов HLS:
```powershell
Remove-Item -Path "data/recordings/hls/*" -Recurse -Force
```

---

## 📚 Связанная Документация

- [Руководство по полевой валидации](../field-validation/LOCAL_CAMERA_VALIDATION_GUIDE.md)
- [Отчёт по 12 задачам](../reports/PHASE1_12_TASKS_EXECUTION_REPORT.md)
- [Статус проекта](../status/PROJECT_STATUS_PHASES.md)

---

## ✅ Чеклист Перед Запуском

### Подготовка:

- [ ] Камеры доступны в сети (ping)
- [ ] RTSP URL и учётные данные известны
- [ ] Конфигурация обновлена
- [ ] API сервер запущен
- [ ] FFmpeg установлен

### После тестирования:

- [ ] Все тесты прошли успешно
- [ ] Отчёты сгенерированы
- [ ] Проблемы задокументированы
- [ ] Метрики собраны

---

*Quick Reference создан: 2026-05-28*  
*Версия: 1.0*
