# Инструкции по сборке библиотеки с FFmpeg

**Дата создания:** 27 January 2026
**Этап:** 2.1.2 - Сборка нативной библиотеки с ENABLE_FFMPEG=ON

---

## ✅ Проверка окружения

### FFmpeg установлен
- ✅ FFmpeg 8.0.1 найден
- ✅ Заголовки найдены в `C:\ffmpeg\include`
- ✅ Кодеки доступны: H.264, H.265, AAC

### Требования для сборки
- CMake 3.15+
- C++ компилятор (Visual Studio или MinGW-w64)
- FFmpeg development files

---

## 🔧 Сборка на Windows

### Вариант 1: Visual Studio (рекомендуется)

1. **Откройте Developer Command Prompt для VS:**
   - Найдите "Developer Command Prompt for VS" в меню Пуск
   - Или запустите: `"C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"`

2. **Перейдите в директорию проекта:**
   ```powershell
   cd D:\GitHub-Ai\IP-CSS
   ```

3. **Соберите библиотеку:**
   ```powershell
   cd native\video-processing
   mkdir build\windows-x64
   cd build\windows-x64

   cmake ..\.. `
       -G "Visual Studio 17 2022" `
       -A x64 `
       -DCMAKE_BUILD_TYPE=Release `
       -DENABLE_FFMPEG=ON `
       -DENABLE_OPENCV=OFF `
       -DFFMPEG_DIR=C:\ffmpeg

   cmake --build . --config Release
   ```

### Вариант 2: MinGW-w64

1. **Установите MinGW-w64:**
   - Скачайте с https://www.mingw-w64.org/
   - Или через MSYS2: `pacman -S mingw-w64-x86_64-gcc cmake`

2. **Соберите библиотеку:**
   ```powershell
   cd native\video-processing
   mkdir build\windows-x64
   cd build\windows-x64

   cmake ..\.. `
       -G "MinGW Makefiles" `
       -DCMAKE_BUILD_TYPE=Release `
       -DENABLE_FFMPEG=ON `
       -DENABLE_OPENCV=OFF `
       -DFFMPEG_DIR=C:\ffmpeg

   cmake --build . --config Release
   ```

---

## ✅ Проверка сборки

После успешной сборки проверьте:

1. **Библиотека создана:**
   ```powershell
   Test-Path build\windows-x64\Release\video_processing.dll
   ```

2. **FFmpeg линкуется:**
   ```powershell
   dumpbin /DEPENDENTS build\windows-x64\Release\video_processing.dll | Select-String -Pattern "avformat|avcodec|avutil"
   ```

3. **Символы экспортируются:**
   ```powershell
   dumpbin /EXPORTS build\windows-x64\Release\video_processing.dll | Select-String -Pattern "rtsp_client|video_decoder"
   ```

---

## 🐛 Решение проблем

### Проблема: CMAKE_CXX_COMPILER not set

**Решение:**
- Используйте Visual Studio Developer Command Prompt
- Или установите MinGW-w64 и добавьте в PATH

### Проблема: FFmpeg not found

**Решение:**
```powershell
$env:FFMPEG_DIR = "C:\ffmpeg"
cmake ..\.. -DFFMPEG_DIR=C:\ffmpeg ...
```

### Проблема: Ошибки компиляции rtsp_client.cpp

**Решение:**
- Убедитесь, что FFmpeg 8.0 API совместим
- Проверьте, что все заголовки FFmpeg доступны
- Проверьте версию компилятора (требуется C++17)

---

## 📝 Следующие шаги

После успешной сборки:
1. ✅ Этап 2.1.2 завершен
2. → Этап 2.2: Unit тесты для декодирования
3. → Этап 2.3: Интеграционные тесты

---

**Последнее обновление:** 27 January 2026
