# Сводка: Настройка Field Validation для NAS платформ

**Дата создания:** 2026-05-17  
**Статус:** ✅ Готово к запуску  
**Версия:** Alfa-0.1.1

---

## 📦 Созданные скрипты

### 1. Конфигурация и обнаружение

| Скрипт | Описание | Статус |
|--------|----------|--------|
| `scripts/nas-field-validation-config.ps1` | Основной файл конфигурации (IP устройств, камеры, мониторинг) | ✅ Создан |
| `scripts/discover-nas-devices.ps1` | Автообнаружение NAS и камер в сети | ✅ Создан |

### 2. Тестирование и мониторинг

| Скрипт | Описание | Статус |
|--------|----------|--------|
| `scripts/nas-resource-monitor.ps1` | Мониторинг ресурсов NAS (CPU, Memory, Disk) | ✅ Создан |
| `scripts/run-nas-field-validation.ps1` | Главный оркестратор field validation | ✅ Создан |

### 3. Вспомогательные скрипты (существующие)

| Скрипт | Описание | Расположение |
|--------|----------|--------------|
| `scripts/archive/nas-field-aggregate.ps1` | Агрегация отчетов полей тестов | ✅ Существует |
| `scripts/test-nas-build.ps1` | Проверка структуры NAS пакетов | ✅ Существует |
| `scripts/archive/build-nas-package.ps1` | Сборка NAS пакетов | ✅ Существует |

### 4. Документация

| Файл | Описание | Расположение |
|------|----------|--------------|
| `docs/reports/FIELD_VALIDATION_QUICK_START.md` | Быстрый старт для тестирования | ✅ Создан |

---

## 🎯 План запуска первой итерации

### Этап 1: Подготовка (15 минут)

```powershell
# 1. Проверьте SSH доступ
ssh-keygen -t rsa -b 4096  # Если нет ключей

# 2. Скопируйте ключи на NAS
ssh-copy-id -i ~/.ssh/id_rsa.pub admin@192.168.1.100  # Synology
ssh-copy-id -i ~/.ssh/id_rsa.pub admin@192.168.1.101  # QNAP (если есть)

# 3. Проверьте подключение
ssh -i ~/.ssh/id_rsa admin@192.168.1.100 "uname -a"
```

### Этап 2: Обнаружение устройств (5 минут)

```powershell
# Автообнаружение в вашей сети
.\scripts\discover-nas-devices.ps1 -ScanNAS -ScanCameras -NetworkRange 192.168.1.0/24

# Результат: docs/reports/network-discovery-YYYYMMDD-HHMMSS.json
```

### Этап 3: Настройка конфигурации (5 минут)

```powershell
# Отредактируйте конфигурацию
notepad scripts\nas-field-validation-config.ps1

# Измените IP адреса под вашу сеть:
# $SynologyConfig.Host = "ваш-ip"
# $TestCameras = @(ваше-список-камер)
```

### Этап 4: Сборка пакетов (10 минут)

```powershell
# Соберите NAS пакеты
.\gradlew.bat :server:api:build

# Соберите Synology пакет
.\scripts\archive\build-nas-package.ps1 -PackageType synology -Arch x86_64 -Version Alfa-0.1.1

# Проверьте пакеты
.\scripts\test-nas-build.ps1 -PackageType synology -Arch x86_64
```

### Этап 5: Запуск Field Validation (24 часа)

```powershell
# Запустите полный тест (рекомендуется)
.\scripts\run-nas-field-validation.ps1 `
    -Platforms Synology `
    -Version Alfa-0.1.1 `
    -Tester "YourName" `
    -LongRunDuration 24 `
    -OutputDir "docs/reports"

# Или ускоренный тест (без long-run)
.\scripts\run-nas-field-validation.ps1 `
    -Platforms Synology `
    -Version Alfa-0.1.1 `
    -SkipS6LongRun `
    -Tester "YourName"
```

### Этап 6: Мониторинг в реальном времени (опционально)

```powershell
# Отдельный мониторинг ресурсов
.\scripts\nas-resource-monitor.ps1 `
    -Host 192.168.1.100 `
    -Duration 24 `
    -Interval 60 `
    -Platform Synology `
    -OutputPath "docs/reports/nas-validation"
```

### Этап 7: Анализ результатов (15 минут)

```powershell
# Обновите агрегатор
.\scripts\archive\nas-field-aggregate.ps1 -Date 2026-05-17

# Откройте отчет
notepad docs\reports\NAS_FIELD_AGGREGATOR_2026-05-17.md

# Откройте детальный отчет
notepad docs\reports\NAS_FIELD_VALIDATION_2026-05-17\FIELD_VALIDATION_REPORT_20260517-*.md
```

---

## 📊 Ожидаемые результаты

### S1 - Fresh Install
- ✅ Пакет установлен на NAS
- ✅ Сервис запущен автоматически
- ✅ Health endpoint доступен

### S2 - Basic Health
- ✅ API работает (порт 8081)
- ✅ Web UI доступен (порт 8080)
- ✅ Логи создаются

### S3 - Restart
- ✅ Сервис можно перезапустить
- ✅ После重启 service auto-starts
- ✅ Health OK после перезапуска

### S4 - Reboot Persistence
- ✅ Автозапуск после reboot системы
- ✅ Данные сохраняются после reboot
- ✅ Конфигурация сохраняется

### S5 - Upgrade
- ✅ Обновление без потери данных
- ✅ Конфигурация мигрирована
- ✅ Новая версия работает

### S6 - Long-run (24h)
- ✅ Memory stable (<85% max)
- ✅ CPU stable (<70% avg)
- ✅ No crashes
- ✅ Записи целы

---

## 🚨 Критические проверки перед запуском

### Сетевые
- [ ] NAS устройства доступны по сети
- [ ] Камеры подключены и отвечают
- [ ] Порты 8080/8081 свободны на NAS

### SSH
- [ ] SSH ключи сгенерированы
- [ ] Ключи скопированы на все NAS
- [ ] Доступ без пароля работает

### Пакеты
- [ ] SPK/QPKG/APK пакеты собраны
- [ ] Проверка структуры прошла успешно
- [ ] Версии синхронизированы

### Ресурсы
- [ ] На NAS достаточно места (>=2GB)
- [ ] Java 17+ установлен на NAS
- [ ] Node.js установлен (для Web UI, опционально)

---

## 📈 Метрики успеха

### Автоматизированная часть (без field validation)
- **Статус:** ✅ GO (100%)
- **NAS сборка:** PASS
- **Smoke prechecks:** PASS (15/15)

### Полевая валидация (S2-S6)
- **Статус:** ⚪ PENDING (требуется оборудование)
- **Ожидаемое время:** 24-48 часов
- **Результат:** GO/CONDITIONAL GO/NO-GO

---

## 🛠️ Troubleshooting

### SSH не работает
```powershell
# Проверьте ключ
ls ~/.ssh/id_rsa

# Пересоздайте если нужно
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N ""

# Скопируйте заново
ssh-copy-id -i ~/.ssh/id_rsa.pub admin@192.168.1.100
```

### Пакет не устанавливается
```powershell
# Проверьте зависимости на NAS
ssh admin@192.168.1.100 "java -version"
ssh admin@192.168.1.100 "df -h"

# Проверьте логи
ssh admin@192.168.1.100 "cat /var/log/pkginstall.log"
```

### Сервис не запускается
```powershell
# Проверьте логи
ssh admin@192.168.1.100 "sudo journalctl -u ipcamera -n 100"

# Проверьте порты
ssh admin@192.168.1.100 "netstat -tlnp | grep 808"

# Перезапустите вручную
ssh admin@192.168.1.100 "sudo systemctl restart ipcamera"
```

---

## 📞 Следующие шаги

### После успешного запуска

1. **Обновите агрегатор отчетов**
   ```powershell
   .\scripts\archive\nas-field-aggregate.ps1 -Date 2026-05-17
   ```

2. **Создайте финальный отчет**
   - Откройте `docs/reports/NAS_FIELD_AGGREGATOR_2026-05-17.md`
   - Заполните фактические результаты
   - Определите GO/NO-GO решение

3. **Подготовьтесь к следующей итерации**
   - Если NO-GO: исправьте проблемы и повторите
   - Если GO: переходите к production deployment
   - Если CONDITIONAL GO: документировать known limitations

### Для других платформ

```powershell
# Запустите для QNAP
.\scripts\run-nas-field-validation.ps1 -Platforms QNAP -LongRunDuration 24

# Запустите для всех платформ параллельно
.\scripts\run-nas-field-validation.ps1 -Platforms Synology,QNAP,Asustor,TrueNAS -LongRunDuration 24
```

---

## ✅ Готовность к запуску

| Компонент | Статус | Примечание |
|-----------|--------|------------|
| Скрипты конфигурации | ✅ Готово | `nas-field-validation-config.ps1` |
| Скрипты обнаружения | ✅ Готово | `discover-nas-devices.ps1` |
| Скрипты мониторинга | ✅ Готово | `nas-resource-monitor.ps1` |
| Главный оркестратор | ✅ Готово | `run-nas-field-validation.ps1` |
| Документация | ✅ Готово | `FIELD_VALIDATION_QUICK_START.md` |
| SSH доступ | ⚪ Требуется настройка | Настройте под вашу сеть |
| NAS оборудование | ⚪ Требуется | Минимум 1 Synology |
| Тестовые камеры | ⚪ Требуется | Минимум 2-4 камеры |

---

**ИТОГО:** Все скрипты готовы к запуску!

**Следующий шаг:** Настройте SSH доступ и IP адреса в вашей сети, затем запустите:

```powershell
.\scripts\run-nas-field-validation.ps1 -Platforms Synology -LongRunDuration 24
```

**Ожидаемое время:** 24-48 часов для полного теста  
**Результат:** Полный отчет о полевой валидации с рекомендацией GO/NO-GO

---

*Создано: 2026-05-17*  
*Версия: 1.0*  
*Статус: Готово к исполнению*
