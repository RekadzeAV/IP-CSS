# Организация локальной сборки пакетов

## Содержание

1. [Архитектура сборки](#архитектура-сборки)
2. [Процесс сборки](#процесс-сборки)
3. [Конфигурация модулей](#конфигурация-модулей)
4. [Управление зависимостями](#управление-зависимостями)
5. [Версионирование](#версионирование)
6. [Стратегии сборки](#стратегии-сборки)
7. [Мониторинг и отладка](#мониторинг-и-отладка)

## Архитектура сборки

### Структура модулей

Проект организован как мультимодульный Gradle проект с четкой иерархией зависимостей:

```
┌─────────────────────────────────────────────────┐
│         Платформо-специфичные приложения         │
│  (android:app, server:api, platforms/*)         │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│              shared (KMM)                       │
│  • Общая бизнес-логика                          │
│  • Репозитории и Use Cases                       │
│  • Модели данных                                 │
└────────────┬───────────────────────┬─────────────┘
             │                       │
             ▼                       ▼
    ┌──────────────┐        ┌──────────────┐
    │ core:network │        │ core:common  │
    │              │        │              │
    │ • ONVIF      │───────▶│ • Типы       │
    │ • RTSP       │        │ • Утилиты    │
    │ • WebSocket  │        │ • Константы  │
    │ • REST API   │        └──────────────┘
    └──────────────┘
             │
             ▼
    ┌──────────────┐
    │core:license  │
    │              │
    │ • Лицензии   │
    │ • Активация  │
    └──────────────┘
```

### Типы модулей

#### 1. Kotlin Multiplatform модули

Модули, поддерживающие несколько платформ:
- `shared` - основной KMM модуль
- `core:common` - общие типы и утилиты
- `core:network` - сетевое взаимодействие
- `core:license` - система лицензирования

**Особенности:**
- Компилируются для Android, iOS, JVM (Desktop)
- Используют `expect/actual` для платформо-специфичного кода
- Публикуются с метаданными для всех платформ

#### 2. Android модули

- `android:app` - Android приложение
- Использует Android Gradle Plugin
- Зависит от KMM модулей

#### 3. JVM модули

- `server:api` - REST API сервер
- Использует Kotlin JVM
- Зависит от KMM модулей

#### 4. Нативные модули (C++)

- `native/video-processing` - обработка видео
- `native/analytics` - AI аналитика
- `native/codecs` - поддержка кодеков
- Собираются через CMake, не через Gradle

## Процесс сборки

### Этапы сборки

```
┌─────────────────┐
│  Подготовка     │
│  • Проверка     │
│  • Очистка      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Компиляция     │
│  • core:common  │
│  • core:network │
│  • shared       │
│  • core:license │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Тестирование  │
│  • Unit tests   │
│  • Integration  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Публикация     │
│  • Maven Local  │
│  • Артефакты    │
└─────────────────┘
```

### Порядок сборки модулей

Модули должны собираться в следующем порядке из-за зависимостей:

1. **core:common** (базовый модуль, без зависимостей)
2. **core:network** (зависит от core:common)
3. **shared** (зависит от core:common и core:network)
4. **core:license** (зависит от shared)

### Автоматизация через скрипты

#### Основной скрипт публикации

```bash
./scripts/publish-local.sh
```

**Что делает:**
- Проверяет окружение (Java, Gradle)
- Очищает предыдущие сборки
- Собирает модули в правильном порядке
- Публикует в локальный Maven репозиторий
- Проверяет успешность публикации
- Выводит инструкции по использованию

#### Ручная сборка через Gradle

```bash
# Сборка всех модулей
./gradlew buildAll

# Публикация всех модулей
./gradlew publishToLocalMaven

# Сборка конкретного модуля
./gradlew :core:common:build
./gradlew :core:network:build
./gradlew :shared:build
./gradlew :core:license:build
```

## Конфигурация модулей

### Базовая структура build.gradle.kts

Каждый публикуемый модуль имеет следующую структуру:

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("maven-publish")
}

kotlin {
    // Платформы
    android()
    jvm("desktop")
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Зависимости
            }
        }
        // Платформо-специфичные source sets
    }
}

android {
    namespace = "com.company.ipcamera.module.name"
    compileSdk = 34
    // ...
}

// Публикация
afterEvaluate {
    publishing {
        publications {
            all {
                if (this is MavenPublication) {
                    groupId = "com.company.ipcamera"
                    version = project.version.toString()
                    // POM метаданные
                }
            }
        }
    }
}
```

### Ключевые параметры

#### Group ID и Artifact ID

- **Group ID**: `com.company.ipcamera` (общий для всех модулей)
- **Artifact ID**: уникальный для каждого модуля
  - `core-common`
  - `core-network`
  - `core-license`
  - `shared`

#### Версия

Версия определяется в `gradle.properties`:
```properties
version=Alfa-0.0.1
group=com.company.ipcamera
```

Все модули используют одну версию для обеспечения совместимости.

### Платформо-специфичные настройки

#### Android

```kotlin
android {
    namespace = "com.company.ipcamera.module.name"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
        targetSdk = 34
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

#### JVM/Desktop

```kotlin
jvm("desktop") {
    compilations.all {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}
```

#### iOS

```kotlin
iosX64()
iosArm64()
iosSimulatorArm64()
```

## Управление зависимостями

### Внутренние зависимости

Модули проекта используют зависимости через `project()`:

```kotlin
dependencies {
    // Зависимость от другого модуля проекта
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":shared"))
}
```

### Внешние зависимости

Внешние зависимости управляются через `gradle/libs.versions.toml`:

```kotlin
dependencies {
    // Использование версионного каталога
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
```

### Разрешение зависимостей

Gradle автоматически разрешает зависимости в следующем порядке:

1. **Локальный Maven репозиторий** (`mavenLocal()`)
2. **Удаленные репозитории** (Maven Central, Google, JetBrains)

**Важно:** `mavenLocal()` должен быть первым в списке репозиториев для приоритета локальных пакетов.

## Версионирование

### Схема версионирования

Проект использует семантическое версионирование с префиксом:

```
[Префикс]-[Major].[Minor].[Patch]
```

Примеры:
- `Alfa-0.0.1` - альфа версия 0.0.1
- `Beta-1.0.0` - бета версия 1.0.0
- `1.0.0` - стабильная версия 1.0.0

### Изменение версии

1. Отредактируйте `gradle.properties`:
   ```properties
   version=Alfa-0.0.2
   ```

2. Пересоберите и опубликуйте:
   ```bash
   ./scripts/publish-local.sh
   ```

### Совместимость версий

Все модули проекта должны использовать одну версию для обеспечения совместимости. Это гарантируется использованием `project.version` в конфигурации публикации.

## Стратегии сборки

### Инкрементальная сборка

Gradle использует инкрементальную сборку для ускорения процесса:

```properties
# gradle.properties
org.gradle.caching=true
org.gradle.parallel=true
kotlin.incremental=true
```

### Кэширование

Gradle кэширует:
- Скомпилированные классы
- Результаты тестов
- Загруженные зависимости

Очистка кэша:
```bash
./gradlew clean
rm -rf ~/.gradle/caches/
```

### Параллельная сборка

Для ускорения сборки используется параллельная компиляция:

```properties
org.gradle.parallel=true
org.gradle.workers.max=4
```

### Оптимизация памяти

```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

## Мониторинг и отладка

### Логирование сборки

#### Подробный вывод

```bash
./gradlew build --info
./gradlew build --debug
```

#### Только ошибки

```bash
./gradlew build --warn
```

### Проверка зависимостей

#### Дерево зависимостей

```bash
./gradlew :shared:dependencies
./gradlew :core:network:dependencies
```

#### Отчет о зависимостях

```bash
./gradlew :shared:dependencyInsight --dependency kotlinx-coroutines-core
```

### Проверка публикации

#### Список публикаций

```bash
./gradlew :core:common:publishing
```

#### Проверка локального репозитория

```bash
ls -la ~/.m2/repository/com/company/ipcamera/
tree ~/.m2/repository/com/company/ipcamera/
```

### Отладка проблем

#### Проблема: Модуль не найден

1. Проверьте версию в `gradle.properties`
2. Проверьте наличие в локальном репозитории:
   ```bash
   ls ~/.m2/repository/com/company/ipcamera/core-common/
   ```
3. Пересоберите модуль:
   ```bash
   ./gradlew :core:common:publishToMavenLocal
   ```

#### Проблема: Конфликт версий

1. Очистите локальный репозиторий:
   ```bash
   rm -rf ~/.m2/repository/com/company/ipcamera/
   ```
2. Пересоберите все модули:
   ```bash
   ./scripts/publish-local.sh
   ```

#### Проблема: Ошибки компиляции

1. Очистите сборку:
   ```bash
   ./gradlew clean
   ```
2. Проверьте зависимости:
   ```bash
   ./gradlew :module:dependencies
   ```
3. Пересоберите:
   ```bash
   ./gradlew :module:build --stacktrace
   ```

### Инструменты для анализа

#### Gradle Build Scan

```bash
./gradlew build --scan
```

Создает детальный отчет о сборке, доступный онлайн.

#### Профилирование

```bash
./gradlew build --profile
```

Создает HTML отчет о времени выполнения задач.

## Интеграция с CI/CD

### GitHub Actions

Пример workflow для публикации:

```yaml
name: Publish to Local Maven

on:
  push:
    branches: [ main, develop ]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Publish to Maven Local
        run: ./gradlew publishToLocalMaven
      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: maven-local
          path: ~/.m2/repository/com/company/ipcamera/
```

### Локальная CI проверка

Перед коммитом проверьте сборку:

```bash
./gradlew clean build testAll
./scripts/publish-local.sh
```

## Дополнительные ресурсы

- [LOCAL_BUILD.md](LOCAL_BUILD.md) - Руководство по использованию локальных пакетов
- [DEVELOPMENT.md](DEVELOPMENT.md) - Руководство по разработке
- [PROJECT_STRUCTURE.md](../PROJECT_STRUCTURE.md) - Структура проекта
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Kotlin Multiplatform Publishing](https://kotlinlang.org/docs/multiplatform-publish-lib.html)

