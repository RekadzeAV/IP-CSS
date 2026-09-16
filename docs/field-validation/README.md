# 🎬 Полевая Валидация - Быстрый Старт

**Версия:** 1.0  
**Дата:** 2026-05-28

---

## 🚀 Что Нужно Сделать

1. **Обнаружить** камеры в локальной сети
2. **Настроить** конфигурацию
3. **Запустить** тесты
4. **Проверить** результаты

**Время:** 1-2 часа  
**Сложность:** ⭐⭐☆☆☆

---

## 📋 Предварительные Требования

### Оборудование:

- [ ] Камеры в локальной сети
- [ ] Доступ к сети (ping)
- [ ] Учётные данные для камер

### Программное обеспечение:

- [ ] PowerShell 7+
- [ ] API сервер запущен (`http://localhost:8080`)
- [ ] FFmpeg установлен (для HLS тестов)

---

## ⚡ Быстрый Старт (5 минут)

### Шаг 1: Обнаружение камер

```powershell
.\scripts\discover-rtsp-cameras.ps1 -Subnet 192.168.1.0/24
```

**Результат:** `diagnostics/discovered-cameras.json`

---

### Шаг 2: Настройка конфигурации

```powershell
# Откройте файл и замените CHANGE_ME на реальные пароли
notepad config/test-cameras-local-network.example.json
```

**Пример:**
```json
{
  "cameras": [
    {
      "id": "camera-1",
      "name": "Living Room",
      "rtspUrl": "rtsp://192.168.1.100:554/stream1",
      "username": "admin",
      "password": "mysecretpassword"
    }
  ]
}
```

---

### Шаг 3: Запуск всех тестов

```powershell
.\scripts\field-validation.ps1 -RunAllTests
```

**Время выполнения:** 5-10 минут

---

### Шаг 4: Проверка результатов

```powershell
# Откройте итоговый отчёт
notepad diagnostics/field-validation/field-validation-report-*.md
```

---

## 📊 Ожидаемые Результаты

### ✅ Успех:

```
RTSP Tests:    Passed: 5, Failed: 0, Skipped: 0
HLS Tests:     Passed: 4, Failed: 0, Skipped: 0
Screenshot:    Passed: 4, Failed: 0, Skipped: 0

Total:         Passed: 13, Failed: 0, Skipped: 0
```

### ❌ Проблемы:

```
RTSP Tests:    Passed: 3, Failed: 2, Skipped: 0
```

**Действие:** Проверьте учётные данные и доступность камер

---

## 🔧 Типовые Сценарии

### Сценарий 1: Одна камера

```powershell
# Создать минимальный конфиг
@{
    cameras = @(
        @{
            id = "test"
            name = "Test"
            rtspUrl = "rtsp://192.168.1.100:554/stream1"
            username = "admin"
            password = "password"
        }
    )
} | ConvertTo-Json | Out-File config/single-camera.json

# Запустить тесты
.\scripts\field-validation.ps1 -ConfigPath config/single-camera.json -RunAllTests
```

---

### Сценарий 2: Только RTSP тесты

```powershell
.\scripts\test-rtsp-real-cameras.ps1 -FullTest
```

---

### Сценарий 3: Длительный HLS тест (1 час)

```powershell
.\scripts\hls-runtime-stability-test.ps1 -FullTest -LongRunDurationSeconds 3600
```

---

### Сценарий 4: Проверка только подключения

```powershell
.\scripts\test-rtsp-real-cameras.ps1 -ConnectionTestOnly
```

---

## 🐛 Устранение Проблем

### Проблема: "No cameras found"

**Причина:** Камеры не доступны в сети

**Решение:**
```powershell
# Проверить ping
ping 192.168.1.100

# Проверить порт
Test-NetConnection -ComputerName 192.168.1.100 -Port 554

# Проверить subnet
.\scripts\discover-rtsp-cameras.ps1 -Subnet 192.168.0.0/24
```

---

### Проблема: "Authentication failed"

**Причина:** Неверные учётные данные

**Решение:**
```powershell
# Проверить пароль в конфигурации
Get-Content config/test-cameras-local-network.json | ConvertFrom-Json

# Обновить пароль в файле
notepad config/test-cameras-local-network.json
```

---

### Проблема: "FFmpeg not found"

**Причина:** FFmpeg не установлен

**Решение:**
```powershell
# Проверить установку
ffmpeg -version

# Установить (Chocolatey)
choco install ffmpeg

# Установить (Scoop)
scoop install ffmpeg
```

---

### Проблема: "Connection timeout"

**Причина:** Камера недоступна или медленная сеть

**Решение:**
```powershell
# Увеличить таймаут в конфигурации
# Добавьте в конфиг:
"timeoutMs": 10000
```

---

## 📁 Структура Отчётов

```
diagnostics/
├── discovered-cameras.json          # Обнаруженные камеры
├── rtsp-tests/
│   ├── rtsp-test-20260528-143022.md # Отчёт RTSP
│   └── rtsp-test-20260528-143022.json
├── hls-tests/
│   ├── hls-runtime-stability-test-20260528-143022.md
│   └── hls-runtime-stability-test-20260528-143022.json
├── screenshot-tests/
│   ├── screenshot-pipeline-test-20260528-143022.md
│   └── screenshot-pipeline-test-20260528-143022.json
└── field-validation/
    └── field-validation-report-20260528-143022.md  # Итоговый отчёт
```

---

## 📊 Критерии Успеха

### Обязательные:

| Тест | Критерий |
|------|----------|
| RTSP Connection | <5s |
| First Frame | <3s |
| Long-run | 120s без сбоев |
| Reconnect | <5s |
| Screenshot | JPEG создан |

### Рекомендуемые:

| Тест | Критерий |
|------|----------|
| Audio | AAC/PCMU/PCMA декодирование |
| HLS TTFF | <500ms |
| Cleanup | 100% процессов завершено |

---

## 🎯 Что Дальше?

### Если ВСЕ тесты PASSED:

✅ **Готово!** Можно переходить к Фазе 2

1. Создайте итоговый отчёт:
```powershell
.\scripts\aggregate-test-results.ps1
```

2. Задокументируйте результаты:
```
docs/reports/FIELD_VALIDATION_REPORT_2026-05-28.md
```

3. Перейдите к чеклисту перехода:
```
docs/planning/PHASE1_TO_PHASE2_CHECKLIST.md
```

---

### Если есть FAILED тесты:

🔴 **Исправьте проблемы перед переходом**

1. Проверьте логи:
```powershell
Get-Content diagnostics/rtsp-tests/rtsp-test-*.md
```

2. Исправьте конфигурацию
3. Запустите тесты снова

---

## 📚 Дополнительная Документация

- [Полное Руководство](LOCAL_CAMERA_VALIDATION_GUIDE.md)
- [Quick Reference Скриптов](SCRIPTS_QUICK_REFERENCE.md)
- [Чеклист Перехода к Фазе 2](../planning/PHASE1_TO_PHASE2_CHECKLIST.md)
- [Итоговая Сводка Сессии](../reports/SESSION_SUMMARY_2026-05-28.md)

---

## ✅ Чеклист Перед Запуском

### Подготовка:

- [ ] Камеры в сети (ping работает)
- [ ] RTSP URL известны
- [ ] Учётные данные известны
- [ ] Конфигурация обновлена
- [ ] API сервер запущен
- [ ] FFmpeg установлен

### После тестирования:

- [ ] Все тесты прошли успешно
- [ ] Отчёты сохранены
- [ ] Проблемы задокументированы
- [ ] Итоговый отчёт создан

---

## 💡 Советы

1. **Начните с одной камеры** — проверьте базовый сценарий
2. **Используйте короткую длительность** для первых тестов (120s)
3. **Проверяйте логи** при проблемах
4. **Сохраняйте отчёты** для истории

---

*Быстрый старт создан: 2026-05-28*  
*Версия: 1.0*
