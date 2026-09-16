# 📊 Система логирования сборок IP-CSS

## Обзор

Система автоматически ведёт структурированные логи всех сборок для:
- **Поиска ошибок** и проблем
- **Анализа производительности** сборок
- **Отслеживания трендов** (успешность, время сборки)
- **Сравнения сборок** между собой

## 📁 Структура

```
IP-CSS/
├── build-logs/                    # Директория логов (создаётся автоматически)
│   ├── build_20250115_120000.log # Полный лог сборки
│   ├── build_20250115_120000.json # JSON-отчёт для анализа
│   └── ...
├── scripts/
│   ├── build-logger.sh           # Библиотека логирования
│   ├── analyze-build-logs.sh     # Анализатор логов
│   ├── build-pipeline.sh         # Основной скрипт (использует логирование)
│   └── ...
```

## 🔧 Использование

### Запуск сборки с логированием

```bash
# Linux/macOS
./scripts/build-pipeline.sh

# Лог автоматически создается в build-logs/build_YYYYMMDD_HHMMSS.log
```

### Просмотр логов

```bash
# Последняя сборка
./scripts/analyze-build-logs.sh latest

# Краткая сводка
./scripts/analyze-build-logs.sh summary

# Временная шкала последних сборок
./scripts/analyze-build-logs.sh timeline
```

### Статистика

```bash
# Общая статистика за 30 дней
./scripts/analyze-build-logs.sh stats

# За последнюю неделю
./scripts/analyze-build-logs.sh stats 7
```

**Пример вывода:**
```
=== Build Statistics (Last 7 Days) ===

Total Builds:      15
Successful:        12
Failed:            3
Success Rate:      80%

Build Duration Stats:
      5 15m 30s
      3 12m 15s
      2 20m 00s

Most Common Errors:
      3 CMake configuration failed
      2 OutOfMemoryError
      1 FFmpeg not found
```

### Поиск ошибок

```bash
# Все ошибки за последние 7 дней
./scripts/analyze-build-logs.sh errors

# Поиск по паттерну (например, ошибки памяти)
./scripts/analyze-build-logs.sh errors "OutOfMemory"

# Поиск FFmpeg ошибок за месяц
./scripts/analyze-build-logs.sh errors "FFmpeg" 30
```

**Пример вывода:**
```
=== Searching for Errors (Last 7 Days) ===

--- 20250115_120000 (2025-01-15) ---
[2025-01-15 12:05:30] [ERROR] CMake configuration failed
[2025-01-15 12:05:31] [ERROR] FFmpeg not found

--- 20250114_100000 (2025-01-14) ---
[2025-01-14 10:15:00] [ERROR] OutOfMemoryError: Java heap space
```

### Сравнение сборок

```bash
./scripts/analyze-build-logs.sh compare 20250115_120000 20250114_100000
```

**Пример вывода:**
```
=== Comparing Builds ===

Build 20250115_120000:
  Status:    SUCCESS
  Duration:  15m 30s
  Errors:    0
  Warnings:  2

Build 20250114_100000:
  Status:    FAILED
  Duration:  5m 12s
  Errors:    3
  Warnings:  1

Differences:
  < SAFE_JOBS_CPP: 4
  > SAFE_JOBS_CPP: 2
```

### Медленные сборки

```bash
# Топ 10 самых медленных за месяц
./scripts/analyze-build-logs.sh slow 30
```

### Неудачные сборки

```bash
# Показать все неудачные сборки за неделю
./scripts/analyze-build-logs.sh failed 7
```

### Предупреждения

```bash
# Показать все предупреждения за последние 7 дней
./scripts/analyze-build-logs.sh warnings 7
```

### Экспорт логов

```bash
# Экспорт за последние 7 дней
./scripts/analyze-build-logs.sh export 7

# Создает директорию logs-export-YYYYMMDD_HHMMSS/
```

### Очистка старых логов

```bash
# Удалить логи старше 30 дней
./scripts/analyze-build-logs.sh clean 30
```

## 📝 Формат логов

### Лог-файл (.log)

```
================================================================
                    BUILD LOG - IP-CSS
================================================================
Build ID:              20250115_120000
Start Time:            2025-01-15 12:00:00
Host:                  build-server
User:                  developer
OS:                    Linux 5.4.0
Working Directory:     /home/user/ip-css
================================================================

[2025-01-15 12:00:00] [INFO] IP-CSS Build Pipeline Starting
[2025-01-15 12:00:00] [INFO] Build ID: 20250115_120000

================================================================
STAGE: Environment Detection
Started: 2025-01-15 12:00:01
================================================================
[2025-01-15 12:00:01] [INFO] === Stage 'Environment Detection' started ===
[2025-01-15 12:00:01] [SUCCESS] Java 17
[2025-01-15 12:00:01] [SUCCESS] CMake 3.22
[2025-01-15 12:00:01] [SUCCESS] Node.js 20
...

----------------------------------------------------------------
Stage: Environment Detection
Ended: 2025-01-15 12:00:05
Exit Code: 0
----------------------------------------------------------------

================================================================
                    BUILD SUMMARY
================================================================
End Time:              2025-01-15 12:15:30
Exit Code:             0

Status:                  SUCCESS
================================================================
```

### JSON-отчёт (.json)

```json
{
    "build_id": "20250115_120000",
    "timestamp": "2025-01-15T12:15:30+03:00",
    "exit_code": 0,
    "status": "success",
    "metrics": {
        "error_count": 0,
        "warning_count": 2
    },
    "log_file": "build-logs/build_20250115_120000.log"
}
```

## 🔍 Использование в CI/CD

### GitHub Actions

```yaml
- name: Build IP-CSS
  run: |
    ./scripts/build-pipeline.sh
  env:
    BUILD_ID: ${{ github.run_id }}

- name: Upload Build Logs
  uses: actions/upload-artifact@v3
  with:
    name: build-logs
    path: build-logs/
    retention-days: 30

- name: Analyze Build Logs
  run: |
    ./scripts/analyze-build-logs.sh summary
    ./scripts/analyze-build-logs.sh errors
```

### GitLab CI

```yaml
build:
  script:
    - ./scripts/build-pipeline.sh
  artifacts:
    paths:
      - build-logs/
    expire_in: 30 days

analyze-logs:
  script:
    - ./scripts/analyze-build-logs.sh stats 7
    - ./scripts/analyze-build-logs.sh export 7
  artifacts:
    paths:
      - logs-export-*/
```

## 📊 Метрики

Система автоматически собирает:

| Метрика | Описание |
|---------|----------|
| `error_count` | Количество ошибок `[ERROR]` в логе |
| `warning_count` | Количество предупреждений `[WARN]` |
| `duration` | Время выполнения сборки |
| `status` | Успех/неудача |
| `timestamp` | Время начала сборки |

## 🛠 Дополнительные команды

```bash
# Помощь
./scripts/analyze-build-logs.sh --help

# Показать все доступные команды
./scripts/analyze-build-logs.sh help
```

## 📈 Примеры использования

### Найти причину частых сбоев

```bash
# Показать все ошибки за месяц
./scripts/analyze-build-logs.sh errors "" 30

# Найти самую частую ошибку
./scripts/analyze-build-logs.sh stats 30 | grep -A 10 "Most Common Errors"
```

### Отследить деградацию производительности

```bash
# Показать медленные сборки
./scripts/analyze-build-logs.sh slow 30

# Сравнить две сборки
./scripts/analyze-build-logs.sh compare 20250115_120000 20250101_100000
```

### Подготовить отчёт для команды

```bash
# Экспорт всех логов за неделю
./scripts/analyze-build-logs.sh export 7

# Статистика
./scripts/analyze-build-logs.sh stats 7 > report.txt

# Добавить список ошибок
./scripts/analyze-build-logs.sh errors 7 >> report.txt
```

## 🧹 Рекомендации

1. **Регулярная очистка**: Запускать `clean 30` раз в месяц
2. **Мониторинг**: Проверять `failed 7` после каждого релиза
3. **Анализ трендов**: Сравнивать `stats 7` и `stats 30` для выявления проблем
4. **Архивирование**: Экспортировать логи перед крупными изменениями

---

*Создано: 2025-01-15*  
*Версия: 1.0.0*
