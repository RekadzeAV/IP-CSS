# Инструкции по установке компонентов для сборки IP-CSS

## Быстрая установка (рекомендуется)

### Вариант 1: Через Chocolatey (требуются права администратора)

1. Запустите PowerShell от имени администратора
2. Установите Chocolatey (если не установлен):
```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

3. Установите все необходимые компоненты:
```powershell
choco install openjdk17 nodejs-lts cmake ffmpeg -y
```

### Вариант 2: Ручная установка

#### 1. Java JDK 17+
- **Скачать**: https://adoptium.net/
- **Выбрать**: OpenJDK 17 (LTS)
- **Проверка**: `java -version` и `javac -version`

#### 2. Node.js 20+
- **Скачать**: https://nodejs.org/
- **Выбрать**: LTS версию (20.x или выше)
- **Проверка**: `node --version` и `npm --version`

#### 3. CMake 3.15+
- **Скачать**: https://cmake.org/download/
- **Установить**: Windows x64 Installer
- **Проверка**: `cmake --version`

#### 4. FFmpeg
- **Через Chocolatey**: `choco install ffmpeg -y`
- **Или скачать**: https://ffmpeg.org/download.html
- **Примечание**: Для сборки нативных библиотек нужны dev библиотеки
- **Проверка**: `ffmpeg -version`

#### 5. Android SDK (только для сборки Android приложения)
- **Установить Android Studio**: https://developer.android.com/studio
- **Или установить только SDK Command-line Tools**: https://developer.android.com/studio#command-tools
- **Настроить переменную окружения**:
  ```powershell
  [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\<Username>\AppData\Local\Android\Sdk", "User")
  ```
- **Установить необходимые компоненты**:
  ```powershell
  $env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat "platforms;android-34" "build-tools;34.0.0" "platform-tools"
  ```

#### 6. C++ компилятор (только для нативных библиотек)
- **Вариант 1**: Visual Studio 2022 с компонентами C++
- **Вариант 2**: MinGW-w64 через Chocolatey: `choco install mingw -y`
- **Проверка**: `g++ --version` или наличие Visual Studio

## Проверка установки

Запустите скрипт проверки:
```powershell
.\scripts\install-build-dependencies.ps1
```

Или проверьте вручную:
```powershell
java -version
javac -version
node --version
npm --version
cmake --version
ffmpeg -version
```

## Текущий статус установки

После запуска скрипта проверки вы увидите статус всех компонентов.

## Следующие шаги

1. Перезапустите терминал после установки новых компонентов
2. Проверьте установку: `.\scripts\install-build-dependencies.ps1`
3. Начните сборку проекта согласно документации


