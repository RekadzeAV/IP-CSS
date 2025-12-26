# Платформа: NAS ARM

## Описание

Платформа для NAS устройств на базе ARM архитектуры с веб-интерфейсом для управления и работы.

## Поддерживаемые NAS системы

- Synology DSM (ARM модели)
- QNAP QTS (ARM модели)
- Asustor ADM (ARM модели)
- Terramaster TOS (ARM модели)

## Архитектура

- **Процессор:** ARMv8/aarch64
- **ОС:** Linux-based (Synology DSM, QNAP QTS, Asustor ADM)
- **Интерфейс:** Веб-интерфейс (Next.js)
- **API:** Ktor Server (JVM)

## Форматы пакетов

- `.spk` - Synology Package
- `.qpkg` - QNAP Package
- `.apk` - Asustor Package (не Android!)

## Структура модулей

```
platforms/nas-arm/
├── server/          # Серверная часть (Ktor API + Web UI)
├── packages/        # Пакеты для разных NAS систем
│   ├── synology/
│   ├── qnap/
│   └── asustor/
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
./gradlew :platforms:nas-arm:server:build -PtargetArch=arm64

# Сборка пакета Synology
./scripts/build-nas-package.sh synology arm64

# Сборка Docker образа
docker build -f platforms/nas-arm/docker/Dockerfile.arm64 -t ip-css-nas-arm:latest .
```

