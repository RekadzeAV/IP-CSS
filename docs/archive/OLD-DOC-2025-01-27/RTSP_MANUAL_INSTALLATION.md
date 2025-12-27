# Ручная установка зависимостей для RTSP клиента

**Дата:** Декабрь 2025  
**Причина:** Установка требует интерактивного подтверждения и прав администратора

---

## ⚠️ Важно

Автоматическая установка зависимостей невозможна, так как требует:
- Интерактивного подтверждения
- Пароля администратора
- TTY терминала

**Необходимо выполнить установку вручную в вашем терминале.**

---

## Шаг 1: Установка Homebrew (если не установлен)

Откройте терминал и выполните:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

**Примечания:**
- Может потребоваться ввод пароля администратора
- Установка может занять несколько минут
- После установки следуйте инструкциям на экране

**Проверка установки:**
```bash
brew --version
```

---

## Шаг 2: Установка зависимостей

После установки Homebrew, выполните:

```bash
brew install cmake ffmpeg pkg-config
```

**Время установки:** 5-15 минут (зависит от скорости интернета и системы)

**Проверка установки:**
```bash
cmake --version
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"
```

---

## Шаг 3: После установки зависимостей

После успешной установки всех зависимостей, вернитесь и выполните:

```bash
# 1. Компиляция библиотеки
./scripts/build-native-lib.sh

# 2. Генерация биндингов
./gradlew :core:network:compileKotlinNative

# 3. Активация кода
# См. RTSP_ACTIVATION_CHECKLIST.md или docs/RTSP_CLIENT_ACTIVATION_GUIDE.md
```

---

## Альтернативные методы установки

### Если Homebrew недоступен

**macOS:**
1. Используйте MacPorts: `sudo port install cmake ffmpeg pkgconfig`
2. Или скачайте и установите вручную:
   - CMake: https://cmake.org/download/
   - FFmpeg: https://ffmpeg.org/download.html

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install -y \
    cmake \
    build-essential \
    pkg-config \
    libavformat-dev \
    libavcodec-dev \
    libavutil-dev \
    libswscale-dev \
    libswresample-dev
```

---

## Проверка после установки

После установки всех зависимостей, проверьте:

```bash
# Проверка CMake
cmake --version
# Ожидаемый вывод: cmake version 3.x.x

# Проверка FFmpeg
pkg-config --exists libavformat && echo "✅ FFmpeg OK" || echo "❌ FFmpeg NOT FOUND"
pkg-config --modversion libavformat
# Ожидаемый вывод: версия FFmpeg (например, 6.0.0)

# Проверка pkg-config
pkg-config --version
# Ожидаемый вывод: версия pkg-config
```

---

## Следующие шаги

После успешной установки всех зависимостей:

1. ✅ Проверьте установку (команды выше)
2. ✅ Скомпилируйте библиотеку: `./scripts/build-native-lib.sh`
3. ✅ Сгенерируйте биндинги: `./gradlew :core:network:compileKotlinNative`
4. ✅ Активируйте код (см. `RTSP_ACTIVATION_CHECKLIST.md`)

---

## Дополнительная помощь

- [RTSP_DEPENDENCIES_STATUS.md](RTSP_DEPENDENCIES_STATUS.md) - Статус зависимостей
- [RTSP_EXECUTION_STATUS.md](RTSP_EXECUTION_STATUS.md) - Статус выполнения
- [docs/RTSP_QUICK_START.md](docs/RTSP_QUICK_START.md) - Быстрый старт
- [RTSP_ACTIVATION_CHECKLIST.md](RTSP_ACTIVATION_CHECKLIST.md) - Чек-лист активации

---

**Выполните установку вручную в терминале, затем продолжите со следующими шагами!**

