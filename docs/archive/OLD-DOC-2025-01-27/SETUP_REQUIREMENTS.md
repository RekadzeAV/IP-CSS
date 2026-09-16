# Требования для активации RTSP клиента

**Дата:** Декабрь 2025

---

## 📦 Необходимые зависимости

### 1. Системные инструменты (macOS)

```bash
# Установка через Homebrew
brew install cmake ffmpeg pkg-config openjdk@17
```

**Что устанавливается:**
- **CMake** (≥ 3.15) - система сборки для нативной библиотеки
- **FFmpeg** - библиотеки для работы с видео/аудио (libavformat, libavcodec, libavutil, libswscale)
- **pkg-config** - утилита для поиска библиотек
- **OpenJDK 17** - Java Runtime для Gradle

### 2. Проверка установки

```bash
# Проверка CMake
cmake --version

# Проверка FFmpeg
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"

# Проверка pkg-config
pkg-config --version

# Проверка Java
java -version
```

---

## 🔧 Альтернативные способы установки

### Если Homebrew не установлен

1. **Установить Homebrew:**
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

2. **Или установить вручную:**
   - CMake: https://cmake.org/download/
   - FFmpeg: https://ffmpeg.org/download.html
   - Java: https://www.java.com/ или https://adoptium.net/

---

## ✅ После установки зависимостей

1. Скомпилировать библиотеку: `./scripts/build-native-lib.sh`
2. Сгенерировать биндинги: `bash gradlew :core:network:compileKotlinNative`
3. Активировать код: следовать `RTSP_ACTIVATION_CHECKLIST.md`

---

## 📚 Дополнительная информация

- **Полные инструкции:** `RTSP_NEXT_STEPS.md`
- **Чек-лист:** `RTSP_ACTIVATION_CHECKLIST.md`
- **Статус:** `RTSP_FINAL_EXECUTION_REPORT.md`

