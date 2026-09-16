# Статус установки зависимостей

**Дата проверки:** Январь 2026

## Результаты проверки

### ✅ CMake
- **Статус:** Установлен
- **Версия:** 4.2
- **Путь:** В PATH

### ✅ FFmpeg
- **Статус:** Установлен
- **Версия:** 8.0.1-full_build
- **Путь:** `C:\Users\Rekad\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.0.1-full_build`
- **Переменная окружения:** `FFMPEG_DIR` установлена автоматически

### ✅ Компилятор C++
- **Статус:** MinGW найден
- **Тип:** MinGW-w64
- **Путь:** В PATH

### ⚠️ OpenCV
- **Статус:** Не установлен (опционален)
- **Примечание:** OpenCV опционален для сборки. Библиотека соберется без него.

## Готовность к сборке

✅ **Все обязательные зависимости установлены**

Можно приступать к сборке нативной библиотеки:

```powershell
.\scripts\build-native-lib.ps1 x64 Release
```

## Рекомендации

1. **Перезапустите терминал** для применения переменных окружения (особенно `FFMPEG_DIR`)

2. **Проверьте переменные окружения:**
   ```powershell
   echo $env:FFMPEG_DIR
   ```

3. **Если FFMPEG_DIR не установлена**, установите вручную:
   ```powershell
   $env:FFMPEG_DIR = "C:\Users\Rekad\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.0.1-full_build"
   [System.Environment]::SetEnvironmentVariable("FFMPEG_DIR", $env:FFMPEG_DIR, "User")
   ```

4. **Для установки OpenCV (опционально):**
   - Скачайте с https://opencv.org/releases/
   - Распакуйте в `C:\opencv`
   - Установите переменную: `$env:OpenCV_DIR = "C:\opencv\build"`

---

**Следующий шаг:** Сборка нативной библиотеки

