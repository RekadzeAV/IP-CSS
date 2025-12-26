# Разделение разработки по платформам

**Дата создания:** Январь 2025
**Версия проекта:** 3.0.0

## Обзор

Проект IP-CSS использует Kotlin Multiplatform (KMP) для кроссплатформенной разработки. Бизнес-логика реализована в общем модуле `:shared`, а платформо-специфичный код разделен по source sets для каждой платформы.

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

## Платформо-специфичные модули

### Android приложение

**Модуль:** `:android:app`

**Структура:**
```
android/app/
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/company/ipcamera/
│   │   │   ├── MainActivity.kt
│   │   │   ├── Application.kt
│   │   │   ├── di/              # Hilt модули
│   │   │   ├── ui/              # Jetpack Compose экраны
│   │   │   │   ├── cameras/
│   │   │   │   ├── recordings/
│   │   │   │   └── settings/
│   │   │   └── viewmodel/       # ViewModels
│   │   └── res/                 # Ресурсы
│   └── test/                    # Unit тесты
└── build.gradle.kts
```

**Зависимости:**
- `:shared` - общая бизнес-логика
- Jetpack Compose
- Hilt
- Navigation Compose
- ExoPlayer (для видео)

### iOS приложение

**Проект:** `ios/IPCameraSurveillance.xcodeproj`

**Структура:**
```
ios/
├── IPCameraSurveillance/
│   ├── App.swift
│   ├── ContentView.swift
│   ├── Views/                   # SwiftUI экраны
│   │   ├── CameraListView.swift
│   │   ├── CameraDetailView.swift
│   │   └── SettingsView.swift
│   ├── ViewModels/             # ViewModels (обертка над KMP)
│   ├── Services/                # Сервисы
│   └── Resources/              # Ресурсы
├── IPCameraSurveillance.xcodeproj
└── Podfile                      # CocoaPods зависимости
```

**Интеграция с KMP:**
- Framework из `:shared` модуля
- Swift/Kotlin interop через `@objc` и `@objcMembers`

### Desktop приложение

**Модуль:** `:desktop` (планируется)

**Структура:**
```
desktop/
├── src/
│   └── main/kotlin/
│       └── com/company/ipcamera/desktop/
│           ├── Main.kt
│           ├── App.kt           # Compose Desktop приложение
│           ├── ui/               # UI компоненты
│           └── di/               # Koin модули
└── build.gradle.kts
```

**Технологии:**
- Compose Desktop
- Koin для DI
- jpackage для упаковки

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

## Серверная часть

### REST API сервер

**Модуль:** `:server:api`

**Технологии:**
- Ktor или Spring Boot (на выбор)
- PostgreSQL или SQLite
- JWT для аутентификации

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

## NAS устройства

### Обзор

**Модуль:** `:server:nas` (планируется)

**Поддерживаемые платформы:**
- ✅ Synology DSM (Linux-based)
- ✅ QNAP QTS (Linux-based)
- ✅ Asustor ADM (Linux-based)
- ✅ TrueNAS CORE (FreeBSD-based)
- ✅ TrueNAS SCALE (Linux + Kubernetes)
- ⚠️ Terramaster TOS (Linux-based)

**Архитектуры:**
- x86_64 (Intel/AMD) - все платформы
- ARMv8 / aarch64 - Synology, QNAP, Asustor, Terramaster
- ARMv7 (устаревшие модели) - ограниченная поддержка

### Synology DSM

**Формат пакета:** `.spk`

**Особенности:**
- Основан на Debian Linux
- Пакетный менеджер: Synology Package Center
- Поддержка Docker: ✅
- API: Synology API, RESTful API

**Разработка:**
- Synology Developer SDK
- Python 3.x, Node.js, Bash
- SPK Builder Tool

**Требования:**
- Архитектура: x86_64 (bromolow, apollolake, geminilake, denverton, v1000, broadwell, broadwellnk), aarch64 (rtd1296), armv7 (alpine)
- Минимум: 4 ГБ RAM, 50 ГБ свободного места

### QNAP QTS

**Формат пакета:** `.qpkg`

**Особенности:**
- Основан на CentOS/RHEL Linux
- Пакетный менеджер: QNAP App Center
- Поддержка Docker: ✅
- Поддержка Kubernetes: ✅ (QKVS)
- API: QNAP API, RESTful API

**Разработка:**
- QNAP Developer SDK
- Python 3.x, Node.js, PHP, Bash
- QPKG Tool

**Требования:**
- Архитектура: x86_64, arm_64, arm_x31
- Минимум: 4 ГБ RAM, 50 ГБ свободного места

### Asustor ADM

**Формат пакета:** `.apk` (не Android!)

**Особенности:**
- Основан на Debian Linux
- Пакетный менеджер: Asustor App Central
- Поддержка Docker: ✅
- API: Asustor API, RESTful API

**Разработка:**
- Asustor Developer SDK
- Python 3.x, Node.js, Bash
- ADM Toolkit

**Требования:**
- Архитектура: x86_64, aarch64, Realtek RTD1296
- Минимум: 2 ГБ RAM, 50 ГБ свободного места

### TrueNAS CORE

**Формат:** FreeBSD Jails, `.pbi`

**Особенности:**
- Основан на FreeBSD 13.x
- Файловая система: ZFS
- Поддержка Docker: ❌ (только через Jails)
- API: FreeNAS API v2.0, RESTful API

**Разработка:**
- FreeBSD SDK
- Jail упаковка
- FreeBSD пакеты

**Требования:**
- Архитектура: x86_64
- Минимум: 8 ГБ RAM, 50 ГБ свободного места

### TrueNAS SCALE

**Формат:** Docker, Helm charts, Kubernetes манифесты

**Особенности:**
- Основан на Debian Linux + Kubernetes
- Файловая система: ZFS
- Поддержка Docker: ✅
- Поддержка Kubernetes: ✅ (встроенный)
- API: RESTful API, Kubernetes API

**Разработка:**
- Docker образы
- Helm charts
- Kubernetes манифесты

**Требования:**
- Архитектура: x86_64
- Минимум: 16 ГБ RAM, 100 ГБ свободного места

### Универсальное решение: Docker

**Преимущества:**
- ✅ Работает на всех платформах с Docker (Synology, QNAP, Asustor, TrueNAS SCALE)
- ✅ Единая сборка для всех архитектур (multi-arch)
- ✅ Изоляция зависимостей
- ✅ Легкое обновление

**Недостатки:**
- ❌ Требует Docker на целевой системе
- ❌ Меньше интеграции с системой NAS

**Multi-arch Docker образ:**
```dockerfile
FROM --platform=$BUILDPLATFORM company/base:latest AS builder
# ... сборка ...

FROM --platform=$TARGETPLATFORM company/runtime:latest
COPY --from=builder /app /app
```

**Платформы Docker:**
- `linux/amd64` - x86_64
- `linux/arm64` - ARMv8 / aarch64

### Архитектура для NAS

```
┌─────────────────────────────────────────┐
│         NAS Operating System            │
│  (DSM/QTS/ADM/TrueNAS/TOS)             │
├─────────────────────────────────────────┤
│                                         │
│  ┌───────────────────────────────────┐ │
│  │   IP-CSS Package/Container        │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   Web UI (Next.js)          │  │ │
│  │  │   Port: 8080                │  │ │
│  │  └─────────────────────────────┘  │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   API Server (Ktor)         │  │ │
│  │  │   Port: 8081                │  │ │
│  │  └─────────────────────────────┘  │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   Shared Module (KMP)       │  │ │
│  │  │   - Business Logic          │  │ │
│  │  │   - Repositories            │  │ │
│  │  └─────────────────────────────┘  │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   Native Libraries (C++)   │  │ │
│  │  │   - Video Processing       │  │ │
│  │  │   - Analytics              │  │ │
│  │  └─────────────────────────────┘  │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   Database (SQLite)         │  │ │
│  │  │   Location: /data/db/       │  │ │
│  │  └─────────────────────────────┘  │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   Storage (Recordings)      │  │ │
│  │  │   Location: /recordings/    │  │ │
│  │  └─────────────────────────────┘  │ │
│  └───────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

**Пути данных для каждой платформы:**
- **Synology**: `/volume1/ip-css/`
- **QNAP**: `/share/CACHEDEV1_DATA/ip-css/`
- **Asustor**: `/volume1/ip-css/`
- **TrueNAS**: `/mnt/tank/ip-css/`

**Аппаратное ускорение:**
- **Intel Quick Sync** (x86_64 с Intel процессорами) - H.264/H.265 декодирование
- **AMD VCE/VCN** (x86_64 с AMD процессорами) - H.264/H.265 декодирование
- **ARM Mali/VideoCore** (ARM64 на некоторых платформах) - ограниченная поддержка

Подробная информация: [NAS_PLATFORMS_ANALYSIS.md](NAS_PLATFORMS_ANALYSIS.md)

## Разделение ответственности

### Что общее (commonMain)

✅ **Должно быть в commonMain:**
- Бизнес-логика (Use Cases)
- Доменные модели
- Интерфейсы репозиториев
- Реализации репозиториев (с использованием expect/actual для платформо-специфичных частей)
- Общие утилиты
- Сетевые клиенты (Ktor Client)

### Что платформо-специфичное

✅ **Android (androidMain):**
- Работа с Android SDK
- SQLDelight Android Driver
- WorkManager
- Android NotificationManager
- Android Keystore для лицензий

✅ **iOS (iosMain):**
- Работа с iOS frameworks
- SQLDelight Native Driver
- BGTaskScheduler
- UNUserNotificationCenter
- iOS Keychain для лицензий

✅ **Desktop (desktopMain):**
- Работа с файловой системой
- SQLDelight JVM Driver
- Системные службы
- Системные уведомления
- Криптография через BouncyCastle

### Что в нативных модулях

✅ **C++ библиотеки:**
- Обработка видео (FFmpeg)
- AI аналитика (OpenCV, TensorFlow Lite)
- RTSP протокол (Live555 или собственная реализация)
- Декодирование/кодирование видео

## Механизм expect/actual

Kotlin Multiplatform использует механизм `expect/actual` для платформо-специфичных реализаций:

```kotlin
// commonMain
expect class Platform {
    val name: String
    val version: String
}

// androidMain
actual class Platform {
    actual val name = "Android"
    actual val version = android.os.Build.VERSION.RELEASE
}

// iosMain
actual class Platform {
    actual val name = "iOS"
    actual val version = UIDevice.currentDevice.systemVersion
}

// desktopMain
actual class Platform {
    actual val name = System.getProperty("os.name")
    actual val version = System.getProperty("os.version")
}
```

## Сборка для разных платформ

### Android

```bash
./gradlew :android:assembleDebug
./gradlew :android:assembleRelease
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
./gradlew :desktop:packageMsi

# Linux
./gradlew :desktop:packageDeb

# macOS
./gradlew :desktop:packageDmg
```

### Все платформы

```bash
./scripts/build-all-platforms.sh
```

## Зависимости между модулями

```
android:app
    ↓
shared (androidMain)
    ↓
core:network, core:license, core:common
    ↓
native (C++ через FFI)

ios (Xcode)
    ↓
shared (iosMain) → framework
    ↓
core:network, core:license, core:common
    ↓
native (C++ через FFI)

desktop
    ↓
shared (desktopMain)
    ↓
core:network, core:license, core:common
    ↓
native (C++ через FFI)
```

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

- [ARCHITECTURE.md](ARCHITECTURE.md) - Архитектура системы
- [PROJECT_STRUCTURE.md](../PROJECT_STRUCTURE.md) - Структура проекта
- [DEVELOPMENT.md](DEVELOPMENT.md) - Руководство по разработке
- [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) - Статус реализации

---

**Последнее обновление:** Январь 2025

