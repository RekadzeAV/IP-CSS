# Руководство по настройке Gradle

**Дата создания:** 28 января 2026  
**Дата объединения:** 29 May 2026  
**Источник:** Объединено из 3 файлов  
- `_to_be_archived/GRADLE_INSTALLATION/GRADLE_INSTALLATION_COMPLETE.md`
- `_to_be_archived/GRADLE_INSTALLATION/GRADLE_INSTALLATION_GUIDE.md`
- `_to_be_archived/GRADLE_INSTALLATION/GRADLE_INSTALLATION_SUCCESS.md`

---

## ✅ Статус установки

- ✅ Java установлена: OpenJDK 17.0.17
- ✅ Gradle 8.9 установлен
- ✅ Gradle Wrapper создан и работает

---

## 📁 Структура проекта

```
IP-CSS/
├── gradle-8.4/                    # Старая версия (можно удалить)
├── gradle-8.9/                    # Текущая версия
├── gradlew.bat                    # ✅ Gradle Wrapper для Windows
├── gradlew                        # ✅ Gradle Wrapper для Unix
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar     # ✅ Wrapper JAR
        └── gradle-wrapper.properties # ✅ Обновлен до версии 8.9
```

---

## 🚀 Использование

### Проверка установки

```powershell
.\gradlew.bat --version
# Вывод: Gradle 8.9
```

### Сборка API сервера

```powershell
.\gradlew.bat :server:api:build --no-daemon
```

### Создание пакета Synology

```powershell
.\scripts\build-nas-package.ps1 -PackageType synology -Arch x86_64 -Version Alfa-0.0.1
```

---

## 📋 Способы установки (для справки)

### Способ 1: Установка Gradle глобально (рекомендуется)

#### Вариант A: Через winget (Windows 10/11)

```powershell
winget install --id=Gradle.Gradle -e
```

#### Вариант B: Через Chocolatey

```powershell
choco install gradle -y
```

#### Вариант C: Ручная установка

1. Скачайте Gradle с официального сайта: https://gradle.org/releases/
2. Распакуйте архив
3. Добавьте `bin` директорию в PATH
4. Проверьте: `gradle --version`

После установки выполните:
```powershell
gradle wrapper --gradle-version 8.4
```

---

### Способ 2: Восстановление Gradle Wrapper из Git

Если проект находится в Git репозитории:

```powershell
git checkout gradlew gradlew.bat gradle/wrapper/
```

---

### Способ 3: Создание Gradle Wrapper вручную

1. Установите Gradle (см. Способ 1)
2. Выполните в корне проекта:

```powershell
gradle wrapper --gradle-version 8.4
```

Это создаст:
- `gradlew` (Unix скрипт)
- `gradlew.bat` (Windows скрипт)
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties` (уже существует)

---

## ⚠️ Известные проблемы

### Проблема 1: Конфигурация Kotlin Multiplatform

**Ошибка:**
```
Cannot add a KotlinSourceSet with name 'nativeMain' as a KotlinSourceSet with that name already exists.
```

**Файл:** `core/network/build.gradle.kts` (строка 318)

**Решение:** Требуется исправить конфигурацию Kotlin Multiplatform в модуле `core:network`

### Проблема 2: Плагины недоступны

**Плагины:**
- `io.gitlab.arturbosch.detekt` - временно отключен
- `org.jlleitschuh.gradle.ktlint` - временно отключен

**Решение:** Обновить версии или добавить репозитории

### Проблема 3: Удален `maven-publish` из plugins

**Решение:** `maven-publish` - это core плагин, не нужно добавлять в plugins

---

## 📝 Исправленные проблемы

1. ✅ Удален `maven-publish` из plugins (core плагин)
2. ✅ Временно отключены проблемные плагины (detekt, ktlint)
3. ✅ Обновлена версия Gradle до 8.9 (требуется для Android Gradle Plugin 8.7.0)

---

## 🚀 Следующие шаги

1. **Исправить конфигурацию Kotlin Multiplatform** в `core/network/build.gradle.kts`
2. **Повторить сборку API сервера**
3. **Создать тестовый SPK пакет**

---

## ⚠️ Примечания

- Требуется Java 17 или выше (✅ установлена)
- Gradle Wrapper автоматически скачает Gradle 8.9 при первом использовании
- Если Gradle установлен глобально, можно использовать `gradle` вместо `.\gradlew.bat`
- Gradle установлен локально в проекте (не глобально)
- Для использования в других проектах рекомендуется установить Gradle глобально

---

**Текущий статус:** ✅ Gradle установлен и готов к использованию  
**Осталось:** Исправить конфигурацию проекта для успешной сборки

---

**Примечание:** Этот документ создан путем объединения трех исходных файлов. Оригинальные файлы сохранены в `_to_be_archived/GRADLE_INSTALLATION/` для отслеживания истории.
