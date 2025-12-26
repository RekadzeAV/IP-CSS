# Структура проекта IP Camera Surveillance System

## Обзор

Проект организован по модульному принципу с четким разделением ответственности между компонентами. Используется Kotlin Multiplatform для кроссплатформенной бизнес-логики и нативные библиотеки C++ для обработки видео и AI-аналитики.

## Корневая структура

```
IP-CSS/
├── .github/                    # GitHub Actions workflows (планируется)
│   └── workflows/
│       ├── ci.yml             # CI pipeline
│       └── cd.yml             # CD pipeline
├── .gradle/                   # Gradle cache (не коммитится)
├── core/                      # Общие кроссплатформенные модули
│   ├── license/               # Система лицензирования
│   │   ├── src/
│   │   │   ├── commonMain/    # Общий код для всех платформ
│   │   │   ├── androidMain/   # Android-специфичные реализации
│   │   │   └── iosMain/       # iOS-специфичные реализации
│   │   └── build.gradle.kts
│   └── network/               # Сетевое взаимодействие
│       ├── src/
│       │   ├── commonMain/
│       │   │   └── kotlin/com/company/ipcamera/core/network/
│       │   │       ├── ApiClient.kt          # REST API клиент
│       │   │       ├── WebSocketClient.kt    # WebSocket клиент
│       │   │       ├── RtspClient.kt         # RTSP клиент (обертка)
│       │   │       ├── OnvifClient.kt        # ONVIF клиент
│       │   │       ├── OnvifTypes.kt         # ONVIF типы
│       │   │       ├── api/                  # API сервисы
│       │   │       │   ├── CameraApiService.kt
│       │   │       │   ├── EventApiService.kt
│       │   │       │   ├── LicenseApiService.kt
│       │   │       │   ├── RecordingApiService.kt
│       │   │       │   ├── SettingsApiService.kt
│       │   │       │   └── UserApiService.kt
│       │   │       └── dto/                  # Data Transfer Objects
│       │   │           ├── ApiResponse.kt
│       │   │           ├── CameraDto.kt
│       │   │           ├── EventDto.kt
│       │   │           ├── LicenseDto.kt
│       │   │           ├── RecordingDto.kt
│       │   │           ├── SettingsDto.kt
│       │   │           └── UserDto.kt
│       │   ├── androidMain/   # Android-специфичные реализации
│       │   └── iosMain/       # iOS-специфичные реализации
│       ├── build.gradle.kts
│       └── README.md
├── docs/                       # Документация
│   ├── ARCHITECTURE.md        # Архитектура системы
│   ├── DEPLOYMENT_GUIDE.md    # Руководство по развертыванию
│   ├── DEVELOPMENT_PLAN.md    # План разработки
│   ├── DEVELOPMENT.md         # Руководство по разработке
│   ├── API.md                 # API документация
│   ├── LICENSE_SYSTEM.md      # Система лицензирования
│   ├── ONVIF_CLIENT.md        # Документация ONVIF клиента
│   ├── RTSP_CLIENT.md         # Документация RTSP клиента
│   ├── WEBSOCKET_CLIENT.md    # Документация WebSocket клиента
│   ├── TESTING.md             # Документация по тестированию
│   ├── TESTS_SUMMARY.md       # Сводка тестов
│   ├── INTEGRATION_GUIDE.md   # Руководство по интеграции
│   ├── IMPLEMENTATION_STATUS.md # Статус реализации
│   ├── MISSING_FUNCTIONALITY.md # Отсутствующая функциональность
│   ├── REQUIRED_LIBRARIES.md  # Необходимые библиотеки
│   └── PROMPT_ANALYSIS.md     # Анализ промта
├── native/                     # Нативные C++ библиотеки
│   ├── video-processing/      # Обработка видео
│   │   ├── src/
│   │   │   ├── frame_processor.cpp
│   │   │   ├── rtsp_client.cpp
│   │   │   ├── stream_manager.cpp
│   │   │   ├── video_decoder.cpp
│   │   │   └── video_encoder.cpp
│   │   ├── include/
│   │   │   ├── frame_processor.h
│   │   │   ├── rtsp_client.h
│   │   │   ├── stream_manager.h
│   │   │   ├── video_decoder.h
│   │   │   └── video_encoder.h
│   │   └── CMakeLists.txt
│   ├── analytics/             # AI аналитика
│   │   ├── src/
│   │   │   ├── anpr_engine.cpp          # Распознавание номеров
│   │   │   ├── face_detector.cpp        # Детекция лиц
│   │   │   ├── motion_detector.cpp      # Детекция движения
│   │   │   ├── object_detector.cpp      # Детекция объектов
│   │   │   └── object_tracker.cpp       # Трекинг объектов
│   │   ├── include/
│   │   │   ├── anpr_engine.h
│   │   │   ├── face_detector.h
│   │   │   ├── motion_detector.h
│   │   │   ├── object_detector.h
│   │   │   └── object_tracker.h
│   │   └── CMakeLists.txt
│   ├── codecs/                # Кодеки
│   │   ├── src/
│   │   │   ├── codec_manager.cpp
│   │   │   ├── h264_codec.cpp
│   │   │   ├── h265_codec.cpp
│   │   │   └── mjpeg_codec.cpp
│   │   ├── include/
│   │   │   ├── codec_manager.h
│   │   │   ├── h264_codec.h
│   │   │   ├── h265_codec.h
│   │   │   └── mjpeg_codec.h
│   │   └── CMakeLists.txt
│   └── CMakeLists.txt         # Корневой CMake файл
├── scripts/                    # Скрипты сборки и развертывания
│   ├── build-all-platforms.sh  # Скрипт сборки для всех платформ
│   ├── create-platform-branches.sh/ps1  # Скрипты создания веток для платформ
│   └── cleanup-old-branches.ps1 # Скрипт очистки старых веток
├── android/                    # Android приложение
│   └── app/                    # Android app модуль
│       ├── src/main/
│       │   ├── kotlin/         # Android код
│       │   └── res/            # Ресурсы
│       └── build.gradle.kts
├── server/                     # Серверная часть
│   ├── api/                    # REST API сервер (Ktor)
│   │   ├── src/main/kotlin/
│   │   │   ├── Application.kt  # Ktor приложение
│   │   │   ├── routes/         # API маршруты
│   │   │   └── di/             # Dependency Injection (Koin)
│   │   └── build.gradle.kts
│   └── web/                    # Веб-приложение (Next.js)
│       ├── package.json
│       ├── tsconfig.json
│       ├── next.config.js
│       └── src/
│           ├── app/            # Next.js App Router страницы
│           ├── components/     # React компоненты
│           ├── store/          # Redux store
│           └── services/       # API сервисы
├── platforms/                  # Платформо-специфичные реализации
│   ├── sbc-arm/               # Микрокомпьютеры ARM (Raspberry Pi и др.)
│   ├── server-x86_64/         # Серверы x86-x64
│   ├── nas-arm/               # NAS ARM (Synology, QNAP, Asustor)
│   ├── nas-x86_64/            # NAS x86-x64
│   ├── client-desktop-x86_64/ # Клиенты Desktop x86-x64
│   ├── client-desktop-arm/    # Клиенты Desktop ARM
│   ├── client-android/        # Клиенты Android (может использовать android/)
│   └── client-ios/            # Клиенты iOS/macOS
├── shared/                     # Kotlin Multiplatform модуль
│   ├── src/
│   │   ├── commonMain/        # Общий код для всех платформ
│   │   │   ├── kotlin/com/company/ipcamera/shared/
│   │   │   │   ├── common/    # Общие утилиты и платформо-независимые сервисы
│   │   │   │   │   ├── BackgroundWorker.kt   # Абстракция фоновой работы
│   │   │   │   │   ├── FileSystem.kt        # Абстракция файловой системы
│   │   │   │   │   ├── NotificationManager.kt # Абстракция уведомлений
│   │   │   │   │   └── Platform.kt          # Информация о платформе
│   │   │   │   ├── domain/    # Доменный слой
│   │   │   │   │   ├── model/ # Доменные модели
│   │   │   │   │   │   ├── Camera.kt
│   │   │   │   │   │   ├── Event.kt
│   │   │   │   │   │   ├── License.kt
│   │   │   │   │   │   ├── Notification.kt
│   │   │   │   │   │   ├── Recording.kt
│   │   │   │   │   │   ├── Settings.kt
│   │   │   │   │   │   └── User.kt
│   │   │   │   │   ├── repository/ # Интерфейсы репозиториев
│   │   │   │   │   │   ├── CameraRepository.kt
│   │   │   │   │   │   ├── EventRepository.kt
│   │   │   │   │   │   ├── LicenseRepository.kt
│   │   │   │   │   │   ├── NotificationRepository.kt
│   │   │   │   │   │   ├── RecordingRepository.kt
│   │   │   │   │   │   ├── SettingsRepository.kt
│   │   │   │   │   │   └── UserRepository.kt
│   │   │   │   │   └── usecase/ # Use Cases (бизнес-логика)
│   │   │   │   │       ├── AddCameraUseCase.kt
│   │   │   │   │       ├── DeleteCameraUseCase.kt
│   │   │   │   │       ├── GetCameraByIdUseCase.kt
│   │   │   │   │       ├── GetCamerasUseCase.kt
│   │   │   │   │       └── UpdateCameraUseCase.kt
│   │   │   │   └── data/      # Слой данных
│   │   │   │       ├── local/ # Локальные источники данных
│   │   │   │       │   ├── CameraEntityMapper.kt
│   │   │   │       │   └── DatabaseFactory.kt
│   │   │   │       └── repository/ # Реализации репозиториев
│   │   │   │           ├── CameraRepositoryImpl.kt
│   │   │   │           ├── EventRepositoryImpl.kt
│   │   │   │           ├── LicenseRepositoryImpl.kt
│   │   │   │           ├── NotificationRepositoryImpl.kt
│   │   │   │           ├── RecordingRepositoryImpl.kt
│   │   │   │           ├── SettingsRepositoryImpl.kt
│   │   │   │           └── UserRepositoryImpl.kt
│   │   │   └── sqldelight/    # SQLDelight схемы
│   │   │       └── com/company/ipcamera/shared/database/
│   │   │           └── CameraDatabase.sq
│   │   ├── androidMain/       # Android-специфичные реализации
│   │   │   └── kotlin/com/company/ipcamera/shared/common/
│   │   │       ├── BackgroundWorker.android.kt
│   │   │       ├── FileSystem.android.kt
│   │   │       ├── NotificationManager.android.kt
│   │   │       └── Platform.android.kt
│   │   ├── iosMain/           # iOS-специфичные реализации
│   │   │   └── kotlin/com/company/ipcamera/shared/common/
│   │   │       ├── BackgroundWorker.ios.kt
│   │   │       ├── FileSystem.ios.kt
│   │   │       ├── NotificationManager.ios.kt
│   │   │       └── Platform.ios.kt
│   │   ├── desktopMain/       # Desktop-специфичные реализации
│   │   │   └── kotlin/com/company/ipcamera/shared/common/
│   │   │       ├── BackgroundWorker.desktop.kt
│   │   │       ├── FileSystem.desktop.kt
│   │   │       ├── NotificationManager.desktop.kt
│   │   │       └── Platform.desktop.kt
│   │   └── commonTest/        # Тесты
│   │       ├── kotlin/com/company/ipcamera/shared/
│   │       │   ├── domain/usecase/     # Тесты Use Cases
│   │       │   ├── data/repository/    # Тесты репозиториев
│   │       │   └── data/local/         # Тесты мапперов
│   │       └── test/                   # Тестовые утилиты
│   │           ├── TestDatabaseFactory.kt
│   │           └── TestDataFactory.kt
│   └── build.gradle.kts
├── .gitignore                 # Git ignore файл
├── build.gradle.kts          # Корневой build файл
├── CHANGELOG.md               # История изменений
├── CONTRIBUTING.md            # Руководство по участию в разработке
├── CURRENT_STATUS.md          # Текущий статус проекта
├── docker-compose.yml         # Docker Compose конфигурация
├── gradle.properties          # Настройки Gradle
├── LICENSE                    # Лицензия проекта
├── PROJECT_ROADMAP.md         # Дорожная карта проекта
├── PROJECT_STRUCTURE.md       # Этот файл
├── README.md                  # Основной README
└── settings.gradle.kts        # Настройки Gradle проекта
```

## Детальная структура модулей

### shared/ - Kotlin Multiplatform модуль

Основной модуль, содержащий всю кроссплатформенную бизнес-логику. Использует Clean Architecture с разделением на слои: domain (доменные модели и use cases), data (реализации репозиториев), common (платформо-независимые утилиты).

**Технологии:**
- Kotlin Multiplatform
- SQLDelight для работы с БД
- Ktor Client для сетевых запросов
- Kotlinx Serialization для сериализации
- Kotlinx Coroutines для асинхронности
- Koin для dependency injection

**Source Sets:**
- `commonMain` - код, общий для всех платформ
- `androidMain` - Android-специфичные реализации
- `iosMain` - iOS-специфичные реализации (arm64, x86_64 для симулятора)
- `desktopMain` - Desktop-специфичные реализации (JVM для Windows, Linux, macOS)
- `commonTest` - тесты

Подробная информация о разделении разработки по платформам: [docs/PLATFORMS.md](docs/PLATFORMS.md) и [PLATFORM_STRUCTURE.md](PLATFORM_STRUCTURE.md)

### core/common/ - Модуль общих типов

Базовый модуль с общими типами данных, используемыми во всем проекте.

**Содержимое:**
- Базовые типы (Resolution, CameraStatus и другие)
- Общие утилиты и константы

**Технологии:**
- Kotlin Multiplatform

### core/license/ - Модуль лицензирования

Модуль для управления лицензиями приложения. Поддерживает Android и iOS.

**Технологии:**
- Kotlin Multiplatform
- Android Security Crypto (для Android)
- BouncyCastle (для криптографии)

### core/network/ - Модуль сетевого взаимодействия

Модуль предоставляет клиенты для различных сетевых протоколов:
- **ApiClient** - REST API клиент на основе Ktor с поддержкой retry, кэширования, логирования
- **WebSocketClient** - WebSocket клиент с автоматическим переподключением
- **RtspClient** - RTSP клиент (обертка для нативной реализации)
- **OnvifClient** - ONVIF клиент для работы с IP-камерами

Также содержит API сервисы и DTO для различных сущностей (Camera, Event, License, Recording, Settings, User).

### native/ - Нативные C++ библиотеки

Нативные библиотеки для обработки видео и AI-аналитики, собранные с помощью CMake.

**Модули:**
- **video-processing/** - обработка видеопотоков, декодирование/кодирование, управление потоками
- **analytics/** - AI-аналитика: детекция объектов, лиц, движения, трекинг, распознавание номеров (ANPR)
- **codecs/** - поддержка кодеков H.264, H.265, MJPEG

### server/api/ - REST API сервер

REST API сервер на базе Ktor для управления системой видеонаблюдения.

**Технологии:**
- Ktor Server
- Kotlin
- Koin (Dependency Injection)

**Endpoints:**
- Камеры (GET, POST, PUT, DELETE, test, discover)
- Health check

### server/web/ - Веб-приложение

Next.js приложение для веб-интерфейса системы видеонаблюдения.

**Технологии:**
- Next.js 15
- TypeScript
- React 18
- Material-UI
- Redux Toolkit

### android/app/ - Android приложение

Android приложение модуль (используется платформой client-android).

**Технологии:**
- Kotlin
- Jetpack Compose (планируется)
- Android SDK

### platforms/ - Платформо-специфичные реализации

Директория для платформо-специфичных реализаций. Каждая платформа имеет свою директорию с README.md.

**Структура:**
- `sbc-arm/` - Микрокомпьютеры ARM (использует :server:api)
- `server-x86_64/` - Серверы x86-x64 (использует :server:api)
- `nas-arm/` - NAS ARM (использует :server:api)
- `nas-x86_64/` - NAS x86-x64 (использует :server:api)
- `client-desktop-x86_64/` - Клиенты Desktop x86-x64
- `client-desktop-arm/` - Клиенты Desktop ARM
- `client-android/` - Клиенты Android (использует :android:app)
- `client-ios/` - Клиенты iOS/macOS

Подробнее: [PLATFORM_STRUCTURE.md](PLATFORM_STRUCTURE.md)

## Зависимости между модулями

```
android/ios/desktop приложения (platforms/)
    ↓
shared (KMM)
    ↓     ↓
    ↓  core:network
    ↓     ↓
    ↓  core:common (базовые типы: Resolution, CameraStatus)
    ↓
core:license
    ↓
native (C++ библиотеки через FFI, не Gradle модули)
```

**Важно:** Модуль `:core:network` зависит только от `:core:common`, а не от `:shared`, что устраняет циклическую зависимость. `:shared` зависит от обоих модулей `:core:network` и `:core:common`.

**Текущая структура модулей Gradle:**
- `:shared` - основной KMM модуль (общая бизнес-логика)
- `:core:common` - модуль общих типов (Resolution, CameraStatus)
- `:core:license` - модуль лицензирования
- `:core:network` - модуль сетевого взаимодействия (Ktor Client, ONVIF, WebSocket, RTSP)
- `:android:app` - Android приложение
- `:server:api` - REST API сервер (Ktor)

Native модули (`native/`) собираются отдельно через CMake и не включены в Gradle как модули.

## Конфигурационные файлы

### Gradle
- `build.gradle.kts` - корневой build файл
- `settings.gradle.kts` - настройки проекта, определение модулей
- `gradle.properties` - свойства Gradle
- `gradle/libs.versions.toml` - версии зависимостей
- `gradle/wrapper/` - Gradle wrapper

### CMake
- `native/CMakeLists.txt` - корневой CMake файл
- `native/*/CMakeLists.txt` - CMake файлы для каждого нативного модуля

### Node.js
- `server/web/package.json` - зависимости и скрипты
- `server/web/tsconfig.json` - конфигурация TypeScript
- `server/web/next.config.js` - конфигурация Next.js

### Docker
- `docker-compose.yml` - Docker Compose конфигурация

### CI/CD
- `.github/workflows/ci.yml` - CI pipeline (планируется)
- `.github/workflows/cd.yml` - CD pipeline (планируется)

## Документация

Вся документация находится в папке `docs/`:

### Основная документация
- `ARCHITECTURE.md` - подробная архитектура системы
- `PLATFORMS.md` - разделение разработки по платформам
- `PROJECT_STRUCTURE.md` - этот файл
- `DEVELOPMENT.md` - руководство по разработке
- `DEVELOPMENT_PLAN.md` - план дальнейшей разработки

### Статус и планирование
- `IMPLEMENTATION_STATUS.md` - статус реализации компонентов
- `MISSING_FUNCTIONALITY.md` - детальный анализ нереализованного функционала

### Техническая документация
- `API.md` - API документация
- `DEPLOYMENT_GUIDE.md` - руководство по развертыванию
- `INTEGRATION_GUIDE.md` - руководство по интеграции библиотек
- `LICENSE_SYSTEM.md` - система лицензирования

### Клиенты и протоколы
- `ONVIF_CLIENT.md` - документация ONVIF клиента
- `RTSP_CLIENT.md` - документация RTSP клиента
- `WEBSOCKET_CLIENT.md` - документация WebSocket клиента

### Инструменты и тестирование
- `DEVELOPMENT_TOOLS.md` - инструменты разработки
- `TESTING.md` - документация по тестированию

## Скрипты

Скрипты для автоматизации находятся в `scripts/`:
- `build-all-platforms.sh` - сборка для всех платформ

## Примечания

1. **Платформенные приложения**:
   - Модуль `android/app/` существует и используется платформой `platforms/client-android/`
   - iOS и Desktop приложения находятся в стадии планирования и будут размещены в соответствующих директориях `platforms/`

2. **База данных**: Используется SQLDelight для работы с локальной базой данных SQLite. Схемы определены в `shared/src/commonMain/sqldelight/`.

3. **Тестирование**: Тесты находятся в `shared/src/commonTest/`. Поддерживаются unit-тесты для use cases, репозиториев и мапперов.

4. **Платформо-специфичный код**: Реализации платформо-специфичных функций находятся в соответствующих source sets (`androidMain`, `iosMain`, `desktopMain`) и реализуют expect/actual механизм Kotlin Multiplatform.
