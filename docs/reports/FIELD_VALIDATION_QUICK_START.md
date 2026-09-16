# Быстрый старт Field Validation для NAS платформ

**Версия:** 1.0  
**Дата:** 2026-05-17  
**Цель:** Запуск первой итерации полевых тестов NAS

---

## 📋 Предварительные требования

### 1. Оборудование

- [ ] Synology NAS (минимум 1 устройство)
- [ ] Доступ в сеть с NAS устройствами
- [ ] Тестовые камеры (минимум 2-4 шт)

### 2. Подготовка окружения

```powershell
# 1. Клонируйте репозиторий (если еще не сделано)
cd C:\path\to\ip-css

# 2. Установите OpenSSH Client (если нет)
# Windows: Настройки → Приложения → Опциональные функции → Добавить "OpenSSH Client"

# 3. Сгенерируйте SSH ключи (если нет)
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa

# 4. Скопируйте SSH ключ на NAS устройства
# Для Synology:
ssh-copy-id -i ~/.ssh/id_rsa.pub admin@192.168.1.100

# Для QNAP:
ssh-copy-id -i ~/.ssh/id_rsa.pub admin@192.168.1.101
```

### 3. Проверка подключения

```powershell
# Проверьте SSH подключение к каждому NAS
ssh -i ~/.ssh/id_rsa admin@192.168.1.100 "uname -a"
ssh -i ~/.ssh/id_rsa admin@192.168.1.101 "uname -a"
```

---

## 🚀 Шаг 1: Обнаружение устройств в сети

```powershell
# Автообнаружение NAS и камер
.\scripts\discover-nas-devices.ps1 -ScanNAS -ScanCameras -NetworkRange 192.168.1.0/24

# Результаты сохранятся в: docs/reports/network-discovery-YYYYMMDD-HHMMSS.json
```

**Ожидаемый вывод:**
```
Начинаем сканирование сети: 192.168.1.0/24
✓ Найдено NAS: Synology - 192.168.1.100
✓ Найдена камера: RTSP - 192.168.1.50:554
✓ Найдена камера: ONVIF - 192.168.1.51:80
```

---

## 📝 Шаг 2: Настройка конфигурации

### Вариант A: Автоматическая (рекомендуется)

Скрипт автоматически использует найденные устройства.

### Вариант B: Ручная настройка

Отредактируйте `scripts\nas-field-validation-config.ps1`:

```powershell
# Synology NAS
$SynologyConfig = @{
    Host = "192.168.1.100"           # IP вашего Synology
    Username = "admin"
    PackagePath = "build/ip-css-Alfa-0.1.1-synology-x86_64.spk"
}

# Тестовые камеры
$TestCameras = @(
    @{
        Name = "Camera1"
        IP = "192.168.1.50"
        Protocol = "RTSP"
        Username = "admin"
        Password = "camera123"
    }
)
```

---

## 🧪 Шаг 3: Сборка NAS пакетов

```powershell
# Собираем пакеты для всех платформ
.\gradlew.bat :server:api:build

# Собираем Synology пакет
.\scripts\build-nas-package.ps1 -PackageType synology -Arch x86_64 -Version Alfa-0.1.1

# Проверяем пакет
.\scripts\test-nas-build.ps1 -PackageType synology -Arch x86_64
```

**Проверка:**
```powershell
# Убедитесь, что пакеты созданы
ls build\ip-css-Alfa-0.1.1-synology-*.spk
```

---

## 🎯 Шаг 4: Запуск Field Validation

### Полный тест (рекомендуется для первой итерации)

```powershell
# Запускаем тест для Synology с 24-часовым long-run
.\scripts\run-nas-field-validation.ps1 `
    -Platforms Synology `
    -Version Alfa-0.1.1 `
    -Tester "YourName" `
    -LongRunDuration 24 `
    -OutputDir "docs/reports"
```

### Ускоренный тест (без long-run)

```powershell
# Пропускаем S6 (long-run) для быстрой проверки
.\scripts\run-nas-field-validation.ps1 `
    -Platforms Synology,QNAP `
    -Version Alfa-0.1.1 `
    -SkipS6LongRun `
    -Tester "YourName"
```

### Тест нескольких платформ параллельно

```powershell
# Все 4 платформы (требует 4 NAS устройства)
.\scripts\run-nas-field-validation.ps1 `
    -Platforms Synology,QNAP,Asustor,TrueNAS `
    -Version Alfa-0.1.1 `
    -LongRunDuration 24 `
    -Tester "YourName"
```

---

## 📊 Шаг 5: Мониторинг в реальном времени

### Отдельный мониторинг ресурсов

```powershell
# Запускаем мониторинг для конкретного NAS
.\scripts\nas-resource-monitor.ps1 `
    -Host 192.168.1.100 `
    -Duration 24 `
    -Interval 60 `
    -Platform Synology `
    -OutputPath "docs/reports/nas-validation"
```

**Мониторинг в реальном времени:**
```
[14:30:00] Итерация 1 / 24 ✓ OK (CPU: 12%, Mem: 2.3GB)
[14:31:00] Итерация 2 / 24 ✓ OK (CPU: 15%, Mem: 2.4GB)
[14:32:00] Итерация 3 / 24 ⚠️ WARNING (CPU: 72%, Mem: 71%)
```

---

## 📄 Шаг 6: Просмотр результатов

### Автоматический агрегатор

```powershell
# Обновляем сводный отчет
.\scripts\archive\nas-field-aggregate.ps1 -Date 2026-05-17

# Открываем агрегатор
notepad docs\reports\NAS_FIELD_AGGREGATOR_2026-05-17.md
```

### Отчеты по платформам

```
docs/reports/NAS_FIELD_VALIDATION_2026-05-17/
├── FIELD_VALIDATION_REPORT_20260517-143000.md  # Главный отчет
├── validation.log                               # Логи выполнения
├── Synology/
│   ├── S1_FreshInstall.md
│   ├── S2_BasicHealth.md
│   ├── S3_Restart.md
│   ├── S4_RebootPersistence.md
│   ├── S5_Upgrade.md
│   └── S6_LongRun.md
├── monitoring/
│   ├── monitor-192.168.1.100-20260517-143000.csv  # Метрики ресурсов
│   └── snapshot-192.168.1.100-20260517-143000.json
└── logs/
    └── validation.log
```

---

## 🔍 Шаг 7: Интерпретация результатов

### Статусы тестов

| Статус | Значение | Действие |
|--------|----------|----------|
| ✅ PASS | Тест пройден | Переход к следующему тесту |
| ❌ FAIL | Тест не пройден | Анализ ошибок, исправление |
| ⚠️ WARN | Предупреждение | Проверка, но не блокирует |
| ⚪ SKIP | Пропущен | Опциональный тест |

### Итоговое решение

| Решение | Критерии | Действие |
|---------|----------|----------|
| **GO** | Все тесты PASS | Релиз одобрен |
| **CONDITIONAL GO** | Критические PASS, опциональные WARN | Релиз с ограничениями |
| **NO-GO** | Критические тесты FAIL | Исправление и повторное тестирование |

---

## 🐛 Устранение неполадок

### SSH подключение не работает

```powershell
# Проверьте SSH ключ
ls ~/.ssh/id_rsa

# Проверьте подключение
ssh -v -i ~/.ssh/id_rsa admin@192.168.1.100

# Если нужно сбросить пароль SSH
ssh-keygen -p -f ~/.ssh/id_rsa
```

### Пакет не устанавливается на NAS

```powershell
# Проверьте структуру пакета
.\scripts\test-nas-build.ps1 -PackageType synology -Arch x86_64

# Проверьте логи установки на NAS
ssh -i ~/.ssh/id_rsa admin@192.168.1.100 "cat /var/log/pkginstall.log"
```

### Сервис не запускается после установки

```powershell
# Проверьте логи сервиса на NAS
ssh -i ~/.ssh/id_rsa admin@192.168.1.100 "sudo journalctl -u ipcamera -n 50"

# Проверьте Java версию
ssh -i ~/.ssh/id_rsa admin@192.168.1.100 "java -version"

# Проверьте доступные порты
ssh -i ~/.ssh/id_rsa admin@192.168.1.100 "netstat -tlnp | grep -E '8080|8081'"
```

### Long-run тест не завершается

```powershell
# Проверьте мониторинг
Get-Job -State Running

# При необходимости остановите
Stop-Job -Id <job-id>

# Проверьте метрики
Import-Csv docs\reports\nas-validation\monitor-*.csv | 
    Select-Object -Last 10
```

---

## 📞 Контакты и поддержка

- **Документация:** `docs/planning/PHASE_3_DETAILED_BACKLOG.md`
- **Отчеты:** `docs/reports/PHASE3_EXECUTION_SUMMARY_2026-05-17.md`
- **Скрипты:** `scripts/nas-field-validation-*.ps1`

---

## ✅ Чек-лист готовности

Перед запуском field validation убедитесь, что:

- [ ] SSH доступ настроен для всех NAS устройств
- [ ] Пакеты NAS собраны и проверены
- [ ] Тестовые камеры доступны в сети
- [ ] Достаточно свободного места на NAS (минимум 2GB)
- [ ] Доступ к портам 8080/8081 свободен
- [ ] SSH ключи скопированы на все устройства
- [ ] Конфигурация обновлена в `nas-field-validation-config.ps1`

---

**Готовы начать? Запустите:**

```powershell
.\scripts\run-nas-field-validation.ps1 -Platforms Synology -LongRunDuration 24
```

Удачи в тестировании! 🚀
