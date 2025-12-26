# Платформа: Микрокомпьютеры ARM (SBC ARM)

## Описание

Платформа для одноплатных компьютеров на базе ARM архитектуры с веб-интерфейсом для управления и работы.

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

## Структура модулей

```
platforms/sbc-arm/
├── server/          # Серверная часть (Ktor API + Web UI)
├── docker/          # Docker конфигурации для ARM
└── build/           # Скрипты сборки для ARM
```

## Используемые общие модули

- `:shared` - общая бизнес-логика
- `:core:common` - базовые типы
- `:core:network` - сетевые клиенты
- `:core:license` - система лицензирования
- `:native` - нативные C++ библиотеки (сборка для ARM)

## Сборка

```bash
# Сборка для ARM64
./gradlew :platforms:sbc-arm:server:build -PtargetArch=arm64

# Сборка Docker образа
docker build -f platforms/sbc-arm/docker/Dockerfile.arm64 -t ip-css-sbc-arm:latest .
```

## Развертывание

```bash
# Запуск через Docker
docker-compose -f platforms/sbc-arm/docker/docker-compose.yml up -d
```

