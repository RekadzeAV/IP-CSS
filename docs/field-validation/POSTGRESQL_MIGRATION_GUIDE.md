# PostgreSQL Migration — Руководство

**Дата:** 2026-05-28  
**Версия:** 1.0

---

## 📋 Что Будет Выполнено

Полная миграция на PostgreSQL включает:

1. ✅ **Валидация** — Проверка готовности системы
2. ✅ **Бэкап** — Создание резервной копии
3. ✅ **Миграция** — Применение Flyway миграций
4. ✅ **Smoke тесты** — Проверка работоспособности
5. ✅ **Rollback test** — Тестирование процедуры отката

---

## ⚡ Быстрый Старт

### Шаг 1: Подготовка PostgreSQL

**Создайте базу данных:**

```sql
-- Подключитесь к PostgreSQL
psql -U postgres

-- Создайте базу данных
CREATE DATABASE ipcamera_staging;

-- Создайте пользователя (опционально)
CREATE USER ipcamera_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE ipcamera_staging TO ipcamera_user;
\q
```

**Проверьте подключение:**

```powershell
# Проверьте, что PostgreSQL доступен
Test-NetConnection -ComputerName localhost -Port 5432

# Проверьте подключение к БД
psql -U postgres -d ipcamera_staging -c "SELECT version();"
```

---

### Шаг 2: Запуск Миграции

```powershell
# Полный запуск
.\scripts\postgresql-migration-full.ps1 `
  -StagingPostgresUrl "jdbc:postgresql://localhost:5432/ipcamera_staging" `
  -StagingUser "postgres" `
  -StagingPassword "your_password"

# Dry Run (только валидация)
.\scripts\postgresql-migration-full.ps1 `
  -StagingPostgresUrl "jdbc:postgresql://localhost:5432/ipcamera_staging" `
  -StagingUser "postgres" `
  -StagingPassword "your_password" `
  -DryRun
```

---

### Шаг 3: Проверка Результатов

```powershell
# Откройте итоговый отчёт
notepad diagnostics\postgresql-migration\postgresql-migration-report-*.md
```

---

## 📊 Параметры Команды

| Параметр | Значение по умолчанию | Описание |
|----------|----------------------|----------|
| `-StagingPostgresUrl` | `""` | **Обязательно!** JDBC URL PostgreSQL |
| `-StagingUser` | `""` | **Обязательно!** Пользователь PostgreSQL |
| `-StagingPassword` | `""` | **Обязательно!** Пароль PostgreSQL |
| `-Environment` | `"staging"` | Имя окружения (staging/production) |
| `-DryRun` | `false` | Только валидация, без изменений |
| `-SkipBackup` | `false` | Пропустить создание бэкапа |
| `-SkipSmoke` | `false` | Пропустить smoke тесты |
| `-SkipRollbackTest` | `false` | Пропустить тест отката |
| `-BackupDir` | `"backup"` | Директория для бэкапов |
| `-OutputDir` | `"diagnostics/postgresql-migration"` | Директория для отчётов |

---

## 🎯 Типовые Сценарии

### Сценарий 1: Full Migration (рекомендуется)

```powershell
.\scripts\postgresql-migration-full.ps1 `
  -StagingPostgresUrl "jdbc:postgresql://localhost:5432/ipcamera_staging" `
  -StagingUser "postgres" `
  -StagingPassword "secure_password_123"
```

**Что делает:**
- Валидация PostgreSQL подключения
- Создание бэкапа
- Применение миграций Flyway
- Smoke тесты
- Тестирование rollback

**Время выполнения:** 5-10 минут

---

### Сценарий 2: Dry Run (валидация только)

```powershell
.\scripts\postgresql-migration-full.ps1 `
  -StagingPostgresUrl "jdbc:postgresql://localhost:5432/ipcamera_staging" `
  -StagingUser "postgres" `
  -StagingPassword "secure_password_123" `
  -DryRun
```

**Что делает:**
- Проверяет формат URL
- Проверяет учётные данные
- Проверяет подключение к PostgreSQL
- Проверяет наличие БД
- Проверяет инструменты
- Проверяет свободное место

**Не изменяет ничего!**

**Время выполнения:** 1-2 минуты

---

### Сценарий 3: Production Cutover

```powershell
.\scripts\postgresql-migration-full.ps1 `
  -StagingPostgresUrl "jdbc:postgresql://prod-db.example.com:5432/ipcamera_prod" `
  -StagingUser "ipcamera_prod_user" `
  -StagingPassword "production_password" `
  -Environment "production"
```

**Что делает:**
- То же, что Full Migration
- Но для production окружения

---

### Сценарий 4: Skip Non-Critical Steps

```powershell
.\scripts\postgresql-migration-full.ps1 `
  -StagingPostgresUrl "jdbc:postgresql://localhost:5432/ipcamera_staging" `
  -StagingUser "postgres" `
  -StagingPassword "secure_password_123" `
  -SkipRollbackTest
```

**Что делает:**
- Пропускает тестирование rollback
- Полезно для экономии времени

---

## 📁 Результаты

После завершения все результаты будут в директории:

```
diagnostics/postgresql-migration/
├── postgresql-migration-report-20260528-143022.md  # Итоговый отчёт
└── postgresql-migration-report-20260528-143022.log # Лог выполнения

backup/
└── ipcamera_backup_20260528-143022.sql  # Бэкап БД

.env.staging  # Конфигурация окружения
```

---

## 🔍 Проверка Результатов

### 1. Итоговый отчёт

```powershell
# Откройте итоговый отчёт
notepad diagnostics\postgresql-migration\postgresql-migration-report-*.md
```

### 2. Ключевые метрики

Ищите в отчёте:

```
## Migration Steps

| Step | Status | Details |
|------|--------|---------|
| Validation | ✅ PASS | 6/6 checks |
| Backup | SUCCESS | backup/ipcamera_backup_20260528-143022.sql |
| Migration | SUCCESS | Flyway applied |
| Smoke Tests | ✅ PASS | 10 passed, 0 failed |
| Rollback Test | TESTED | Tested |
```

### 3. Успешное завершение

```
✅ Migration completed successfully!

Next Steps:
1. Monitor server logs for 24h
2. Verify all API endpoints
3. Test with real clients
4. Prepare production cutover plan
```

---

## 🐛 Устранение Проблем

### Проблема 1: "PostgreSQL unreachable"

**Причина:** PostgreSQL недоступен или неправильный хост/порт

**Решение:**
```powershell
# Проверьте подключение вручную
Test-NetConnection -ComputerName localhost -Port 5432

# Проверьте, что PostgreSQL запущен
# Windows:
Get-Service -Name postgresql*

# Linux:
sudo systemctl status postgresql

# Проверьте конфигурацию PostgreSQL
cat /etc/postgresql/*/main/postgresql.conf | grep listen_addresses
```

---

### Проблема 2: "Authentication failed"

**Причина:** Неверные учётные данные

**Решение:**
```powershell
# Проверьте учётные данные вручную
psql -U postgres -d ipcamera_staging -c "SELECT 1;"

# Если не работает, обновите пароль
# Windows (PostgreSQL installer):
# По умолчанию: postgres / postgres (или пароль, указанный при установке)

# Linux:
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'new_password';"
```

---

### Проблема 3: "Database does not exist"

**Причина:** База данных не создана

**Решение:**
```sql
-- Подключитесь к PostgreSQL
psql -U postgres

-- Создайте базу данных
CREATE DATABASE ipcamera_staging;

-- Проверьте
\l
```

---

### Проблема 4: "pg_dump not found"

**Причина:** PostgreSQL инструменты не в PATH

**Решение:**
```powershell
# Найдите pg_dump
Get-Command pg_dump -ErrorAction SilentlyContinue

# Если не найден, добавьте в PATH
$env:Path += ";C:\Program Files\PostgreSQL\15\bin"

# Или установите PostgreSQL
choco install postgresql15
```

---

### Проблема 5: "Flyway migration failed"

**Причина:** Ошибка в миграциях

**Решение:**
```powershell
# Проверьте логи Flyway
Get-Content build/reports/flyway/*.log | Select-Object -Last 50

# Попробуйте baseline
./gradlew flywayBaseline

# Или очистите схему Flyway (осторожно!)
./gradlew flywayClean
```

---

## 🔄 Откат Миграции

### Если что-то пошло не так:

```powershell
# 1. Остановите сервер
# (если запущен)

# 2. Восстановите конфигурацию
# Удалите или переименуйте .env.staging
Rename-Item .env.staging .env.staging.bak

# 3. Восстановите БД из бэкапа
$backupFile = "backup\ipcamera_backup_20260528-143022.sql"
psql -U postgres -d ipcamera_staging -f $backupFile

# 4. Перезапустите сервер
.\gradlew :server:api:run
```

### Автоматизированный откат:

```powershell
.\scripts\postgresql-cutover.ps1 -Operation rollback
```

---

## 📊 Интерпретация Результатов

### Статусы:

| Статус | Значение | Действие |
|--------|----------|----------|
| ✅ PASS | Проверка пройдена | Продолжать |
| ❌ FAIL | Проверка не пройдена | Исправить |
| ⚠️ WARN | Предупреждение | Проверить |
| SUCCESS | Операция успешна | Продолжать |
| SKIPPED | Пропущено | Проверить причины |
| TESTED | Протестировано | OK |

### Критерии Успеха:

| Этап | Критерий успеха |
|------|-----------------|
| Validation | 0 ошибок |
| Backup | Файл создан |
| Migration | SUCCESS |
| Smoke Tests | 0 ошибок |
| Rollback Test | TESTED |

---

## 📋 Чеклист Перед Запуском

### Обязательное:

- [ ] PostgreSQL установлен и запущен
- [ ] База данных создана
- [ ] Учётные данные известны
- [ ] PostgreSQL инструменты в PATH (pg_dump, psql)
- [ ] Свободно место на диске (≥100MB)

### Рекомендуемое:

- [ ] Dry Run выполнен успешно
- [ ] Есть резервная копия текущей БД
- [ ] Остановлены клиенты (чтобы не было записей во время миграции)
- [ ] Готовность к откату в случае проблем

---

## 💡 Советы

1. **Всегда начинайте с Dry Run** — проверяет всё без изменений
2. **Создайте бэкап вручную перед миграцией** — дополнительная безопасность
3. **Запускайте в нерабочее время** — минимизирует влияние на пользователей
4. **Сохраняйте все отчёты** — для истории и аудита
5. **Протестируйте rollback** — убедитесь, что сможете откатиться

---

## 📞 После Успешной Миграции

### 1. Мониторинг:

```powershell
# Проверьте логи сервера
Get-Content server\api\build\logs\app.log -Tail 100 -Wait

# Проверьте подключение к БД
curl http://localhost:8080/api/v1/health
```

### 2. Smoke тесты:

```powershell
.\scripts\migration-smoke-test.ps1 -BaseUrl http://localhost:8080
```

### 3. Документирование:

- Обновите `docs/reports/POSTGRESQL_CUTOVER_REPORT_*.md`
- Добавьте результаты в `FIELD_VALIDATION_SUMMARY.md`

---

## 📚 Связанная Документация

- PostgreSQL Cutover Script *(утерян/в архиве)*
- Migration Smoke Test *(утерян/в архиве)*
- [Чеклист Перехода к Фазе 2](../planning/PHASE1_TO_PHASE2_CHECKLIST.md)

---

*Руководство создано: 2026-05-28*  
*Версия: 1.0*
