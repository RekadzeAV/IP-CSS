# Платформа: NAS x86-x64

## Описание

Платформа для NAS устройств на базе x86-x64 архитектуры с веб-интерфейсом для управления и работы.

## Поддерживаемые NAS системы

- Synology DSM (x86_64 модели)
- QNAP QTS (x86_64 модели)
- Asustor ADM (x86_64 модели)
- TrueNAS CORE (FreeBSD)
- TrueNAS SCALE (Linux + Kubernetes)
- Terramaster TOS (x86_64 модели)

## Архитектура

- **Процессор:** x86-64 (Intel/AMD)
- **ОС:** Linux-based или FreeBSD (TrueNAS CORE)
- **Интерфейс:** Веб-интерфейс (Next.js)
- **API:** Ktor Server (JVM)

## Форматы пакетов

- `.spk` - Synology Package
- `.qpkg` - QNAP Package
- `.apk` - Asustor Package (не Android!)
- `.pbi` - TrueNAS CORE Package
- Docker/Kubernetes - TrueNAS SCALE

## Структура модулей

```
platforms/nas-x86_64/
├── server/          # Конфигурация сервера
│   ├── application.conf
│   ├── config.yaml
│   └── README.md
├── packages/        # Пакеты для разных NAS систем
│   ├── synology/    # Synology SPK пакет
│   ├── qnap/        # QNAP QPKG пакет
│   ├── asustor/     # Asustor APK пакет
│   └── truenas/     # TrueNAS (Docker/Kubernetes)
├── docker/          # Docker конфигурации для x86_64
│   ├── Dockerfile
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
- `:native` - нативные C++ библиотеки (сборка для x86_64)

## Реализация

Базовая структура NAS платформы реализована. См. [IMPLEMENTATION.md](IMPLEMENTATION.md) для деталей.

## Сборка

```bash
# Сборка API сервера (требуется перед сборкой пакетов)
./gradlew :server:api:build

# Сборка пакета Synology
./scripts/build-nas-package.sh synology x86_64 Alfa-0.0.1

# Сборка пакета QNAP
./scripts/build-nas-package.sh qnap x86_64 Alfa-0.0.1

# Сборка пакета Asustor
./scripts/build-nas-package.sh asustor x86_64 Alfa-0.0.1

# Сборка Docker образа
docker build -f platforms/nas-x86_64/docker/Dockerfile -t ip-css-nas-x86_64:latest ../..

# Использование Docker Compose
cd platforms/nas-x86_64/docker
docker-compose up -d
```



