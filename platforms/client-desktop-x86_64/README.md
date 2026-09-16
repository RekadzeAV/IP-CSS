# Платформа: Клиенты Desktop x86-x64

## Описание

Клиентские приложения для настольных систем на базе x86-x64 архитектуры.

## Поддерживаемые операционные системы

- Windows 10/11 (x64)
- Linux (Ubuntu, Debian, Fedora, Arch и др.) (x64)
- macOS Intel (x64)

## Архитектура

- **Процессор:** x86-64 (Intel/AMD)
- **UI Framework:** Compose Desktop
- **Язык:** Kotlin/JVM

## Структура модулей

```
platforms/client-desktop-x86_64/
├── app/             # Desktop приложение
│   ├── windows/     # Windows-специфичные модули
│   ├── linux/       # Linux-специфичные модули
│   └── macos/       # macOS Intel-специфичные модули
└── build/           # Скрипты сборки
```

## Используемые общие модули

- `:shared` - общая бизнес-логика
- `:core:common` - базовые типы
- `:core:network` - сетевые клиенты
- `:core:license` - система лицензирования

## Сборка

```bash
# Сборка для Windows
./gradlew :platforms:client-desktop-x86_64:app:packageMsi

# Сборка для Linux
./gradlew :platforms:client-desktop-x86_64:app:packageDeb

# Сборка для macOS Intel
./gradlew :platforms:client-desktop-x86_64:app:packageDmg
```

## Документация

- **[docs/DESKTOP_DETAILED_PLAN.md](../../archive/docs/guides/DESKTOP_DETAILED_PLAN.md)** - Детальный план реализации (8 этапов)
- **[docs/DESKTOP_PLAN_SUMMARY.md](../../docs/archive/2026-04-27/plans-obsolete/DESKTOP_PLAN_SUMMARY.md)** - Краткая сводка плана
- **[docs/DESKTOP_REFINEMENT_PLAN.md](../../archive/docs/guides/DESKTOP_REFINEMENT_PLAN.md)** - План доработки (574 часа)
- **[docs/DESKTOP_OPTIMIZATION_GUIDE.md](../../archive/docs/guides/DESKTOP_OPTIMIZATION_GUIDE.md)** - Руководство по оптимизации
- **[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)** - Текущий статус реализации (~70%)



