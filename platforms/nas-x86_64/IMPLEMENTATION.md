# Реализация NAS x86_64 платформы

**Дата создания:** 26 January 2026
**Версия:** Alfa-0.0.1
**Статус:** Базовая структура реализована

## ✅ Реализовано

### 1. Структура директорий

Создана полная структура для NAS x86_64 платформы:

```
platforms/nas-x86_64/
├── server/              # Конфигурация сервера
│   ├── application.conf # Ktor конфигурация
│   ├── config.yaml      # YAML конфигурация
│   └── README.md        # Документация
├── docker/              # Docker конфигурации
│   ├── Dockerfile       # Dockerfile для x86_64
│   ├── docker-compose.yml
│   └── README.md
└── packages/            # Пакеты для NAS систем
    ├── synology/        # Synology SPK
    ├── qnap/            # QNAP QPKG
    ├── asustor/         # Asustor APK
    └── truenas/         # TrueNAS (Docker/Kubernetes)
```

### 2. Конфигурационные файлы

- **application.conf** - Ktor server configuration (HOCON)
- **config.yaml** - Application configuration (YAML)

Поддерживаются переменные окружения для настройки:
- JWT_SECRET
- DATABASE_PATH
- RECORDINGS_PATH
- SCREENSHOTS_PATH
- REDIS_HOST/PORT
- CORS_ALLOWED_ORIGINS

### 3. Docker конфигурации

- **Dockerfile** - Multi-stage build для x86_64
- **docker-compose.yml** - Полная конфигурация с API, Web и Redis

Особенности:
- Multi-stage build для уменьшения размера образа
- Health checks для всех сервисов
- Volume mounts для данных и логов
- Сеть для внутренней коммуникации

### 4. Скрипты запуска/остановки

Улучшенные скрипты для всех NAS систем:

#### Synology
- `packages/synology/package/bin/start.sh`
- `packages/synology/package/bin/stop.sh`

#### QNAP
- `packages/qnap/scripts/start.sh`
- `packages/qnap/scripts/stop.sh`

#### Asustor
- `packages/asustor/package/bin/start.sh`
- `packages/asustor/package/bin/stop.sh`

Все скрипты включают:
- Проверку Java
- Создание директорий
- Управление PID файлами
- Graceful shutdown
- Обработку ошибок

### 5. Поддерживаемые NAS системы

- ✅ Synology DSM (x86_64)
- ✅ QNAP QTS (x86_64)
- ✅ Asustor ADM (x86_64)
- ✅ TrueNAS SCALE (Docker/Kubernetes)

## 📋 Следующие шаги

1. Тестирование на реальных NAS устройствах
2. Добавление иконок для пакетов
3. Интеграция с NAS API (опционально)
4. Создание скриптов автоматической сборки
5. Настройка CI/CD для автоматической сборки пакетов

## 🚀 Использование

### Сборка пакетов

```bash
# Synology
./scripts/build-nas-package.sh synology x86_64 Alfa-0.0.1

# QNAP
./scripts/build-nas-package.sh qnap x86_64 Alfa-0.0.1

# Asustor
./scripts/build-nas-package.sh asustor x86_64 Alfa-0.0.1

# TrueNAS
./scripts/build-nas-package.sh truenas x86_64 Alfa-0.0.1
```

### Docker развертывание

```bash
cd platforms/nas-x86_64/docker
docker-compose up -d
```

## 📚 Документация

- [README.md](README.md) - Общая информация о платформе
- [server/README.md](server/README.md) - Конфигурация сервера
- [docker/README.md](docker/README.md) - Docker конфигурация
- [packages/README.md](packages/README.md) - Информация о пакетах

## 🔧 Требования

- Java 17 или выше
- Node.js 18+ (для Web UI)
- FFmpeg (для обработки видео)
- Redis (опционально, для кэширования)

## 📝 Примечания

- Все пути к данным используют стандартные директории NAS систем
- Конфигурация может быть переопределена через переменные окружения
- Скрипты поддерживают как систему systemd, так и традиционные init скрипты
