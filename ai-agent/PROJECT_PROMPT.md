# Комплексный промпт проекта IP-CSS

**Дата создания:** 26 January 2026
**Версия проекта:** Alfa-0.0.1
**Текущий прогресс:** ~60%
**Последнее обновление:** 26 January 2026

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../docs/DOCUMENTATION_INDEX.md)
> **📌 Актуальный baseline по готовности модулей и release gates:** [docs/status/MODULE_STATUS_BASELINE_2026-04-23.md](../docs/status/MODULE_STATUS_BASELINE_2026-04-23.md)
> **🔐 KMP stabilization docs:** [docs/kmp-phase1-progress.md](../docs/kmp-phase1-progress.md), [docs/kmp-phase1-dod-checklist.md](../docs/kmp-phase1-dod-checklist.md), [docs/kmp-security-contract.md](../docs/kmp-security-contract.md)

---

## 🎯 ОПИСАНИЕ ПРОЕКТА

**IP Camera Surveillance System (IP-CSS)** - это кроссплатформенная система видеонаблюдения с IP-камер с продвинутой AI-аналитикой. Система работает на всех основных платформах: мобильных (Android, iOS), десктопных (Windows, Linux, macOS) и серверных/NAS (Linux, Synology, QNAP, Asustor, TrueNAS).

### Основные возможности:

#### Критически необходимые (MVP)
- 🎥 Поддержка множества IP-камер (RTSP, ONVIF, HTTP, MJPEG)
- 📹 Непрерывная запись и запись по событиям
- 📺 Просмотр видео в реальном времени и воспроизведение записей
- 🔍 Автоматическое обнаружение камер (WS-Discovery, UPnP)
- 🎮 Полное управление камерами (PTZ, настройки, мониторинг)
- 🔐 Аутентификация и авторизация (JWT, RBAC, 2FA, LDAP/AD, SSO, Kerberos)
- 🌐 Веб-интерфейс для удаленного управления
- 📱 Мобильные приложения (Android, iOS)
- 💻 Desktop приложения (Windows, Linux, macOS)

#### Важные функции
- 🤖 AI-аналитика (детекция объектов, трекинг, детекция движения)
- 🔔 Множественные каналы уведомлений (Push, Email, SMS, Telegram, Webhook)
- 📊 Система событий (генерация, просмотр, фильтрация, экспорт)
- 💾 Управление записями (поиск, экспорт, управление хранилищем)
- ⚙️ Настройки и конфигурация (импорт/экспорт, шаблоны, резервное копирование)

#### Дополнительные функции
- 🚗 Расширенная AI-аналитика (ANPR, распознавание лиц, анализ поведения)
- ☁️ Облачная синхронизация и хранение
- 🎯 Расширенное управление PTZ (паттерны, автотрекинг, геозоны)
- 👥 Многопользовательский режим (группы, делегирование, аудит)
- 🔌 Интеграция с внешними системами (умный дом, системы безопасности, облачные сервисы)
- 🔒 Расширенная безопасность (шифрование, аудит, соответствие стандартам)
- 🔌 Плагинная архитектура (система плагинов, магазин)
- 📈 Расширенная аналитика и отчеты (дашборды, статистика, визуализация)

#### Инновационные функции
- 🧠 AI-аналитика следующего поколения (предиктивная аналитика, поведенческий анализ)
- 🥽 AR/VR интеграция
- ⚡ Edge AI и распределенная обработка
- 🗣️ Голосовое управление
- 📊 Бизнес-аналитика (ROI анализ, прогнозирование, рекомендации)

---

## 🏗️ АРХИТЕКТУРА

### Архитектурный подход
Проект построен по принципу **Clean Architecture** с использованием **Kotlin Multiplatform** для кроссплатформенной бизнес-логики и нативных UI фреймворков для каждой платформы.

**Важное изменение:** Проект реорганизован по платформам. Каждая платформа имеет свою директорию в `platforms/` с подробной документацией. См. [PLATFORM_STRUCTURE.md](../docs/PLATFORM_STRUCTURE.md) для подробностей о платформах и структуре веток Git.

### Слои архитектуры:

1. **Слой данных (Data Layer)**
   - Репозитории с абстракциями для доступа к данным
   - Локальная БД (SQLite с SQLDelight)
   - Файловая система (видеоархивы, конфигурации)
   - Сетевые API (облачные сервисы)
   - IP-камеры (RTSP, ONVIF)

2. **Доменный слой (Domain Layer)**
   - Use Cases (бизнес-сценарии)
   - Модели (Camera, Recording, Event, User, Settings, Notification)
   - Интерфейсы репозиториев
   - Доменные сервисы (аналитика)
   - ⏸️ Модель License и сервис лицензирования (отложено - вынесено за рамки проекта)

3. **Слой представления (Presentation Layer)**
   - Android: Jetpack Compose, Hilt DI, Compose Navigation
   - iOS: SwiftUI, Swinject DI, SwiftUI Navigation
   - Desktop: Compose Desktop, Koin DI
   - Web: React + TypeScript, Material-UI, Redux Toolkit

4. **Слой инфраструктуры (Infrastructure Layer)**
   - `:core:common` - общие типы (Resolution, CameraStatus)
   - `:core:network` - сетевое взаимодействие (Ktor Client)
   - ⏸️ `:core:license` - система лицензирования (отложено - вынесено за рамки проекта)
   - `:native:video-processing` - обработка видео на C++/FFmpeg/OpenCV
   - `:native:analytics` - AI аналитика на C++/OpenCV/TensorFlow Lite

---

## 📁 СТРУКТУРА ПРОЕКТА

```
IP-CSS/
├── shared/                    # Kotlin Multiplatform модуль
│   ├── src/
│   │   ├── commonMain/        # Общий код для всех платформ
│   │   │   ├── domain/         # Доменный слой
│   │   │   │   ├── model/       # Модели (Camera, Recording, Event, etc.)
│   │   │   │   ├── repository/  # Интерфейсы репозиториев
│   │   │   │   └── usecase/     # Use Cases
│   │   │   ├── data/            # Слой данных
│   │   │   │   ├── local/       # Локальные источники (SQLDelight)
│   │   │   │   └── repository/  # Реализации репозиториев
│   │   │   └── common/          # Общие утилиты
│   │   ├── androidMain/         # Android-специфичные реализации
│   │   ├── iosMain/             # iOS-специфичные реализации
│   │   ├── desktopMain/         # Desktop-специфичные реализации
│   │   └── commonTest/          # Тесты
│   └── build.gradle.kts
│
├── core/                       # Общие кроссплатформенные модули
│   ├── common/                 # Базовые типы (Resolution, CameraStatus)
│   ├── license/                # ⏸️ Система лицензирования (отложено)
│   └── network/                # Сетевое взаимодействие
│       ├── ApiClient.kt        # REST API клиент (Ktor)
│       ├── WebSocketClient.kt # WebSocket клиент
│       ├── OnvifClient.kt     # ONVIF клиент
│       ├── RtspClient.kt      # RTSP клиент (обертка)
│       ├── api/                # API сервисы
│       └── dto/                # Data Transfer Objects
│
├── native/                     # Нативные C++ библиотеки
│   ├── CMakeLists.txt         # Корневой CMake файл
│   ├── video-processing/       # Обработка видео
│   │   ├── CMakeLists.txt
│   │   ├── src/
│   │   │   ├── rtsp_client.cpp      # RTSP клиент (FFmpeg)
│   │   │   ├── video_decoder.cpp    # Декодер видео
│   │   │   ├── video_encoder.cpp    # Кодировщик видео
│   │   │   ├── frame_processor.cpp  # Обработка кадров (OpenCV)
│   │   │   └── stream_manager.cpp   # Управление потоками
│   │   └── include/
│   │       ├── rtsp_client.h
│   │       ├── video_decoder.h
│   │       ├── video_encoder.h
│   │       ├── frame_processor.h
│   │       └── stream_manager.h
│   ├── analytics/              # AI аналитика
│   │   ├── CMakeLists.txt
│   │   ├── src/
│   │   │   ├── object_detector.cpp  # Детекция объектов (OpenCV/TensorFlow Lite)
│   │   │   ├── object_tracker.cpp   # Трекинг объектов
│   │   │   ├── motion_detector.cpp  # Детекция движения (OpenCV)
│   │   │   ├── face_detector.cpp    # Детекция лиц (OpenCV)
│   │   │   └── anpr_engine.cpp      # Распознавание номеров (ANPR)
│   │   └── include/
│   │       ├── object_detector.h
│   │       ├── object_tracker.h
│   │       ├── motion_detector.h
│   │       ├── face_detector.h
│   │       └── anpr_engine.h
│   └── codecs/                 # Кодеки
│       ├── CMakeLists.txt
│       ├── src/
│       │   ├── codec_manager.cpp
│       │   ├── h264_codec.cpp
│       │   ├── h265_codec.cpp
│       │   └── mjpeg_codec.cpp
│       └── include/
│           ├── codec_manager.h
│           ├── h264_codec.h
│           ├── h265_codec.h
│           └── mjpeg_codec.h
│
├── android/                    # Android приложение
│   └── app/                   # Android app модуль
│       └── src/main/
│           ├── kotlin/        # Android код
│           └── res/           # Ресурсы
│
├── server/                     # Серверная часть
│   ├── api/                    # REST API сервер (Ktor)
│   │   └── src/main/kotlin/
│   │       ├── Application.kt  # Ktor приложение
│   │       ├── routes/         # API маршруты
│   │       └── di/             # Dependency Injection (Koin)
│   └── web/                    # Веб-интерфейс (Next.js)
│       └── src/
│           ├── app/            # Next.js App Router страницы
│           ├── components/     # React компоненты
│           ├── store/          # Redux store
│           └── services/       # API сервисы
│
├── platforms/                  # Платформо-специфичные реализации
│   ├── sbc-arm/               # Микрокомпьютеры ARM (Raspberry Pi и др.)
│   │   └── README.md          # Документация платформы
│   ├── server-x86_64/         # Серверы x86-x64
│   │   └── README.md          # Документация платформы
│   ├── nas-arm/               # NAS ARM (Synology, QNAP, Asustor)
│   │   └── README.md          # Документация платформы
│   ├── nas-x86_64/            # NAS x86-x64
│   │   └── README.md          # Документация платформы
│   ├── client-desktop-x86_64/ # Клиенты Desktop x86-x64
│   │   └── README.md          # Документация платформы
│   ├── client-desktop-arm/    # Клиенты Desktop ARM
│   │   └── README.md          # Документация платформы
│   ├── client-android/        # Метаданные платформы; исходники в android/app
│   │   └── README.md          # Документация платформы
│   └── client-ios/            # Клиенты iOS/macOS
│       └── README.md          # Документация платформы
│
└── docs/                      # Документация
```

---

## 🛠️ ТЕХНОЛОГИЧЕСКИЙ СТЕК

### Backend / Shared
- **Kotlin Multiplatform** 2.0.21
- **Ktor** 2.3.12 (клиент и сервер)
- **SQLDelight** 2.0.3 (кроссплатформенная БД)
- **Kotlinx Coroutines** 1.8.0
- **Kotlinx Serialization** 1.7.0 (JSON, XML)
- **Koin** 3.6.0 (Dependency Injection)
- **Kotlinx DateTime** 0.6.0
- **ktor-server-auth** 2.3.12 (аутентификация на сервере)
- **ktor-server-auth-jwt** 2.3.12 (JWT аутентификация)
- **unboundid-ldapsdk** 6.0.8 (LDAP/AD интеграция)
- **spring-security-saml2-service-provider** 6.2.0 (SAML SSO, опционально)
- **oauth2-oidc-sdk** 10.0.2 (OAuth 2.0 / OIDC, опционально)

### Android
- **Jetpack Compose** 1.7.0
- **Android SDK** 26-34
- **SQLDelight Android Driver**
- **Ktor Client Android**
- **Android Security Crypto** 1.1.0-alpha06
- **WorkManager** 2.9.1

### iOS
- **SwiftUI** (UI фреймворк)
- **SQLDelight Native Driver**
- **Ktor Client Darwin**
- **iOS Keychain Services**

### Desktop
- **Compose Desktop** 1.7.0
- **SQLDelight SQLite Driver**
- **Ktor Client Java**

### Web Frontend
- **Next.js** 15.0.3
- **React** 18.3.1
- **TypeScript** 5.6.3
- **Material-UI** 5.16.7
- **Redux Toolkit** 2.2.7
- **Axios** 1.7.7
- **Socket.io-client** 4.7.5
- **React Player** 2.16.0
- **HLS.js** 1.5.12

### Нативные библиотеки (C++)
- **CMake** 3.15+
- **FFmpeg** (libavformat, libavcodec, libavutil, libswscale, libswresample)
- **OpenCV** 4.10.0 (обработка изображений, детекция)
- **TensorFlow Lite** 2.16.0 (AI модели)
- **CUDA** / **OpenCL** (GPU ускорение, опционально)

### Инструменты разработки
- **Gradle** 8.7.0
- **Detekt** 1.24.0 (статический анализ)
- **Ktlint** 12.0.0 (форматирование)
- **Dokka** 1.9.20 (документация)

---

## 📊 ТЕКУЩИЙ СТАТУС РЕАЛИЗАЦИИ

### ✅ Реализовано (~60%)

#### Инфраструктура (100%)
- ✅ Модульная структура проекта
- ✅ Gradle конфигурация для всех модулей
- ✅ SQLDelight настройка и схемы
- ✅ Полная документация
- ✅ CI/CD пайплайны (.github/workflows)
- ✅ Docker конфигурация

#### Доменный слой (~45%)
- ✅ Полная модель `Camera` с PTZ, потоками, настройками, аналитикой
- ✅ Модели: `Recording`, `Event`, `User`, `Settings`, `Notification`
- ⏸️ Модель `License` (отложено - вынесено за рамки проекта)
- ✅ Интерфейсы репозиториев: CameraRepository, RecordingRepository, EventRepository, UserRepository, SettingsRepository, NotificationRepository
- ⏸️ Интерфейс LicenseRepository (отложено - вынесено за рамки проекта)
- ✅ **15 Use Cases реализовано:**
  - **Управление камерами (5):** AddCameraUseCase, GetCamerasUseCase, GetCameraByIdUseCase, UpdateCameraUseCase, DeleteCameraUseCase
  - **Обнаружение камер (4):** DiscoverCamerasUseCase, DiscoverAndAddCameraUseCase, AddDiscoveredCameraUseCase, TestDiscoveredCameraUseCase
  - **Управление записями (6):** StartRecordingUseCase, StopRecordingUseCase, PauseRecordingUseCase, ResumeRecordingUseCase, GetRecordingsUseCase, DeleteRecordingUseCase

#### Слой данных (~65%)
- ✅ SQLDelight схемы для всех сущностей (камеры, записи, события, пользователи, настройки, уведомления)
- ✅ **Реализации репозиториев:**
  - ✅ `CameraRepositoryImpl` (SQLDelight) - полный CRUD + discover + test
  - ✅ `RecordingRepositoryImplSqlDelight` - полный CRUD с пагинацией
  - ✅ `EventRepositoryImplSqlDelight` - полный CRUD с фильтрацией
  - ✅ `UserRepositoryImplSqlDelight` - полный CRUD
  - ✅ `SettingsRepositoryImplSqlDelight` - полный CRUD
  - ✅ `NotificationRepositoryImplSqlDelight` - полный CRUD
- ✅ Entity мапперы: CameraEntityMapper, RecordingEntityMapper, EventEntityMapper, UserEntityMapper, SettingsEntityMapper, NotificationEntityMapper
- ✅ `DatabaseFactory` для Android, iOS, Desktop
- ✅ `discoverCameras()` - реализован (использует OnvifClient)
- ✅ `testConnection()` - реализован (использует OnvifClient)
- ✅ Unit тесты для репозиториев и use cases

#### Сетевой слой (~40%)
- ✅ `ApiClient` - полностью реализован (HTTP клиент с retry, cache, error handling)
- ✅ API сервисы (интерфейсы): CameraApiService, RecordingApiService, EventApiService, UserApiService, SettingsApiService
- ⏸️ LicenseApiService (отложено - вынесено за рамки проекта)
- ✅ DTO модели для всех сущностей
- ⚠️ `OnvifClient` - частично (~40%):
  - ✅ Базовые методы (getCapabilities, getDeviceInformation, getProfiles, getStreamUri)
  - ✅ PTZ управление (movePtz, stopPtz)
  - ✅ testConnection()
  - ❌ WS-Discovery (discoverCameras возвращает пустой список)
  - ❌ Полноценный XML парсинг (используется упрощенный через regex)
- ⚠️ `WebSocketClient` - частично (~80%):
  - ✅ Подключение/отключение
  - ✅ Автоматическое переподключение
  - ✅ Подписки на каналы
  - ❌ Обработка бинарных сообщений
- ⚠️ `RtspClient` - частично (~10%):
  - ✅ Kotlin обертка с базовой структурой
  - ✅ Нативная C++ библиотека с заголовками
  - ❌ Интеграция Kotlin ↔ C++ (FFI биндинги)
  - ❌ Реальная реализация RTSP протокола

#### Лицензирование (⏸️ Отложено)
- ⏸️ `LicenseManager` - вынесено за рамки проекта
  - ✅ Базовая структура сохранена для будущей доработки
  - ❌ Онлайн/офлайн активация не реализованы
  - ❌ Платформо-специфичные реализации требуют доработки
  - **Статус:** Требуется полная переработка, реализация в отдельной доработке

#### Серверная часть (~85%)
- ✅ REST API сервер (Ktor) - полный набор endpoints
- ✅ Серверные сервисы (VideoRecordingService, VideoStreamService, ScreenshotService, HlsGeneratorService, FfmpegService, StorageService, PasswordService)
- ✅ Серверные репозитории (ServerUserRepository, ServerEventRepository, ServerRecordingRepository, ServerSettingsRepository)
- ✅ Middleware (AuthorizationMiddleware, RateLimitMiddleware, CookieAuthMiddleware, SecurityLogger, RequestValidator)
- ✅ Redis интеграция для rate limiting
- ✅ REST API сервер (Ktor) - полный набор endpoints:
  - ✅ Камеры: `GET`, `POST`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/test`, `GET /discover`
  - ✅ Записи: `GET`, `GET /{id}`, `DELETE /{id}`, `GET /{id}/download`, `POST /{id}/export`, `POST /start`, `POST /stop/{cameraId}`, `POST /pause/{cameraId}`, `POST /resume/{cameraId}`
  - ✅ События: `GET`, `GET /{id}`, `DELETE /{id}`, `POST /{id}/acknowledge`, `POST /acknowledge`, `GET /statistics`
  - ✅ Пользователи: `GET /me`, `GET`, `POST`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` (только для администраторов)
  - ✅ Настройки: `GET`, `PUT`, `GET /{key}`, `PUT /{key}`, `DELETE /{key}`, `GET /system`, `POST /export`, `POST /import`, `POST /reset`
  - ✅ **Потоки (StreamRoutes):**
    - ✅ `POST /streams/start` - запуск потока
    - ✅ `POST /streams/stop/{cameraId}` - остановка потока
    - ✅ `GET /streams/status/{cameraId}` - статус потока
  - ✅ **HLS потоки (HlsRoutes):**
    - ✅ `GET /hls/{cameraId}/playlist.m3u8` - получение HLS плейлиста
    - ✅ `GET /hls/{cameraId}/segment-{index}.ts` - получение сегмента видео
  - ✅ **RTSP потоки:** `GET /rtsp/{cameraId}` - RTSP URL для прямого подключения
  - ✅ **Скриншоты (ScreenshotRoutes):**
    - ✅ `POST /screenshots/create/{cameraId}` - создание скриншота
    - ✅ `GET /screenshots/{id}` - получение скриншота
    - ✅ `GET /screenshots/{id}/download` - скачивание скриншота
  - ✅ `GET /api/v1/health` - health check endpoint
- ✅ **VideoRecordingService** - полная реализация управления записями:
  - ✅ Управление жизненным циклом записей (старт, стоп, пауза, возобновление)
  - ✅ Интеграция с RTSP клиентом для получения видеопотоков
  - ✅ Сохранение записей в файлы (MP4, MKV, AVI, MOV, FLV)
  - ✅ Генерация thumbnail'ов через FFmpeg
  - ✅ Автоматическая очистка старых записей
- ✅ **StreamRoutes** - управление видеопотоками:
  - ✅ Запуск/остановка потоков
  - ✅ HLS генерация для веб-плеера
  - ✅ Статус потоков
- ✅ **ScreenshotService** - создание снимков с камер:
  - ✅ Создание скриншотов по запросу
  - ✅ Сохранение и получение скриншотов
- ✅ WebSocket сервер (`/api/v1/ws`):
  - ✅ JWT аутентификация для WebSocket
  - ✅ Подписки на каналы (cameras, events, recordings, notifications)
  - ✅ Broadcast событий в каналы
  - ✅ Менеджер сессий (WebSocketSessionManager)
  - ✅ Обработка отключений и переподключений
  - ✅ Интеграция с репозиториями (Events, Recordings, Cameras) для real-time обновлений
- ✅ Аутентификация и авторизация (JWT):
  - ✅ JWT токены (access + refresh) через JwtConfig
  - ✅ Endpoints аутентификации (`POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`)
  - ✅ JWT middleware для защиты маршрутов
- ✅ AuthorizationMiddleware для проверки прав доступа (RBAC)
- ✅ Rate limiting для login endpoint (RateLimitMiddleware - 5 попыток в 15 минут)
- ✅ Хеширование паролей (BCrypt через PasswordService)
- ✅ Логирование попыток входа
- ✅ Защита всех endpoints через RBAC (ADMIN, OPERATOR, VIEWER, GUEST)
- ✅ CookieAuthMiddleware - управление аутентификацией через cookies
- ✅ SecurityLogger - логирование событий безопасности
- ✅ RequestValidator - валидация входящих запросов
- ✅ Redis интеграция (RedisConfig) для rate limiting
- ❌ Расширенная аутентификация (LDAP/AD, SSO, Kerberos) - для корпоративных сред

#### Веб-интерфейс (~70%)
- ✅ Конфигурация Next.js 15.0.3
- ✅ Базовые страницы: login, dashboard, cameras (список, детали, добавление, редактирование)
- ✅ Страницы событий (Events): список, фильтрация, подтверждение, массовые операции, статистика
- ✅ Страницы записей (Recordings): список, скачивание, экспорт, фильтрация
- ✅ Страницы настроек (Settings): управление настройками, импорт/экспорт, сброс
- ✅ React компоненты: Layout, CameraCard, ProtectedRoute, VideoPlayer, WebSocketProvider, WebSocketNotificationHandler
- ✅ Redux store: authSlice, camerasSlice, eventsSlice, recordingsSlice, settingsSlice, websocketSlice
- ✅ API сервисы: authService, cameraService, eventService, recordingService, settingsService, streamService
- ✅ Аутентификация и защита маршрутов
- ✅ **WebSocket интеграция:**
  - ✅ useWebSocket hook (`server/web/src/hooks/useWebSocket.ts`)
  - ✅ WebSocketProvider компонент (`server/web/src/components/WebSocketProvider/`)
  - ✅ WebSocketNotificationHandler компонент (`server/web/src/components/WebSocketNotificationHandler/`)
  - ✅ websocketSlice для управления состоянием WebSocket соединения
  - ✅ Интеграция с Redux slices для real-time обновлений (cameras, events, recordings)
  - ✅ Автоматическое переподключение
  - ✅ Обработка ошибок подключения
- ⚠️ Видеоплеер (требует интеграции с RTSP клиентом через HLS)
- **Расположение:** `server/web/src/`

#### Нативные библиотеки (~5%)
- ✅ CMake конфигурация для всех модулей
- ✅ Заголовочные файлы (.h) и исходники (.cpp) созданы
- ❌ FFI биндинги для Kotlin (cinterop)
- ❌ Полная реализация функций (большинство - заглушки)
- ❌ Интеграция с OpenCV и TensorFlow Lite

### ❌ Не реализовано / В разработке

#### UI компоненты
- 🟡 Android UI (Jetpack Compose) - базовая структура и экраны реализованы (~30%):
  - ✅ **Экраны (android/app/src/main/java/.../ui/screens/):**
    - ✅ CameraListScreen - список камер
    - ✅ CameraDetailScreen - детали камеры
    - ✅ CameraAddScreen - добавление камеры
    - ✅ VideoViewScreen - просмотр видео
    - ✅ RecordingsScreen - список записей
    - ✅ EventsScreen - список событий
    - ✅ EventDetailScreen - детали события
    - ✅ SettingsScreen - настройки
    - ⏸️ LicenseScreen - лицензирование (отложено, код сохранен)
  - ✅ **ViewModels (android/app/src/main/java/.../viewmodel/):**
    - ✅ CameraListViewModel
    - ✅ CameraDetailViewModel
    - ✅ CameraAddViewModel
    - ✅ VideoViewViewModel
    - ✅ RecordingsViewModel
    - ✅ EventsViewModel
    - ✅ SettingsViewModel
    - ⏸️ LicenseViewModel (отложено, код сохранен)
  - ✅ **Компоненты:**
    - ✅ ExoVideoPlayer - видеоплеер на основе ExoPlayer
    - ✅ AppNavigation - навигационный граф
  - ✅ **Dependency Injection:**
    - ✅ AppModule (Koin) - полная конфигурация DI
    - ✅ Все репозитории подключены
    - ✅ Все ViewModels подключены
  - ⚠️ Требуется интеграция с RTSP и доработка функциональности
  - **Расположение:** `android/app/src/main/java/com/company/ipcamera/android/`
- ❌ iOS UI (SwiftUI экраны)
- ❌ Desktop UI (Compose Desktop экраны)

#### Функциональность
- ⚠️ RTSP клиент (частично реализован ~10%, структура готова, требуется интеграция с нативной библиотекой)
- 🟡 Запись видео (~64% - см. canonical 1.8: `docs/status/CANONICAL_BREAKDOWN_1_8_VIDEO_AND_RECORDING.md`)
- ⚠️ Просмотр видео в реальном времени (требуется интеграция RTSP клиента с видеоплеером)
- ⚠️ AI-аналитика (базовая структура ~5%, требуется полная реализация)
- ❌ Use Cases для аналитики
- ⏸️ Use Cases для лицензирования (отложено - вынесено за рамки проекта)

#### Тестирование
- ❌ Unit тесты (базовые есть для CameraRepository и Use Cases)
- ❌ Integration тесты
- ❌ E2E тесты
- ❌ UI тесты

---

## 🔑 КЛЮЧЕВЫЕ КОМПОНЕНТЫ

### Доменные модели

#### Camera
```kotlin
data class Camera(
    val id: String,
    val name: String,
    val url: String,
    val username: String?,
    val password: String?,
    val model: String?,
    val status: CameraStatus,
    val resolution: Resolution?,
    val fps: Int,
    val bitrate: Int,
    val codec: String,
    val audio: Boolean,
    val ptz: PTZConfig?,
    val streams: List<StreamConfig>,
    val settings: CameraSettings,
    val statistics: CameraStatistics?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastSeen: Long?
)
```

#### Recording
- Модель записи видео с метаданными (начало, конец, длительность, путь к файлу, качество, формат)

#### Event
- Модель события (тип, важность, камера, временная метка, описание, метаданные, миниатюра, видео)

### Репозитории

#### CameraRepository
```kotlin
interface CameraRepository {
    suspend fun getCameras(): List<Camera>
    suspend fun getCameraById(id: String): Camera?
    suspend fun addCamera(camera: Camera): Result<Camera>
    suspend fun updateCamera(camera: Camera): Result<Camera>
    suspend fun removeCamera(id: String): Result<Unit>
    suspend fun discoverCameras(): List<DiscoveredCamera>
    suspend fun testConnection(camera: Camera): ConnectionTestResult
    suspend fun getCameraStatus(id: String): CameraStatus
}
```

**Реализация:** `CameraRepositoryImpl` использует SQLDelight для локального хранения и OnvifClient для обнаружения и тестирования камер.

### Use Cases

Реализованы **15 Use Cases**:

**Управление камерами (5):**
- `AddCameraUseCase` - добавление новой камеры с валидацией
- `GetCamerasUseCase` - получение списка всех камер
- `GetCameraByIdUseCase` - получение камеры по ID
- `UpdateCameraUseCase` - обновление камеры
- `DeleteCameraUseCase` - удаление камеры

**Обнаружение камер (4):**
- `DiscoverCamerasUseCase` - автоматическое обнаружение камер в сети
- `DiscoverAndAddCameraUseCase` - обнаружение и добавление камеры
- `AddDiscoveredCameraUseCase` - добавление обнаруженной камеры
- `TestDiscoveredCameraUseCase` - тестирование обнаруженной камеры

**Управление записями (6):**
- `StartRecordingUseCase` - запуск записи с камеры
- `StopRecordingUseCase` - остановка записи
- `PauseRecordingUseCase` - пауза записи
- `ResumeRecordingUseCase` - возобновление записи
- `GetRecordingsUseCase` - получение списка записей
- `DeleteRecordingUseCase` - удаление записи

### Сетевые клиенты

#### ApiClient
Полностью реализованный HTTP клиент на Ktor с:
- Конфигурацией (baseUrl, timeouts, headers)
- Сериализацией через Kotlinx Serialization (JSON)
- Обработкой ошибок с типизированными `ApiError`
- Retry логикой с экспоненциальной задержкой
- Кэшированием ответов для GET запросов

#### OnvifClient
Частично реализован (~40%):
- ✅ Базовые методы (getCapabilities, getDeviceInformation, getProfiles, getStreamUri)
- ✅ PTZ управление (movePtz, stopPtz)
- ✅ testConnection()
- ❌ WS-Discovery (discoverCameras возвращает пустой список)
- ❌ Полноценный XML парсинг (используется упрощенный через regex)
- ❌ Digest Authentication (только Basic)

#### WebSocketClient
Частично реализован (~80%):
- ✅ Подключение/отключение
- ✅ Автоматическое переподключение
- ✅ Подписки на каналы
- ✅ Обработка текстовых сообщений
- ❌ Обработка бинарных сообщений
- ❌ Очередь сообщений при отключении

#### RtspClient
Частично реализован (~10%):
- ✅ Kotlin обертка с базовой структурой
- ✅ Нативная C++ библиотека с заголовками
- ❌ Интеграция Kotlin ↔ C++ (FFI биндинги)
- ❌ Реальная реализация RTSP протокола (все функции - заглушки)

### Серверные сервисы

#### VideoRecordingService
Полностью реализован (~100%):
- ✅ Управление жизненным циклом записей (старт, стоп, пауза, возобновление)
- ✅ Интеграция с RTSP клиентом для получения видеопотоков
- ✅ Сохранение записей в файлы (MP4, MKV, AVI, MOV, FLV)
- ✅ Генерация thumbnail'ов через FFmpeg
- ✅ Автоматическая очистка старых записей
- ✅ Управление хранилищем записей
- ✅ Интеграция с SQLDelight репозиторием для сохранения метаданных
- ✅ Обработка ошибок и логирование
- **Расположение:** `server/api/src/main/kotlin/.../service/VideoRecordingService.kt`

#### VideoStreamService
Полностью реализован (~100%):
- ✅ Управление видеопотоками (запуск/остановка)
- ✅ Интеграция с RTSP клиентом
- ✅ Управление состоянием потоков
- ✅ Мониторинг активных потоков
- ✅ Обработка ошибок подключения
- **Расположение:** `server/api/src/main/kotlin/.../service/VideoStreamService.kt`

#### HlsGeneratorService
Полностью реализован (~100%):
- ✅ Генерация HLS плейлистов (.m3u8)
- ✅ Создание сегментов видео (.ts)
- ✅ Управление жизненным циклом HLS потоков
- ✅ Автоматическая очистка старых сегментов
- ✅ Конфигурация качества потока (StreamQuality)
- **Расположение:** `server/api/src/main/kotlin/.../service/HlsGeneratorService.kt`

#### FfmpegService
Полностью реализован (~100%):
- ✅ Обертка для работы с FFmpeg
- ✅ Генерация thumbnail'ов из видео
- ✅ Конвертация форматов видео
- ✅ Извлечение метаданных из видео
- ✅ Обработка ошибок FFmpeg
- **Расположение:** `server/api/src/main/kotlin/.../service/FfmpegService.kt`

#### StorageService
Полностью реализован (~100%):
- ✅ Управление хранилищем записей
- ✅ Проверка свободного места на диске
- ✅ Автоматическая очистка старых записей
- ✅ Расчет использования хранилища
- ✅ Управление путями хранения
- **Расположение:** `server/api/src/main/kotlin/.../service/StorageService.kt`

#### ScreenshotService
Полностью реализован (~100%):
- ✅ Создание снимков с камер по запросу
- ✅ Сохранение скриншотов
- ✅ Получение и скачивание скриншотов
- ✅ Генерация thumbnail'ов из скриншотов
- ✅ API endpoints для работы со скриншотами
- **Расположение:** `server/api/src/main/kotlin/.../service/ScreenshotService.kt`

#### PasswordService
Полностью реализован (~100%):
- ✅ Хеширование паролей (BCrypt)
- ✅ Проверка паролей
- ✅ Генерация безопасных паролей
- **Расположение:** `server/api/src/main/kotlin/.../service/PasswordService.kt`

### Серверные репозитории

**Важно:** На сервере используются отдельные реализации репозиториев, оптимизированные для серверной части. Они могут отличаться от реализаций в модуле `:shared`.

#### ServerUserRepository
- ✅ Отдельная реализация для сервера (не использует shared)
- ✅ Работа с пользователями на сервере
- ✅ Интеграция с JWT аутентификацией
- ✅ Хеширование паролей через PasswordService
- ✅ Управление ролями и правами доступа
- **Расположение:** `server/api/src/main/kotlin/.../repository/ServerUserRepository.kt`

#### ServerEventRepository
- ✅ Реализация репозитория событий для сервера
- ✅ Интеграция с WebSocket для real-time обновлений
- ✅ SQLDelight для локального хранения
- ✅ Фильтрация и пагинация событий
- ✅ Массовые операции (acknowledge)
- **Расположение:** `server/api/src/main/kotlin/.../repository/ServerEventRepository.kt`

#### ServerRecordingRepository / ServerRecordingRepositorySqlDelight
- ✅ Реализация репозитория записей для сервера
- ✅ SQLDelight версия для локального хранения метаданных
- ✅ Интеграция с VideoRecordingService
- ✅ Интеграция с WebSocket для real-time обновлений
- ✅ Управление файлами записей через StorageService
- ✅ Фильтрация и пагинация записей
- **Расположение:**
  - `server/api/src/main/kotlin/.../repository/ServerRecordingRepository.kt`
  - `server/api/src/main/kotlin/.../repository/ServerRecordingRepositorySqlDelight.kt`

#### ServerSettingsRepository
- ✅ Реализация репозитория настроек для сервера
- ✅ Управление системными настройками
- ✅ SQLDelight для хранения настроек
- ✅ Импорт/экспорт настроек
- ✅ Валидация значений настроек
- **Расположение:** `server/api/src/main/kotlin/.../repository/ServerSettingsRepository.kt`

---

## 🎯 ПРИОРИТЕТЫ РАЗРАБОТКИ

### Фаза 1: MVP (Минимально жизнеспособный продукт) - Высокий приоритет (4-6 месяцев)

**Цель:** Создать рабочую версию с базовым функционалом для всех платформ.

1. ✅ Базовая структура проекта
2. ✅ Модели данных (Camera, Recording, Event, User, Settings, Notification)
   ⏸️ Модель License (отложено - вынесено за рамки проекта)
3. ✅ Интерфейсы репозиториев
4. ✅ Реализация репозиториев (SQLDelight для всех сущностей)
5. ✅ Use Cases (15 use cases: управление камерами, обнаружение, запись)
6. ✅ База данных (SQLDelight схемы для всех сущностей, DAO, мапперы)
7. ✅ REST API сервер (полный набор endpoints для всех сущностей)
8. ⚠️ ONVIF клиент для discoverCameras() (WS-Discovery требует доработки)
9. ✅ Реализация testConnection()

**Критически необходимые функции:**
10. 🟡 **Базовая работа с видео (~60%):**
    - ⚠️ RTSP клиент (частично реализован ~10%, требуется интеграция с нативной библиотекой)
    - ⚠️ Видеоплеер для всех платформ (требуется интеграция с RTSP клиентом)
    - ✅ Запись потоков в файлы (MP4, MKV, AVI, MOV, FLV) - VideoRecordingService реализован
    - ✅ Управление записью (старт/стоп/пауза/возобновление) - Use Cases и API готовы
    - ⚠️ Просмотр записей с временной шкалой (требуется интеграция с RTSP)
    - ✅ Экспорт записей в различные форматы - API endpoint готов
    - ✅ Автоматическая очистка старых записей - реализовано в VideoRecordingService

11. ❌ **Обнаружение и подключение камер:**
    - Полная реализация WS-Discovery (UDP multicast)
    - UPnP обнаружение для камер без ONVIF
    - Мастер добавления камеры с пошаговым процессом
    - Автоматическое определение параметров камеры
    - Тест подключения с детальной диагностикой
    - Поддержка различных типов камер (ONVIF, RTSP, HTTP, MJPEG)
    - Импорт/экспорт списка камер

12. 🟡 **Базовый пользовательский интерфейс:**
    - 🟡 Android приложение (Jetpack Compose) - базовая структура и экраны реализованы (~30%):
      - ✅ Все основные экраны (CameraList, CameraDetail, CameraAdd, VideoView, Recordings, Events, EventDetail, Settings)
      - ⏸️ LicenseScreen (отложено, код сохранен)
      - ✅ Все ViewModels реализованы
      - ✅ DI конфигурация (Koin AppModule)
      - ✅ ExoVideoPlayer компонент
      - ⚠️ Требуется интеграция с RTSP и доработка функциональности
    - ❌ iOS приложение (SwiftUI) - экраны отсутствуют
    - ❌ Desktop приложение (Compose Desktop) - Windows, Linux, macOS отсутствует
    - ✅ Веб-интерфейс - все основные страницы реализованы (~70%):
      - ✅ Dashboard, Cameras, Events, Recordings, Settings
      - ✅ WebSocket интеграция
      - ⚠️ Видеоплеер требует интеграции с RTSP через HLS

13. ✅ **Аутентификация и авторизация (базовая):**
    - ✅ **Базовая аутентификация реализована:**
      - ✅ JWT токены (access + refresh)
      - ✅ Роли: ADMIN, OPERATOR, VIEWER, GUEST
      - ✅ Права доступа на уровне операций (RBAC)
      - ✅ Middleware для проверки прав (AuthorizationMiddleware)
      - ✅ Защита всех API endpoints через RBAC
      - ✅ Rate limiting (RateLimitMiddleware)
      - ✅ Хеширование паролей (BCrypt через PasswordService)
      - ✅ SecurityLogger для аудита
      - ❌ Восстановление пароля через email
      - ❌ Двухфакторная аутентификация (2FA)
      - ✅ История входов (логирование)
      - ✅ Блокировка после неудачных попыток (rate limiting)
    - **Интеграция с доменом (Active Directory / LDAP):**
      - LDAP аутентификация
      - Синхронизация пользователей с доменом
      - Маппинг групп AD на роли системы
      - Автоматическое обновление прав при изменении групп
      - Поддержка вложенных и универсальных групп
    - **SSO (Single Sign-On):**
      - SAML 2.0 поддержка
      - OAuth 2.0 / OpenID Connect
      - WS-Federation (опционально)
      - Интеграция с корпоративными IdP (Azure AD, Okta, Keycloak)
    - **Kerberos аутентификация:**
      - SPNEGO токены обработка
      - Интеграция с Key Distribution Center (KDC)
      - Поддержка делегирования билетов (опционально)
      - Автоматический вход для пользователей домена Windows

14. ❌ **Базовое управление камерами:**
    - Полное PTZ управление (движение, зум, фокус, пресеты, паттерны)
    - Настройка параметров камеры (яркость, контрастность, разрешение, FPS, битрейт)
    - Управление записью (запуск/остановка, расписание, запись по событиям)
    - Статистика работы камеры (время работы, количество записей, использование диска, сетевой трафик)
    - Мониторинг состояния камер (автоматическая проверка, уведомления, история статусов)

**Метрики успеха MVP:**
- ✅ Работа с 5+ камерами одновременно
- ✅ Запись и просмотр видео
- ✅ Базовый UI на всех платформах
- ✅ Аутентификация работает
- ✅ Стабильность 99%+

---

### Фаза 2: Основной функционал - Средний приоритет (3-4 месяца)

**Цель:** Добавить важные функции для полноценного продукта.

1. ❌ **AI-аналитика (базовая):**
    - Детекция движения (адаптивный фоновый вычитатель, зоны детекции, фильтрация ложных срабатываний)
    - Детекция объектов (люди, транспорт, животные, базовые классы COCO dataset)
    - Трекинг объектов (отслеживание между кадрами, уникальные ID, траектории)
    - Генерация событий на основе аналитики (автоматическое создание, классификация, миниатюры)

2. ❌ **Система уведомлений:**
    - Push-уведомления (Android FCM, iOS APNS, Desktop системные)
    - Email уведомления (SMTP интеграция, HTML шаблоны, вложения)
    - SMS уведомления (Twilio, AWS SNS)
    - Telegram бот (отправка уведомлений, команды управления, групповые чаты)
    - Webhook интеграция (настраиваемые форматы, retry логика)
    - Правила уведомлений (условия срабатывания, фильтры, расписание, группировка)

3. ❌ **Управление записями:**
    - Просмотр записей (календарь, временная шкала, многокамерный просмотр)
    - Поиск по записям (по дате, камере, типу события, объектам)
    - Экспорт записей (различные форматы, обрезка, выбор качества, водяные знаки)
    - Управление хранилищем (квоты, автоматическая очистка, сжатие, перенос)
    - Резервное копирование (автоматическое копирование, синхронизация с облаком)

4. ❌ **Система событий:**
    - Генерация событий (автоматическая из аналитики, ручное создание, импорт)
    - Просмотр событий (список с фильтрами, детали с видео, временная шкала, статистика)
    - Фильтрация событий (по типу, важности, камере, дате, статусу подтверждения)
    - Управление событиями (подтверждение, массовое подтверждение, удаление, теги)
    - Экспорт событий (CSV отчеты, PDF отчеты, экспорт видео)

5. ❌ **Настройки и конфигурация:**
    - Системные настройки (общие, запись, аналитика, уведомления, сеть, безопасность)
    - Настройки камер (шаблоны, массовое применение, импорт/экспорт)
    - Управление конфигурацией (импорт/экспорт всей конфигурации, резервное копирование, версионирование)
    - Пользовательские настройки (язык, тема, уведомления, приватность)

6. ⚠️ Веб-интерфейс (завершение: события, записи, настройки, видеоплеер)
7. ⏸️ Лицензирование (отложено - вынесено за рамки проекта, требует полной переработки)
8. ❌ WebSocket сервер для real-time обновлений

**Метрики успеха Фазы 2:**
- ✅ AI-аналитика работает в реальном времени
- ✅ Уведомления доставляются
- ✅ Управление записями полнофункционально
- ✅ 100+ активных пользователей

---

### Фаза 3: Расширенный функционал - Средний приоритет (4-6 месяцев)

**Цель:** Добавить функции для конкурентного преимущества.

1. ❌ **Расширенная AI-аналитика:**
    - Распознавание номеров (ANPR): детекция, OCR, валидация, база данных, поиск
    - Распознавание лиц: детекция, извлечение признаков, сравнение, база данных, поиск, группировка
    - Анализ поведения: детекция подозрительного поведения, трекинг траекторий, детекция оставленных/пропавших предметов
    - Подсчет объектов: подсчет людей/транспорта, статистика посещаемости, heat maps
    - Классификация объектов: детальный анализ, атрибуты, классификация действий

2. ❌ **Облачная синхронизация:**
    - Синхронизация между устройствами (камеры, настройки, события, конфликт-резолюция)
    - Облачное хранилище (загрузка записей, автоматическая синхронизация, выборочная синхронизация, шифрование)
    - Удаленный доступ (доступ из любой точки, просмотр через облако, управление через облако, мобильный доступ)
    - Резервное копирование (автоматическое, восстановление, версионирование)

3. ❌ **Расширенное управление PTZ:**
    - Автоматические паттерны (создание, запуск по расписанию/событиям)
    - Tour режим (последовательный обзор пресетов, настраиваемые маршруты)
    - Автотрекинг (автоматическое отслеживание объектов, умное кадрирование, плавное движение)
    - Геозоны (определение зон интереса, автоматическое движение к зоне, уведомления)

4. ❌ **Многопользовательский режим:**
    - Управление пользователями (создание/редактирование, назначение ролей, управление правами)
    - Группы пользователей (создание групп, назначение прав, массовое управление)
    - Делегирование прав (временный доступ, ограниченный доступ по времени, доступ к определенным камерам)
    - Аудит действий (логирование всех действий, история изменений, отчеты по активности)

5. ❌ **Интеграция с внешними системами:**
    - API для интеграции (RESTful API, WebSocket API, GraphQL API, SDK)
    - Интеграция с системами безопасности (охранные системы, контроль доступа, пожарные системы)
    - Интеграция с умным домом (Home Assistant, OpenHAB, HomeKit, Google Home, Amazon Alexa)
    - Интеграция с облачными сервисами (AWS IoT, Azure IoT, Google Cloud IoT, MQTT брокеры)

6. ❌ Desktop приложения (полная реализация)
7. ❌ NAS версии (Synology, QNAP, Asustor, TrueNAS)

**Метрики успеха Фазы 3:**
- ✅ ANPR и распознавание лиц работают
- ✅ Облачная синхронизация стабильна
- ✅ Интеграции работают
- ✅ 1000+ активных пользователей

---

### Фаза 4: Продвинутые функции - Низкий приоритет (6+ месяцев)

**Цель:** Инновационные функции для будущего развития.

1. ❌ **Расширенная безопасность:**
    - Шифрование (записи AES-256, передаваемые данные TLS 1.3, база данных, certificate pinning)
    - Аудит безопасности (детальное логирование, мониторинг подозрительной активности, алерты, отчеты)
    - Соответствие стандартам (GDPR, ISO 27001, SOC 2, HIPAA)
    - Защита от атак (rate limiting, DDoS защита, защита от SQL инъекций, XSS, CSRF)

2. ❌ **Управление приватностью:**
    - Размытие лиц (автоматическое, выборочное, по зонам)
    - Анонимизация (удаление метаданных, анонимизация объектов, контроль доступа)
    - Управление данными (право на удаление GDPR, экспорт данных, ограничение хранения)

3. ❌ **Плагинная архитектура:**
    - Система плагинов (API для разработки, загрузка, управление, изоляция)
    - Магазин плагинов (каталог, установка, обновление, рейтинги)
    - Примеры плагинов (интеграции, алгоритмы аналитики, кастомные уведомления, экспорт)

4. ❌ **Webhook и события:**
    - Система событий (публикация, подписка, фильтрация)
    - Webhook интеграция (настраиваемые webhooks, retry логика, аутентификация, валидация)
    - Event streaming (поток событий в реальном времени, Kafka/MQTT интеграция)

5. ❌ **Расширенная аналитика и отчеты:**
    - Дашборды (настраиваемые, виджеты, графики, экспорт)
    - Отчеты (ежедневные/недельные/месячные, автоматическая отправка, кастомные, шаблоны)
    - Статистика (по камерам, событиям, использованию, сравнительная)
    - Визуализация (heat maps, траектории движения, временные графики, географические карты)

6. ❌ **Улучшение UX/UI:**
    - Адаптивный дизайн (различные размеры экранов, оптимизация для мобильных, tablet-friendly)
    - Персонализация (настраиваемые темы, персональные дашборды, избранные камеры, быстрые действия)
    - Удобство использования (поиск, горячие клавиши, голосовые команды, жесты)
    - Мультиязычность (множество языков, локализация, RTL поддержка)

7. ❌ **Офлайн режим:**
    - Работа без интернета (кэширование данных, офлайн просмотр записей, офлайн управление, синхронизация)
    - Ограниченная функциональность (базовые функции доступны, очередь операций)

8. ❌ **Голосовое управление:**
    - Голосовые команды (управление камерами, поиск, навигация)
    - Интеграция с ассистентами (Google Assistant, Amazon Alexa, Apple Siri, Yandex Алиса)

9. ❌ **AI-аналитика следующего поколения:**
    - Предиктивная аналитика (прогнозирование событий, анализ паттернов, раннее предупреждение)
    - Поведенческий анализ (детекция аномалий, обучение на основе истории, адаптивные алгоритмы)
    - Семантическое понимание (понимание сцен, контекстный анализ, описания на естественном языке)
    - Мультимодальный анализ (комбинация видео и аудио, анализ звуков, детекция голосовых команд)

10. ❌ **AR/VR интеграция:**
    - AR просмотр камер (наложение информации, виртуальные камеры, 3D визуализация)
    - VR мониторинг (виртуальный контрольный центр, иммерсивный просмотр, 360° камеры)

11. ❌ **Edge AI и распределенная обработка:**
    - Обработка на краю сети (аналитика на камере, уменьшение нагрузки, быстрая реакция)
    - Распределенная обработка (кластеризация серверов, балансировка нагрузки, горизонтальное масштабирование)
    - Адаптивная обработка (динамическое распределение ресурсов, приоритизация задач)

12. ❌ **Бизнес-аналитика:**
    - Анализ эффективности (ROI анализ, анализ покрытия, оптимизация размещения камер)
    - Прогнозирование (прогноз событий, использование ресурсов, потребность в хранилище)
    - Рекомендации (по настройке, размещению камер, оптимизации производительности)

---

## 🔴 КРИТИЧЕСКИЕ ПРОБЛЕМЫ БЕЗОПАСНОСТИ

**Статус:** 🟡 **УЛУЧШЕНО** - Критические уязвимости частично устранены, но проект все еще требует доработки для продакшена

**Выявлено 25 уязвимостей безопасности** (6 критических, 9 высоких, 10 средних)

### ✅ Устраненные критические уязвимости:

1. **Аутентификация и авторизация на сервере API** ✅ **ИСПРАВЛЕНО**
   - ✅ JWT токены (access + refresh) реализованы
   - ✅ JWT middleware для защиты всех маршрутов
   - ✅ Система авторизации и проверки прав доступа (RBAC) реализована
   - ✅ Все endpoints защищены через AuthorizationMiddleware

2. **Небезопасные привилегии Docker контейнера** ✅ **ИСПРАВЛЕНО**
   - ✅ Удален SYS_ADMIN capability
   - ✅ Удален seccomp:unconfined
   - ✅ Добавлен non-root пользователь (appuser:1000:1000)
   - ✅ Read-only файловая система с tmpfs для временных файлов

3. **Небезопасная CORS конфигурация** ✅ **ИСПРАВЛЕНО**
   - ✅ Удален anyHost()
   - ✅ Настроен whitelist доменов через переменную окружения CORS_ALLOWED_ORIGINS
   - ✅ Ограничены HTTP методы (GET, POST, PUT, DELETE, OPTIONS)
   - ✅ Настроен allowCredentials = true

### ⚠️ Остающиеся критические уязвимости:

4. **Отсутствие принудительного HTTPS** ⚠️ **ТРЕБУЕТ ДОРАБОТКИ**
   - Cleartext traffic разрешен
   - Нет настройки reverse proxy с TLS
   - **Риск:** Перехват данных в открытом виде

5. **Отсутствие certificate pinning** ⚠️ **ТРЕБУЕТ ДОРАБОТКИ**
   - Нет проверки SSL/TLS сертификатов
   - **Риск:** MITM атаки

6. ⏸️ **Система лицензирования** - **ОТЛОЖЕНА**
   - Функционал вынесен за рамки проекта
   - Требуется полная переработка архитектуры
   - Будет реализован в отдельной доработке

**Детальный отчет:** [docs/SECURITY_AUDIT_REPORT.md](../archive/docs-duplicates-2026-08-08/SECURITY_AUDIT_REPORT.md)
**План устранения:** [docs/SECURITY_REMEDIATION_PLAN.md](../archive/docs-deprecated-2026-09-04/SECURITY_REMEDIATION_PLAN.md)
**Критические блокеры:** docs/planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md *(утерян/в архиве)* (3/6 завершено)

---

## 📝 КРИТИЧЕСКИЕ ПРОБЕЛЫ

### Критически важные (блокируют MVP)
1. **WS-Discovery в OnvifClient** - частично реализован (~40%), требуется доработка UDP multicast для автоматического обнаружения камер
2. **RTSP клиент** - частично реализован (~10%), требуется интеграция с нативной C++ библиотекой через FFI и полная реализация протокола
3. **UI компоненты** - частично реализованы:
   - ✅ Web (~70%) - все основные страницы готовы
   - 🟡 Android (~30%) - базовая структура и экраны
   - ❌ iOS - отсутствует
   - ❌ Desktop - отсутствует
4. **Запись видео** - частично реализована (~64%, по canonical 1.8):
   - ✅ VideoRecordingService - полная реализация
   - ✅ Use Cases для записи (6 штук)
   - ✅ API endpoints для записи
   - ⚠️ Требуется интеграция с RTSP клиентом для просмотра
5. **Аутентификация и авторизация** - базовая реализация завершена:
   - ✅ Серверная аутентификация (JWT) реализована
   - ✅ RBAC реализован (ADMIN, OPERATOR, VIEWER, GUEST)
   - ✅ Защита всех endpoints через AuthorizationMiddleware
   - ❌ Интеграция с Active Directory / LDAP отсутствует
   - ❌ Поддержка SSO (SAML/OIDC) отсутствует
   - ❌ Поддержка Kerberos отсутствует
   - ❌ Синхронизация пользователей с доменом отсутствует
6. **Базовое управление камерами** - частично реализовано (требуется полное PTZ управление, настройки, статистика)

### Важные (для полноценного продукта)
7. **AI-аналитика** - нативные библиотеки требуют реализации (детекция движения, объектов, трекинг)
8. **WebSocket сервер** - не реализован (real-time обновления)
9. **Система уведомлений** - не реализована (Push, Email, SMS, Telegram, Webhook)
10. **Управление записями** - не реализовано (поиск, экспорт, управление хранилищем)
11. **Система событий** - частично реализована (требуется генерация, просмотр, фильтрация, экспорт)
12. **Настройки и конфигурация** - частично реализована (требуется интерфейс, импорт/экспорт)

### Дополнительные (для конкурентного преимущества)
13. **Расширенная AI-аналитика** - не реализована (ANPR, распознавание лиц, анализ поведения)
14. **Облачная синхронизация** - не реализована (синхронизация между устройствами, облачное хранилище)
15. **Расширенное управление PTZ** - не реализовано (паттерны, автотрекинг, геозоны)
16. **Многопользовательский режим** - не реализован (группы, делегирование, аудит)
17. **Интеграция с доменом** - не реализована (LDAP/AD, SSO, Kerberos, синхронизация пользователей)
18. **Интеграция с внешними системами** - не реализована (умный дом, системы безопасности, облачные сервисы)

**Полный анализ функционала:** См. [docs/FUNCTIONALITY_ANALYSIS.md](../archive/docs/analysis/FUNCTIONALITY_ANALYSIS.md)

---

## 🚨 КРИТИЧЕСКИЕ БЛОКЕРЫ

**Статус:** 🟡 В процессе устранения (3/6 завершено)

### ✅ Завершенные блокеры:

1. **Расширение RBAC в EventRoutes/RecordingRoutes** ✅
   - Все endpoints защищены через `requireRole()`
   - RBAC проверки для всех операций
   - Все маршруты (CameraRoutes, EventRoutes, RecordingRoutes, UserRoutes, SettingsRoutes) защищены

2. **Интеграция WebSocket с сервисами для real-time обновлений** ✅
   - EventRepository интегрирован с WebSocket (события: created, updated, acknowledged, bulk_acknowledged)
   - RecordingRepository интегрирован с WebSocket (записи: created, updated, deleted)
   - CameraRepository интегрирован с WebSocket через CameraRoutes (камеры: created, updated, deleted)
   - WebSocketSessionManager реализован
   - Обработка отключений и переподключений

3. **Аутентификация и авторизация на сервере** ✅
   - JWT токены реализованы (access + refresh)
   - RBAC реализован (ADMIN, OPERATOR, VIEWER, GUEST)
   - Все endpoints защищены через AuthorizationMiddleware
   - Rate limiting реализован (RateLimitMiddleware)
   - Хеширование паролей (BCrypt через PasswordService)
   - SecurityLogger для аудита безопасности

### ⚠️ Остающиеся критические блокеры:

4. **RTSP клиент - интеграция с нативной библиотекой** ⚠️
   - Текущий статус: ~10% (структура готова)
   - Требуется: интеграция Kotlin ↔ C++ (FFI биндинги)
   - Требуется: полная реализация RTSP протокола
   - Оценка времени: 2-3 месяца

5. **Завершение веб-интерфейса** ⚠️
   - Текущий статус: ~70%
   - ✅ Все основные страницы реализованы
   - ✅ WebSocket интеграция реализована (useWebSocket hook, WebSocketProvider)
   - ✅ Redux store со всеми slices
   - ⚠️ Требуется: интеграция видеоплеера с RTSP клиентом через HLS
   - ⚠️ Требуется: доработка функциональности видеоплеера

6. **Дополнительная безопасность для корпоративных сред** ⚠️
   - Требуется: интеграция с LDAP/Active Directory
   - Требуется: поддержка SSO (SAML 2.0, OAuth 2.0 / OIDC)
   - Требуется: поддержка Kerberos
   - Требуется: синхронизация пользователей с доменом

**Детальный план:** CRITICAL_BLOCKERS_REMEDIATION_PLAN.md *(утерян/в архиве)*

---

## 🔧 КОНФИГУРАЦИЯ И ЗАВИСИМОСТИ

### Gradle модули
- `:shared` - Kotlin Multiplatform модуль (общая бизнес-логика)
- `:core:common` - общие типы (Resolution, CameraStatus и др.)
- ⏸️ `:core:license` - система лицензирования (отложено)
- `:core:network` - сетевое взаимодействие (Ktor Client, ONVIF, WebSocket, RTSP)
- `:android:app` - Android приложение
- `:server:api` - REST API сервер (Ktor)

### Зависимости между модулями
```
android/ios/desktop приложения (platforms/)
    ↓
shared (KMM)
    ↓     ↓
    ↓  core:network
    ↓     ↓
    ↓  core:common
    ↓
⏸️ core:license (отложено)
    ↓
native (C++ библиотеки через FFI, не Gradle модули)
```

**Важно:**
- `:core:network` зависит только от `:core:common`, а не от `:shared`, что устраняет циклическую зависимость
- `:shared` зависит от `:core:common` и использует `:core:network`
- Нативные C++ библиотеки (`native/`) собираются через CMake и интегрируются через FFI биндинги
- Серверные платформы (`platforms/sbc-arm/`, `platforms/server-x86_64/`, и др.) используют `:server:api`
- Все платформы используют общие модули: `:shared`, `:core:common`, `:core:network`
- ⏸️ `:core:license` - отложен (вынесен за рамки проекта)
- Структура веток Git: trunk-based — `main` (точка истины) + короткоживущие `feature/*`/`chore/*`/`refactor/*`; платформенные ветки `develop/platform-*`/`test/platform-*` удалены 15.09.2026 (бэкап: `archive/git-branches-backup-2026-09-15/`)

---

## 📚 ДОКУМЕНТАЦИЯ

### Основная документация
- `README.md` - общее описание проекта
- `PROJECT_PROMPT.md` - комплексный промпт проекта (этот файл)
- `PROJECT_STRUCTURE.md` - детальная структура проекта
- `PLATFORM_STRUCTURE.md` - структура платформ и веток Git
- `CURRENT_STATUS.md` - текущее состояние проекта (~20% прогресса)
- `TIMELINE.md` - временная шкала проекта (прошлое, настоящее, будущее)
- `PROJECT_ROADMAP.md` - карта выполнения проекта (детальный статус по компонентам)
- `DEVELOPMENT_ROADMAP.md` - карта разработки проекта (метрики и приоритеты)
- `DOCUMENTATION_INDEX.md` - полный индекс всей документации
- `docs/README.md` - навигация по документации
- `docs/ARCHITECTURE.md` - архитектура системы
- `docs/API.md` - API документация
- `docs/DEVELOPMENT.md` - руководство по разработке
- `docs/IMPLEMENTATION_STATUS.md` - статус реализации компонентов
- `docs/MISSING_FUNCTIONALITY.md` - детальный анализ нереализованного функционала
- `docs/DOCUMENTATION_GAPS.md` - анализ недостающей документации

### Анализ и отчеты
- `docs/DEEP_ANALYSIS_2025.md` - углубленный расширенный анализ проекта (january 2026)
- `docs/PROJECT_FULL_ANALYSIS.md` - полный углубленный анализ всего проекта
- `docs/ANALYSIS_SUMMARY_2025.md` - сводка анализа проекта
- `docs/SECURITY_AUDIT_REPORT.md` - отчет аудита безопасности (25 уязвимостей)
- `docs/SECURITY_REMEDIATION_PLAN.md` - план устранения уязвимостей
- `docs/USER_MANAGEMENT_SSO_KERBEROS_ANALYSIS.md` - анализ управления пользователями и интеграции с SSO/Kerberos
- `docs/DOCUMENTATION_GAPS.md` - анализ недостающей документации

### Техническая документация
- `docs/ONVIF_CLIENT.md` - документация ONVIF клиента
- `docs/RTSP_CLIENT.md` - документация RTSP клиента
- `docs/WEBSOCKET_CLIENT.md` - документация WebSocket клиента
- ⏸️ `docs/LICENSE_SYSTEM.md` - система лицензирования (отложено)
- `docs/DEPLOYMENT_GUIDE.md` - руководство по развертыванию
- `docs/INTEGRATION_GUIDE.md` - руководство по интеграции библиотек
- `docs/NAS_PLATFORMS_ANALYSIS.md` - анализ NAS платформ
- `docs/NATIVE_LIBRARIES_INTEGRATION.md` - интеграция нативных библиотек
- `docs/REQUIRED_LIBRARIES.md` - необходимые библиотеки
- `docs/LIBRARIES_INTEGRATION_SUMMARY.md` - сводка интеграции библиотек

### Руководства
- `docs/USER_GUIDE.md` - руководство пользователя
- `docs/OPERATOR_GUIDE.md` - руководство оператора
- `docs/ADMINISTRATOR_GUIDE.md` - руководство администратора
- `docs/TESTING.md` - руководство по тестированию
- `docs/DEVELOPMENT_TOOLS.md` - инструменты разработки
- `docs/TIMELINE_GUIDE.md` - руководство по ведению временной шкалы
- `docs/TIMELINE_SETUP.md` - настройка временной шкалы

---

## 🚀 БЫСТРЫЙ СТАРТ

### Требования
- JDK 17+
- Android Studio / Xcode (для мобильных платформ)
- Docker (для серверной версии)
- CMake 3.15+ (для нативных библиотек)
- Node.js 20+ (для веб-интерфейса)

### Сборка
```bash
# Клонировать репозиторий
# git clone <repository-url>
# cd ip-camera-surveillance-system

# Собрать все модули
./gradlew buildAll

# Собрать конкретную платформу
./gradlew :android:assembleDebug
./gradlew :shared:build

# Собрать нативные библиотеки
cd native
mkdir build && cd build
cmake ..
make
```

### Запуск
```bash
# Docker (рекомендуется)
docker-compose up -d

# Или локально
./gradlew :server:api:run
cd server/web && npm run dev
```

---

## 🔐 БЕЗОПАСНОСТЬ

### Защита данных
- Шифрование данных в покое (AES-256)
- Шифрование данных в движении (TLS 1.3)
- Безопасное хранение ключей (HSM, Keychain, Keystore)

### Контроль доступа
- **Ролевая модель (RBAC):**
  - Роли: ADMIN, OPERATOR, VIEWER, GUEST
  - Гранулярные разрешения (permissions)
  - Проверка прав на уровне операций
  - Маппинг групп AD на роли системы
- **Аутентификация:**
  - JWT токены (access + refresh)
  - LDAP/Active Directory интеграция
  - SSO (SAML 2.0, OAuth 2.0 / OIDC)
  - Kerberos аутентификация
  - Двухфакторная аутентификация (2FA)
- **Сессионное управление:**
  - Короткоживущие access tokens (15-30 минут)
  - Refresh token rotation
  - Отзыв токенов
  - HttpOnly cookies для refresh tokens
- **Аудит доступа:**
  - Логирование всех попыток входа
  - Логирование операций с пользователями
  - Логирование изменений прав доступа
  - Логирование доступа к защищенным ресурсам
  - Централизованный сбор логов (ELK, Splunk)

---

## 📈 МЕТРИКИ ПРОГРЕССА

### По модулям:
- **Инфраструктура:** 100%
- **Доменный слой:** ~45%
- **Слой данных:** ~65%
- **Сетевой слой:** ~40%
- ⏸️ **Лицензирование:** Отложено (вынесено за рамки проекта)
- **UI компоненты:** ~55% (Web ~70%, Android ~30%)
- **Серверная часть:** ~85%
- **Безопасность:** ~50% (базовая JWT готова)
- **Нативные библиотеки:** ~5%
- **Тестирование:** ~15%
- **Запись видео:** ~64%
- **AI-аналитика:** ~5%

### Общий прогресс: ~60%

---

## 🎓 КЛЮЧЕВЫЕ ПРИНЦИПЫ РАЗРАБОТКИ

1. **Clean Architecture** - четкое разделение на слои
2. **Kotlin Multiplatform** - общая бизнес-логика для всех платформ
3. **Dependency Injection** - Koin для Kotlin, Hilt для Android, Swinject для iOS
4. **Reactive Programming** - Kotlinx Coroutines для асинхронности
5. **Type Safety** - строгая типизация, Result типы для обработки ошибок
6. **Testability** - модульная архитектура для легкого тестирования
7. **Documentation** - подробная документация всех компонентов

---

## 🔄 CI/CD

### GitHub Actions
- `.github/workflows/ci.yml` - CI pipeline (сборка, тесты, линтинг)
- `.github/workflows/cd.yml` - CD pipeline (деплой, релизы)

### Docker
- `Dockerfile` - образ для API сервера
- `docker-compose.yml` - оркестрация сервисов

---

## 📞 ПОДДЕРЖКА

Документация проекта находится в директории `docs/` и корневых файлах проекта.
Для получения дополнительной информации см. `README.md` и `DOCUMENTATION_INDEX.md`.

---

---

## 📋 ПОЛНЫЙ СПИСОК ФУНКЦИЙ

Проект включает **27 основных функций**, разделенных на 4 категории приоритетов:

### 🔴 Критически необходимые функции (MVP) - 5 функций
1. Базовая работа с видео (RTSP, запись, просмотр, экспорт)
2. Обнаружение и подключение камер (WS-Discovery, UPnP, мастер добавления)
3. Базовый пользовательский интерфейс (Android, iOS, Desktop, Web)
4. Аутентификация и авторизация:
   - Базовая: JWT токены, RBAC, 2FA
   - Доменная интеграция: LDAP/AD, SSO (SAML/OIDC), Kerberos
   - Синхронизация пользователей с доменом
   - Авторизация на основе групп домена
5. Базовое управление камерами (PTZ, настройки, статистика, мониторинг)

### 🟡 Важные функции - 5 функций
6. AI-аналитика (базовая - движение, объекты, трекинг)
7. Система уведомлений (Push, Email, SMS, Telegram, Webhook)
8. Управление записями (просмотр, поиск, экспорт, хранилище)
9. Система событий (генерация, просмотр, фильтрация, экспорт)
10. Настройки и конфигурация (системные, камер, пользовательские)

### 🟢 Дополнительные функции - 12 функций
11. Расширенная AI-аналитика (ANPR, лица, поведение, подсчет)
12. Облачная синхронизация (между устройствами, облачное хранилище)
13. Расширенное управление PTZ (паттерны, автотрекинг, геозоны)
14. Многопользовательский режим (группы, делегирование, аудит)
15. Интеграция с внешними системами (умный дом, безопасность, облако)
16. Расширенная безопасность (шифрование, аудит, соответствие)
17. Управление приватностью (размытие лиц, анонимизация)
18. Плагинная архитектура (система плагинов, магазин)
19. Webhook и события (публикация, подписка, streaming)
20. Расширенная аналитика и отчеты (дашборды, статистика, визуализация)
21. Бизнес-аналитика (ROI, прогнозирование, рекомендации)
22. Улучшение UX/UI (адаптивный дизайн, персонализация, мультиязычность)
23. Офлайн режим (кэширование, офлайн просмотр, синхронизация)
24. Голосовое управление (команды, интеграция с ассистентами)

### 🔵 Инновационные функции - 3 функции
25. AI-аналитика следующего поколения (предиктивная, поведенческая, семантическая)
26. AR/VR интеграция (AR просмотр, VR мониторинг)
27. Edge AI и распределенная обработка (обработка на краю, кластеризация)

**Детальный анализ каждой функции:** См. [docs/FUNCTIONALITY_ANALYSIS.md](../archive/docs/analysis/FUNCTIONALITY_ANALYSIS.md)

**Общая оценка времени до полной реализации всех функций:** 18-24 месяца

---

---

## 🔍 СТРУКТУРА ПЛАТФОРМ И ВЕТОК GIT

Проект разделен на 8 платформ, каждая со своей директорией в `platforms/` с подробной документацией в README.md:

### Серверные платформы с веб-интерфейсом:
1. **Микрокомпьютеры ARM** (`platforms/sbc-arm/`)
   - Raspberry Pi, Orange Pi и др.
   - Использует `:server:api` и `:server:web`
   - Документация: `platforms/sbc-arm/README.md`

2. **Серверы x86-x64** (`platforms/server-x86_64/`)
   - Linux, Windows, macOS серверы
   - Использует `:server:api` и `:server:web`
   - Документация: `platforms/server-x86_64/README.md`

3. **NAS ARM** (`platforms/nas-arm/`)
   - Synology, QNAP, Asustor (ARM модели)
   - Поддиректории: `packages/synology/`, `packages/qnap/`, `packages/asustor/`
   - Использует `:server:api` и `:server:web`
   - Документация: `platforms/nas-arm/README.md`

4. **NAS x86-x64** (`platforms/nas-x86_64/`)
   - Synology, QNAP, Asustor, TrueNAS (x86_64 модели)
   - Поддиректории: `packages/synology/`, `packages/qnap/`, `packages/asustor/`
   - Использует `:server:api` и `:server:web`
   - Документация: `platforms/nas-x86_64/README.md`

### Клиентские платформы:
5. **Клиенты Desktop x86-x64** (`platforms/client-desktop-x86_64/`)
   - Windows, Linux, macOS (Intel)
   - Использует `:shared` и Compose Desktop
   - Документация: `platforms/client-desktop-x86_64/README.md`

6. **Клиенты Desktop ARM** (`platforms/client-desktop-arm/`)
   - Linux ARM64, macOS Apple Silicon
   - Использует `:shared` и Compose Desktop
   - Документация: `platforms/client-desktop-arm/README.md`

7. **Клиенты Android** (`android/app` — исходники; `platforms/client-android/` — только метаданные платформы)
   - Мобильные устройства Android
   - Использует `:android:app` и `:shared`
   - Документация: `platforms/client-android/README.md`

8. **Клиенты iOS/macOS** (`platforms/client-ios/`)
   - iOS и macOS устройства
   - Использует `:shared` и SwiftUI
   - Документация: `platforms/client-ios/README.md`

### Структура веток Git:
- `main` / `master` - основная стабильная ветка
- `develop/platform-*` - ветки разработки для каждой платформы
- `test/platform-*` - ветки тестирования для каждой платформы

**Подробнее:** [PLATFORM_STRUCTURE.md](../docs/PLATFORM_STRUCTURE.md)

---

## 📋 ИЗВЕСТНЫЕ ПРОБЛЕМЫ И ОГРАНИЧЕНИЯ

### Технические долги:
- ⚠️ 142+ вхождения TODO/FIXME в коде (требуется рефакторинг)
- ⚠️ Недостаточное тестовое покрытие (~15%)
- ⚠️ Многие компоненты имеют только базовые реализации

**Детальный анализ:** [docs/TECHNICAL_DEBT.md](../archive/docs-deprecated-2026-09-04/TECHNICAL_DEBT.md) - полный документ по техническому долгу с разбивкой по типам доработок и примерными сроками реализации

### Ограничения текущей версии:
- RTSP клиент не полностью реализован (требуется интеграция с нативной библиотекой)
- WS-Discovery в OnvifClient частично работает (требуется доработка)
- ✅ Аутентификация на сервере реализована (JWT, RBAC)
- 🟡 UI компоненты частично реализованы (Android ~30%, веб ~70%, iOS и Desktop отсутствуют)

---

**Последнее обновление:** 26 January 2026
**Версия:** Alfa-0.0.1
**Документ обновлен:**
- Обновлен прогресс проекта с ~55% до ~60%
- Добавлена информация о серверных сервисах (VideoRecordingService, VideoStreamService, ScreenshotService, HlsGeneratorService, FfmpegService, StorageService, PasswordService)
- Добавлена информация о серверных репозиториях (ServerUserRepository, ServerEventRepository, ServerRecordingRepository, ServerSettingsRepository)
- Добавлена информация о дополнительных middleware (CookieAuthMiddleware, SecurityLogger, RequestValidator, Redis интеграция)
- Добавлена детальная информация об Android приложении (экраны, ViewModels, компоненты, DI)
- Добавлена детальная информация о WebSocket интеграции в веб-интерфейсе (компоненты, hooks, Redux slice)
- Обновлена информация о Use Cases (15 вместо 5)
- Обновлена информация о репозиториях (SQLDelight реализации)
- Обновлена информация о серверной части (потоки, скриншоты, управление записью, HLS endpoints)
- Обновлена информация о веб-интерфейсе (WebSocket интеграция, все страницы, Redux slices)
- Обновлена информация о безопасности (частично исправлено, добавлены middleware)
- Обновлена информация о критических блокерах (статус выполнения)
- Добавлена детальная информация о структуре платформ с расположением документации

