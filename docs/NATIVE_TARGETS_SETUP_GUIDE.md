# Руководство по настройке нативных таргетов (Kotlin/Native)

Это руководство описывает процесс настройки и сборки нативных таргетов для Kotlin Multiplatform проекта IP-CSS.

## 📋 Содержание

1. [Поддерживаемые платформы](#поддерживаемые-платформы)
2. [Предварительные требования](#предварительные-требования)
3. [Сборка Live555 библиотеки](#сборка-live555-библиотеки)
4. [Сборка FFmpeg библиотеки](#сборка-ffmpeg-библиотеки)
5. [Настройка cinterop в Gradle](#настройка-cinterop-в-gradle)
6. [Компиляция Kotlin/Native](#компиляция-kotlinnative)
7. [Тroubleshooting](#troubleshooting)

---

## Поддерживаемые платформы

### Текущая поддержка

| Платформа | Target | Статус | Примечания |
|-----------|--------|--------|------------|
| Windows x64 | `mingwX64` | 🟢 Готово | Live555 cinterop настроен |
| Linux x64 | `linuxX64` | 🟡 Частично | Требует Live555 сборку |
| macOS x64 | `macosX64` | 🟡 Частично | Требует Live555 сборку |
| macOS ARM64 | `macosArm64` | 🟡 Частично | Требует Live555 сборку |
| iOS x64 | `iosX64` | 🔴 Отключено | Симулятор x86_64 |
| iOS ARM64 | `iosArm64` | 🔴 Отключено | Реальные устройства |
| iOS Simulator ARM64 | `iosSimulatorArm64` | 🔴 Отключено | Симулятор ARM64 |
| Android Native ARM64 | `androidNativeArm64` | 🔴 Отключено | Требует NDK |

### Временное отключение

Нативные таргеты iOS и Android Native временно отключены через флаг в `gradle.properties`:

```properties
ipcss.disableNativeTargets=true
```

Для активации установите `false` и выполните настройку библиотек для каждой платформы.

---

## Предварительные требования

### Общие требования

- **Kotlin** 2.0.0+
- **Gradle** 8.9+
- **CMake** 3.22+
- **Git** 2.30+

### Windows

```powershell
# Установка Chocolatey (если нет)
Set-ExecutionPolicy Bypass -Scope Process -Force; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))

# Установка зависимостей
choco install cmake git python ninja vcpython27

# Установка Visual Studio Build Tools (с C++ компонентами)
# https://visualstudio.microsoft.com/visual-cpp-build-tools/
```

### macOS

```bash
# Установка Xcode Command Line Tools
xcode-select --install

# Установка Homebrew (если нет)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка зависимостей
brew install cmake git python3 ninja
```

### Linux (Ubuntu/Debian)

```bash
# Установка зависимостей
sudo apt update
sudo apt install -y cmake git python3 python3-pip ninja-build build-essential

# Для Linux x64
sudo apt install -y libssl-dev pkg-config
```

---

## Сборка Live555 библиотеки

### 1. Клонирование репозитория

```bash
cd native
git clone https://github.com/live555/live555.git
cd live555
```

### 2. Сборка для Windows (mingwX64)

```powershell
# Переход в директорию live555
cd native\live555

# Генерация проекта с CMake
mkdir build
cd build
cmake .. -G Ninja -DCMAKE_BUILD_TYPE=Release -DCMAKE_SYSTEM_NAME=Windows

# Сборка
cmake --build . --config Release

# Библиотеки будут в build/lib/
```

### 3. Сборка для macOS (macosX64, macosArm64)

```bash
# Для x64
cd native/live555
mkdir build-macos-x64
cd build-macos-x64
cmake .. -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_OSX_ARCHITECTURES="x86_64" \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=10.15

cmake --build .

# Для ARM64
cd ../..
mkdir build-macos-arm64
cd build-macos-arm64
cmake .. -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_OSX_ARCHITECTURES="arm64" \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=11.0

cmake --build .
```

### 4. Сборка для Linux (linuxX64)

```bash
cd native/live555
mkdir build-linux
cd build-linux
cmake .. -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_SYSTEM_NAME=Linux \
  -DCMAKE_SYSTEM_PROCESSOR=x86_64

cmake --build .
```

### 5. Структура выходных файлов

После сборки библиотеки должны быть размещены в:

```
native/live555/lib/
├── windows/
│   └── x64/
│       ├── liveMedia.lib
│       ├── groupsock.lib
│       ├── UsageEnvironment.lib
│       └── BasicUsageEnvironment.lib
├── macos/
│   ├── x64/
│   │   └── libliveMedia.a (и другие)
│   └── arm64/
│       └── libliveMedia.a (и другие)
└── linux/
    └── x64/
        └── libliveMedia.a (и другие)
```

---

## Сборка FFmpeg библиотеки

### 1. Клонирование репозитория

```bash
cd native
git clone https://git.ffmpeg.org/ffmpeg.git ffmpeg
cd ffmpeg
```

### 2. Сборка для Windows (mingwX64)

```powershell
cd native/ffmpeg

# Конфигурация для Windows x64
./configure --target-os=mingw32 \
  --arch=x86_64 \
  --enable-static \
  --disable-shared \
  --enable-cross-compile \
  --cross-prefix=x86_64-w64-mingw32- \
  --prefix=$(pwd)/../ffmpeg-build/windows/x64

# Сборка
make -j$(nproc)
make install
```

### 3. Сборка для macOS

```bash
# Для x64
cd native/ffmpeg
./configure --target-os=darwin \
  --arch=x86_64 \
  --enable-static \
  --disable-shared \
  --prefix=$(pwd)/../ffmpeg-build/macos/x64

make -j$(sysctl -n hw.ncpu)
make install

# Для ARM64
./configure --target-os=darwin \
  --arch=arm64 \
  --enable-static \
  --disable-shared \
  --prefix=$(pwd)/../ffmpeg-build/macos/arm64

make -j$(sysctl -n hw.ncpu)
make install
```

### 4. Сборка для Linux

```bash
cd native/ffmpeg
./configure --target-os=linux \
  --arch=x86_64 \
  --enable-static \
  --disable-shared \
  --prefix=$(pwd)/../ffmpeg-build/linux/x64

make -j$(nproc)
make install
```

### 5. Структура выходных файлов

```
native/ffmpeg-build/
├── windows/
│   └── x64/
│       ├── lib/
│       │   ├── libavcodec.a
│       │   ├── libavformat.a
│       │   └── libavutil.a
│       └── include/
├── macos/
│   ├── x64/
│   │   └── lib/
│   └── arm64/
│       └── lib/
└── linux/
    └── x64/
        └── lib/
```

---

## Настройка cinterop в Gradle

### Файл: `core/network/build.gradle.kts`

```kotlin
kotlin {
    // Windows (MinGW)
    mingwX64("nativeWindows") {
        compilations.getByName("main") {
            cinterops {
                create("live555") {
                    defFile(project.file("src/nativeWindowsInterop/live555.def"))
                    includeDirs {
                        allHeaders("native/live555/include")
                        headerLibrary("native/live555/lib/windows/x64")
                    }
                }
                create("ffmpeg") {
                    defFile(project.file("src/nativeWindowsInterop/ffmpeg.def"))
                    includeDirs {
                        allHeaders("native/ffmpeg-build/windows/x64/include")
                        headerLibrary("native/ffmpeg-build/windows/x64/lib")
                    }
                }
            }
        }
    }

    // Linux
    linuxX64("nativeLinux") {
        compilations.getByName("main") {
            cinterops {
                create("live555") {
                    defFile(project.file("src/nativeLinuxInterop/live555.def"))
                    includeDirs {
                        allHeaders("native/live555/include")
                        headerLibrary("native/live555/lib/linux/x64")
                    }
                }
                create("ffmpeg") {
                    defFile(project.file("src/nativeLinuxInterop/ffmpeg.def"))
                    includeDirs {
                        allHeaders("native/ffmpeg-build/linux/x64/include")
                        headerLibrary("native/ffmpeg-build/linux/x64/lib")
                    }
                }
            }
        }
    }

    // macOS x64
    macosX64("nativeMacosX64") {
        compilations.getByName("main") {
            cinterops {
                create("live555") {
                    defFile(project.file("src/nativeMacosX64Interop/live555.def"))
                    includeDirs {
                        allHeaders("native/live555/include")
                        headerLibrary("native/live555/lib/macos/x64")
                    }
                }
                create("ffmpeg") {
                    defFile(project.file("src/nativeMacosX64Interop/ffmpeg.def"))
                    includeDirs {
                        allHeaders("native/ffmpeg-build/macos/x64/include")
                        headerLibrary("native/ffmpeg-build/macos/x64/lib")
                    }
                }
            }
        }
    }

    // macOS ARM64
    macosArm64("nativeMacosArm64") {
        compilations.getByName("main") {
            cinterops {
                create("live555") {
                    defFile(project.file("src/nativeMacosArm64Interop/live555.def"))
                    includeDirs {
                        allHeaders("native/live555/include")
                        headerLibrary("native/live555/lib/macos/arm64")
                    }
                }
                create("ffmpeg") {
                    defFile(project.file("src/nativeMacosArm64Interop/ffmpeg.def"))
                    includeDirs {
                        allHeaders("native/ffmpeg-build/macos/arm64/include")
                        headerLibrary("native/ffmpeg-build/macos/arm64/lib")
                    }
                }
            }
        }
    }
}
```

### Файл def (пример: `live555.def`)

```
module = "live555"
headers = "include/live555.h"
compilerOpts = "-I${projectDir}/native/live555/include"
linkerOpts = "-L${projectDir}/native/live555/lib -lliveMedia -lgroupsock -lUsageEnvironment -lBasicUsageEnvironment"
```

---

## Компиляция Kotlin/Native

### Генерация cinterop биндингов

```bash
# Windows
.\gradlew :core:network:generateCInteropLive555NativeWindows

# Linux
./gradlew :core:network:generateCInteropLive555NativeLinux

# macOS
./gradlew :core:network:generateCInteropLive555NativeMacosX64
./gradlew :core:network:generateCInteropLive555NativeMacosArm64
```

### Компиляция для конкретной платформы

```bash
# Windows
.\gradlew :core:network:compileKotlinNativeWindows

# Linux
./gradlew :core:network:compileKotlinLinuxX64

# macOS
./gradlew :core:network:compileKotlinMacosX64
./gradlew :core:network:compileKotlinMacosArm64

# iOS (только на macOS)
./gradlew :core:network:compileKotlinIosX64
./gradlew :core:network:compileKotlinIosArm64
./gradlew :core:network:compileKotlinIosSimulatorArm64
```

### Сборка всех нативных таргетов

```bash
# Для текущей платформы
.\gradlew :core:network:build

# Для всех платформ (требуется кросс-компиляция)
.\gradlew :core:network:compileKotlinLinuxX64 ^
           :core:network:compileKotlinMacosX64 ^
           :core:network:compileKotlinMacosArm64 ^
           :core:network:compileKotlinNativeWindows
```

---

## Troubleshooting

### Ошибка: "cinterop: failed to run clang"

**Причина:** Отсутствует компилятор C/C++ для целевой платформы.

**Решение:**
```bash
# Windows
# Установите Visual Studio Build Tools с C++ компонентами

# macOS
xcode-select --install

# Linux
sudo apt install build-essential clang
```

### Ошибка: "Header file not found"

**Причина:** Неправильный путь к заголовкам в cinterop конфигурации.

**Решение:**
```kotlin
// Проверьте, что файлы существуют
includeDirs {
    allHeaders("native/live555/include") // Должен содержать live555.h
}
```

### Ошибка: "Library not found"

**Причина:** Библиотеки не скомпилированы или неправильный путь.

**Решение:**
```bash
# Проверьте наличие библиотек
ls native/live555/lib/windows/x64/
# Должны быть: liveMedia.lib, groupsock.lib, и т.д.
```

### Ошибка: "Target platform mismatch"

**Причина:** Библиотеки собраны для неправильной архитектуры.

**Решение:**
```bash
# Проверьте архитектуру библиотек
# Windows
dumpbin /headers native/live555/lib/windows/x64/liveMedia.lib

# macOS
file native/live555/lib/macos/x64/libliveMedia.a

# Linux
file native/live555/lib/linux/x64/libliveMedia.a
```

### Ошибка: "CMake configuration failed"

**Причина:** Отсутствуют зависимости или неправильные флаги конфигурации.

**Решение:**
```bash
# Проверьте логи CMake
cmake .. -DCMAKE_VERBOSE_MAKEFILE=ON

# Убедитесь, что все зависимости установлены
# Для Windows: Visual Studio C++ Tools
# Для macOS: Xcode Command Line Tools
# Для Linux: build-essential, libssl-dev
```

---

## Проверка успешной настройки

### 1. Проверка cinterop

```bash
.\gradlew :core:network:generateCInteropLive555NativeWindows --info
```

Ожидаемый вывод:
```
> Task :core:network:generateCInteropLive555NativeWindows
cinterop:live555 - generating...
cinterop:live555 - generated successfully
```

### 2. Проверка компиляции

```bash
.\gradlew :core:network:compileKotlinNativeWindows
```

Ожидаемый вывод:
```
> Task :core:network:compileKotlinNativeWindows
BUILD SUCCESSFUL
```

### 3. Проверка тестов

```bash
.\gradlew :core:network:mingwX64Test
```

---

## Автоматизация сборки

### Скрипт для Windows (`scripts/build-native-deps.ps1`)

```powershell
param(
    [string]$Platform = "all",
    [string]$Config = "Release"
)

function Build-Live555 {
    param([string]$Arch, [string]$OutputDir)
    
    Write-Host "Building Live555 for $Arch..."
    cd native/live555
    
    mkdir -p $OutputDir
    cmake -B $OutputDir -G Ninja `
        -DCMAKE_BUILD_TYPE=$Config `
        -DCMAKE_OSX_ARCHITECTURES=$Arch `
        -DCMAKE_SYSTEM_NAME=$Platform
    
    cmake --build $OutputDir --config $Config
    
    cd ../..
}

if ($Platform -eq "all" -or $Platform -eq "windows") {
    Build-Live555 -Arch "x64" -OutputDir "native/live555/build-windows"
}

if ($Platform -eq "all" -or $Platform -eq "macos") {
    Build-Live555 -Arch "x86_64" -OutputDir "native/live555/build-macos-x64"
    Build-Live555 -Arch "arm64" -OutputDir "native/live555/build-macos-arm64"
}

if ($Platform -eq "all" -or $Platform -eq "linux") {
    Build-Live555 -Arch "x86_64" -OutputDir "native/live555/build-linux"
}

Write-Host "Native dependencies build complete!"
```

---

## Ссылки

- [Kotlin/Native Documentation](https://kotlinlang.org/docs/native.html)
- [Live555 Official Website](http://www.live555.com/liveMedia/)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [CMake Documentation](https://cmake.org/documentation/)
- [Kotlin Multiplatform Mobile - cinterop](https://kotlinlang.org/docs/native-cinterop.html)

---

## Поддержка

При возникновении проблем:

1. Проверьте логи сборки с флагом `--info`
2. Убедитесь, что все зависимости установлены
3. Проверьте соответствие архитектур библиотек и таргетов
4. Откройте issue в репозитории с полными логами сборки

---

**Версия документа:** 1.0  
**Дата последнего обновления:** 2026-06-14