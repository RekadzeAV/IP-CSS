# Локальная сборка и публикация пакетов

Это руководство описывает процесс сборки и публикации модулей проекта в локальный Maven репозиторий для использования в других проектах.

## Обзор

Проект IP-CSS состоит из нескольких модулей, которые могут быть опубликованы как отдельные библиотеки:

- `core:common` - Базовые типы и утилиты
- `core:network` - Сетевые клиенты (ONVIF, RTSP, WebSocket)
- `core:license` - Система лицензирования
- `shared` - Общий Kotlin Multiplatform модуль

## Требования

- JDK 17 или выше
- Gradle (используется Gradle Wrapper)
- Maven (для локального репозитория, обычно устанавливается вместе с Java)

## Быстрый старт

### 1. Сборка и публикация всех модулей

Используйте готовый скрипт для сборки и публикации всех модулей:

```bash
./scripts/publish-local.sh
```

Скрипт выполняет следующие действия:
1. Проверяет зависимости (Java, Gradle)
2. Очищает предыдущие сборки
3. Собирает все модули в правильном порядке зависимостей
4. Публикует модули в локальный Maven репозиторий (`~/.m2/repository`)
5. Проверяет успешность публикации

### 2. Ручная публикация через Gradle

Вы также можете использовать Gradle напрямую:

```bash
# Публикация всех модулей
./gradlew publishToLocalMaven

# Публикация конкретного модуля
./gradlew :core:common:publishToMavenLocal
./gradlew :core:network:publishToMavenLocal
./gradlew :core:license:publishToMavenLocal
./gradlew :shared:publishToMavenLocal
```

## Использование опубликованных пакетов

### В Kotlin Multiplatform проекте

Добавьте в `build.gradle.kts` вашего проекта:

```kotlin
repositories {
    mavenLocal()  // Важно: должен быть первым для приоритета локальных пакетов
    mavenCentral()
    google()
    // ... другие репозитории
}

dependencies {
    val ipCameraVersion = "Alfa-0.0.1"  // Версия из gradle.properties
    
    // Core modules
    implementation("com.company.ipcamera:core-common:$ipCameraVersion")
    implementation("com.company.ipcamera:core-network:$ipCameraVersion")
    implementation("com.company.ipcamera:core-license:$ipCameraVersion")
    
    // Shared module
    implementation("com.company.ipcamera:shared:$ipCameraVersion")
}
```

### В Android проекте

Для Android проектов используйте те же зависимости, но убедитесь, что указаны правильные варианты:

```kotlin
dependencies {
    val ipCameraVersion = "Alfa-0.0.1"
    
    // Android варианты будут выбраны автоматически
    implementation("com.company.ipcamera:core-common:$ipCameraVersion")
    implementation("com.company.ipcamera:core-network:$ipCameraVersion")
    implementation("com.company.ipcamera:core-license:$ipCameraVersion")
    implementation("com.company.ipcamera:shared:$ipCameraVersion")
}
```

### В JVM/Desktop проекте

```kotlin
dependencies {
    val ipCameraVersion = "Alfa-0.0.1"
    
    // JVM варианты будут выбраны автоматически
    implementation("com.company.ipcamera:core-common:$ipCameraVersion")
    implementation("com.company.ipcamera:core-network:$ipCameraVersion")
    implementation("com.company.ipcamera:core-license:$ipCameraVersion")
    implementation("com.company.ipcamera:shared:$ipCameraVersion")
}
```

## Структура опубликованных артефактов

После публикации модули будут доступны в локальном Maven репозитории:

```
~/.m2/repository/com/company/ipcamera/
├── core-common/
│   └── Alfa-0.0.1/
│       ├── core-common-Alfa-0.0.1.pom
│       ├── core-common-Alfa-0.0.1-metadata.jar
│       ├── core-common-Alfa-0.0.1-android.jar
│       ├── core-common-Alfa-0.0.1-desktop.jar
│       └── ...
├── core-network/
│   └── Alfa-0.0.1/
│       └── ...
├── core-license/
│   └── Alfa-0.0.1/
│       └── ...
└── shared/
    └── Alfa-0.0.1/
        └── ...
```

## Версионирование

Версия пакетов определяется в файле `gradle.properties`:

```properties
version=Alfa-0.0.1
group=com.company.ipcamera
```

Для изменения версии отредактируйте этот файл перед публикацией.

## Порядок зависимостей

Модули имеют следующие зависимости:

```
shared
  ├── core:common
  └── core:network
      └── core:common

core:license
  └── shared
      ├── core:common
      └── core:network
          └── core:common
```

Скрипт `publish-local.sh` автоматически собирает модули в правильном порядке.

## Устранение проблем

### Пакеты не находятся в других проектах

1. Убедитесь, что `mavenLocal()` добавлен в список репозиториев
2. Проверьте, что `mavenLocal()` находится первым в списке (для приоритета)
3. Проверьте версию пакета в `gradle.properties`
4. Убедитесь, что публикация прошла успешно: `ls ~/.m2/repository/com/company/ipcamera/`

### Ошибки при сборке

1. Убедитесь, что все зависимости собраны в правильном порядке
2. Очистите кэш Gradle: `./gradlew clean`
3. Очистите локальный репозиторий и пересоберите: `rm -rf ~/.m2/repository/com/company/ipcamera/`

### Конфликты версий

Если у вас есть несколько версий пакетов в локальном репозитории, удалите старые версии:

```bash
rm -rf ~/.m2/repository/com/company/ipcamera/*/Alfa-0.0.0
```

## Публикация в удаленный репозиторий

Для публикации в удаленный Maven репозиторий (например, Maven Central, GitHub Packages) необходимо:

1. Настроить учетные данные в `~/.gradle/gradle.properties` или через переменные окружения
2. Добавить конфигурацию репозитория в `build.gradle.kts` каждого модуля
3. Использовать задачу `publish` вместо `publishToMavenLocal`

Пример конфигурации для GitHub Packages:

```kotlin
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/username/repo")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

## Связанная документация

- [BUILD_QUICK_REFERENCE.md](BUILD_QUICK_REFERENCE.md) - Быстрая справка по командам и конфигурации
- [BUILD_ORGANIZATION.md](BUILD_ORGANIZATION.md) - Детальная организация процесса сборки
- [BUILD_TROUBLESHOOTING.md](BUILD_TROUBLESHOOTING.md) - Устранение проблем при сборке
- [DEVELOPMENT.md](DEVELOPMENT.md) - Руководство по разработке
- [PROJECT_STRUCTURE.md](../PROJECT_STRUCTURE.md) - Структура проекта

## Дополнительные ресурсы

- [Gradle Publishing Guide](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Kotlin Multiplatform Publishing](https://kotlinlang.org/docs/multiplatform-publish-lib.html)
- [Maven Local Repository](https://maven.apache.org/guides/introduction/introduction-to-repositories.html#local-repository)

