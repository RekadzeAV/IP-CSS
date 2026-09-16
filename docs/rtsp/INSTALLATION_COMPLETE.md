# Установка зависимостей для сборки RTSP клиента - Завершено

**Дата:** Январь 2026
**Статус:** ✅ Установка завершена

## Установленные компоненты

### ✅ Visual Studio Build Tools 2022

**Статус:** Установлено через WinGet

**Компоненты:**
- Visual Studio Build Tools 2022 (версия 17.14.25)
- C++ build tools (установка запущена)

**Примечание:** Для использования MSVC компилятора необходимо:
1. Запустить Developer Command Prompt, или
2. Выполнить: `"C:\Program Files\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat"`

### ✅ MinGW-w64

**Статус:** Установлено через WinGet

**Детали:**
- Версия: MinGW-W64 x86_64-ucrt-posix-seh, r4
- Компилятор: g++ 15.2.0
- Путь: `C:\Users\Rekad\AppData\Local\Microsoft\WinGet\Packages\BrechtSanders.WinLibs.POSIX.UCRT_Microsoft.Winget.Source_8wekyb3d8bbwe\mingw64\bin\g++.exe`

**Статус:** ✅ Готов к использованию

### ✅ FFmpeg

**Статус:** Установлено

**Исполняемый файл:**
- Версия: FFmpeg 8.0.1-full_build
- Путь: `C:\Users\Rekad\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.0.1-full_build\bin\ffmpeg.exe`

**Dev библиотеки:**
- Путь: `C:\ffmpeg`
- Заголовочные файлы: `C:\ffmpeg\include\`
- Библиотеки: `C:\ffmpeg\lib\`
- Переменная окружения: `FFMPEG_DIR=C:\ffmpeg` (установлена)

**Статус:** ✅ Готов к использованию

### ✅ CMake

**Статус:** Уже установлен

**Версия:** CMake 4.2.1

**Статус:** ✅ Готов к использованию

## Проверка установки

Для проверки всех зависимостей выполните:

```powershell
.\scripts\verify-build-dependencies.ps1
```

## Следующие шаги

Теперь можно приступить к сборке нативной библиотеки:

```powershell
# Сборка с MinGW (рекомендуется, так как уже в PATH)
.\scripts\build-rtsp-native-libs.ps1 windows x64 Release

# Или сборка с MSVC (требует Developer Command Prompt)
# Откройте Developer Command Prompt и выполните:
cd D:\GitHub-Ai\IP-CSS
.\scripts\build-rtsp-native-libs.ps1 windows x64 Release
```

## Переменные окружения

Установлены следующие переменные окружения:

- `FFMPEG_DIR=C:\ffmpeg` (пользовательская переменная)

Для использования в текущей сессии:

```powershell
$env:FFMPEG_DIR = "C:\ffmpeg"
```

## Примечания

1. **MSVC компилятор:** Требует запуска из Developer Command Prompt или выполнения `vcvars64.bat`
2. **MinGW компилятор:** Готов к использованию сразу, так как находится в PATH
3. **FFmpeg:** Dev библиотеки установлены и готовы к использованию

## Устранение проблем

Если возникли проблемы:

1. **Проверьте установку:**
   ```powershell
   .\scripts\verify-build-dependencies.ps1
   ```

2. **Для MSVC:** Убедитесь, что C++ компоненты установлены в Visual Studio Build Tools

3. **Для FFmpeg:** Убедитесь, что переменная `FFMPEG_DIR` установлена:
   ```powershell
   $env:FFMPEG_DIR = "C:\ffmpeg"
   ```

---

**Связанные документы:**
- [NATIVE_LIBRARY_BUILD.md](NATIVE_LIBRARY_BUILD.md) - Руководство по сборке
- [BUILD_QUICKSTART.md](BUILD_QUICKSTART.md) - Быстрый старт
