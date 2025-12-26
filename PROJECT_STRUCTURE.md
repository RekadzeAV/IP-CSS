# Структура проекта IP Camera Surveillance System

## Обзор

Проект организован по модульному принципу с четким разделением ответственности между компонентами.

## Корневая структура

```
IP-CSS/
├── .github/                    # GitHub Actions workflows
│   └── workflows/
│       ├── ci.yml             # CI pipeline
│       └── cd.yml             # CD pipeline
├── .gradle/                   # Gradle cache (не коммитится)
├── android/                    # Android приложение
│   ├── app/                   # Основное приложение
│   ├── ui/                    # UI компоненты
│   └── platform/              # Android-специфичные реализации
├── core/                      # Общие кроссплатформенные модули
│   ├── license/               # Система лицензирования
│   ├── network/               # Сетевое взаимодействие
│   ├── database/              # База данных
│   └── utils/                 # Утилиты
├── desktop/                    # Desktop приложения
│   ├── common/                # Общий UI и логика
│   ├── windows/              # Windows специфичные настройки
│   ├── linux/                 # Linux специфичные настройки
│   └── macos/                 # macOS специфичные настройки
├── docs/                       # Документация
│   ├── ARCHITECTURE.md        # Архитектура системы
│   ├── DEPLOYMENT_GUIDE.md    # Руководство по развертыванию
│   ├── API.md                 # API документация
│   ├── LICENSE_SYSTEM.md      # Система лицензирования
│   ├── DEVELOPMENT.md         # Руководство по разработке
│   └── PROMPT_ANALYSIS.md     # Анализ промта
├── ios/                        # iOS приложение
│   ├── IPCameraSurveillance/  # Основное приложение
│   ├── ui/                    # SwiftUI компоненты
│   └── platform/              # iOS-специфичные реализации
├── native/                     # Нативные C++ библиотеки
│   ├── video-processing/      # Обработка видео
│   ├── analytics/             # AI аналитика
│   ├── codecs/                # Кодеки
│   └── CMakeLists.txt         # CMake конфигурация
├── scripts/                    # Скрипты сборки и развертывания
│   └── build-all-platforms.sh # Скрипт сборки всех платформ
├── server/                     # Серверная часть
│   ├── api/                   # REST API сервер
│   ├── nas/                   # Версия для NAS устройств
│   └── web/                   # Веб-интерфейс (React)
├── shared/                     # Kotlin Multiplatform модуль
│   ├── common/                # Общие модели и интерфейсы
│   ├── domain/                # Use cases и бизнес-правила
│   └── data/                  # Источники данных и репозитории
├── .gitignore                 # Git ignore правила
├── .env.example               # Пример переменных окружения
├── build.gradle.kts          # Корневой build файл
├── CHANGELOG.md               # История изменений
├── CONTRIBUTING.md            # Руководство по внесению вклада
├── docker-compose.yml         # Docker Compose конфигурация
├── gradle.properties          # Gradle свойства
├── LICENSE                    # Лицензия проекта
├── PROJECT_STRUCTURE.md       # Этот файл
├── README.md                  # Основной README
└── settings.gradle.kts        # Настройки Gradle проекта
```

## Детальная структура модулей

### shared/ - Kotlin Multiplatform

```
shared/
├── src/
│   ├── commonMain/
│   │   └── kotlin/com/company/ipcamera/shared/
│   │       ├── common/        # Общие утилиты и платформо-специфичные интерфейсы
│   │       ├── domain/        # Доменный слой
│   │       │   ├── model/     # Бизнес-модели
│   │       │   ├── repository/# Интерфейсы репозиториев
│   │       │   └── usecase/   # Use cases
│   │       └── data/          # Слой данных
│   │           ├── local/     # Локальные источники данных
│   │           ├── remote/    # Удаленные источники данных
│   │           └── repository/# Реализации репозиториев
│   ├── androidMain/           # Android-специфичные реализации
│   ├── iosMain/               # iOS-специфичные реализации
│   └── desktopMain/           # Desktop-специфичные реализации
└── build.gradle.kts
```

### android/ - Android приложение

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/company/ipcamera/android/
│   │   │   ├── MainActivity.kt
│   │   │   └── Application.kt
│   │   └── res/               # Ресурсы
│   └── build.gradle.kts
├── ui/
│   └── src/main/java/com/company/ipcamera/android/ui/
│       └── screens/           # Compose экраны
└── platform/
    └── src/main/java/com/company/ipcamera/android/platform/
        └── implementations/  # Платформо-специфичные реализации
```

### ios/ - iOS приложение

```
ios/
├── IPCameraSurveillance/
│   ├── AppDelegate.swift
│   ├── ContentView.swift
│   └── Screens/              # SwiftUI экраны
├── IPCameraSurveillance.xcodeproj
└── IPCameraSurveillance.xcworkspace
```

### server/ - Серверная часть

```
server/
├── api/
│   ├── src/main/kotlin/com/company/ipcamera/server/
│   │   ├── api/              # REST API endpoints
│   │   ├── websocket/         # WebSocket сервер
│   │   ├── service/           # Бизнес-логика сервера
│   │   └── config/            # Конфигурация
│   └── build.gradle.kts
├── web/
│   ├── src/
│   │   ├── components/       # React компоненты
│   │   ├── pages/            # Next.js страницы
│   │   ├── store/             # Redux store
│   │   └── utils/             # Утилиты
│   ├── package.json
│   └── next.config.js
└── nas/
    └── packages/              # Пакеты для различных NAS
```

### native/ - C++ библиотеки

```
native/
├── video-processing/
│   ├── src/                  # Исходные файлы C++
│   ├── include/               # Заголовочные файлы
│   └── CMakeLists.txt
├── analytics/
│   ├── src/
│   ├── include/
│   └── CMakeLists.txt
├── codecs/
│   ├── src/
│   ├── include/
│   └── CMakeLists.txt
└── CMakeLists.txt
```

## Зависимости между модулями

```
android/ios/desktop
    ↓
shared (KMM)
    ↓
core (license, network, database, utils)
    ↓
native (C++ библиотеки)
```

## Конфигурационные файлы

### Gradle
- `build.gradle.kts` - корневой build файл
- `settings.gradle.kts` - настройки проекта
- `gradle.properties` - свойства Gradle
- `gradle/wrapper/` - Gradle wrapper

### CMake
- `native/CMakeLists.txt` - корневой CMake файл
- `native/*/CMakeLists.txt` - CMake файлы для каждого модуля

### Node.js
- `server/web/package.json` - зависимости и скрипты
- `server/web/tsconfig.json` - TypeScript конфигурация
- `server/web/next.config.js` - Next.js конфигурация

### Docker
- `docker-compose.yml` - Docker Compose конфигурация
- `.env.example` - пример переменных окружения

### CI/CD
- `.github/workflows/ci.yml` - CI pipeline
- `.github/workflows/cd.yml` - CD pipeline

## Документация

Все документы находятся в `docs/`:
- `ARCHITECTURE.md` - детальное описание архитектуры
- `DEPLOYMENT_GUIDE.md` - руководство по развертыванию
- `API.md` - API документация
- `LICENSE_SYSTEM.md` - система лицензирования
- `DEVELOPMENT.md` - руководство по разработке
- `PROMPT_ANALYSIS.md` - анализ промта и недостающих компонентов

## Скрипты

Скрипты для автоматизации находятся в `scripts/`:
- `build-all-platforms.sh` - сборка всех платформ
- Другие скрипты для развертывания и обслуживания

## Следующие шаги

1. Реализовать базовые репозитории и Use Cases
2. Создать UI компоненты для всех платформ
3. Разработать серверную часть (API, WebSocket)
4. Интегрировать нативные библиотеки
5. Написать тесты и документацию

