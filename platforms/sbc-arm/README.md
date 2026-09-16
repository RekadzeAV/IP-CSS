# Платформа: Микрокомпьютеры ARM (SBC ARM)

## Описание

Текущий статус платформы: **planned as dedicated runtime module**.  
Отдельного runtime-модуля в `platforms/sbc-arm/` сейчас нет: используется общий backend/frontend стек (`server/api` + `server/web`) с ARM-ориентированными артефактами/деплоем.

## Поддерживаемые устройства

- Raspberry Pi (ARMv7, ARMv8)
- Orange Pi (ARMv7, ARMv8)
- Rock64 (ARMv8)
- Odroid (ARMv7, ARMv8)
- Другие одноплатные компьютеры на базе ARM

## Архитектура

- **Процессор:** ARM (ARMv7, ARMv8/aarch64)
- **ОС:** Linux (Debian, Ubuntu, Raspbian)
- **Интерфейс:** Веб-интерфейс (Next.js)
- **API:** Ktor Server (JVM)

## Фактическая структура (сейчас)

```
platforms/sbc-arm/
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
docker build -f platforms/sbc-arm/docker/Dockerfile.arm64 -t ip-css-sbc-arm:latest .
```

## Развертывание

```bash
# Запуск через Docker
docker-compose -f platforms/sbc-arm/docker/docker-compose.yml up -d
```



