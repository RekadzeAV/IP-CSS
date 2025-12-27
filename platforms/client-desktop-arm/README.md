# Платформа: Клиенты Desktop ARM

## Описание

Клиентские приложения для настольных систем на базе ARM архитектуры.

## Поддерживаемые операционные системы

- Linux ARM64 (Ubuntu, Debian, Fedora и др.)
- macOS Apple Silicon (M1, M2, M3 и др.)

## Архитектура

- **Процессор:** ARM64 (aarch64)
- **UI Framework:** Compose Desktop
- **Язык:** Kotlin/JVM

## Структура модулей

```
platforms/client-desktop-arm/
├── app/             # Desktop приложение
│   ├── linux/       # Linux ARM64-специфичные модули
│   └── macos/       # macOS Apple Silicon-специфичные модули
└── build/           # Скрипты сборки
```

## Используемые общие модули

- `:shared` - общая бизнес-логика
- `:core:common` - базовые типы
- `:core:network` - сетевые клиенты
- `:core:license` - система лицензирования

## Сборка

```bash
# Сборка для Linux ARM64
./gradlew :platforms:client-desktop-arm:app:packageDeb -PtargetArch=arm64

# Сборка для macOS Apple Silicon
./gradlew :platforms:client-desktop-arm:app:packageDmg -PtargetArch=arm64
```


