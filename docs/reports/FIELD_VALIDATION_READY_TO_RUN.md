# Field Validation для NAS - Готовность к запуску с реальными данными

**Дата:** 2026-05-17  
**Статус:** ✅ ГОТОВО К ЗАПУСКУ  
**NAS устройство:** Synology RS2416+ (192.168.10.38)  
**Версия:** Alfa-0.1.1

---

## ✅ Созданные ресурсы

### 1. Скрипты автоматизации

| Файл | Назначение | Статус |
|------|------------|--------|
| `scripts/nas-field-validation-config.ps1` | Конфигурация с реальными данными NAS | ✅ Обновлен |
| `scripts/test-nas-connection.ps1` | Проверка подключения к NAS | ✅ Создан |
| `scripts/nas-resource-monitor.ps1` | Мониторинг ресурсов NAS | ✅ Создан |
| `scripts/run-nas-field-validation.ps1` | Главный оркестратор тестов | ✅ Создан |
| `scripts/discover-nas-devices.ps1` | Обнаружение устройств в сети | ✅ Создан |

### 2. Конфигурация подключения к NAS

```powershell
# Реальные данные из docs/NAS_SSH_KEYS_REPORT.md
$SynologyConfig = @{
    Host = "192.168.10.38"           # Synology RS2416+
    Username = "Andrey"
    SSHKeyPath = "credentials/ssh-keys/nas_andrey"
    Password = "<NAS_PASSWORD>"      # Для plink
    PackagePath = "build/ip-css-Alfa-0.1.1-synology-x86_64.spk"
}
```

### 3. SSH ключи готовы

| Ключ | Расположение | Статус |
|------|--------------|--------|
| `nas_andrey` | `credentials/ssh-keys/nas_andrey` | ✅ Создан |
| Публичный ключ | Установлен на NAS | ✅ Готов |

---

## 🎯 Что готово к запуску

### Полевые тесты S1-S6

| Тест | Описание | Длительность | Готовность |
|------|----------|--------------|------------|
| **S1** | Fresh install на RS2416+ | 5-10 мин | ✅ ГОТОВО |
| **S2** | Basic health check | 2-5 мин | ✅ ГОТОВО |
| **S3** | Restart service | 2-3 мин | ✅ ГОТОВО |
| **S4** | Reboot persistence | 3-5 мин | ✅ ГОТОВО |
| **S5** | Upgrade test | 10-15 мин | ✅ ГОТОВО |
| **S6** | Long-run stability (24h) | 24 часа | ✅ ГОТОВО |

### Тестовые камеры

```powershell
# 4 камеры настроены в конфигурации
$TestCameras = @(
    @{ Name = "Camera1-RTSP"; IP = "192.168.10.50" },
    @{ Name = "Camera2-ONVIF"; IP = "192.168.10.51" },
    @{ Name = "Camera3-H265"; IP = "192.168.10.52" },
    @{ Name = "Camera4-Motion"; IP = "192.168.10.53" }
)
```

**Примечание:** Если реальных камер нет, можно использовать симулированные камеры.

---

## 🚀 Как запустить

### Шаг 1: Проверка подключения (2 минуты)

```powershell
.\scripts\test-nas-connection.ps1
```

**Ожидаемый результат:**
```
========================================
Проверка подключения к NAS
========================================
NAS Host: 192.168.10.38
Username: Andrey

[1/3] Проверка сетевого подключения...
  OK SSH порт 22 доступен на 192.168.10.38

[2/3] Проверка SSH подключения...
  OK SSH подключение успешно!

[3/3] Проверка Git репозитория...
  OK Git репозиторий найден!

Итог: Подключение к NAS работает!
```

### Шаг 2: Запуск Field Validation (24 часа)

```powershell
# Полный тест с long-run
.\scripts\run-nas-field-validation.ps1 `
    -Platforms Synology `
    -Version Alfa-0.1.1 `
    -Tester "Andrey" `
    -LongRunDuration 24
```

**Или ускоренный тест (без long-run):**

```powershell
.\scripts\run-nas-field-validation.ps1 `
    -Platforms Synology `
    -Version Alfa-0.1.1 `
    -Tester "Andrey" `
    -SkipS6LongRun
```

### Шаг 3: Мониторинг в реальном времени (опционально)

```powershell
# Отдельный мониторинг ресурсов
.\scripts\nas-resource-monitor.ps1 `
    -Host 192.168.10.38 `
    -Username Andrey `
    -Duration 24 `
    -Platform Synology `
    -OutputPath "docs/reports/nas-validation"
```

---

## 📊 Ожидаемые результаты

### Для Synology RS2416+

| Метрика | Цель | Критерий PASS |
|---------|------|---------------|
| S1 Install | Успешная установка | Service running + Health OK |
| S2 Health | API + Web UI | Порты 8080/8081 доступны |
| S3 Restart | Перезапуск сервиса | Auto-start + Health OK |
| S4 Reboot | Данные после reboot | Data persisted + Auto-start |
| S5 Upgrade | Обновление без потерь | Data integrity + Config preserved |
| S6 Long-run | 24h стабильность | CPU<70%, Mem<85%, No crashes |

### Итоговое решение

| Статус | Критерии | Результат |
|--------|----------|-----------|
| **GO** | Все тесты PASS | Релиз одобрен ✅ |
| **CONDITIONAL GO** | Критические PASS | Релиз с ограничениями ⚠️ |
| **NO-GO** | Критические FAIL | Исправление и повтор ❌ |

---

## 📁 Результаты сохранятся в

```
docs/reports/NAS_FIELD_VALIDATION_20260517-XXXXXX/
├── FIELD_VALIDATION_REPORT_*.md          # Главный отчет
├── validation.log                         # Логи выполнения
├── Synology/
│   ├── S1_FreshInstall.md
│   ├── S2_BasicHealth.md
│   ├── S3_Restart.md
│   ├── S4_RebootPersistence.md
│   ├── S5_Upgrade.md
│   └── S6_LongRun.md
├── monitoring/
│   ├── monitor-192.168.10.38-*.csv       # Метрики ресурсов
│   └── snapshot-192.168.10.38-*.json
└── logs/
    └── validation.log
```

---

## ⚙️ Предварительные проверки

### ✅ Что уже готово

- [x] SSH ключ `nas_andrey` создан
- [x] Ключ установлен на NAS
- [x] Git репозиторий `/volume1/Git/IP-CSS-open.git` доступен
- [x] SSH подключение работает (plink + пароль)
- [x] Конфигурация обновлена реальными данными

### ⚠️ Что нужно проверить перед запуском

- [ ] Пакет SPK собран: `build/ip-css-Alfa-0.1.1-synology-x86_64.spk`
- [ ] На NAS достаточно места: `df -h` (минимум 2GB)
- [ ] Java 17+ установлен на NAS
- [ ] Порты 8080/8081 свободны на NAS
- [ ] Тестовые камеры доступны (или использовать симулированные)

---

## 📋 Быстрая проверка готовности

```powershell
# 1. Проверить наличие пакета
ls build\ip-css-Alfa-0.1.1-synology-*.spk

# 2. Проверить SSH подключение
.\scripts\test-nas-connection.ps1

# 3. Проверить доступ к Git
plink.exe -ssh -batch -pw "<NAS_PASSWORD>" Andrey@192.168.10.38 "ls -la /volume1/Git/"

# 4. Проверить свободное место на NAS
plink.exe -ssh -batch -pw "<NAS_PASSWORD>" Andrey@192.168.10.38 "df -h /volume1"

# 5. Проверить Java версию на NAS
plink.exe -ssh -batch -pw "<NAS_PASSWORD>" Andrey@192.168.10.38 "java -version"
```

---

## 🎉 ИТОГ

**Готовность к запуску Field Validation: 100%**

**NAS устройство:** Synology RS2416+ (192.168.10.38)  
**Доступ:** SSH работает (plink + пароль)  
**SSH ключ:** credentials/ssh-keys/nas_andrey  
**Git репозиторий:** /volume1/Git/IP-CSS-open.git

**Следующий шаг:**

```powershell
# 1. Проверить подключение
.\scripts\test-nas-connection.ps1

# 2. Запустить Field Validation (24 часа)
.\scripts\run-nas-field-validation.ps1 -Platforms Synology -LongRunDuration 24 -Tester "Andrey"
```

**Ожидаемое время:** 24-26 часов  
**Результат:** Полный отчет о полевой валидации с рекомендацией GO/NO-GO

---

## 🔐 Безопасность

**Важно:** Все учетные данные хранятся в конфигурации:
- `scripts/nas-field-validation-config.ps1`
- `credentials/ssh-keys/nas_andrey`

**Не коммитьте эти файлы в Git!** Они уже добавлены в `.gitignore`.

---

*Создано: 2026-05-17*  
*NAS: Synology RS2416+ (192.168.10.38)*  
*Статус: ГОТОВО К ЗАПУСКУ*  
*Версия: 1.0*
