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
├── server/          # Конфигурация сервера
│   ├── application.conf
│   ├── config.yaml
│   └── README.md
├── packages/        # Пакеты для разных NAS систем
│   ├── synology/    # Synology SPK пакет
│   ├── qnap/        # QNAP QPKG пакет
│   └── asustor/     # Asustor APK пакет
├── docker/          # Docker конфигурации для ARM
│   ├── Dockerfile.arm64
│   ├── docker-compose.yml
│   └── README.md
└── IMPLEMENTATION.md # Статус реализации
```

## Используемые общие модули

- `:server:api` - Ktor API сервер (используется как JAR)
- `server/web` - Next.js веб-интерфейс
- `:shared` - общая бизнес-логика
- `:core:common` - базовые типы
- `:core:network` - сетевые клиенты
- `:core:license` - система лицензирования
- `:native` - нативные C++ библиотеки (сборка для ARM)

## Реализация

Базовая структура NAS платформы реализована. См. [IMPLEMENTATION.md](IMPLEMENTATION.md) для деталей.

## Сборка

```bash
# Сборка API сервера (требуется перед сборкой пакетов)
./gradlew :server:api:build -PtargetArch=arm64

# Сборка пакета Synology
./scripts/build-nas-package.sh synology arm64 Alfa-0.0.1

# Сборка пакета QNAP
./scripts/build-nas-package.sh qnap arm64 Alfa-0.0.1

# Сборка пакета Asustor
./scripts/build-nas-package.sh asustor arm64 Alfa-0.0.1

# Сборка Docker образа
docker build -f platforms/nas-arm/docker/Dockerfile.arm64 -t ip-css-nas-arm64:latest ../..

# Использование Docker Compose
cd platforms/nas-arm/docker
docker-compose up -d
```



