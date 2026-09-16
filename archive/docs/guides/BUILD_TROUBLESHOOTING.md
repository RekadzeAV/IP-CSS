# Устранение проблем при локальной сборке

## Содержание

1. [Общие проблемы](#общие-проблемы)
2. [Проблемы с зависимостями](#проблемы-с-зависимостями)
3. [Проблемы с публикацией](#проблемы-с-публикацией)
4. [Проблемы с версиями](#проблемы-с-версиями)
5. [Проблемы с платформами](#проблемы-с-платформами)
6. [Проблемы производительности](#проблемы-производительности)

## Общие проблемы

### Проблема: Gradle не найден

**Симптомы:**
```
bash: ./gradlew: No such file or directory
```

**Решение:**
1. Убедитесь, что вы находитесь в корневой директории проекта
2. Проверьте наличие файла `gradlew`:
   ```bash
   ls -la gradlew
   ```
3. Если файл отсутствует, создайте Gradle Wrapper:
   ```bash
   gradle wrapper --gradle-version 8.5
   ```

### Проблема: Недостаточно прав на выполнение

**Симптомы:**
```
Permission denied: ./gradlew
```

**Решение:**
```bash
chmod +x gradlew
chmod +x scripts/*.sh
```

### Проблема: Java не найдена

**Симптомы:**
```
Error: JAVA_HOME is not set
```

**Решение:**
1. Установите JDK 17 или выше
2. Установите переменную окружения:
   ```bash
   export JAVA_HOME=/path/to/jdk
   export PATH=$JAVA_HOME/bin:$PATH
   ```
3. Проверьте версию:
   ```bash
   java -version
   ```

### Проблема: Несовместимая версия Java

**Симптомы:**
```
Unsupported class file major version XX
```

**Решение:**
1. Проверьте версию Java:
   ```bash
   java -version
   ```
2. Установите JDK 17 или выше
3. Обновите `JAVA_HOME`:
   ```bash
   export JAVA_HOME=/path/to/jdk17
   ```

## Проблемы с зависимостями

### Проблема: Модуль не найден при сборке

**Симптомы:**
```
Could not resolve: project :core:common
```

**Решение:**
1. Проверьте, что модуль существует в `settings.gradle.kts`:
   ```kotlin
   include(":core:common")
   ```
2. Убедитесь, что модуль собран перед использованием:
   ```bash
   ./gradlew :core:common:build
   ```
3. Проверьте порядок сборки в скрипте

### Проблема: Циклические зависимости

**Симптомы:**
```
Circular dependency detected
```

**Решение:**
1. Проверьте дерево зависимостей:
   ```bash
   ./gradlew :module:dependencies
   ```
2. Убедитесь, что зависимости направлены в одну сторону:
   ```
   core:common → core:network → shared
   ```
3. Избегайте обратных зависимостей

### Проблема: Конфликт версий зависимостей

**Симптомы:**
```
Conflict: version X vs version Y
```

**Решение:**
1. Проверьте используемые версии:
   ```bash
   ./gradlew :module:dependencyInsight --dependency library-name
   ```
2. Принудительно установите версию в `build.gradle.kts`:
   ```kotlin
   configurations.all {
       resolutionStrategy {
           force("library:name:version")
       }
   }
   ```
3. Обновите версию в `libs.versions.toml`

### Проблема: Зависимость не загружается

**Симптомы:**
```
Could not resolve: library:name:version
```

**Решение:**
1. Проверьте доступность репозиториев:
   ```kotlin
   repositories {
       mavenCentral()
       google()
       // ...
   }
   ```
2. Проверьте интернет-соединение
3. Очистите кэш Gradle:
   ```bash
   rm -rf ~/.gradle/caches/
   ./gradlew --refresh-dependencies
   ```

## Проблемы с публикацией

### Проблема: Пакет не публикуется

**Симптомы:**
```
Task 'publishToMavenLocal' not found
```

**Решение:**
1. Убедитесь, что плагин `maven-publish` применен:
   ```kotlin
   plugins {
       id("maven-publish")
   }
   ```
2. Проверьте конфигурацию публикации в `build.gradle.kts`

### Проблема: Пакет не находится в других проектах

**Симптомы:**
```
Could not resolve: com.company.ipcamera:module:version
```

**Решение:**
1. Убедитесь, что `mavenLocal()` добавлен в репозитории:
   ```kotlin
   repositories {
       mavenLocal()  // Должен быть первым!
       mavenCentral()
   }
   ```
2. Проверьте версию пакета:
   ```bash
   ls ~/.m2/repository/com/company/ipcamera/module/
   ```
3. Убедитесь, что версия совпадает с `gradle.properties`

### Проблема: Неполная публикация (отсутствуют платформы)

**Симптомы:**
Пакет опубликован, но отсутствуют артефакты для некоторых платформ

**Решение:**
1. Проверьте конфигурацию платформ в `build.gradle.kts`:
   ```kotlin
   kotlin {
       android()
       jvm("desktop")
       iosX64()
       // ...
   }
   ```
2. Убедитесь, что все платформы скомпилированы:
   ```bash
   ./gradlew :module:build
   ```
3. Пересоберите и опубликуйте:
   ```bash
   ./gradlew :module:clean publishToMavenLocal
   ```

### Проблема: Поврежденные артефакты

**Симптомы:**
```
Corrupted JAR file
```

**Решение:**
1. Удалите поврежденные артефакты:
   ```bash
   rm -rf ~/.m2/repository/com/company/ipcamera/module/version/
   ```
2. Пересоберите и опубликуйте:
   ```bash
   ./gradlew :module:clean publishToMavenLocal
   ```

## Проблемы с версиями

### Проблема: Несовместимые версии модулей

**Симптомы:**
```
Module A requires version X, but found version Y
```

**Решение:**
1. Убедитесь, что все модули используют одну версию из `gradle.properties`
2. Очистите локальный репозиторий:
   ```bash
   rm -rf ~/.m2/repository/com/company/ipcamera/
   ```
3. Пересоберите все модули:
   ```bash
   ./scripts/publish-local.sh
   ```

### Проблема: Старые версии в кэше

**Симптомы:**
Используется старая версия пакета, хотя версия обновлена

**Решение:**
1. Очистите локальный репозиторий:
   ```bash
   rm -rf ~/.m2/repository/com/company/ipcamera/
   ```
2. Очистите кэш Gradle:
   ```bash
   ./gradlew clean
   rm -rf ~/.gradle/caches/
   ```
3. Пересоберите:
   ```bash
   ./scripts/publish-local.sh
   ```

## Проблемы с платформами

### Проблема: Ошибка компиляции для Android

**Симптомы:**
```
Android compilation error
```

**Решение:**
1. Проверьте версию Android SDK:
   ```bash
   ./gradlew :module:androidDependencies
   ```
2. Убедитесь, что `compileSdk` установлен правильно:
   ```kotlin
   android {
       compileSdk = 34
   }
   ```
3. Обновите Android Gradle Plugin при необходимости

### Проблема: Ошибка компиляции для iOS

**Симптомы:**
```
iOS compilation error
```

**Решение:**
1. Убедитесь, что вы на macOS (iOS компиляция требует Xcode)
2. Проверьте наличие Xcode:
   ```bash
   xcodebuild -version
   ```
3. Для Linux/Windows: iOS компиляция недоступна, используйте только JVM/Android

### Проблема: Ошибка компиляции для JVM

**Симптомы:**
```
JVM target mismatch
```

**Решение:**
1. Проверьте версию JVM target:
   ```kotlin
   jvm("desktop") {
       compilations.all {
           kotlinOptions {
               jvmTarget = "11"
           }
       }
   }
   ```
2. Убедитесь, что используется совместимая версия Java

## Проблемы производительности

### Проблема: Медленная сборка

**Симптомы:**
Сборка занимает слишком много времени

**Решение:**
1. Включите параллельную сборку:
   ```properties
   org.gradle.parallel=true
   org.gradle.workers.max=4
   ```
2. Включите кэширование:
   ```properties
   org.gradle.caching=true
   ```
3. Увеличьте память:
   ```properties
   org.gradle.jvmargs=-Xmx4096m
   ```
4. Используйте Gradle Daemon:
   ```properties
   org.gradle.daemon=true
   ```

### Проблема: Нехватка памяти

**Симптомы:**
```
OutOfMemoryError
```

**Решение:**
1. Увеличьте память в `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
   ```
2. Закройте другие приложения
3. Используйте инкрементальную сборку:
   ```properties
   kotlin.incremental=true
   ```

### Проблема: Медленная загрузка зависимостей

**Симптомы:**
Долгая загрузка зависимостей из репозиториев

**Решение:**
1. Используйте локальный прокси-репозиторий (Nexus, Artifactory)
2. Кэшируйте зависимости:
   ```bash
   ./gradlew --refresh-dependencies
   ```
3. Проверьте скорость интернет-соединения

## Диагностика

### Сбор диагностической информации

```bash
# Подробный вывод сборки
./gradlew build --info --stacktrace

# Отчет о зависимостях
./gradlew :module:dependencies > dependencies.txt

# Отчет о задачах
./gradlew :module:tasks --all

# Build scan
./gradlew build --scan
```

### Проверка окружения

```bash
# Версия Java
java -version

# Версия Gradle
./gradlew --version

# Переменные окружения
env | grep -E "(JAVA|GRADLE|ANDROID)"

# Размер локального репозитория
du -sh ~/.m2/repository/com/company/ipcamera/
```

### Логи и отчеты

Gradle создает логи в:
- `~/.gradle/daemon/` - логи daemon
- `build/reports/` - отчеты о тестах
- `build/logs/` - логи сборки

## Получение помощи

Если проблема не решена:

1. Соберите диагностическую информацию:
   ```bash
   ./gradlew build --scan
   ```
2. Проверьте документацию:
   - [LOCAL_BUILD.md](LOCAL_BUILD.md)
   - [BUILD_ORGANIZATION.md](BUILD_ORGANIZATION.md)
3. Создайте issue с:
   - Описанием проблемы
   - Шагами для воспроизведения
   - Логами сборки
   - Информацией об окружении

