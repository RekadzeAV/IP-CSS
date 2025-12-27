# Быстрая установка компонентов для сборки IP-CSS

**Версия документации:** 1.0
**Дата последнего обновления:** 27 декабря 2025

---

## Текущий статус

На основе проверки системы, вот что нужно установить:

### ✅ Уже установлено:
- Java 19 (JDK доступен)
- Gradle Wrapper

### ❌ Требуется установить:
- Node.js 20+
- CMake 3.15+
- FFmpeg
- C++ компилятор (опционально, для нативных библиотек)
- Android SDK (опционально, для Android приложения)

## Способ 1: Автоматическая установка через Chocolatey (рекомендуется)

### Шаг 1: Установите Chocolatey (если не установлен)

Запустите PowerShell от имени администратора и выполните:

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

### Шаг 2: Установите все компоненты

```powershell
choco install openjdk17 nodejs-lts cmake ffmpeg -y
```

### Шаг 3: Перезапустите терминал

После установки закройте и снова откройте терминал для применения изменений PATH.

### Шаг 4: Проверьте установку

```powershell
.\scripts\check-dependencies.ps1
```

## Способ 2: Ручная установка

### Node.js

1. Скачайте: https://nodejs.org/
2. Установите LTS версию (20.x или выше)
3. Проверьте: `node --version` и `npm --version`

### CMake

1. Скачайте: https://cmake.org/download/
2. Выберите: Windows x64 Installer
3. Установите с опцией "Add CMake to system PATH"
4. Проверьте: `cmake --version`

### FFmpeg

1. Через Chocolatey: `choco install ffmpeg -y`
2. Или скачайте: https://ffmpeg.org/download.html
3. Добавьте в PATH
4. Проверьте: `ffmpeg -version`

**Примечание**: Для сборки нативных библиотек также нужны dev библиотеки FFmpeg (libavformat-dev и т.д.). На Windows рекомендуется использовать vcpkg или скачать dev версию отдельно.

### Android SDK (только для Android)

1. Установите Android Studio: https://developer.android.com/studio
2. Или установите только Command-line Tools: https://developer.android.com/studio#command-tools
3. Настройте переменную окружения:
   ```powershell
   [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk", "User")
   ```
4. Установите компоненты:
   ```powershell
   $env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   ```

### C++ компилятор (только для нативных библиотек)

**Вариант 1**: Visual Studio 2022
- Установите Visual Studio 2022 с компонентами "Desktop development with C++"

**Вариант 2**: MinGW-w64
```powershell
choco install mingw -y
```

## Проверка после установки

Запустите скрипт проверки:

```powershell
.\scripts\check-dependencies.ps1
```

Все компоненты должны показывать [OK].

## Готово!

После установки всех компонентов вы можете начать сборку проекта:

```powershell
# Базовая сборка
.\gradlew.bat build

# Сборка всех модулей
.\gradlew.bat buildAll
```

