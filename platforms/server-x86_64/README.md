# Платформа: Серверы x86-x64

## Описание

Платформа для серверных систем на базе x86-x64 архитектуры с веб-интерфейсом для управления и работы.

## Поддерживаемые системы

- Linux (Ubuntu, Debian, CentOS, RHEL)
- Windows Server
- macOS Server

## Архитектура

- **Процессор:** x86-64 (Intel/AMD)
- **ОС:** Linux, Windows, macOS
- **Интерфейс:** Веб-интерфейс (Next.js)
- **API:** Ktor Server (JVM)

## Структура модулей

```
platforms/server-x86_64/
├── server/          # Серверная часть (Ktor API + Web UI)
├── docker/          # Docker конфигурации для x86_64
└── build/           # Скрипты сборки для x86_64
```

## Используемые общие модули

- `:shared` - общая бизнес-логика
- `:core:common` - базовые типы
- `:core:network` - сетевые клиенты
- `:core:license` - система лицензирования
- `:native` - нативные C++ библиотеки (сборка для x86_64)

## Сборка

```bash
# Сборка для x86_64
./gradlew :platforms:server-x86_64:server:build

# Сборка Docker образа
docker build -f platforms/server-x86_64/docker/Dockerfile -t ip-css-server:latest .
```

## Развертывание

```bash
# Запуск через Docker
docker-compose -f platforms/server-x86_64/docker/docker-compose.yml up -d
```

