# Быстрая справка по локальной сборке

## Команды

### Сборка

```bash
# Собрать все модули
./gradlew buildAll

# Собрать конкретный модуль
./gradlew :core:common:build
./gradlew :core:network:build
./gradlew :shared:build
./gradlew :core:license:build

# Очистить и собрать
./gradlew clean buildAll
```

### Публикация

```bash
# Публикация всех модулей (рекомендуется)
./scripts/publish-local.sh

# Публикация через Gradle
./gradlew publishToLocalMaven

# Публикация конкретного модуля
./gradlew :core:common:publishToMavenLocal
./gradlew :core:network:publishToMavenLocal
./gradlew :shared:publishToMavenLocal
./gradlew :core:license:publishToMavenLocal
```

### Тестирование

```bash
# Запустить все тесты
./gradlew testAll

# Тесты конкретного модуля
./gradlew :core:common:test
./gradlew :shared:test
```

### Диагностика

```bash
# Проверка зависимостей
./gradlew :module:dependencies

# Подробный вывод
./gradlew build --info --stacktrace

# Build scan
./gradlew build --scan
```

## Конфигурация

### Версия

Изменить в `gradle.properties`:
```properties
version=Alfa-0.0.1
group=com.company.ipcamera
```

### Использование в проекте

```kotlin
repositories {
    mavenLocal()  // Первым!
    mavenCentral()
    google()
}

dependencies {
    val ipCameraVersion = "Alfa-0.0.1"
    
    implementation("com.company.ipcamera:core-common:$ipCameraVersion")
    implementation("com.company.ipcamera:core-network:$ipCameraVersion")
    implementation("com.company.ipcamera:core-license:$ipCameraVersion")
    implementation("com.company.ipcamera:shared:$ipCameraVersion")
}
```

## Порядок сборки

```
1. core:common      (базовый)
2. core:network     (зависит от core:common)
3. shared           (зависит от core:common, core:network)
4. core:license     (зависит от shared)
```

## Расположение артефактов

```
~/.m2/repository/com/company/ipcamera/
├── core-common/
├── core-network/
├── core-license/
└── shared/
```

## Быстрое решение проблем

### Пакет не найден
```bash
# Пересобрать и опубликовать
./scripts/publish-local.sh
```

### Конфликт версий
```bash
# Очистить локальный репозиторий
rm -rf ~/.m2/repository/com/company/ipcamera/
# Пересобрать
./scripts/publish-local.sh
```

### Ошибки компиляции
```bash
# Очистить и пересобрать
./gradlew clean
./gradlew buildAll
```

## Документация

- [LOCAL_BUILD.md](LOCAL_BUILD.md) - Полное руководство
- [BUILD_ORGANIZATION.md](BUILD_ORGANIZATION.md) - Детальная организация
- [BUILD_TROUBLESHOOTING.md](BUILD_TROUBLESHOOTING.md) - Устранение проблем

