# Руководство по установке зависимостей

**Дата:** Январь 2026
**Статус:** ✅ Готово к использованию

## Быстрый старт

### Windows

```powershell
# Запустите PowerShell от имени администратора
.\scripts\install-dependencies.ps1
```

### Linux/macOS

```bash
# Сделайте скрипт исполняемым
chmod +x scripts/install-dependencies.sh

# Запустите скрипт
./scripts/install-dependencies.sh
```

## Требования

### Обязательные зависимости

1. **CMake 3.15+** - система сборки
2. **FFmpeg** - обработка видео и аудио
3. **C++ компилятор** - компиляция нативного кода

### Опциональные зависимости

4. **OpenCV** - обработка изображений (опционально)

## Детальные инструкции по платформам

### Windows

#### Способ 1: Автоматическая установка (рекомендуется)

```powershell
# Запустите PowerShell от имени администратора
.\scripts\install-dependencies.ps1
```

Скрипт автоматически:
- Проверит наличие Chocolatey и установит при необходимости
- Установит CMake через Chocolatey
- Установит FFmpeg через Chocolatey
- Проверит наличие компилятора (Visual Studio или MinGW)
- Настроит переменные окружения

#### Способ 2: Ручная установка

**1. Установка Chocolatey (если не установлен):**

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

**2. Установка CMake:**

```powershell
choco install cmake -y
```

**3. Установка FFmpeg:**

```powershell
choco install ffmpeg -y
```

Или скачайте с https://ffmpeg.org/download.html и распакуйте в `C:\ffmpeg`, затем установите переменную окружения:

```powershell
$env:FFMPEG_DIR = "C:\ffmpeg"
[System.Environment]::SetEnvironmentVariable("FFMPEG_DIR", "C:\ffmpeg", "User")
```

**4. Установка компилятора:**

**Вариант A: Visual Studio 2022 (рекомендуется)**
- Скачайте с https://visualstudio.microsoft.com/
- Установите компонент "Desktop development with C++"

**Вариант B: MinGW-w64**
```powershell
choco install mingw -y
```

**5. Установка OpenCV (опционально):**

Скачайте с https://opencv.org/releases/ и распакуйте в `C:\opencv`, затем:

```powershell
$env:OpenCV_DIR = "C:\opencv\build"
[System.Environment]::SetEnvironmentVariable("OpenCV_DIR", "C:\opencv\build", "User")
```

Или через vcpkg:
```powershell
vcpkg install opencv:x64-windows
```

### Linux (Ubuntu/Debian)

```bash
# Обновление списка пакетов
sudo apt-get update

# Установка CMake
sudo apt-get install -y cmake

# Установка FFmpeg и библиотек
sudo apt-get install -y ffmpeg libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev

# Установка компилятора
sudo apt-get install -y build-essential

# Установка OpenCV (опционально)
sudo apt-get install -y libopencv-dev
```

### Linux (Fedora/RHEL)

```bash
# Установка CMake
sudo dnf install -y cmake

# Установка FFmpeg
sudo dnf install -y ffmpeg ffmpeg-devel

# Установка компилятора
sudo dnf groupinstall -y "Development Tools"

# Установка OpenCV (опционально)
sudo dnf install -y opencv-devel
```

### macOS

```bash
# Установка Homebrew (если не установлен)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка CMake
brew install cmake

# Установка FFmpeg
brew install ffmpeg

# Установка компилятора (Xcode Command Line Tools)
xcode-select --install

# Установка OpenCV (опционально)
brew install opencv
```

## Проверка установки

### Windows

```powershell
# Проверка CMake
cmake --version

# Проверка FFmpeg
ffmpeg -version

# Проверка компилятора
cl  # для Visual Studio
# или
g++ --version  # для MinGW
```

### Linux/macOS

```bash
# Проверка CMake
cmake --version

# Проверка FFmpeg
ffmpeg -version

# Проверка компилятора
g++ --version
# или
clang++ --version
```

## Переменные окружения

### Windows

После установки FFmpeg через Chocolatey, скрипт автоматически установит переменную `FFMPEG_DIR`. Если нужно установить вручную:

```powershell
# Для текущей сессии
$env:FFMPEG_DIR = "C:\ffmpeg"

# Постоянно для пользователя
[System.Environment]::SetEnvironmentVariable("FFMPEG_DIR", "C:\ffmpeg", "User")
```

Для OpenCV:

```powershell
[System.Environment]::SetEnvironmentVariable("OpenCV_DIR", "C:\opencv\build", "User")
```

### Linux/macOS

Обычно не требуется, так как библиотеки находятся в стандартных местах. Если установлены в нестандартное место:

```bash
# Для FFmpeg
export PKG_CONFIG_PATH=/path/to/ffmpeg/lib/pkgconfig:$PKG_CONFIG_PATH

# Для OpenCV
export OpenCV_DIR=/path/to/opencv
```

## Устранение проблем

### FFmpeg не найден CMake

**Windows:**
1. Убедитесь, что FFmpeg установлен: `ffmpeg -version`
2. Проверьте переменную окружения: `echo $env:FFMPEG_DIR`
3. Если не установлена, установите вручную (см. выше)

**Linux/macOS:**
1. Убедитесь, что установлены dev-пакеты:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev

   # macOS
   brew install ffmpeg
   ```

2. Проверьте через pkg-config:
   ```bash
   pkg-config --modversion libavformat
   ```

### CMake не найден

**Windows:**
- Перезапустите терминал после установки
- Проверьте PATH: `$env:Path -split ';' | Select-String cmake`

**Linux/macOS:**
- Убедитесь, что CMake установлен: `which cmake`
- Если установлен через snap/flatpak, может потребоваться добавление в PATH

### Компилятор не найден

**Windows:**
- Для Visual Studio: запустите "Developer Command Prompt for VS 2022"
- Для MinGW: убедитесь, что MinGW в PATH

**Linux/macOS:**
- Установите build-essential (Linux) или Xcode Command Line Tools (macOS)

### OpenCV не найден

OpenCV опционален. Если не установлен, библиотека соберется без него:

```bash
cmake .. -DENABLE_OPENCV=OFF
```

## Следующие шаги

После установки всех зависимостей:

1. **Перезапустите терминал** для применения переменных окружения

2. **Соберите нативную библиотеку:**
   ```bash
   # Windows
   .\scripts\build-native-lib.ps1 x64 Release

   # Linux/macOS
   ./scripts/build-all-platforms.sh Release
   ```

3. **Проверьте результат:**
   ```bash
   # Windows
   Get-ChildItem -Recurse native\video-processing\lib\*\*\video_processing.*

   # Linux/macOS
   ls -lh native/video-processing/lib/*/*/libvideo_processing.*
   ```

## Текущий статус установки

После выполнения скрипта проверки:

- ✅ CMake: установлен (версия 4.2)
- ✅ FFmpeg: установлен (версия 8.0.1)
- ✅ Компилятор: MinGW найден
- ⚠️ OpenCV: опционален (не установлен)

## Дополнительная информация

- [CMake документация](https://cmake.org/documentation/)
- [FFmpeg документация](https://ffmpeg.org/documentation.html)
- [OpenCV документация](https://docs.opencv.org/)

---

**Последнее обновление:** 26 January 2026

