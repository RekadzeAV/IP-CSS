# Changelog

Все значимые изменения в проекте документируются в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.0.0/),
и проект придерживается [Semantic Versioning](https://semver.org/lang/ru/).

## [Unreleased]

### В разработке
- REST API сервер (базовые endpoints)
- RTSP клиент (полная реализация)
- ONVIF WS-Discovery для автоматического обнаружения камер
- UI компоненты для Android, iOS, Desktop и Web
- Нативные библиотеки (C++) для видео обработки и аналитики
- Интеграция Live555 для RTSP клиента
- Интеграция TensorFlow Lite для AI аналитики

## [3.1.0] - 2025-01-XX

### Добавлено

#### Инфраструктура
- ✅ Модульная структура проекта (shared, core, native, server)
- ✅ Gradle конфигурация для всех модулей с Kotlin Multiplatform
- ✅ Version Catalog (`gradle/libs.versions.toml`) для управления зависимостями
- ✅ Поддержка платформ: Android, iOS (x64, arm64, simulator), Desktop (JVM)

#### Доменный слой
- ✅ Полная модель `Camera` с поддержкой PTZ, потоков, настроек, аналитики
- ✅ Модели данных: `Recording`, `Event`, `User`, `License`, `Settings`, `Notification`
- ✅ Интерфейсы репозиториев для всех доменных сущностей
- ✅ 5 Use Cases для управления камерами:
  - `AddCameraUseCase` - добавление новой камеры
  - `GetCamerasUseCase` - получение списка камер
  - `GetCameraByIdUseCase` - получение камеры по ID
  - `UpdateCameraUseCase` - обновление камеры
  - `DeleteCameraUseCase` - удаление камеры

#### Слой данных
- ✅ SQLDelight интеграция с полной схемой для камер
- ✅ `CameraRepositoryImpl` с CRUD операциями
- ✅ `CameraEntityMapper` для преобразования между DB entity и domain model
- ✅ Реализации репозиториев:
  - `RecordingRepositoryImpl`
  - `EventRepositoryImpl`
  - `UserRepositoryImpl`
  - `SettingsRepositoryImpl`
  - `NotificationRepositoryImpl`
  - `LicenseRepositoryImpl`
- ✅ Платформо-специфичные `DatabaseFactory` для Android и iOS
- ✅ Индексы базы данных для оптимизации запросов

#### Сетевой слой
- ✅ `ApiClient` - базовый HTTP клиент на Ktor с поддержкой:
  - Конфигурация (baseUrl, timeouts, headers)
  - Сериализация через Kotlinx Serialization (JSON)
  - Обработка ошибок с типизированными `ApiError`
  - Retry логика с экспоненциальной задержкой
  - Кэширование ответов для GET запросов
- ✅ `WebSocketClient` с поддержкой:
  - Автоматическое переподключение
  - Подписки на каналы
  - Обработка событий через `WebSocketEventHandler`
  - Состояния подключения через StateFlow
- ✅ Структура `RtspClient` и `OnvifClient`
- ✅ API сервисы (интерфейсы):
  - `CameraApiService`
  - `RecordingApiService`
  - `EventApiService`
  - `UserApiService`
  - `LicenseApiService`
  - `SettingsApiService`
- ✅ DTO модели для всех API сущностей

#### ONVIF клиент
- ✅ Базовая структура `OnvifClient` с поддержкой XML парсинга
- ✅ Интеграция `ktor-serialization-kotlinx-xml` для SOAP запросов
- ✅ Типы данных: `DiscoveredCamera`, `StreamInfo`, `CameraCapabilities`
- ⚠️ WS-Discovery (требует доработки для автоматического обнаружения)

#### Лицензирование
- ✅ `LicenseManager` с базовой структурой:
  - Онлайн и офлайн активация лицензий
  - Валидация лицензий
  - Управление функциями
- ✅ Платформо-специфичные реализации:
  - Android: `EncryptedSharedPreferences` с `MasterKey`
  - iOS: Keychain Services с Security framework
- ✅ Поддержка BouncyCastle для криптографических операций
- ✅ Типы ошибок и предупреждений лицензий

#### Нативные библиотеки (C++)
- ✅ CMake конфигурация для всех модулей:
  - `native/video-processing` - обработка видео
  - `native/analytics` - аналитика
  - `native/codecs` - кодеки
- ✅ Интеграция FFmpeg:
  - Декодер видео (H.264, H.265, MJPEG)
  - Конвертация YUV в RGB через SwsContext
  - Callback система для получения кадров
- ✅ Интеграция OpenCV:
  - Обработка кадров (`frame_processor`)
  - Детекция движения (`motion_detector`)
  - Детекция лиц (`face_detector`)
  - Детекция объектов (`object_detector`)
- ✅ Структуры файлов для:
  - RTSP клиента
  - Видео декодера/энкодера
  - Трекера объектов
  - ANPR движка

#### Платформо-специфичные реализации
- ✅ `Platform` (expect/actual) для Android, iOS, Desktop
- ✅ `FileSystem` (expect/actual) для всех платформ
- ✅ `NotificationManager` (expect/actual) для всех платформ
- ✅ `BackgroundWorker` (expect/actual) для всех платформ

#### Тестирование
- ✅ Настроены зависимости для тестирования (kotlin-test, mockk, turbine)
- ✅ `TestDatabaseFactory` для unit тестов
- ✅ `TestDataFactory` для создания тестовых данных
- ✅ Базовые тесты для репозиториев и use cases

#### Документация
- ✅ Полная архитектурная документация (`docs/ARCHITECTURE.md`)
- ✅ API документация (`docs/API.md`)
- ✅ Руководство по разработке (`docs/DEVELOPMENT.md`)
- ✅ Документация по лицензированию (`docs/LICENSE_SYSTEM.md`)
- ✅ Документация по ONVIF (`docs/ONVIF_CLIENT.md`)
- ✅ Документация по RTSP (`docs/RTSP_CLIENT.md`)
- ✅ Документация по WebSocket (`docs/WEBSOCKET_CLIENT.md`)
- ✅ Статус реализации (`docs/IMPLEMENTATION_STATUS.md`)
- ✅ Дорожная карта (`PROJECT_ROADMAP.md`)
- ✅ Текущий статус (`CURRENT_STATUS.md`)
- ✅ Интеграция библиотек (`docs/INTEGRATION_COMPLETE.md`)

### Изменено
- Обновлена архитектура на Kotlin Multiplatform для кроссплатформенной разработки
- Улучшена структура проекта с четким разделением модулей
- Оптимизирована конфигурация Gradle с использованием Version Catalog

### В процессе
- ⚠️ ONVIF WS-Discovery для автоматического обнаружения камер
- ⚠️ Полная реализация `testConnection()` в `CameraRepositoryImpl`
- ⚠️ Полная реализация `discoverCameras()` в `CameraRepositoryImpl`
- ⚠️ RTSP клиент (нативная реализация через Live555)
- ⚠️ FFI биндинги для Kotlin (cinterop) для нативных библиотек

### Запланировано
- REST API сервер (Ktor/Spring Boot)
- UI компоненты (Jetpack Compose, SwiftUI, Compose Desktop, React)
- Полная реализация записи видео
- AI-аналитика с TensorFlow Lite
- Веб-интерфейс (Next.js)

## [3.0.0] - 2025-01-20

### Добавлено
- Кроссплатформенная поддержка (Android, iOS, Windows, Linux, macOS, NAS)
- Новая система лицензирования v3 с офлайн-активацией
- AI-аналитика с детекцией объектов и трекингом
- Распознавание номеров автотранспорта (ANPR)
- Веб-интерфейс для удаленного управления
- REST API и WebSocket API
- Поддержка множественных камер (до 16+)
- Непрерывная запись и запись по событиям
- Система уведомлений (Push, Email, SMS, Telegram)
- Облачная синхронизация и хранение
- Шифрование данных (AES-256-GCM)
- Ролевая модель доступа (RBAC)

### Изменено
- Полная переработка архитектуры на Kotlin Multiplatform
- Обновлен формат лицензий (требуется миграция)
- Улучшена производительность обработки видео
- Оптимизировано энергопотребление на мобильных устройствах

### Исправлено
- Проблемы с переподключением к камерам
- Утечки памяти при длительной записи
- Ошибки синхронизации между устройствами

### Безопасность
- Улучшена криптографическая защита лицензий
- Добавлена проверка целостности данных
- Усилена защита от взлома

## [2.5.0] - 2023-12-15

### Добавлено
- Базовая поддержка ONVIF
- Детекция движения
- Экспорт видео

## [2.0.0] - 2023-10-01

### Добавлено
- Поддержка Android и iOS
- Базовая запись видео

## [1.0.0] - 2023-01-01

### Добавлено
- Первый релиз
- Поддержка Windows
- Базовый просмотр камер

