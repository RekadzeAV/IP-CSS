# Phase 2 Pre-Merge Verification Script

**Версия:** 0.1.2-beta  
**Дата:** 11 June 2026  
**Статус:** ✅ **READY TO USE**

---

## Описание

Автоматизированный скрипт для проверки готовности Phase 2 к merge в main branch.

---

## Использование

```powershell
# Запуск полной проверки
.\scripts\verify-phase2-merge.ps1

# Запуск с детальным выводом
.\scripts\verify-phase2-merge.ps1 -Verbose

# Запуск только проверки сборки
.\scripts\verify-phase2-merge.ps1 -CheckBuildOnly

# Запуск только проверки тестов
.\scripts\verify-phase2-merge.ps1 -CheckTestsOnly

# Экспорт отчета в JSON
.\scripts\verify-phase2-merge.ps1 -ReportJson "verify-report.json"
```

---

## Что проверяет скрипт

### 1. Проверка сборки

- [x] C++ проект компилируется без ошибок
- [x] Нет критических предупреждений
- [x] DLL сгенерирована корректно
- [x] Файл имеет правильную дату компиляции

### 2. Проверка документации

- [x] Все отчеты созданы
- [x] README обновлен
- [x] Changelog актуален
- [x] Инструкции по развертыванию существуют

### 3. Проверка инструментов

- [x] Тестовый скрипт существует
- [x] Утилита мониторинга существует
- [x] Файлы имеют правильный формат

### 4. Проверка кода

- [x] Helper функция `build_rtsp_url()` существует
- [x] getaddrinfo используется вместо gethostbyname
- [x] Thread synchronization реализован
- [x] Error handling в RTP thread

### 5. Проверка тестов

- [x] MediaMTX доступен (если запущен)
- [x] RTSP URL форматирование корректное
- [x] Нет crash-ов при подключении

---

## Вывод

### Успешная проверка

```powershell
✅ Phase 2 Pre-Merge Verification PASSED

Проверено: 15 пунктов
Провалено: 0 пунктов
Время: 2.5s

Готовность к merge: ✅ YES
```

### Неудачная проверка

```powershell
❌ Phase 2 Pre-Merge Verification FAILED

Проверено: 15 пунктов
Провалено: 3 пункта

Проваленные проверки:
  ❌ MediaMTX не запущен
  ❌ DLL не найдена
  ❌ README не обновлен

Время: 1.2s

Готовность к merge: ❌ NO
```

---

## Пример использования

### Полная проверка перед merge

```powershell
# Переход в корень проекта
cd E:\GitHub-Ai\IP-CSS

# Запуск проверки
.\scripts\verify-phase2-merge.ps1 -Verbose

# Ожидаемый вывод:
#
# Phase 2 Pre-Merge Verification
# ==============================
# [✓] Проверка сборки...
#     - BUILD SUCCESSFUL
#     - video_processing.dll exists
# [✓] Проверка документации...
#     - All reports created
#     - README updated
# [✓] Проверка инструментов...
#     - test_rtsp_integration.ps1 exists
#     - rtsp_monitor.ps1 exists
# [✓] Проверка кода...
#     - build_rtsp_url() found
#     - getaddrinfo used
# [✓] Проверка тестов...
#     - No critical issues
#
# ✅ ALL CHECKS PASSED
# Ready for merge: YES
```

---

## Troubleshooting

### Проблема: "BUILD FAILED"

**Решение:**
```powershell
# Пересобрать проект
cd native/video-processing/build
cmake --build . --config Release

# Проверить ошибки
cmake --build . --config Release 2>&1 | Select-String "error"
```

### Проблема: "MediaMTX not running"

**Решение:**
```powershell
# Запустить MediaMTX
docker run -d --name ip-camera-mediamtx --restart=always `
  -p 8554:8554 iting1103/rtsp-simple-server:latest

# Проверить статус
docker ps | Select-String "ip-camera-mediamtx"
```

### Проблема: "DLL not found"

**Решение:**
```powershell
# Проверить путь
Test-Path "native/video-processing/lib/windows/x64/video_processing.dll"

# Пересобрать
cd native/video-processing/build
cmake --build . --config Release
```

---

## Интеграция с CI/CD

### GitHub Actions

```yaml
name: Phase 2 Pre-Merge Check

on:
  pull_request:
    branches: [main]

jobs:
  verify:
    runs-on: windows-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup PowerShell
        uses: microsoft/powershell-action@v1
      
      - name: Run Pre-Merge Verification
        run: |
          .\scripts\verify-phase2-merge.ps1 -ReportJson "verify-report.json"
      
      - name: Upload Report
        uses: actions/upload-artifact@v3
        with:
          name: verify-report
          path: verify-report.json
```

### GitLab CI

```yaml
phase2-verify:
  stage: test
  image: mcr.microsoft.com/powershell:latest
  script:
    - .\scripts\verify-phase2-merge.ps1 -Verbose
  artifacts:
    paths:
      - verify-report.json
```

---

## Формат отчета JSON

```json
{
  "timestamp": "2026-06-11T22:30:00Z",
  "version": "0.1.2-beta",
  "status": "PASSED",
  "checks": {
    "build": {
      "status": "PASSED",
      "details": {
        "compilation": "SUCCESS",
        "warnings": 4,
        "errors": 0,
        "dllExists": true
      }
    },
    "documentation": {
      "status": "PASSED",
      "details": {
        "reportsCount": 9,
        "readmeUpdated": true,
        "changelogExists": true
      }
    },
    "tools": {
      "status": "PASSED",
      "details": {
        "testScriptExists": true,
        "monitorExists": true
      }
    },
    "code": {
      "status": "PASSED",
      "details": {
        "helperFunctionFound": true,
        "getaddrinfoUsed": true,
        "threadSyncImplemented": true
      }
    },
    "tests": {
      "status": "PASSED",
      "details": {
        "noCrashes": true,
        "urlFormattingCorrect": true
      }
    }
  },
  "summary": {
    "totalChecks": 15,
    "passed": 15,
    "failed": 0,
    "readyForMerge": true
  }
}
```

---

## Поддержка

**Issues:** https://github.com/RekadzeAV/IP-CSS/issues  
**Documentation:** docs/README.md *(утерян/в архиве)*  
**Phase 2 Report:** [docs/reports/PHASE2_FINAL_SUMMARY_2026-06-11.md](../reports/PHASE2_FINAL_SUMMARY_2026-06-11.md)

---

**Создан:** 11 June 2026  
**Автор:** Koda AI Assistant  
**Версия:** 0.1.2-beta
