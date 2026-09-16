# Установка FFmpeg для разработки на Windows

**Проблема:** Стандартная установка FFmpeg через WinGet/Chocolatey содержит только исполняемые файлы, но не библиотеки для разработки.

## Решение: Установка FFmpeg с dev-пакетами

### Вариант 1: Скачать готовую сборку (рекомендуется)

1. **Скачайте FFmpeg для Windows:**
   - Перейдите на https://www.gyan.dev/ffmpeg/builds/
   - Скачайте "ffmpeg-release-full.7z" (полная версия с dev-пакетами)
   - Или используйте прямую ссылку: https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-full.7z

2. **Распакуйте архив:**
   ```powershell
   # Распакуйте в C:\ffmpeg
   Expand-Archive -Path ffmpeg-release-full.7z -DestinationPath C:\ffmpeg
   ```

3. **Установите переменную окружения:**
   ```powershell
   $env:FFMPEG_DIR = "C:\ffmpeg"
   [System.Environment]::SetEnvironmentVariable("FFMPEG_DIR", "C:\ffmpeg", "User")
   ```

4. **Проверьте структуру:**
   ```
   C:\ffmpeg\
   ├── bin\          # Исполняемые файлы
   ├── include\      # Заголовочные файлы
   │   ├── libavformat\
   │   ├── libavcodec\
   │   └── ...
   └── lib\          # Библиотеки (.lib и .dll)
   ```

### Вариант 2: Использовать vcpkg

1. **Установите vcpkg:**
   ```powershell
   git clone https://github.com/Microsoft/vcpkg.git C:\vcpkg
   cd C:\vcpkg
   .\bootstrap-vcpkg.bat
   ```

2. **Установите FFmpeg через vcpkg:**
   ```powershell
   .\vcpkg install ffmpeg:x64-windows
   ```

3. **Интегрируйте с CMake:**
   ```powershell
   .\vcpkg integrate install
   ```

4. **Используйте при сборке:**
   ```powershell
   cmake .. -DCMAKE_TOOLCHAIN_FILE=C:\vcpkg\scripts\buildsystems\vcpkg.cmake
   ```

### Вариант 3: Собрать из исходников

Это самый сложный вариант, требует установки MSYS2 и компилятора.

## Проверка установки

После установки проверьте:

```powershell
# Проверка заголовочных файлов
Test-Path "C:\ffmpeg\include\libavformat\avformat.h"

# Проверка библиотек
Get-ChildItem "C:\ffmpeg\lib" -Filter "avformat*.lib"
```

## Временное решение: Сборка без FFmpeg

Если FFmpeg с dev-пакетами недоступен, можно собрать библиотеку без него:

```powershell
cmake .. -DENABLE_FFMPEG=OFF
```

**Ограничения:**
- RTSP клиент будет иметь ограниченную функциональность
- Декодирование видео будет недоступно
- HLS генерация на сервере все равно будет работать (использует FFmpeg как отдельный процесс)

## Рекомендация

Для разработки рекомендуется использовать **Вариант 1** (скачать готовую сборку), так как это самый простой и быстрый способ получить все необходимые файлы.

---

**Примечание:** Текущая установка FFmpeg через WinGet содержит только исполняемые файлы и подходит для использования FFmpeg как отдельного процесса (например, для HLS генерации), но не для компиляции нативной библиотеки, которая требует линковки с библиотеками FFmpeg.

