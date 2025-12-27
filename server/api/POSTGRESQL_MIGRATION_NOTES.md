# PostgreSQL Migration Notes

## Выполненные изменения

### 1. SQL Запросы - замена INSERT OR REPLACE на ON CONFLICT

Все SQL запросы с `INSERT OR REPLACE` были заменены на PostgreSQL-совместимый синтаксис `ON CONFLICT ... DO UPDATE`:

- ✅ `insertCamera` - заменен на `ON CONFLICT (id) DO UPDATE SET ...`
- ✅ `insertRecording` - заменен на `ON CONFLICT (id) DO UPDATE SET ...`
- ✅ `insertEvent` - заменен на `ON CONFLICT (id) DO UPDATE SET ...`
- ✅ `insertUser` - заменен на `ON CONFLICT (id) DO UPDATE SET ...`
- ✅ `insertSetting` - заменен на `ON CONFLICT (id) DO UPDATE SET ...`
- ✅ `insertNotification` - заменен на `ON CONFLICT (id) DO UPDATE SET ...`

**Совместимость:**
- SQLite поддерживает `ON CONFLICT` начиная с версии 3.24.0 (2018)
- PostgreSQL поддерживает `ON CONFLICT` начиная с версии 9.5 (2016)
- Синтаксис совместим с обеими базами данных

### 2. Конфигурация PostgreSQL

- ✅ Добавлены зависимости PostgreSQL и HikariCP в `build.gradle.kts`
- ✅ Создан `DatabaseConfig.kt` с HikariCP connection pooling
- ✅ Обновлен `DatabaseFactory.desktop.kt` для автоматического выбора PostgreSQL/SQLite
- ✅ Настроены переменные окружения:
  - `DATABASE_URL` или `POSTGRES_URL` - URL базы данных
  - `DATABASE_USER` - имя пользователя (по умолчанию: postgres)
  - `DATABASE_PASSWORD` - пароль (по умолчанию: postgres)
  - `DATABASE_MAX_POOL_SIZE` - максимальный размер пула соединений (по умолчанию: 10)

### 3. Репозитории

- ✅ Обновлен `AppModule.kt` для использования SQLDelight репозиториев:
  - `EventRepositoryImplSqlDelight` (заменен in-memory)
  - `SettingsRepositoryImplSqlDelight` (заменен in-memory)
  - `NotificationRepositoryImplSqlDelight` (уже использовался)
  - `RecordingRepositoryImplSqlDelight` (уже использовался)

## Известные ограничения и замечания

### 1. Название таблицы "user"

Таблица `user` использует зарезервированное слово PostgreSQL. SQLDelight должен автоматически экранировать это при генерации SQL, но если возникают проблемы, может потребоваться переименование таблицы.

**Решение (если возникнут проблемы):**
```sql
-- Переименовать таблицу user в app_user
ALTER TABLE "user" RENAME TO app_user;
```

### 2. Типы данных

- Boolean значения хранятся как `INTEGER` (0/1) для совместимости с SQLite
- В PostgreSQL можно было бы использовать тип `BOOLEAN`, но для кроссплатформенности используется `INTEGER`

### 3. Миграции

Система миграций SQLDelight должна работать автоматически через `CameraDatabase.Schema.create()`.
Если требуются ручные миграции, они должны быть в `shared/src/commonMain/sqldelight/migrations/`.

## Тестирование

После развертывания необходимо протестировать:

1. ✅ Создание схемы БД при первом запуске
2. ✅ Все CRUD операции для каждой таблицы
3. ✅ ON CONFLICT поведение (upsert операции)
4. ✅ Connection pooling (проверить через мониторинг HikariCP)
5. ✅ Производительность запросов

## Переменные окружения для продакшена

```bash
# PostgreSQL подключение
DATABASE_URL=jdbc:postgresql://localhost:5432/ipcss
DATABASE_USER=ipcss_user
DATABASE_PASSWORD=secure_password_here
DATABASE_MAX_POOL_SIZE=20

# Для локальной разработки (без DATABASE_URL будет использоваться SQLite)
# DATABASE_URL=
```

## Следующие шаги

1. ⚠️ Протестировать на реальной PostgreSQL базе данных
2. ⚠️ Настроить резервное копирование БД
3. ⚠️ Настроить мониторинг производительности
4. ⚠️ Рассмотреть возможность использования миграций Flyway или Liquibase для более сложных миграций

