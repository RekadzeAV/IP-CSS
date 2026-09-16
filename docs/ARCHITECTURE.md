# Архитектура кроссплатформенной системы видеонаблюдения

**Дата обновления:** January 2026
**Версия проекта:** Alfa-0.0.1

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

**См. также:** [DEEP_ANALYSIS_2025.md](archive/2026-04-27/analyses-old/DEEP_ANALYSIS_2025.md) - углубленный анализ архитектуры и компонентов

## Обзор архитектуры

Система построена по принципу многослойной Clean Architecture с использованием Kotlin Multiplatform для кроссплатформенной бизнес-логики и нативных UI фреймворков для каждой платформы.

**Важное изменение:** Проект реорганизован по платформам. Каждая платформа имеет свою директорию в `platforms/` с подробной документацией. См. [PLATFORM_STRUCTURE.md](PLATFORM_STRUCTURE.md) и [PLATFORMS.md](PLATFORMS.md) для подробностей.

## Структура платформ

Проект разделен на следующие платформы:

### Серверные платформы с веб-интерфейсом

1. **Микрокомпьютеры ARM** (`platforms/sbc-arm/`)
   - Raspberry Pi, Orange Pi, Rock64 и др.
   - Веб-интерфейс для управления

2. **Серверы x86-x64** (`platforms/server-x86_64/`)
   - Linux, Windows, macOS серверы
   - Веб-интерфейс для управления

3. **NAS ARM** (`platforms/nas-arm/`)
   - Synology, QNAP, Asustor (ARM модели)
   - Веб-интерфейс для управления

4. **NAS x86-x64** (`platforms/nas-x86_64/`)
   - Synology, QNAP, Asustor, TrueNAS (x86_64 модели)
   - Веб-интерфейс для управления

### Клиентские платформы

5. **Клиенты Desktop x86-x64** (`platforms/client-desktop-x86_64/`)
   - Windows, Linux, macOS (Intel)
   - Нативное приложение (Compose Desktop)

6. **Клиенты Desktop ARM** (`platforms/client-desktop-arm/`)
   - Linux ARM64, macOS Apple Silicon
   - Нативное приложение (Compose Desktop)

7. **Клиенты Android** (`android/app` — Jetpack Compose; метаданные платформы в `platforms/client-android/README.md`)
   - Мобильные устройства Android
   - Нативное приложение (Jetpack Compose)

8. **Клиенты iOS/macOS** (`platforms/client-ios/`)
   - iOS и macOS устройства
   - Нативное приложение (SwiftUI)

Все серверные платформы используют общие модули:
- `:server:api` - REST API сервер (Ktor)
- `server/web` - Веб-интерфейс (Next.js)

Все платформы используют общие модули:
- `:shared` - Kotlin Multiplatform модуль с общей бизнес-логикой
- `:core:common`, `:core:network` - общие модули
- ⏸️ `:core:license` - отложен (вынесен за рамки проекта)
- `native/` - нативные C++ библиотеки

## Архитектурные принципы

1. **Принцип единой ответственности** - каждый модуль отвечает за одну конкретную задачу
2. **Разделение ответственности** - четкое разделение на слои: данные, домен, представление
3. **Инверсия зависимостей** - зависимости направлены к абстракциям, а не к реализациям
4. **Принцип подстановки Барбары Лисков** - объекты могут быть заменены их подтипами
5. **Принцип открытости/закрытости** - открыты для расширения, закрыты для модификации

## Слои архитектуры

### 1. Слой данных (Data Layer)

**Ответственность**: Работа с источниками данных (локальными, сетевыми, облачными)

**Компоненты**:
- **Репозитории** - абстракции для доступа к данным
- **Data Sources** - конкретные реализации источников данных
  - Локальная БД (SQLite с SQLDelight)
  - Файловая система (видеоархивы, конфигурации)
  - Сетевые API (облачные сервисы)
  - IP-камеры (RTSP, ONVIF)

**Технологии**:
- Kotlin Multiplatform для общей логики
- SQLDelight для кроссплатформенной работы с SQLite
- Ktor Client для сетевых запросов

### 2. Доменный слой (Domain Layer)

**Ответственность**: Бизнес-логика и правила приложения

**Компоненты**:
- **Use Cases** - конкретные бизнес-сценарии
- **Модели** - бизнес-сущности (Камера, Событие)
- ⏸️ Модель Лицензия (отложено)
- **Репозитории интерфейсы** - абстракции для слоя данных
- **Сервисы** - доменные сервисы (аналитика)
- ⏸️ Сервис лицензирования (отложено)

**Особенности**:
- Полностью на Kotlin Multiplatform
- Не зависит от фреймворков и платформ
- Содержит unit тесты для бизнес-логики

### 3. Слой представления (Presentation Layer)

**Ответственность**: Пользовательский интерфейс и взаимодействие

**Архитектурный паттерн**: MVI (Model-View-Intent) или MVVM

**Компоненты**:
- **View** - UI компоненты
- **ViewModel/Presenter** - управление состоянием UI
- **State** - иммутабельное состояние экрана
- **Intent/Action** - пользовательские действия

**Платформенно-специфичные реализации**:

#### Серверные платформы (Веб-интерфейс):
- **Frontend**: React + TypeScript
- **UI Framework**: Material-UI / Next.js
- **Состояние**: Redux Toolkit
- **API клиент**: Axios

#### Android:
- **UI Framework**: Jetpack Compose
- **DI**: Hilt
- **Навигация**: Compose Navigation
- **Фоновая работа**: WorkManager, ForegroundService

#### iOS:
- **UI Framework**: SwiftUI
- **DI**: Swinject
- **Навигация**: SwiftUI Navigation
- **Фоновая работа**: BackgroundTasks, BGTaskScheduler

#### Desktop (Windows, Linux, macOS):
- **UI Framework**: Compose Desktop
- **DI**: Koin
- **Навигация**: собственный роутер на Compose
- **Фоновая работа**: системные службы/демоны

### 4. Слой инфраструктуры (Infrastructure Layer)

**Ответственность**: Кроссплатформенные сервисы и утилиты

**Модули**:
- `:core:common` - общие типы и модели (Resolution, CameraStatus)
- `:core:network` - сетевое взаимодействие (Ktor Client)
- ⏸️ `:core:license` - система лицензирования (отложено)
- `:core:auth` - аутентификация и авторизация (JVM только)
  - LDAP клиент
  - Kerberos клиент
  - SSO провайдеры (SAML, OIDC)
  - JWT управление
- `:native:video-processing` - обработка видео на C++/OpenCV
- `:native:analytics` - AI аналитика на C++/TensorFlow Lite
  - Детекция объектов (YOLO, TensorFlow Lite)
  - Распознавание номеров авто (ANPR/LPR)
  - Трекинг объектов (IoU Matching, Kalman Filter, DeepSORT)
  - Детекция движения (MOG2, Frame Difference)
  - Распознавание лиц (Haar Cascades, DNN)
  - Подробнее: [docs/AI_ANALYTICS.md](AI_ANALYTICS.md)

## Модульная структура

### Корневая структура проекта:
```
IP-CSS/
├── shared/ # Kotlin Multiplatform модуль
│ ├── src/
│ │ ├── commonMain/ # Общий код для всех платформ
│ │ ├── androidMain/ # Android-специфичные реализации
│ │ ├── iosMain/    # iOS-специфичные реализации
│ │ └── desktopMain/ # Desktop-специфичные реализации
│ └── build.gradle.kts
├── platforms/      # Платформо-специфичные реализации
│ ├── sbc-arm/
│ ├── server-x86_64/
│ ├── nas-arm/
│ ├── nas-x86_64/
│ ├── client-desktop-x86_64/
│ ├── client-desktop-arm/
│ ├── client-android/
│ └── client-ios/
├── android/        # Android приложение
├── server/         # Серверная часть
│ ├── api/         # REST API (Ktor)
│ └── web/         # Веб-интерфейс (Next.js)
├── native/        # Нативные C++ библиотеки
│ ├── video-processing/
│ ├── analytics/
│ └── codecs/
├── core/          # Общие кроссплатформенные модули
│ ├── common/      # Общие типы
│ ├── network/     # Сетевые клиенты
│ └── license/     # ⏸️ Система лицензирования (отложено)
└── docs/          # Документация
```

### Зависимости между модулями:
```
Платформы (серверные и клиентские)
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

## Кроссплатформенная коммуникация

### 1. Общая бизнес-логика (KMM)
```kotlin
// shared/src/commonMain/kotlin/com/company/domain/
interface CameraRepository {
    suspend fun getCameras(): List<Camera>
    suspend fun addCamera(camera: Camera)
    suspend fun removeCamera(id: String)
}
```

### 2. Нативные библиотеки (C++)
```cmake
# Интеграция C++ библиотек через FFI (Foreign Function Interface)
native fun processVideoFrame(frame: ByteArray): ProcessedFrame
native fun detectObjects(frame: ByteArray): List<Detection>
native fun recognizeLicensePlate(image: ByteArray): String? // ⏸️ Отложено: лицензирование вынесено за рамки проекта
```

### 3. Сетевая синхронизация
- Протокол: HTTP/2 + Protocol Buffers
- Сервисы: Синхронизация настроек, удаленное управление
- ⏸️ Лицензионный сервер (отложено)
- Безопасность: TLS 1.3, JWT токены, шифрование end-to-end

### 4. Аутентификация и авторизация
- **Клиентская часть (KMM):**
  - JWT токены (access + refresh)
  - Хранение токенов (SecureStorage на каждой платформе)
  - Автоматическое обновление токенов
- **Серверная часть (JVM):**
  - JWT верификация
  - LDAP/AD интеграция
  - Kerberos аутентификация
  - SSO провайдеры (SAML, OIDC)
  - Синхронизация пользователей
  - Авторизация на основе групп

## ⏸️ Система лицензирования (ОТЛОЖЕНО)

> **Статус:** Функционал лицензирования вынесен за рамки проекта и будет реализован в отдельной доработке после полной переработки архитектуры.

**Текущее состояние:**
- Базовая структура `LicenseManager` сохранена (~20% реализации)
- Код находится в `core/license/` но не используется в текущей версии
- Требуется полная переработка архитектуры системы лицензирования

**Планируемая архитектура (для будущей реализации):**
```
Клиент (любая платформа)
    ↓ (HTTPS + TLS)
Лицензионный сервер (микросервис)
    ↓
База лицензий (PostgreSQL)
    ↓
Система биллинга (интеграция)
```

**Детали:** См. [docs/LICENSE_SYSTEM.md](../archive/docs-deprecated-2026-09-04/LICENSE_SYSTEM.md)

## Масштабирование и отказоустойчивость

### Вертикальное масштабирование:
- Оптимизация под разные классы устройств
- Адаптивная обработка видео
- Динамическое распределение ресурсов

### Горизонтальное масштабирование (enterprise):
```
Клиенты → Load Balancer → [Сервер 1, Сервер 2, Сервер N]
                              ↓
                       Общее хранилище (SAN/NAS)
                              ↓
                       База данных (кластер)
```

### Стратегии отказоустойчивости:
1. Репликация данных - синхронная/асинхронная репликация БД
2. Кластеризация - активный-активный или активный-пассивный
3. Геораспределение - дата-центры в разных регионах
4. Автоматическое восстановление - health checks, auto-restart

## Мониторинг и логирование

### Централизованное логирование:
- Структурированные логи (JSON)
- Сбор логов со всех компонентов
- Анализ и алертинг (ELK stack, Grafana)

### Метрики производительности:
- Время отклика API
- Использование ресурсов (CPU, память, диск, сеть)
- Качество видео (FPS, задержки, потеря кадров)
- Статистика пользователей (активные сессии, операции)

### Health checks:
- Endpoint /health на всех сервисах
- Проверка зависимостей (БД, внешние сервисы)
- Детальная информация о состоянии

## Развертывание

### Контейнеризация:
- Мульти-архитектурные образы (amd64, arm64)
- Многоступенчатые сборки для уменьшения размера
- Health checks и graceful shutdown

### Оркестрация (Kubernetes):
- Deployment для каждого микросервиса
- Service discovery и load balancing
- Horizontal Pod Autoscaling
- Rolling updates с blue-green deployment

### Инфраструктура как код:
- Terraform для облачных ресурсов
- Ansible для конфигурации
- GitOps для непрерывного развертывания

## Разделение по платформам

Проект использует Kotlin Multiplatform для кроссплатформенной разработки. Подробная информация о разделении разработки по платформам доступна в [PLATFORMS.md](PLATFORMS.md) и [PLATFORM_STRUCTURE.md](PLATFORM_STRUCTURE.md).

### Основные принципы:
- **Общий код** (`commonMain`) - бизнес-логика, модели, use cases
- **Платформо-специфичный код** (`androidMain`, `iosMain`, `desktopMain`) - UI, системные API, платформенные сервисы
- **Нативные библиотеки** (C++) - обработка видео, AI аналитика

### Source Sets:
- `commonMain` - код, общий для всех платформ
- `androidMain` - Android-специфичные реализации
- `iosMain` - iOS-специфичные реализации (arm64, x86_64 для симулятора)
- `desktopMain` - Desktop-специфичные реализации (JVM для Windows, Linux, macOS)

### Иерархия Source Sets (Phase 2 Architecture)

**Важное обновление:** В модуле `core:network` внедрена иерархическая структура source sets для эффективного разделения JVM-кода между Android и Desktop.

```
commonMain (все платформы)
    ↓
    jvmMain (общий JVM код: Live555, FFmpeg bindings)
    ↓
    ├─→ androidMain (Android-specific: Ktor Android engine)
    └─→ desktopMain (Desktop-specific: Ktor Java engine, JavaCV)
```

**Преимущества:**
- Общие JVM реализации (RTSP клиент, сетевые утилиты) размещаются в `jvmMain`
- Android и Desktop разделяют один код без дублирования
- Platform-specific код (JavaCV для Desktop, Android SDK) остаётся изолированным

**Примеры файлов:**
- `jvmMain/` — `Live555RTSPClient.jvm.kt`, `WSDiscovery.jvm.kt`, `CertificatePinner.jvm.kt`
- `androidMain/` — `VideoDecoder.android.kt` (stub), `CertificatePinner.android.kt` (stub)
- `desktopMain/` — `VideoDecoder.jvm.kt` (с JavaCV), `NativeUtils.kt`

**Подробное руководство:** См. [KMP_SOURCE_SETS_GUIDE.md](../archive/docs/guides/KMP_SOURCE_SETS_GUIDE.md)

## Структура веток Git

> **Обновлено 15.09.2026:** платформо-ориентированная структура веток упразднена — `develop/platform-*`, `test/platform-*` (а также `dev/*`, `test/<платформа>`) удалены из репозитория (бэкап: `archive/git-branches-backup-2026-09-15/`).

Действует trunk-based модель:

- `main` - единственная долгоживущая ветка (точка истины)
- `feature/*`, `chore/*`, `refactor/*` - короткоживущие ветки от `main`
- `dependabot/*` - автоматические обновления зависимостей

Подробнее см. [CONTRIBUTING.md](../CONTRIBUTING.md); историческая модель — [PLATFORM_STRUCTURE.md](PLATFORM_STRUCTURE.md).

## Безопасность

### Защита данных:
- Шифрование данных в покое (AES-256)
- Шифрование данных в движении (TLS 1.3)
- Безопасное хранение ключей (HSM, Keychain, Keystore)
- Certificate pinning для защиты от MITM атак

### Аутентификация и авторизация

#### Архитектура аутентификации

Система поддерживает множественные методы аутентификации:

```
┌─────────────────────────────────────────────────────────────┐
│                    Корпоративная сеть                       │
│                                                              │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │ Active       │      │ Key          │                    │
│  │ Directory    │◄────►│ Distribution │                    │
│  │ (Domain      │      │ Center (KDC)  │                    │
│  │ Controller)  │      │              │                    │
│  └──────┬───────┘      └──────┬───────┘                    │
│         │                      │                            │
│         │ LDAP/              │ Kerberos                    │
│         │ Kerberos            │ Protocol                   │
│         │                      │                            │
│         ▼                      ▼                            │
│  ┌──────────────────────────────────────────┐               │
│  │     IP-CSS Server                       │               │
│  │                                          │               │
│  │  ┌────────────────────────────────────┐ │               │
│  │  │ Authentication Layer               │ │               │
│  │  │  - LDAP Client                     │ │               │
│  │  │  - Kerberos Client                 │ │               │
│  │  │  - SSO Provider (SAML/OIDC)        │ │               │
│  │  │  - JWT Token Manager              │ │               │
│  │  └────────────────────────────────────┘ │               │
│  │                                          │               │
│  │  ┌────────────────────────────────────┐ │               │
│  │  │ Authorization Layer                │ │               │
│  │  │  - Role Mapping                    │ │               │
│  │  │  - Permission Engine               │ │               │
│  │  │  - Group Membership Check          │ │               │
│  │  └────────────────────────────────────┘ │               │
│  │                                          │               │
│  │  ┌────────────────────────────────────┐ │               │
│  │  │ User Sync Service                  │ │               │
│  │  │  - Periodic Sync                   │ │               │
│  │  │  - Event-driven Updates            │ │               │
│  │  │  - Conflict Resolution             │ │               │
│  │  └────────────────────────────────────┘ │               │
│  └──────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

#### Методы аутентификации

1. **JWT токены (базовая)**
   - Access tokens (15-30 минут)
   - Refresh tokens (7-30 дней)
   - Token rotation
   - HttpOnly cookies для refresh tokens

2. **LDAP / Active Directory**
   - Прямая аутентификация через LDAP
   - Синхронизация пользователей
   - Маппинг групп AD на роли системы
   - Поддержка вложенных и универсальных групп

3. **SSO (Single Sign-On)**
   - SAML 2.0
   - OAuth 2.0 / OpenID Connect
   - WS-Federation (опционально)
   - Интеграция с Azure AD, Okta, Keycloak

4. **Kerberos**
   - SPNEGO токены
   - Интеграция с KDC
   - Автоматический вход для пользователей домена Windows
   - Поддержка делегирования билетов (опционально)

#### Контроль доступа:
- **Ролевая модель (RBAC):**
  - Роли: ADMIN, OPERATOR, VIEWER, GUEST
  - Гранулярные разрешения (permissions)
  - Проверка прав на уровне операций
  - Маппинг групп AD на роли системы
- **Многофакторная аутентификация (2FA):**
  - TOTP (Time-based One-Time Password)
  - SMS коды
  - Email коды
- **Сессионное управление:**
  - Короткоживущие access tokens
  - Refresh token rotation
  - Отзыв токенов
  - Блокировка после неудачных попыток
- **Аудит доступа:**
  - Логирование всех попыток входа
  - Логирование операций с пользователями
  - Логирование изменений прав доступа
  - Логирование доступа к защищенным ресурсам
  - Централизованный сбор логов (ELK, Splunk)

#### Синхронизация пользователей

- **Периодическая синхронизация:**
  - Фоновая задача по расписанию (каждые 5 минут)
  - Полная синхронизация (раз в 24 часа)
  - Синхронизация при входе пользователя
- **Обработка изменений:**
  - Создание новых пользователей
  - Обновление существующих пользователей
  - Деактивация удаленных пользователей
  - Обновление ролей на основе групп
- **Разрешение конфликтов:**
  - Локальный vs доменный пользователь
  - Приоритет доменных данных
  - Ручное разрешение конфликтов

### Соответствие стандартам:
- GDPR - защита персональных данных
- ISO 27001 - управление информационной безопасностью
- SOC 2 - безопасность, доступность, конфиденциальность

## Связанные документы

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

### Основные документы
- **[DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)** - Полный индекс документации
- **[README.md](../README.md)** - Обзор проекта
- **[CURRENT_STATUS.md](status/CURRENT_STATUS.md)** - Текущее состояние проекта

### Архитектура и структура
- [PLATFORM_STRUCTURE.md](PLATFORM_STRUCTURE.md) - Структура платформ и веток Git
- [PLATFORMS.md](PLATFORMS.md) - Разделение разработки по платформам
- [PROJECT_STRUCTURE.md](../PROJECT_STRUCTURE.md) - Структура проекта

### Статус и анализ
- [IMPLEMENTATION_STATUS.md](../archive/docs-duplicates-2026-08-08/IMPLEMENTATION_STATUS.md) - Статус реализации компонентов
- [MISSING_FUNCTIONALITY.md](../archive/docs-deprecated-2026-09-04/MISSING_FUNCTIONALITY.md) - Детальный анализ нереализованного функционала
- [DEEP_ANALYSIS_2025.md](archive/2026-04-27/analyses-old/DEEP_ANALYSIS_2025.md) - Углубленный расширенный анализ проекта (january 2026) ⚠️ Архив

### Разработка
- [DEVELOPMENT.md](DEVELOPMENT.md) - Руководство по разработке
- [DEVELOPMENT_PLAN.md](../archive/docs-deprecated-2026-09-04/DEVELOPMENT_PLAN.md) - План дальнейшей разработки
- [status/PROJECT_STATUS.md](status/PROJECT_STATUS.md) - Карта выполнения проекта
- [planning/DEVELOPMENT_ROADMAP.md](planning/DEVELOPMENT_ROADMAP.md) - Карта разработки проекта

### Безопасность
- [SECURITY_REMEDIATION_PLAN.md](../archive/docs-deprecated-2026-09-04/SECURITY_REMEDIATION_PLAN.md) - План устранения уязвимостей
- [status/ПЛАН_БЕЗОПАСНОСТЬ_2026.md](status/ПЛАН_БЕЗОПАСНОСТЬ_2026.md) - Подробный план доработки безопасности
- [archive/docs-legacy-2026-04-27/SECURITY_AUDIT_REPORT.md](archive/docs-legacy-2026-04-27/SECURITY_AUDIT_REPORT.md) - Отчет аудита безопасности (архив)
- [archive/docs-legacy-2026-04-27/USER_MANAGEMENT_SSO_KERBEROS_ANALYSIS.md](archive/docs-legacy-2026-04-27/USER_MANAGEMENT_SSO_KERBEROS_ANALYSIS.md) - Анализ SSO/Kerberos (архив)

### Тестирование
- [E2E_TESTING.md](../archive/docs/guides/E2E_TESTING.md) - Руководство по E2E тестированию ⭐ НОВОЕ
- [TESTING.md](TESTING.md) - Руководство по тестированию
- [TESTS_SUMMARY.md](../archive/docs-duplicates-2026-08-08/TESTS_SUMMARY.md) - Сводка по тестам

---

**Последнее обновление:** 27 April 2026
