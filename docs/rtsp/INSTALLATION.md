# Руководство по установке зависимостей для RTSP клиента

**Версия документации:** 3.0
**Дата обновления:** 27 января 2025
**Предыдущая версия:** 2.0 (архивирована: 27 января 2025)

> **📚 Архивная документация:** Старые версии документов сохранены в `docs/archive/`

---

## 📋 Содержание

1. [Требуемые зависимости](#требуемые-зависимости)
2. [Установка на macOS](#установка-на-macos)
3. [Установка на Linux](#установка-на-linux)
4. [Проверка установки](#проверка-установки)
5. [Следующие шаги](#следующие-шаги)

---

## Требуемые зависимости

### CMake
- **Требуется:** ≥ 3.15
- **Назначение:** Сборка нативной библиотеки
- **Установка (macOS):** `brew install cmake`
- **Установка (Ubuntu/Debian):** `sudo apt-get install cmake`

### FFmpeg
- **Требуется:** libavformat, libavcodec, libavutil, libswscale, libswresample
- **Назначение:** Обработка видео и аудио потоков
- **Установка (macOS):** `brew install ffmpeg`
- **Установка (Ubuntu/Debian):** `sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev`

### pkg-config
- **Требуется:** для обнаружения FFmpeg
- **Назначение:** Определение путей и флагов компиляции
- **Установка (macOS):** `brew install pkg-config`
- **Установка (Ubuntu/Debian):** `sudo apt-get install pkg-config`

---

## Установка на macOS

### ⚠️ Важно

Автоматическая установка зависимостей невозможна, так как требует:
- Интерактивного подтверждения
- Пароля администратора
- TTY терминала

**Необходимо выполнить установку вручную в вашем терминале.**

---

### Шаг 1: Установка Homebrew (если не установлен)

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

### Шаг 2: Установка зависимостей

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

### Альтернативные методы установки

**Если Homebrew недоступен:**

1. Используйте MacPorts: `sudo port install cmake ffmpeg pkgconfig`
2. Или скачайте и установите вручную:
   - CMake: https://cmake.org/download/
   - FFmpeg: https://ffmpeg.org/download.html

---

## Установка на Linux

### Ubuntu/Debian

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

**Проверка установки:**
```bash
cmake --version
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"
```

---

## Проверка установки

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
4. ✅ Активируйте код (см. [ACTIVATION.md](ACTIVATION.md))

---

## ⚠️ Важно

Установка зависимостей требует:
- Доступ к интернету (для загрузки)
- Права администратора (для установки системных пакетов)
- Интерактивного подтверждения (для некоторых установщиков)

**После установки зависимостей** можно продолжить с компиляции библиотеки и активации кода.

---

## 📚 Дополнительная документация

**Для активации:**
- 📋 **[ACTIVATION.md](ACTIVATION.md)** - Чек-лист и шаги активации
- 📖 **[docs/RTSP_CLIENT_ACTIVATION_GUIDE.md](../../archive/docs-duplicates-2026-08-08/RTSP_CLIENT_ACTIVATION_GUIDE.md)** - Полное руководство

**Для справки:**
- 📄 **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Статус реализации и отчеты
- 📄 **[docs/RTSP_QUICK_START.md](../../archive/docs-duplicates-2026-08-08/RTSP_QUICK_START.md)** - Быстрый старт

---

**Версия документации:** 3.0
**Последнее обновление:** 26 January 2026
**Предыдущая версия:** 2.0 (архивирована: 27 января 2025)
**Создано:** January 2026

