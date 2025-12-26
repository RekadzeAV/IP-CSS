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
├── server/          # Серверная часть (Ktor API + Web UI)
├── packages/        # Пакеты для разных NAS систем
│   ├── synology/
│   ├── qnap/
│   ├── asustor/
│   └── truenas/
├── docker/          # Docker конфигурации для x86_64
├── kubernetes/      # Kubernetes манифесты для TrueNAS SCALE
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
./gradlew :platforms:nas-x86_64:server:build

# Сборка пакета Synology
./scripts/build-nas-package.sh synology x86_64

# Сборка Docker образа
docker build -f platforms/nas-x86_64/docker/Dockerfile -t ip-css-nas-x86_64:latest .

# Сборка Kubernetes манифестов
./scripts/build-kubernetes.sh truenas-scale
```

