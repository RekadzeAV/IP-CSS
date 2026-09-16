# IP-CSS Build Pipeline v2.0

Автоматизированный скрипт сборки проекта IP-CSS с полным логированием и анализом.

## 🚀 Быстрый старт

```bash
# Linux/macOS
chmod +x scripts/build.sh
./scripts/build.sh

# Windows
powershell -ExecutionPolicy Bypass -File scripts/build.ps1
```

## 📋 Режимы сборки

| Режим | Описание | Команда |
|-------|----------|---------|
| **full** | Полная сборка (по умолчанию) | `./scripts/build.sh full` |
| **quick** | Быстрая проверка (тесты) | `./scripts/build.sh quick` |
| **native** | Только C++ библиотеки | `./scripts/build.sh native` |
| **kotlin** | Только Kotlin модули | `./scripts/build.sh kotlin` |
| **web** | Только веб-интерфейс | `./scripts/build.sh web` |
| **clean** | Очистка кэшей | `./scripts/build.sh clean` |

## 📊 Логирование

Все логи сохраняются в `build-logs/`:

```
build-logs/
├── build_20250115_120000.log    # Полный лог
├── build_20250115_120000.json   # JSON-отчёт
└── ...
```

### Анализ логов

```bash
# Статистика за неделю
./scripts/analysis/analyze-build-logs.sh stats 7

# Поиск ошибок
./scripts/analysis/analyze-build-logs.sh errors "OutOfMemory"

# Временная шкала
./scripts/analysis/analyze-build-logs.sh timeline

# Последняя сборка
./scripts/analysis/analyze-build-logs.sh latest
```

## 📁 Структура скриптов

```
scripts/
├── build.sh                   # Универсальный запуск
├── build-pipeline.sh          # Linux/macOS
├── build-pipeline.ps1         # Windows
├── core/                      # Ядро
│   ├── build-logger.sh        # Логирование
│   └── build-gradle-tasks.sh  # Gradle задачи
├── analysis/                  # Анализ
│   └── analyze-build-logs.sh  # Анализатор
├── validation/                # Валидация
│   ├── validate-artifacts.sh  # Проверка артефактов
│   └── check-git-lfs.sh       # Git LFS
└── docs/                      # Документация
    ├── BUILD_PIPELINE_README.md
    └── LOGGING_README.md
```

## ⚙️ Переменные окружения

```bash
export BUILD_NATIVE=true
export BUILD_KOTLIN=true
export BUILD_WEB=true
export BUILD_DOCKER=false
export BUILD_ID=my-build-123  # Кастомный ID сборки
```

## 🐛 Обработка ошибок

Скрипт автоматически пытается исправить:
- **OutOfMemory** → Уменьшение SAFE_JOBS
- **No space left** → Очистка кэша
- **Killed** → Уменьшение параллелизма
- **Network timeout** → Повторная попытка (3 раза)

Максимум **3 попытки** на каждый этап.

## ✅ Предварительные требования

- **Java 17+**
- **CMake 3.15+**
- **Node.js 20+**
- **Git**
- **Git LFS** (рекомендуется)

## 📖 Дополнительные ресурсы

- [LOGGING_README.md](./LOGGING_README.md) — Руководство по логированию
- [CHANGES_SUMMARY.md](./CHANGES_SUMMARY.md) — Сводка изменений
