# Migration Guide для Production

**Версия:** 1.0  
**Дата:** 27 April 2026  
**Актуально для:** SQLDelight migrations, PostgreSQL cutover

---

## 📋 Содержание

1. [Введение](#введение)
2. [Подготовка к миграции](#подготовка-к-миграции)
3. [Процесс миграции](#процесс-миграции)
4. [Откат миграции](#откат-миграции)
5. [Troubleshooting](#troubleshooting)
6. [Best Practices](#best-practices)

---

## Введение

Этот документ описывает процесс миграции базы данных в production окружении IP-CSS.

**Что покрывает:**
- ✅ SQLDelight migrations (SQLite → PostgreSQL)
- ✅ Версионирование схемы БД
- ✅ MigrationManager для автоматических миграций
- ✅ Production smoke тесты
- ✅ Rollback procedure

**Не покрывает:**
- ❌ Данные пользователя (нужен отдельный backup)
- ❌ Миграция конфигурации (см. CONFIGURATION_MIGRATION_GUIDE.md)

---

## Подготовка к миграции

### 1. Проверка текущей версии БД

```bash
# Проверка версии SQLite
sqlite3 data/surveillance.db "PRAGMA user_version;"

# Проверка версии PostgreSQL
psql -U surveillance -d surveillance -c "SELECT version FROM schema_version;"
```

### 2. Создание backup

**SQLite:**
```bash
# Полный backup
cp data/surveillance.db data/surveillance.backup.$(date +%Y%m%d_%H%M%S)

# Опционально: сжатие
gzip data/surveillance.backup.*
```

**PostgreSQL:**
```bash
# Backup всех баз
pg_dumpall -U surveillance > backup_all_$(date +%Y%m%d_%H%M%S).sql

# Backup конкретной базы
pg_dump -U surveillance surveillance > backup_surveillance_$(date +%Y%m%d_%H%M%S).sql

# Backup только данных (без схемы)
pg_dump -U surveillance --data-only surveillance > backup_data_$(date +%Y%m%d_%H%M%S).sql
```

### 3. Проверка production smoke тестов

```bash
# Запуск smoke тестов
./gradlew :shared:desktopTest --tests "*MigrationProductionSmokeTest*"

# Ожидаемый результат: все тесты PASS
# ✅ fresh migration
# ✅ legacy migration
# ✅ idempotency
# ✅ downgrade guard
# ✅ bulk operations performance
```

### 4. Проверка свободного места

```bash
# SQLite
df -h data/

# PostgreSQL
df -h /var/lib/postgresql/data
```

**Требуемое место:** минимум 2x от текущего размера БД

### 5. Уведомление пользователей

Перед миграцией в production:
- Отправить уведомление пользователям о планируемом простое
- Отключить новые подключения к системе
- Дождаться завершения всех активных записей

---

## Процесс миграции

### Вариант A: Автоматическая миграция (рекомендуется)

SQLDelight автоматически применяет миграции при запуске через `MigrationManager`.

```bash
# 1. Остановить сервис
docker compose stop surveillance

# 2. Запустить миграцию
docker compose run --rm surveillance \
  java -jar app.jar --migrate

# 3. Проверить статус
docker compose run --rm surveillance \
  java -jar app.jar --migration-status

# 4. Запустить сервис
docker compose up -d surveillance
```

### Вариант B: Ручная миграция

```sql
-- 1. Проверить текущую версию
PRAGMA user_version; -- или SELECT version FROM schema_version;

-- 2. Применить миграцию (пример для v1 → v2)
-- Файл миграции: migrations/1_to_2.sql

sqlite3 data/surveillance.db < migrations/1_to_2.sql

-- 3. Обновить версию
PRAGMA user_version = 2;

-- 4. Проверить успешность
SELECT COUNT(*) FROM cameras;
```

### Вариант C: PostgreSQL cutover

**Runbook:** см. `POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md`

```bash
# 1. Подготовить PostgreSQL
docker compose up -d postgres

# 2. Запустить миграцию через Flyway
docker compose run --rm surveillance \
  flyway -url=jdbc:postgresql://postgres:5432/surveillance \
         -user=surveillance \
         -password=${DB_PASSWORD} \
         migrate

# 3. Проверить статус
docker compose logs surveillance | grep "Flyway"

# 4. Переключить приложение на PostgreSQL
# В .env изменить:
# DATABASE_URL=jdbc:postgresql://postgres:5432/surveillance
# DB_MODE=postgres

# 5. Перезапустить сервис
docker compose restart surveillance
```

---

## Откат миграции

### Автоматический откат (если поддерживается)

```bash
# Проверка возможности отката
java -jar app.jar --can-downgrade-to 1

# Откат
java -jar app.jar --downgrade-to 1
```

### Ручной откат

**SQLite:**
```bash
# 1. Остановить сервис
docker compose stop surveillance

# 2. Восстановить backup
cp data/surveillance.backup.* data/surveillance.db

# 3. Проверить целостность
sqlite3 data/surveillance.db "PRAGMA integrity_check;"

# 4. Запустить сервис
docker compose up -d surveillance
```

**PostgreSQL:**
```bash
# 1. Остановить сервис
docker compose stop surveillance

# 2. Восстановить из backup
psql -U surveillance -d surveillance < backup_surveillance_*.sql

# 3. Проверить целостность
psql -U surveillance -d surveillance -c "SELECT COUNT(*) FROM cameras;"

# 4. Запустить сервис
docker compose up -d surveillance
```

### Откат через Flyway

```bash
# Откат на одну версию
flyway -url=jdbc:postgresql://postgres:5432/surveillance \
       -user=surveillance \
       -password=${DB_PASSWORD} \
       undo

# Откат на конкретную версию
flyway -url=jdbc:postgresql://postgres:5432/surveillance \
       -user=surveillance \
       -password=${DB_PASSWORD} \
       migrate -target=1
```

---

## Troubleshooting

### Ошибка: Migration failed

**Симптомы:**
```
Migration failed: Table 'cameras' already exists
```

**Решение:**
```bash
# 1. Проверить текущую версию
sqlite3 data/surveillance.db "PRAGMA user_version;"

# 2. Очистить миграции (осторожно! только для dev)
rm -rf data/migrations/*.done

# 3. Перезапустить миграцию
java -jar app.jar --migrate --force
```

### Ошибка: Column already exists

**Симптомы:**
```
Migration failed: Column 'new_column' already exists
```

**Решение:**
```sql
-- Проверить существование колонки
PRAGMA table_info(cameras);

-- Если колонка есть, пропустить миграцию
-- Отредактировать migration script и добавить guard:
-- ALTER TABLE cameras ADD COLUMN new_column TEXT; -- ONLY IF NOT EXISTS
```

### Ошибка: Constraint violation

**Симптомы:**
```
Migration failed: FOREIGN KEY constraint failed
```

**Решение:**
```bash
# 1. Найти нарушающие записи
sqlite3 data/surveillance.db "
  SELECT * FROM recordings 
  WHERE camera_id NOT IN (SELECT id FROM cameras);
"

# 2. Удалить или исправить нарушающие записи
sqlite3 data/surveillance.db "
  DELETE FROM recordings 
  WHERE camera_id NOT IN (SELECT id FROM cameras);
"

# 3. Перезапустить миграцию
```

### Ошибка: Out of disk space

**Симптомы:**
```
Migration failed: disk full
```

**Решение:**
```bash
# 1. Освободить место
du -sh data/*
rm -rf data/logs/*.log.old

# 2. Проверить свободное место
df -h

# 3. Перезапустить миграцию
```

### Ошибка: Data loss после отката

**Симптомы:**
```
Записи пропали после rollback
```

**Решение:**
```bash
# 1. Проверить backup
ls -lh data/surveillance.backup.*

# 2. Восстановить из последнего backup
cp data/surveillance.backup.YYYYMMDD_HHMMSS data/surveillance.db

# 3. Проверить целостность
sqlite3 data/surveillance.db "PRAGMA integrity_check;"

# 4. Если backup нет, восстановить из PostgreSQL dump
pg_restore -U surveillance -d surveillance backup_surveillance_*.dump
```

---

## Best Practices

### 1. Тестирование миграций

**Всегда тестируйте миграции перед production:**

```bash
# 1. Создать test БД
sqlite3 test.db "PRAGMA user_version = 1;"

# 2. Вставить тестовые данные
# 3. Применить миграцию
# 4. Проверить целостность данных
# 5. Удалить test.db
```

### 2. Idempotency

**Миграции должны быть идемпотентными:**

```sql
-- ❌ Плохо: повторный запуск вызовет ошибку
CREATE TABLE cameras (...);

-- ✅ Хорошо: проверьте существование таблицы
CREATE TABLE IF NOT EXISTS cameras (...);
```

### 3. Backwards compatibility

**Новая версия приложения должна работать со старой схемой:**

```kotlin
// ✅ Хорошо: graceful degradation
val columnValue = if (columnExists("new_column")) {
    row.getString("new_column")
} else {
    defaultValue
}
```

### 4. Monitoring

**Мониторьте миграции в production:**

```bash
# Логирование миграций
tail -f logs/app.log | grep "Migration"

# Проверка статуса
curl http://localhost:8080/api/v1/health | jq .database

# Метрики
curl http://localhost:8080/api/v1/metrics | grep migration
```

### 5. Rollback rehearsal

**Регулярно тренируйте откат:**

```bash
# 1. Сделать backup
# 2. Применить миграцию
# 3. Проверить работу
# 4. Откатить
# 5. Проверить что backup работает
# 6. Повторить миграцию
```

---

## Checklists

### Перед миграцией

- [ ] Backup создан и проверен
- [ ] Production smoke тесты пройдены
- [ ] Свободно >2x места от размера БД
- [ ] Пользователи уведомлены
- [ ] Новые подключения отключены
- [ ] Active записи завершены
- [ ] Rollback procedure протестирован

### После миграции

- [ ] Версия БД обновлена
- [ ] Все таблицы созданы
- [ ] Все индексы созданы
- [ ] Данные сохранены (проверка COUNT)
- [ ] Smoke тесты пройдены
- [ ] Приложения работают
- [ ] Миграции logged
- [ ] Monitoring активен

### При откате

- [ ] Backup восстановлен
- [ ] Целостность проверена
- [ ] Версия БД откатилась
- [ ] Приложения работают
- [ ] Данные восстановлены
- [ ] Инцидент задокументирован

---

## Связанные документы

- [MigrationManager Integration Test](../../shared/src/desktopTest/kotlin/com/company/ipcamera/shared/data/local/MigrationManagerIntegrationTest.kt)
- Migration Production Smoke Test *(утерян/в архиве)*
- [POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md](../planning/POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md)
- [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) §1.3

---

## История изменений

| Версия | Дата | Изменение | Автор |
|--------|------|-----------|-------|
| 1.0 | 2026-04-27 | Initial version | NLP-Core-Team |

---

**Последнее обновление:** 27 April 2026  
**Следующая проверка:** 2026-07-27
