# Руководство по сборке нативных библиотек video_processing

## Обзор

Нативная библиотека `video_processing` должна быть собрана для каждой целевой платформы:
- Linux x64
- macOS x64 (Intel)
- macOS arm64 (Apple Silicon)
- Windows x64 (уже собрана)

## Методы сборки

### Метод 1: Прямая сборка на целевой системе (Рекомендуется)

#### Linux x64

**Требования:**
```bash
sudo apt-get update
sudo apt-get install -y cmake build-essential pkg-config
sudo apt-get install -y libavcodec-dev libavformat-dev libavutil-dev libswscale-dev libswresample-dev
```

**Сборка:**
```bash
./scripts/build-video-processing-linux.sh
```

**Результат:**
- `native/video-processing/lib/linux/x64/libvideo_processing.so`

#### macOS (arm64 или x64)

**Требования:**
```bash
brew install cmake ffmpeg
```

**Сборка:**
```bash
# Apple Silicon (arm64)
./scripts/build-video-processing-macos.sh arm64

# Intel (x64)
./scripts/build-video-processing-macos.sh x64
```

**Результат:**
- `native/video-processing/lib/macos/arm64/libvideo_processing.dylib`
- `native/video-processing/lib/macos/x64/libvideo_processing.dylib`

### Метод 2: Сборка через Docker (Linux)

Если у вас нет доступа к Linux системе, но есть Docker:

```bash
./scripts/build-video-processing-docker.sh linux
```

**Требования:**
- Docker установлен и запущен
- Достаточно места на диске

### Метод 3: GitHub Actions (Автоматическая сборка)

Библиотеки автоматически собираются при push в репозиторий через GitHub Actions.

**Использование:**
1. Push изменений в `native/video-processing/`
2. GitHub Actions автоматически соберет библиотеки
3. Скачайте артефакты из Actions → Artifacts

**Ручной запуск:**
1. Перейдите в Actions → Build Native Video Processing Libraries
2. Run workflow → Run workflow

### Метод 4: Удаленная сборка

#### Через SSH на Linux сервер

```bash
# На локальной машине
scp -r native/video-processing user@linux-server:/tmp/

# На Linux сервере
ssh user@linux-server
cd /tmp/video-processing
./scripts/build-video-processing-linux.sh

# Вернуть результат
scp user@linux-server:/tmp/video-processing/lib/linux/x64/libvideo_processing.so \
    native/video-processing/lib/linux/x64/
```

#### Через macOS (если доступен)

```bash
# На macOS
cd native/video-processing
./scripts/build-video-processing-macos.sh arm64  # или x64
```

## Проверка сборки

### Проверка наличия библиотеки

```bash
# Linux
ls -lh native/video-processing/lib/linux/x64/libvideo_processing.so

# macOS
ls -lh native/video-processing/lib/macos/arm64/libvideo_processing.dylib
ls -lh native/video-processing/lib/macos/x64/libvideo_processing.dylib
```

### Проверка экспорта символов

```bash
# Linux
nm -D native/video-processing/lib/linux/x64/libvideo_processing.so | grep video_decoder

# macOS
nm -gU native/video-processing/lib/macos/arm64/libvideo_processing.dylib | grep video_decoder
```

### Проверка зависимостей

```bash
# Linux
ldd native/video-processing/lib/linux/x64/libvideo_processing.so

# macOS
otool -L native/video-processing/lib/macos/arm64/libvideo_processing.dylib
```

## Устранение проблем

### Проблема: FFmpeg не найден

**Linux:**
```bash
pkg-config --modversion libavcodec
# Если не найдено:
sudo apt-get install libavcodec-dev libavformat-dev libavutil-dev libswscale-dev
```

**macOS:**
```bash
brew list ffmpeg
# Если не установлен:
brew install ffmpeg
```

### Проблема: CMake не найден

**Linux:**
```bash
sudo apt-get install cmake
```

**macOS:**
```bash
brew install cmake
```

### Проблема: Ошибки компиляции

1. Проверьте версию компилятора:
   ```bash
   gcc --version  # Должен быть >= 7.0
   ```

2. Проверьте версию CMake:
   ```bash
   cmake --version  # Должен быть >= 3.10
   ```

3. Проверьте логи сборки на ошибки

### Проблема: Библиотека не найдена при запуске

1. Проверьте путь к библиотеке в `build.gradle.kts`
2. Убедитесь, что библиотека скопирована в правильную директорию
3. Проверьте права доступа:
   ```bash
   chmod +x native/video-processing/lib/linux/x64/libvideo_processing.so
   ```

## Интеграция в проект

После сборки библиотеки автоматически используются Kotlin/Native через cinterop.

Проверка:
```bash
./gradlew :core:network:compileKotlinNativeLinux
```

Если компиляция успешна, библиотека правильно интегрирована.

## Статус сборки

### ✅ Windows x64
- Статус: Собрана
- Расположение: `native/video-processing/lib/windows/x64/video_processing.dll`

### ⏳ Linux x64
- Статус: Требует сборки
- Метод: Прямая сборка на Linux или Docker

### ⏳ macOS arm64
- Статус: Требует сборки
- Метод: Прямая сборка на macOS

### ⏳ macOS x64
- Статус: Требует сборки
- Метод: Прямая сборка на macOS Intel

## Рекомендации

1. **Для разработки**: Используйте прямую сборку на целевой системе
2. **Для CI/CD**: Используйте GitHub Actions
3. **Для тестирования**: Используйте Docker для Linux
4. **Для продакшена**: Собирайте на чистой системе с минимальными зависимостями

## Следующие шаги

После успешной сборки:
1. Проверьте библиотеку (см. раздел "Проверка сборки")
2. Интегрируйте в проект (автоматически через cinterop)
3. Активируйте `VideoDecoder.native.kt` (раскомментируйте код)
4. Протестируйте на целевой платформе
