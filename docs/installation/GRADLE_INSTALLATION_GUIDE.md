# Инструкция по установке Gradle

**Дата:** 28 января 2026

## ✅ Статус

- ✅ Java установлен: OpenJDK 17.0.17
- ❌ Gradle Wrapper не установлен
- ❌ Gradle не установлен глобально

## 🚀 Способы установки

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

## 📝 После установки

Проверьте установку:

```powershell
.\gradlew.bat --version
```

Затем соберите API сервер:

```powershell
.\gradlew.bat :server:api:build --no-daemon
```

И создайте пакет:

```powershell
.\scripts\build-nas-package.ps1 -PackageType synology -Arch x86_64 -Version Alfa-0.0.1
```

---

## ⚠️ Примечания

- Требуется Java 17 или выше (✅ установлена)
- Gradle Wrapper автоматически скачает Gradle 8.4 при первом использовании
- Если Gradle установлен глобально, можно использовать `gradle` вместо `.\gradlew.bat`

---

**Рекомендация:** Используйте Способ 1 (установка Gradle глобально), затем создайте wrapper командой `gradle wrapper`.
