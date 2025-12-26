# Разделение разработки по платформам

**Дата обновления:** Декабрь 2025
**Версия проекта:** Alfa-0.0.1

## Обзор

Проект IP-CSS использует Kotlin Multiplatform (KMP) для кроссплатформенной разработки. Бизнес-логика реализована в общем модуле `:shared`, а платформо-специфичный код разделен по source sets для каждой платформы.

**Важное изменение:** Проект реорганизован по платформам. Каждая платформа имеет свою директорию в `platforms/` с подробной документацией. См. [PLATFORM_STRUCTURE.md](../../PLATFORM_STRUCTURE.md) для подробностей.

## Структура платформ

Проект разделен на следующие платформы:

### 1. Серверные платформы с веб-интерфейсом

#### Микрокомпьютеры ARM (SBC ARM)
- **Директория:** `platforms/sbc-arm/`
- **Модули:** `:server:api`, `server/web`
- **Архитектура:** ARM (ARMv7, ARMv8/aarch64)
- **Документация:** [platforms/sbc-arm/README.md](../../platforms/sbc-arm/README.md)

#### Серверы x86-x64
- **Директория:** `platforms/server-x86_64/`
- **Модули:** `:server:api`, `server/web`
- **Архитектура:** x86-64 (Intel/AMD)
- **Документация:** [platforms/server-x86_64/README.md](../../platforms/server-x86_64/README.md)

#### NAS ARM
- **Директория:** `platforms/nas-arm/`
- **Модули:** `:server:api`, `server/web`
- **Архитектура:** ARM (ARMv8/aarch64)
- **Документация:** [platforms/nas-arm/README.md](../../platforms/nas-arm/README.md)

#### NAS x86-x64
- **Директория:** `platforms/nas-x86_64/`
- **Модули:** `:server:api`, `server/web`
- **Архитектура:** x86-64
- **Документация:** [platforms/nas-x86_64/README.md](../../platforms/nas-x86_64/README.md)

### 2. Клиентские платформы

#### Клиенты Desktop x86-x64
- **Директория:** `platforms/client-desktop-x86_64/`
- **ОС:** Windows, Linux, macOS (Intel)
- **UI:** Compose Desktop
- **Документация:** [platforms/client-desktop-x86_64/README.md](../../platforms/client-desktop-x86_64/README.md)

#### Клиенты Desktop ARM
- **Директория:** `platforms/client-desktop-arm/`
- **ОС:** Linux ARM64, macOS Apple Silicon
- **UI:** Compose Desktop
- **Документация:** [platforms/client-desktop-arm/README.md](../../platforms/client-desktop-arm/README.md)

#### Клиенты Android
- **Директория:** `platforms/client-android/`
- **Модуль:** `:android:app`
- **UI:** Jetpack Compose
- **Документация:** [platforms/client-android/README.md](../../platforms/client-android/README.md)

#### Клиенты iOS/macOS
- **Директория:** `platforms/client-ios/`
- **UI:** SwiftUI
- **Документация:** [platforms/client-ios/README.md](../../platforms/client-ios/README.md)

## Архитектура платформ

```
┌─────────────────────────────────────────────────────────────┐
│                    Kotlin Multiplatform                      │
│                      (KMP / KMM)                            │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Android    │  │     iOS      │  │   Desktop    │     │
│  │   (JVM)      │  │  (Native)    │  │    (JVM)     │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                 │                 │              │
│         └─────────────────┼─────────────────┘              │
│                            │                                 │
│                   ┌────────▼────────┐                       │
│                   │   :shared       │                       │
│                   │  (commonMain)   │                       │
│                   └────────┬────────┘                       │
│                            │                                 │
│         ┌──────────────────┼──────────────────┐            │
│         │                  │                  │             │
│  ┌──────▼──────┐  ┌───────▼──────┐  ┌───────▼──────┐     │
│  │:core:common │  │:core:network │  │:core:license │     │
│  └─────────────┘  └──────────────┘  └──────────────┘     │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                            │
                   ┌────────▼────────┐
                   │  Native (C++)  │
                   │  (FFI через    │
                   │   cinterop)    │
                   └─────────────────┘
```

## Структура source sets

### Общий код (commonMain)

**Расположение:** `shared/src/commonMain/kotlin/`

**Содержит:**
- Доменные модели (Camera, Recording, Event, User, License, Settings, Notification)
- Интерфейсы репозиториев
- Use Cases (бизнес-логика)
- Реализации репозиториев (используют expect/actual для платформо-специфичных частей)
- Общие утилиты и сервисы

**Зависимости:**
- `:core:common` - базовые типы (Resolution, CameraStatus)
- `:core:network` - сетевые клиенты
- `:core:license` - система лицензирования

### Android (androidMain)

**Расположение:** `shared/src/androidMain/kotlin/`

**Содержит:**
- Реализации expect/actual для Android:
  - `Platform.android.kt` - информация о платформе
  - `FileSystem.android.kt` - работа с файловой системой
  - `NotificationManager.android.kt` - уведомления через NotificationManager
  - `BackgroundWorker.android.kt` - фоновая работа через WorkManager
  - `DatabaseFactory.android.kt` - инициализация SQLDelight для Android

**Технологии:**
- Android SDK
- SQLDelight Android Driver
- WorkManager для фоновых задач
- Android NotificationManager

**Модуль приложения:** `:android:app`
- Jetpack Compose UI
- Hilt для Dependency Injection
- Navigation Compose
- ViewModels

### iOS (iosMain)

**Расположение:** `shared/src/iosMain/kotlin/`

**Содержит:**
- Реализации expect/actual для iOS:
  - `Platform.ios.kt` - информация о платформе
  - `FileSystem.ios.kt` - работа с файловой системой через NSFileManager
  - `NotificationManager.ios.kt` - уведомления через UNUserNotificationCenter
  - `BackgroundWorker.ios.kt` - фоновая работа через BGTaskScheduler
  - `DatabaseFactory.ios.kt` - инициализация SQLDelight для iOS

**Технологии:**
- Kotlin/Native для iOS
- SQLDelight Native Driver
- CocoaPods для зависимостей
- BackgroundTasks framework

**Xcode проект:** `ios/IPCameraSurveillance.xcodeproj`
- SwiftUI для UI
- Swinject для Dependency Injection
- SwiftUI Navigation

### Desktop (desktopMain)

**Расположение:** `shared/src/desktopMain/kotlin/`

**Содержит:**
- Реализации expect/actual для Desktop:
  - `Platform.desktop.kt` - информация о платформе (Windows/Linux/macOS)
  - `FileSystem.desktop.kt` - работа с файловой системой
  - `NotificationManager.desktop.kt` - системные уведомления
  - `BackgroundWorker.desktop.kt` - системные службы/демоны
  - `DatabaseFactory.desktop.kt` - инициализация SQLDelight для Desktop

**Технологии:**
- Kotlin/JVM
- SQLDelight JVM Driver
- Compose Desktop для UI
- Koin для Dependency Injection

**Поддерживаемые платформы:**
- Windows (x64)
- Linux (x64, ARM64)
- macOS (x64, ARM64)

## Серверные модули

### REST API сервер

**Модуль:** `:server:api`

**Технологии:**
- Ktor Server
- SQLDelight (SQLite)
- JWT для аутентификации (планируется)

**Платформы:**
- JVM (Windows, Linux, macOS)
- Docker контейнеры

### Веб-интерфейс

**Модуль:** `server/web/`

**Технологии:**
- Next.js
- React
- TypeScript
- Redux Toolkit

**Платформы:**
- Node.js (все платформы)
- Docker контейнеры

## Нативные библиотеки (C++)

**Расположение:** `native/`

**Модули:**
- `native/video-processing/` - обработка видео, RTSP клиент
- `native/analytics/` - AI аналитика (детекция объектов, ANPR, лица)
- `native/codecs/` - кодеки (H.264, H.265, MJPEG)

**Интеграция с KMP:**
- FFI (Foreign Function Interface) через cinterop
- `.def` файлы для генерации Kotlin биндингов
- Платформо-специфичная компиляция через CMake

**Поддерживаемые платформы:**
- Android (armeabi-v7a, arm64-v8a, x86, x86_64)
- iOS (arm64, x86_64 для симулятора)
- Desktop (Windows x64, Linux x64/ARM64, macOS x64/ARM64)
- Linux Server/NAS (x86_64, ARM64)

## Структура веток Git

Для каждой платформы созданы отдельные ветки разработки и тестирования:

### Ветки для серверных платформ:
- `develop/platform-sbc-arm` → `test/platform-sbc-arm`
- `develop/platform-server-x86_64` → `test/platform-server-x86_64`
- `develop/platform-nas-arm` → `test/platform-nas-arm`
- `develop/platform-nas-x86_64` → `test/platform-nas-x86_64`

### Ветки для клиентских платформ:
- `develop/platform-client-desktop-x86_64` → `test/platform-client-desktop-x86_64`
- `develop/platform-client-desktop-arm` → `test/platform-client-desktop-arm`
- `develop/platform-client-android` → `test/platform-client-android`
- `develop/platform-client-ios` → `test/platform-client-ios`

Подробнее см. [PLATFORM_STRUCTURE.md](../../PLATFORM_STRUCTURE.md).

## Сборка для разных платформ

### Android

```bash
./gradlew :android:app:assembleDebug
./gradlew :android:app:assembleRelease
```

### iOS

```bash
# Сборка framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# В Xcode
xcodebuild -workspace IPCameraSurveillance.xcworkspace \
           -scheme IPCameraSurveillance \
           -configuration Release
```

### Desktop

```bash
# Windows
./gradlew :platforms:client-desktop-x86_64:app:packageMsi

# Linux
./gradlew :platforms:client-desktop-x86_64:app:packageDeb

# macOS
./gradlew :platforms:client-desktop-x86_64:app:packageDmg
```

### Сервер

```bash
# x86_64
./gradlew :server:api:build

# ARM
./gradlew :server:api:build -PtargetArch=arm64
```

### Все платформы

```bash
./scripts/build-all-platforms.sh
```

## Зависимости между модулями

```
android/ios/desktop приложения
    ↓
shared (KMM)
    ↓     ↓
    ↓  core:network
    ↓     ↓
    ↓  core:common (базовые типы: Resolution, CameraStatus)
    ↓
core:license
    ↓
native (C++ библиотеки через FFI)
```

**Важно:** Модуль `:core:network` зависит только от `:core:common`, а не от `:shared`, что устраняет циклическую зависимость. `:shared` зависит от обоих модулей `:core:network` и `:core:common`.

## Рекомендации по разработке

### 1. Минимизация платформо-специфичного кода

- Выносите общую логику в `commonMain`
- Используйте expect/actual только когда необходимо
- Избегайте дублирования кода между платформами

### 2. Тестирование

- Unit тесты в `commonTest` для общей логики
- Платформо-специфичные тесты в соответствующих source sets
- Integration тесты для каждой платформы

### 3. Документация

- Документируйте платформо-специфичные особенности
- Указывайте ограничения для каждой платформы
- Обновляйте примеры кода для всех платформ

## Связанные документы

- [PLATFORM_STRUCTURE.md](../../PLATFORM_STRUCTURE.md) - Структура платформ и веток
- [ARCHITECTURE.md](ARCHITECTURE.md) - Архитектура системы
- [PROJECT_STRUCTURE.md](../../PROJECT_STRUCTURE.md) - Структура проекта
- [DEVELOPMENT.md](DEVELOPMENT.md) - Руководство по разработке
- [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) - Статус реализации

---

**Последнее обновление:** Декабрь 2025
