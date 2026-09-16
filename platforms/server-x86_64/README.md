# Платформа: Серверы x86-x64

## Описание

Текущий статус платформы: **planned as dedicated runtime module**.  
На текущий момент отдельного runtime-модуля в `platforms/server-x86_64/` нет: фактическая серверная реализация живет в `server/api` и `server/web`.

## Поддерживаемые системы

- Linux (Ubuntu, Debian, CentOS, RHEL)
- Windows Server
- macOS Server

## Архитектура

- **Процессор:** x86-64 (Intel/AMD)
- **ОС:** Linux, Windows, macOS
- **Интерфейс:** Веб-интерфейс (Next.js)
- **API:** Ktor Server (JVM)

## Фактическая структура (сейчас)

```
platforms/server-x86_64/
└── README.md

Реальные исполняемые компоненты:

server/api/          # Ktor API (Gradle module :server:api)
server/web/          # Next.js UI (Node.js project)
```

## Используемые общие модули (фактически)

- `:shared` - общая бизнес-логика
- `:core:common` - базовые типы
- `:core:network` - сетевые клиенты
- `native/` - нативные C++ библиотеки (по необходимости)

## Сборка (фактическая)

```bash
# Сборка backend API
./gradlew :server:api:build

# Сборка web UI
cd server/web && npm ci && npm run build

# Сборка Docker образа
docker build -f platforms/server-x86_64/docker/Dockerfile -t ip-css-server:latest .
```

## Развертывание

```bash
# Запуск через Docker
docker-compose -f platforms/server-x86_64/docker/docker-compose.yml up -d
```



