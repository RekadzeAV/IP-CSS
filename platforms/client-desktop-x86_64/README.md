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


