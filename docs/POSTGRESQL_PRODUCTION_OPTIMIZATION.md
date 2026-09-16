# PostgreSQL Production Optimization Guide

**Дата:** 2026-01-28  
**Статус:** ✅ Завершено  
**Версия:** 1.0

---

## 📋 Обзор

Этот документ описывает оптимизации PostgreSQL для production-развертывания IP-CSS.

### Что было добавлено:

1. **Миграция V6** - Production оптимизации
2. **DatabasePerformanceService** - Мониторинг производительности
3. **Материализованные представления** - Пре-вычисленные агрегаты
4. **Автоматический vacuum** - Настройки для high-write таблиц
5. **Мониторинг пула соединений** - HikariCP метрики

---

## 🚀 Быстрый старт

### 1. Настройка PostgreSQL

```bash
# .env.production
DATABASE_URL=jdbc:postgresql://localhost:5432/ipcss
DATABASE_USER=postgres
DATABASE_PASSWORD=your_secure_password
DATABASE_MAX_POOL_SIZE=20
ENABLE_FLYWAY=true
```

### 2. Применение миграций

Миграции применяются автоматически при старте сервера через Flyway.

```bash
# Запуск сервера
cd server/api
./gradlew run
```

### 3. Мониторинг производительности

DatabasePerformanceService запускается автоматически и выполняет:

- Обновление материализованных представлений (каждые 5 минут)
- Мониторинг пула соединений (каждую 1 минуту)
- Проверки здоровья БД (каждые 10 минут)

---

## 📊 Материализованные представления

### camera_statistics_mv

Пре-вычисленная статистика по камерам:

```sql
SELECT * FROM camera_statistics_mv WHERE camera_id = 'camera-123';
```

**Поля:**
- `total_recordings` - Всего записей
- `total_recording_duration` - Общая длительность записей
- `total_storage_used` - Общее использование хранилища
- `total_events` - Всего событий
- `unacknowledged_events` - Неподтвержденные события
- `last_recording_time` - Последняя запись
- `last_event_time` - Последнее событие

### recent_events_mv

События за последние 24 часа:

```sql
SELECT * FROM recent_events_mv WHERE camera_id = 'camera-123' ORDER BY timestamp DESC LIMIT 50;
```

---

## 🔧 Конфигурация производительности

### Переменные окружения

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `DB_SLOW_QUERY_THRESHOLD_MS` | 1000 | Порог медленного запроса (мс) |
| `DATABASE_MAX_POOL_SIZE` | 10 | Максимальный размер пула соединений |
| `DATABASE_MIN_IDLE` | 2 | Минимальное количество idle соединений |
| `ENABLE_FLYWAY` | true | Включить миграции Flyway |

### Рекомендуемые настройки PostgreSQL (postgresql.conf)

```conf
# Memory Settings
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 512MB
work_mem = 16MB

# Connection Settings
max_connections = 100

# WAL Settings
wal_buffers = 16MB
checkpoint_completion_target = 0.9

# Logging
log_min_duration_statement = 1000  # Логировать запросы > 1 сек
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on
```

---

## 📈 Мониторинг

### Проверка здоровья БД

```sql
SELECT * FROM db_health_check();
```

**Результат:**
- `database_size` - Размер базы данных
- `table_count` - Количество таблиц
- `last_vacuum` - Время последнего vacuum

### Мониторинг пула соединений

```sql
SELECT * FROM connection_pool_metrics ORDER BY recorded_at DESC LIMIT 10;
```

### Логирование медленных запросов

```sql
-- Найти топ-10 медленных запросов
SELECT query_text, execution_time_ms, recorded_at 
FROM query_performance_log 
WHERE is_slow_query = 1 
ORDER BY recorded_at DESC 
LIMIT 10;
```

---

## 🔨 Partitioning для таблицы recording

Для больших объемов данных (100K+ записей) рекомендуется partitioning по месяцам.

### Создание партиций

Автоматически создается миграцией V6 для следующих 12 месяцев.

### Добавление новой партиции

```sql
-- Добавить партицию для следующего месяца
CREATE TABLE recording_2026_03 PARTITION OF recording_part 
FOR VALUES FROM (1740787200) TO (1743465600);
```

### Преимущества

- Ускорение查询 по временным диапазонам
- Быстрая очистка старых данных (DROP TABLE вместо DELETE)
- Уменьшение размера индексов

---

## 🧹 Автоматический Vacuum

### Настройки для high-write таблиц

| Таблица | vacuum_scale_factor | analyze_scale_factor |
|---------|---------------------|----------------------|
| `camera` | 0.1 | 0.1 |
| `recording` | 0.05 | 0.02 |
| `event` | 0.05 | 0.02 |
| `notification` | 0.1 | 0.05 |

### Мониторинг vacuum

```sql
SELECT 
    relname,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE relname IN ('camera', 'recording', 'event', 'notification');
```

---

## 🎯 Рекомендации для Production

### 1. Размеры пула соединений

| Сценарий | max_pool_size | min_idle |
|----------|---------------|----------|
| Small (1-10 камер) | 10 | 2 |
| Medium (10-50 камер) | 20 | 5 |
| Large (50-200 камер) | 50 | 10 |
| Enterprise (200+ камер) | 100 | 20 |

### 2. Read Replica (опционально)

Для распределения нагрузки чтения:

```bash
DATABASE_READ_REPLICA_URL=jdbc:postgresql://replica:5432/ipcss
DATABASE_READ_REPLICA_POOL_SIZE=10
```

### 3. Backup стратегия

Используйте `DatabaseBackupService` для автоматических backup:

```bash
# Ежедневный backup в 2:00
0 2 * * * pg_dump ipcss > /backups/ipcss_$(date +\%Y\%m\%d).sql
```

### 4. Мониторинг метрик

Интегрируйте с Prometheus/Grafana через JMX:

```yaml
# docker-compose.monitoring.yml
services:
  jmx_exporter:
    image: bitnami/jmx-exporter
    ports:
      - "9999:9999"
```

---

## 🐛 Troubleshooting

### Проблема: Медленные запросы

**Решение:**
1. Проверить `query_performance_log`
2. Добавить недостающие индексы
3. Увеличить `work_mem`
4. Выполнить `ANALYZE` на таблицах

### Проблема: Исчерпан пул соединений

**Решение:**
1. Увеличить `DATABASE_MAX_POOL_SIZE`
2. Проверить на утечки соединений
3. Оптимизировать долгие запросы
4. Добавить read replica

### Проблема: Большой размер БД

**Решение:**
1. Включить partitioning для `recording`
2. Настроить автоматическую очистку старых записей
3. Сжать старые партиции
4. Использовать `VACUUM FULL` (в maintenance окно)

---

## 📚 Связанные документы

- DatabaseConfig.kt *(утерян/в архиве)*
- DatabasePerformanceService.kt *(утерян/в архиве)*
- V6__Add_postgresql_production_optimizations.sql *(утерян/в архиве)*

---

**Подготовлено:** NLP-Core-Team  
**Дата:** 2026-01-28
