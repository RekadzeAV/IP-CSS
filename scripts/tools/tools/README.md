# Phase 2 RTSP Client - Tools README

**Версия:** 0.1.2-beta  
**Дата:** 11 June 2026  
**Статус:** ✅ **PRODUCTION READY**

---

## 📁 Обзор инструментов

Phase 2 включает 3 основных инструмента для тестирования и мониторинга RTSP client.

---

## 🔧 Инструменты

### 1. Automated Test Script

**Файл:** `test_rtsp_integration.ps1`

**Назначение:** Автоматизированное тестирование RTSP client с MediaMTX сервером.

**Использование:**
```powershell
# Базовое тестирование
.\test_rtsp_integration.ps1

# С детальным выводом
.\test_rtsp_integration.ps1 -Verbose

# С экспортом JSON отчета
.\test_rtsp_integration.ps1 -ReportJson "test-results.json"

# Тестирование конкретной камеры
.\test_rtsp_integration.ps1 -Url rtsp://192.168.1.100:554/test
```

**Проверяет:**
- ✅ Подключение к RTSP серверу
- ✅ Получение RTSP описания (DESCRIBE)
- ✅ Установка RTP/RTCP сессии (SETUP)
- ✅ Запуск стриминга (PLAY)
- ✅ Получение видео/аудио кадров
- ✅ Graceful shutdown (TEARDOWN)

**Вывод:**
```
========================================
RTSP Integration Test
========================================
[✓] Connecting to rtsp://localhost:8554/test
[✓] DESCRIBE successful
[✓] SETUP successful (RTP: 5004, RTCP: 5005)
[✓] PLAY started
[✓] Received 150 frames (3 seconds)
[✓] TEARDOWN successful
========================================
Test PASSED ✅
```

---

### 2. RTSP Monitor

**Файл:** `tools\rtsp_monitor.ps1`

**Назначение:** Мониторинг RTSP подключений в реальном времени.

**Использование:**
```powershell
# Мониторинг одной камеры
.\tools\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test -Continuous

# Мониторинг нескольких камер
.\tools\rtsp_monitor.ps1 -Urls @(
    "rtsp://192.168.1.100:554/cam1",
    "rtsp://192.168.1.101:554/cam2"
) -Continuous -LogPath "monitor.log"

# Разовое подключение
.\tools\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test

# С детальным выводом
.\tools\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test -Verbose
```

**Параметры:**
- `-Url` - RTSP URL камеры (обязательный, если не `-Urls`)
- `-Urls` - Массив RTSP URL для мониторинга нескольких камер
- `-Continuous` - Непрерывный мониторинг с авто-переподключением
- `-LogPath` - Путь к файлу лога
- `-Verbose` - Детальный вывод
- `-TimeoutSec` - Таймаут подключения (по умолчанию: 10)

**Вывод:**
```
========================================
RTSP Monitor - 192.168.1.100:554/test
========================================
Time: 2026-06-11 23:45:00
Status: Connected
FPS: 25.0
Frames Received: 1250
Bytes Received: 5.2 MB
Jitter: 2.3 ms
Dropped Frames: 0
========================================
```

**Логирование:**
```powershell
# Логи сохраняются в формате CSV
.\tools\rtsp_monitor.ps1 -Url rtsp://camera:554/stream -Continuous -LogPath "rtsp.log"

# Пример лога:
# Timestamp,Status,URL,FrameCount,BytesReceived,FPS,Jitter,DroppedFrames
# 2026-06-11 23:45:00,Connected,rtsp://camera:554/stream,1250,5452000,25.0,2.3,0
```

---

### 3. Pre-Merge Verification

**Файл:** `scripts\verify-phase2-merge.ps1`

**Назначение:** Автоматизированная проверка готовности к merge в main branch.

**Использование:**
```powershell
# Полная проверка
.\scripts\verify-phase2-merge.ps1 -Verbose

# Только проверка сборки
.\scripts\verify-phase2-merge.ps1 -CheckBuildOnly

# Только проверка тестов
.\scripts\verify-phase2-merge.ps1 -CheckTestsOnly

# Экспорт JSON отчета
.\scripts\verify-phase2-merge.ps1 -ReportJson "verify-report.json"
```

**Проверяет:**
1. ✅ Build (DLL существует, сборка успешна)
2. ✅ Documentation (все отчеты созданы)
3. ✅ Tools (тестовые скрипты существуют)
4. ✅ Code Quality (helper функция, getaddrinfo, thread sync)
5. ✅ Project Status (статус обновлен)

**Вывод:**
```
========================================
Phase 2 Pre-Merge Verification
========================================
Duration: 0.06s
Checks Passed: 15
Checks Failed: 0
Ready for Merge: YES ✅
========================================
```

---

## 📊 Примеры использования

### Сценарий 1: Локальное тестирование

```powershell
# 1. Запустить MediaMTX
docker run -d --name ip-camera-mediamtx -p 8554:8554 iting1103/rtsp-simple-server:latest

# 2. Опубликовать тестовый поток
ffmpeg -re -f lavfi -i testsrc=duration=60 -c:v libx264 -f rtsp rtsp://localhost:8554/test

# 3. Протестировать RTSP client
.\test_rtsp_integration.ps1 -Verbose

# 4. Запустить мониторинг
.\tools\rtsp_monitor.ps1 -Url rtsp://localhost:8554/test -Continuous
```

### Сценарий 2: Production deployment

```powershell
# 1. Проверить готовность к deploy
.\scripts\verify-phase2-merge.ps1 -Verbose

# 2. Запустить мониторинг production камеры
.\tools\rtsp_monitor.ps1 -Urls @(
    "rtsp://prod-camera-1:554/stream1",
    "rtsp://prod-camera-2:554/stream2"
) -Continuous -LogPath "production.log"

# 3. Проверить логи через час
Get-Content "production.log" -Tail 100
```

### Сценарий 3: CI/CD интеграция

```yaml
# .github/workflows/phase2-verify.yml
name: Phase 2 Pre-Merge Check

on:
  pull_request:
    branches: [main]

jobs:
  verify:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run Pre-Merge Verification
        run: .\scripts\verify-phase2-merge.ps1 -ReportJson "report.json"
      
      - name: Upload Report
        uses: actions/upload-artifact@v3
        with:
          name: verify-report
          path: report.json
```

---

## 🔍 Troubleshooting

### Проблема: "MediaMTX not running"

**Решение:**
```powershell
# Запустить MediaMTX
docker run -d --name ip-camera-mediamtx -p 8554:8554 iting1103/rtsp-simple-server:latest

# Проверить статус
docker ps | Select-String "ip-camera-mediamtx"
```

### Проблема: "Connection timeout"

**Решение:**
```powershell
# Проверить доступность камеры
Test-NetConnection -ComputerName 192.168.1.100 -Port 554

# Проверить firewall
netsh advfirewall firewall show rule name=all | Select-String "554"
```

### Проблема: "DLL not found"

**Решение:**
```powershell
# Пересобрать проект
cd native/video-processing/build
cmake --build . --config Release

# Проверить путь
Test-Path "native/video-processing/lib/windows/x64/video_processing.dll"
```

---

## 📈 Performance Monitoring

### Metrics Collection

```powershell
# Собрать метрики производительности
.\tools\rtsp_monitor.ps1 -Url rtsp://camera:554/stream -Continuous -LogPath "metrics.csv"

# Анализ результатов
Import-Csv "metrics.csv" | Measure-Object -Property FPS -Average
Import-Csv "metrics.csv" | Measure-Object -Property DroppedFrames -Sum
```

### Grafana Dashboard

Импортируйте `tools/rtsp_monitor_dashboard.json` в Grafana для визуализации метрик.

---

## 📚 Дополнительная документация

- [Quick Start Guide](../../../docs/rtsp/QUICK_START_GUIDE_2026-06-11.md)
- [Deployment Guide](../../../archive/docs/deployment/DEPLOYMENT_PHASE2_2026-06-11.md)
- [Integration Test Script](../../../docs/testing/RTSP_INTEGRATION_TEST_SCRIPT_2026-06-11.md)
- [Pre-Merge Verification](../../../docs/scripts/VERIFY_PHASE2_MERGE_2026-06-11.md)

---

## 📞 Support

**Issues:** https://github.com/RekadzeAV/IP-CSS/issues  
**Documentation:** docs/README.md *(утерян/в архиве)*  
**Phase 2 Reports:** [docs/reports/PHASE2_FINAL_SUMMARY_2026-06-11.md](../../../docs/reports/PHASE2_FINAL_SUMMARY_2026-06-11.md)

---

**Created:** 11 June 2026  
**Version:** 0.1.2-beta  
**Status:** ✅ **PRODUCTION READY**
