# Руководство по активации RTSP клиента

**Версия документации:** 3.0
**Дата обновления:** 27 января 2025
**Предыдущая версия:** 2.0 (архивирована: 27 января 2025)

> **📚 Архивная документация:** Старые версии документов сохранены в `docs/archive/`

---

## 📋 Содержание

1. [Чек-лист активации](#чек-лист-активации)
2. [Шаги активации](#шаги-активации)
3. [Статус выполнения](#статус-выполнения)
4. [Следующие шаги](#следующие-шаги)

---

## Чек-лист активации

Используйте этот чек-лист для отслеживания прогресса активации RTSP клиента.

### ✅ Шаг 1: Установка зависимостей

- [ ] Установлен CMake (≥ 3.15)
  ```bash
  cmake --version
  ```

- [ ] Установлен FFmpeg (libavformat, libavcodec, libavutil, libswscale)
  ```bash
  pkg-config --exists libavformat && echo "OK" || echo "NOT FOUND"
  ```

- [ ] Установлен pkg-config
  ```bash
  pkg-config --version
  ```

**Команда для macOS:**
```bash
brew install cmake ffmpeg pkg-config
```

---

### ✅ Шаг 2: Компиляция нативной библиотеки

- [ ] Библиотека скомпилирована
  ```bash
  ./scripts/build-native-lib.sh
  ```

- [ ] Библиотека существует:
  - macOS: `native/video-processing/build/libvideo_processing.dylib`
  - Linux: `native/video-processing/build/libvideo_processing.so`

- [ ] Символы экспортированы (проверить):
  ```bash
  # macOS
  nm -gU native/video-processing/build/libvideo_processing.dylib | grep rtsp_client

  # Linux
  nm -D native/video-processing/build/libvideo_processing.so | grep rtsp_client
  ```

- [ ] Библиотека скопирована в lib/:
  - macOS: `native/video-processing/lib/macos/libvideo_processing.dylib`
  - Linux: `native/video-processing/lib/linux/libvideo_processing.so`

---

### ✅ Шаг 3: Генерация cinterop биндингов

- [ ] Kotlin/Native проект скомпилирован
  ```bash
  ./gradlew :core:network:compileKotlinNative
  ```

- [ ] Биндинги сгенерированы (проверить наличие файлов):
  ```
  core/network/build/bin/native/.../klib/.../com/company/ipcamera/core/network/rtsp/rtsp_client/
  ```

---

### ✅ Шаг 4: Активация кода

#### 4.1 Импорты

- [ ] Раскомментирован импорт в `NativeRtspClient.native.kt` (строка ~14):
  ```kotlin
  import com.company.ipcamera.core.network.rtsp.rtsp_client.*
  ```

#### 4.2 Методы

- [ ] Раскомментирована реализация `create()`
- [ ] Раскомментирована реализация `connect()`
- [ ] Раскомментирована реализация `disconnect()`
- [ ] Раскомментирована реализация `getStatus()`
- [ ] Раскомментирована реализация `play()`
- [ ] Раскомментирована реализация `stop()`
- [ ] Раскомментирована реализация `pause()`
- [ ] Раскомментирована реализация `getStreamCount()`
- [ ] Раскомментирована реализация `getStreamType()`
- [ ] Раскомментирована реализация `getStreamInfo()`

#### 4.3 Вспомогательные функции

- [ ] Раскомментирована функция `handleToPointer()`
- [ ] Раскомментирована функция `convertNativeStatus()`
- [ ] Раскомментирована функция `convertNativeStreamType()`
- [ ] Раскомментирована функция `convertStreamType()`

#### 4.4 Callbacks

- [ ] Реализован `setFrameCallback()` с использованием StableRef
- [ ] Реализован `setStatusCallback()` с использованием StableRef
- [ ] Реализована функция `convertNativeFrame()` (если нужна)

#### 4.5 Метод destroy()

- [ ] Раскомментирована реализация `destroy()`

---

### ✅ Шаг 5: Проверка компиляции

- [ ] Проект компилируется без ошибок
  ```bash
  ./gradlew :core:network:compileKotlinNative
  ```

- [ ] Нет ошибок "Unresolved reference"
- [ ] Нет ошибок типов

---

### ✅ Шаг 6: Базовое тестирование

- [ ] Создан тестовый RTSP клиент
- [ ] Тест подключения (connect)
- [ ] Тест воспроизведения (play)
- [ ] Тест остановки (stop)
- [ ] Тест отключения (disconnect)

---

## Шаги активации

### ⚠️ Текущая ситуация

**Зависимости не установлены:**
- ❌ CMake не найден
- ❌ FFmpeg не найден
- ❌ pkg-config не найден
- ❌ Homebrew не найден

**Попытка автоматической установки:**
- ❌ Не удалось - требуется интерактивный режим и права администратора

**Причина:** Установка зависимостей требует:
- Интерактивного подтверждения (TTY терминал)
- Пароля администратора (sudo)
- Доступа к интернету

**Решение:** Необходимо установить вручную в терминале (см. [INSTALLATION.md](INSTALLATION.md))

---

### ✅ Что подготовлено

1. ✅ **Скрипт для сборки** - `scripts/build-native-lib.sh`
2. ✅ **Полная документация** - все необходимые документы
3. ✅ **Структура кода** - готова к активации
4. ✅ **Чек-лист** - для отслеживания прогресса

---

## Статус выполнения

### Шаг 1: Компиляция библиотеки
- ❌ **Не выполнено** - зависимости не установлены
- **Ошибка:** CMake не найден
- **Требуется:** Установка CMake, FFmpeg, pkg-config

### Шаг 2: Генерация биндингов
- ❌ **Не выполнено** - требует скомпилированную библиотеку
- **Зависит от:** Шаг 1

### Шаг 3: Активация кода
- ❌ **Не выполнено** - требует сгенерированные биндинги
- **Зависит от:** Шаг 2

---

## Следующие шаги

### 1️⃣ Установить зависимости

```bash
# macOS (требует Homebrew)
brew install cmake ffmpeg pkg-config

# Проверка установки
cmake --version
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"
pkg-config --version
```

**Ожидаемое время:** 5-15 минут (зависит от скорости интернета)

---

### 2️⃣ Скомпилировать нативную библиотеку

```bash
./scripts/build-native-lib.sh
```

**Проверка успешной компиляции:**
```bash
# macOS
ls -lh native/video-processing/build/libvideo_processing.dylib

# Должен быть файл размером > 0
```

**Ожидаемое время:** 2-5 минут

---

### 3️⃣ Сгенерировать cinterop биндинги

```bash
./gradlew :core:network:compileKotlinNative
```

**Проверка успешной генерации:**
```bash
find core/network/build -name "*rtsp_client*" -type f | head -5
```

**Ожидаемое время:** 1-3 минуты

---

### 4️⃣ Активировать код

Следуйте инструкциям в чек-листе выше:
1. Раскомментировать импорт в `NativeRtspClient.native.kt`
2. Раскомментировать методы
3. Раскомментировать вспомогательные функции
4. Проверить компиляцию

**Ожидаемое время:** 5-10 минут

---

## 🚀 Быстрый старт

> **Примечание:** Это краткое руководство для быстрой активации. Для подробных инструкций см. разделы ниже.

### Минимальные шаги

1. **Установите зависимости:**
   ```bash
   # macOS
   brew install cmake ffmpeg pkg-config

   # Ubuntu/Debian
   sudo apt-get install cmake build-essential pkg-config \
       libavformat-dev libavcodec-dev libavutil-dev \
       libswscale-dev libswresample-dev
   ```

2. **Скомпилируйте библиотеку:**
   ```bash
   ./scripts/build-native-lib.sh
   ```

3. **Сгенерируйте биндинги:**
   ```bash
   ./gradlew :core:network:compileKotlinNative
   ```

4. **Активируйте код:**
   - Откройте `core/network/src/nativeMain/.../NativeRtspClient.native.kt`
   - Раскомментируйте импорт: `import com.company.ipcamera.core.network.rtsp.rtsp_client.*`
   - Раскомментируйте реализацию методов (найдите `// TODO: После компиляции cinterop...`)
   - Раскомментируйте вспомогательные функции

5. **Проверьте компиляцию:**
   ```bash
   ./gradlew :core:network:compileKotlinNative
   ```

---

## 📚 Дополнительная документация

**Для активации:**
- 📋 **[ACTIVATION.md](ACTIVATION.md)** - Этот документ (чек-лист и шаги)
- 📖 **[INSTALLATION.md](INSTALLATION.md)** - Инструкции по установке зависимостей

**Для справки:**
- 📄 **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Статус реализации и отчеты

---

## 💡 Примечания

- Все инструменты и документация готовы
- Код структурирован и готов к активации
- После установки зависимостей процесс займет 10-30 минут
- Следуйте чек-листу для отслеживания прогресса

---

## 📝 Примечания

После каждого шага проверяйте результат перед переходом к следующему.

При возникновении ошибок см.:
- [RTSP_CLIENT_ACTIVATION_GUIDE.md](../../archive/docs-duplicates-2026-08-08/RTSP_CLIENT_ACTIVATION_GUIDE.md) - раздел "Известные проблемы"
- [RTSP_BUILD_INSTRUCTIONS.md](BUILD_QUICKSTART.md) - раздел "Решение проблем"

---

**Версия документации:** 3.0
**Последнее обновление:** 26 January 2026
**Предыдущая версия:** 2.0 (архивирована: 27 января 2025)
**Создано:** January 2026

