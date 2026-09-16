# Настройка CI/CD для тестов FFmpeg

**Дата:** 27 January 2026

---

## 📋 Обзор

GitHub Actions workflow для автоматического запуска тестов FFmpeg декодирования.

---

## 🚀 Быстрый старт

### Автоматический запуск

Workflow автоматически запускается при:
- Push в `main` или `develop` (если изменены файлы видео/RTSP)
- Pull Request в `main` или `develop`
- Ручной запуск через `workflow_dispatch`

### Интеграционные тесты

Интеграционные тесты запускаются:
- При ручном запуске workflow
- При коммите с сообщением `[test-integration]`

---

## 📝 Конфигурация

### Secrets (для интеграционных тестов)

Настройте следующие secrets в GitHub:
- `RTSP_TEST_CAMERA_URL` - RTSP URL тестовой камеры
- `RTSP_TEST_CAMERA_USER` - Имя пользователя
- `RTSP_TEST_CAMERA_PASS` - Пароль

**Настройка:**
1. Settings → Secrets and variables → Actions
2. New repository secret
3. Добавить каждый secret

---

## 🔧 Workflow Jobs

### test: Unit тесты

**Платформы:**
- Ubuntu Latest
- Windows Latest
- macOS Latest

**Шаги:**
1. Установка FFmpeg
2. Установка CMake
3. Сборка нативной библиотеки
4. Сборка тестов
5. Запуск unit тестов
6. Загрузка результатов

### integration: Интеграционные тесты

**Платформа:** Ubuntu Latest

**Шаги:**
1. Установка зависимостей
2. Сборка библиотеки и тестов
3. Настройка конфигурации
4. Запуск интеграционных тестов
5. Анализ результатов
6. Загрузка отчетов

---

## 📊 Результаты

### Артефакты

После выполнения workflow доступны:
- `test-results-<os>` - результаты unit тестов для каждой ОС
- `integration-test-results` - результаты интеграционных тестов

### Просмотр результатов

1. Перейти в Actions
2. Выбрать выполненный workflow
3. Скачать артефакты
4. Открыть HTML отчеты

---

## 🔧 Локальный запуск

### Имитация CI/CD

```bash
# Установка зависимостей (Ubuntu)
sudo apt-get update
sudo apt-get install -y \
    libavformat-dev \
    libavcodec-dev \
    libavutil-dev \
    libswscale-dev \
    libswresample-dev \
    cmake \
    build-essential

# Сборка и тестирование
cd native/video-processing
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release -DENABLE_FFMPEG=ON -DENABLE_OPENCV=OFF
cmake --build . --config Release -j$(nproc)

cd ../test
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release -DENABLE_FFMPEG=ON
cmake --build . --config Release -j$(nproc)
./video_processing_tests
```

---

## 🐛 Решение проблем

### Проблема: FFmpeg not found

**Решение:**
- Проверьте, что FFmpeg установлен в workflow
- Убедитесь, что пути правильные

### Проблема: Tests fail on Windows

**Решение:**
- Проверьте, что используется правильный компилятор
- Убедитесь, что FFmpeg доступен для Windows

### Проблема: Integration tests skipped

**Решение:**
- Проверьте, что secrets настроены
- Убедитесь, что workflow запущен с правильными условиями

---

## 📝 Кастомизация

### Добавление новых платформ

Отредактируйте `matrix.os` в workflow:

```yaml
matrix:
  os: [ubuntu-latest, windows-latest, macos-latest, ubuntu-20.04]
```

### Изменение триггеров

Отредактируйте секцию `on:`:

```yaml
on:
  push:
    branches: [ main, develop, feature/* ]
  schedule:
    - cron: '0 0 * * *'  # Ежедневно в полночь
```

---

**Последнее обновление:** 27 January 2026
