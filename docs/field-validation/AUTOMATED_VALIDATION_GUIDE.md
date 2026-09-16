# 🚀 Автоматизированная Полевая Валидация — Инструкция

**Дата:** 2026-05-28  
**Версия:** 1.0

---

## 📋 Что Будет Выполнено

Автоматизированный скрипт выполнит все этапы полевой валидации последовательно:

1. ✅ **Pre-flight Check** — Проверка готовности системы
2. ✅ **Camera Discovery** — Обнаружение камер в сети
3. ✅ **RTSP Testing** — Тестирование подключения и потоков
4. ✅ **HLS Testing** — Тестирование стабильности HLS
5. ✅ **Screenshot Testing** — Тестирование захвата кадров
6. ✅ **Aggregation** — Сборка итогового отчёта

---

## ⚡ Быстрый Старт

### Шаг 1: Подготовка

```powershell
# Перейдите в директорию проекта
cd path\to\IP-CSS

# Проверьте, что скрипты существуют
Test-Path scripts\auto-field-validation.ps1  # Должно вернуть True
```

### Шаг 2: Настройка Конфигурации

**ВАЖНО:** Перед запуском настройте конфигурацию камер!

```powershell
# Откройте файл конфигурации
notepad config\test-cameras-local-network.example.json
```

**Что нужно изменить:**
1. Замените `CHANGE_ME` на реальные пароли
2. Проверьте IP адреса ваших камер
3. Убедитесь, что RTSP URL правильные

**Пример конфигурации:**
```json
{
  "cameras": [
    {
      "id": "camera-1",
      "name": "Living Room",
      "rtspUrl": "rtsp://192.168.1.100:554/stream1",
      "username": "admin",
      "password": "mysecretpassword"
    },
    {
      "id": "camera-2",
      "name": "Garden",
      "rtspUrl": "rtsp://192.168.1.101:554/live.sdp",
      "username": "admin",
      "password": "anotherpassword"
    }
  ]
}
```

### Шаг 3: Запуск API Сервера

```powershell
# Запустите API сервер (если ещё не запущен)
.\gradlew :server:api:run
```

Сервер должен быть доступен по `http://localhost:8080`

### Шаг 4: Запуск Автоматизированной Валидации

```powershell
# Полный запуск (рекомендуется)
.\scripts\auto-field-validation.ps1

# Быстрый режим (для первой проверки)
.\scripts\auto-field-validation.ps1 -QuickMode

# С кастомной подсетью
.\scripts\auto-field-validation.ps1 -Subnet 192.168.0.0/24
```

---

## 📊 Параметры Команды

| Параметр | Значение по умолчанию | Описание |
|----------|----------------------|----------|
| `-Subnet` | `192.168.1.0/24` | Подсеть для сканирования камер |
| `-ConfigPath` | `config/test-cameras-local-network.example.json` | Путь к конфигурации |
| `-ApiBaseUrl` | `http://localhost:8080` | URL API сервера |
| `-HlsDurationSeconds` | `120` | Длительность HLS теста (секунды) |
| `-QuickMode` | `false` | Быстрый режим (30s вместо 120s) |
| `-SkipDiscovery` | `false` | Пропустить обнаружение камер |
| `-SkipRtsp` | `false` | Пропустить RTSP тесты |
| `-SkipHls` | `false` | Пропустить HLS тесты |
| `-SkipScreenshot` | `false` | Пропустить Screenshot тесты |
| `-OutputDir` | `diagnostics/field-validation-automated` | Директория для результатов |

---

## 🎯 Типовые Сценарии

### Сценарий 1: Полный тест (рекомендуется)

```powershell
.\scripts\auto-field-validation.ps1
```

**Что делает:**
- Запускает все тесты
- Использует стандартную длительность (120s для HLS)
- Генерирует полный отчёт

**Время выполнения:** 10-15 минут

---

### Сценарий 2: Быстрый тест (первая проверка)

```powershell
.\scripts\auto-field-validation.ps1 -QuickMode
```

**Что делает:**
- Запускает все тесты в быстром режиме
- HLS тест всего 30 секунд
- Пропускает некоторые проверки

**Время выполнения:** 3-5 минут

---

### Сценарий 3: Только RTSP тесты

```powershell
.\scripts\auto-field-validation.ps1 -SkipHls -SkipScreenshot
```

**Что делает:**
- Запускает только Discovery и RTSP тесты
- Быстрая проверка подключения

**Время выполнения:** 2-3 минуты

---

### Сценарий 4: Длительный HLS тест

```powershell
.\scripts\auto-field-validation.ps1 -SkipRtsp -SkipScreenshot -HlsDurationSeconds 3600
```

**Что делает:**
- Запускает только HLS тест на 1 час
- Для проверки стабильности

**Время выполнения:** 60+ минут

---

### Сценарий 5: Кастомная подсеть

```powershell
.\scripts\auto-field-validation.ps1 -Subnet 10.0.0.0/24
```

**Что делает:**
- Сканирует другую подсеть
- Полезно для офисных сетей

---

## 📁 Результаты

После завершения все результаты будут в директории:

```
diagnostics/field-validation-automated/
├── field-validation-report-20260528-143022.md  # Итоговый отчёт
├── discovered-cameras.json                      # Обнаруженные камеры
├── rtsp-tests/                                  # RTSP результаты
│   ├── rtsp-test-*.md
│   └── rtsp-test-*.json
├── hls-tests/                                   # HLS результаты
│   ├── hls-runtime-stability-test-*.md
│   └── hls-runtime-stability-test-*.json
├── screenshot-tests/                            # Screenshot результаты
│   ├── screenshot-pipeline-test-*.md
│   └── screenshot-pipeline-test-*.json
└── aggregated/                                  # Агрегированные результаты
    └── aggregated-test-results-*.md
```

---

## 🔍 Проверка Результатов

### 1. Итоговый отчёт

```powershell
# Откройте итоговый отчёт
notepad diagnostics\field-validation-automated\field-validation-report-*.md
```

### 2. Ключевые метрики

Ищите в отчёте:

```
## Сводка

| Этап | Статус | Детали |
|------|--------|--------|
| Discovery | **PASSED** | 5 камер найдено |
| RTSP | **PASSED** | 5 passed, 0 failed |
| HLS | **PASSED** | 4 passed, 0 failed |
| Screenshot | **PASSED** | 4 passed, 0 failed |
```

### 3. Успешное завершение

```
✅ Полевая валидация завершена успешно!
```

---

## 🐛 Устранение Проблем

### Проблема 1: "Pre-flight check failed"

**Причина:** Система не готова к тестированию

**Решение:**
```powershell
# Запустите pre-flight отдельно для детального отчёта
.\scripts\field-validation-preflight.ps1

# Проверьте:
# - PowerShell версии (требуется 7+)
# - Наличие всех скриптов
# - Доступность API сервера
# - Наличие FFmpeg
```

---

### Проблема 2: "No cameras found"

**Причина:** Камеры не обнаружены в сети

**Решение:**
```powershell
# Проверьте подсеть
.\scripts\discover-rtsp-cameras.ps1 -Subnet 192.168.0.0/24

# Проверьте доступность камер вручную
Test-NetConnection -ComputerName 192.168.1.100 -Port 554

# Проверьте конфигурацию
notepad config\test-cameras-local-network.example.json
```

---

### Проблема 3: "Authentication failed"

**Причина:** Неверные учётные данные в конфигурации

**Решение:**
```powershell
# Проверьте пароли в конфигурации
Get-Content config\test-cameras-local-network.example.json | ConvertFrom-Json

# Обновите пароли
notepad config\test-cameras-local-network.example.json
```

---

### Проблема 4: "API server not available"

**Причина:** API сервер не запущен

**Решение:**
```powershell
# Проверьте, запущен ли сервер
curl http://localhost:8080/api/v1/health

# Если не запущен, запустите:
.\gradlew :server:api:run
```

---

### Проблема 5: "FFmpeg not found"

**Причина:** FFmpeg не установлен или не в PATH

**Решение:**
```powershell
# Проверьте установку
ffmpeg -version

# Установите FFmpeg
choco install ffmpeg

# Или скачайте с https://ffmpeg.org/download.html
```

---

## 📊 Интерпретация Результатов

### Статусы:

| Статус | Значение | Действие |
|--------|----------|----------|
| ✅ PASSED | Тест пройден | Продолжать |
| ⚠️ SKIPPED | Тест пропущен | Проверить причины |
| ❌ FAILED | Тест не пройден | Исправить ошибки |

### Критерии Успеха:

| Этап | Критерий успеха |
|------|-----------------|
| Discovery | ≥1 камера найдена |
| RTSP | ≥90% тестов пройдены |
| HLS | Все тесты пройдены |
| Screenshot | ≥90% тестов пройдены |

### Рекомендации:

- **75-100% успеха:** Готово к переходу
- **50-74% успеха:** Требуется исправление проблем
- **<50% успеха:** Критические проблемы, требуется пересмотр

---

## 🔄 Повторный Запуск

Если тесты не пройдены:

```powershell
# Исправьте проблемы
# ...

# Перезапустите в быстром режиме
.\scripts\auto-field-validation.ps1 -QuickMode

# Если успешно, запустите полный тест
.\scripts\auto-field-validation.ps1
```

---

## 📞 Следующие Шаги После Успеха

1. **Проверьте отчёты:**
   ```powershell
   Get-ChildItem diagnostics\field-validation-automated\ -Recurse | Select-Object Name, LastWriteTime
   ```

2. **Сохраните результаты:**
   ```powershell
   Copy-Item -Path diagnostics\field-validation-automated -Destination results\field-validation-$(Get-Date -Format 'yyyyMMdd') -Recurse
   ```

3. **Перейдите к PostgreSQL cutover:**
   ```powershell
   .\scripts\migration-smoke-test.ps1 -BaseUrl http://staging.example.com:8080
   ```

4. **Задокументируйте результаты:**
   - Обновите `docs/reports/FIELD_VALIDATION_SUMMARY.md`
   - Добавьте ссылки на отчёты

---

## 💡 Советы

1. **Начните с Quick Mode** для первой проверки
2. **Проверьте конфигурацию** перед запуском
3. **Сохраняйте отчёты** для истории
4. **Запускайте ночью** длительные тесты (24h)
5. **Документируйте все проблемы**

---

## 📚 Связанная Документация

- [Полевая Валидация — Руководство](LOCAL_CAMERA_VALIDATION_GUIDE.md)
- [Quick Reference Скриптов](SCRIPTS_QUICK_REFERENCE.md)
- [Чеклист Перехода к Фазе 2](../planning/PHASE1_TO_PHASE2_CHECKLIST.md)

---

*Инструкция создана: 2026-05-28*  
*Версия: 1.0*
