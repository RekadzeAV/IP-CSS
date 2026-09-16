# Руководство по автоматизации тестирования

**Дата:** 27 January 2026

---

## 📋 Обзор

Автоматизированные скрипты для упрощения запуска тестов FFmpeg декодирования.

---

## 🚀 Быстрый старт

### 1. Настройка тестовых камер

#### Windows:
```powershell
.\scripts\setup-test-cameras.ps1
```

#### Linux/macOS:
```bash
chmod +x scripts/setup-test-cameras.sh
./scripts/setup-test-cameras.sh
```

Скрипт создаст файл `test_config.json` на основе примера и поможет его настроить.

### 2. Запуск тестов

#### Windows:
```powershell
# Все тесты (unit + integration)
.\scripts\run-ffmpeg-tests.ps1

# Только unit тесты
.\scripts\run-ffmpeg-tests.ps1 --Unit

# Только интеграционные тесты
.\scripts\run-ffmpeg-tests.ps1 --Integration

# С указанием конфигурации
.\scripts\run-ffmpeg-tests.ps1 --Integration --Config my_config.json
```

#### Linux/macOS:
```bash
chmod +x scripts/run-ffmpeg-tests.sh

# Все тесты
./scripts/run-ffmpeg-tests.sh

# Только unit тесты
./scripts/run-ffmpeg-tests.sh --unit

# Только интеграционные тесты
./scripts/run-ffmpeg-tests.sh --integration

# С указанием конфигурации
./scripts/run-ffmpeg-tests.sh --integration --config my_config.json
```

---

## 📝 Скрипты

### `setup-test-cameras.ps1` / `setup-test-cameras.sh`

**Назначение:** Настройка конфигурационного файла для тестовых камер.

**Функции:**
- Создание `test_config.json` на основе примера
- Проверка существования файла
- Открытие в редакторе для редактирования
- Вывод инструкций по настройке

**Использование:**
```bash
./scripts/setup-test-cameras.sh
```

### `run-ffmpeg-tests.ps1` / `run-ffmpeg-tests.sh`

**Назначение:** Запуск тестов с автоматическим логированием результатов.

**Параметры:**
- `--unit, -u` - Запустить только unit тесты
- `--integration, -i` - Запустить интеграционные тесты
- `--config, -c <file>` - Путь к конфигурационному файлу
- `--help, -h` - Показать справку

**Функции:**
- Проверка наличия собранных тестов
- Автоматическое логирование результатов
- Сохранение результатов в файл с временной меткой
- Вывод статуса выполнения

**Примеры:**
```bash
# Все тесты
./scripts/run-ffmpeg-tests.sh

# Unit тесты
./scripts/run-ffmpeg-tests.sh --unit

# Интеграционные тесты с конфигурацией
./scripts/run-ffmpeg-tests.sh --integration --config test_config.json
```

---

## 📊 Результаты тестирования

### Автоматическое логирование

Скрипты автоматически сохраняют результаты в файлы:
- **Формат:** `test_results_YYYYMMDD_HHMMSS.txt`
- **Расположение:** `native/video-processing/test/build/`

### Структура результатов

```
=== FFmpeg Testing Script ===

✅ Test executable found

=== Running Tests ===
Executable: /path/to/video_processing_tests
Arguments: --integration --config test_config.json

Running tests...
Results will be saved to: test_results_20260127_143022.txt

=== Video Processing Library Tests ===
...
=== Test Results ===
Passed: 10
Failed: 0
Total: 10

=== Test Results ===
✅ All tests passed!
Results saved to: test_results_20260127_143022.txt
```

---

## 🔧 Настройка

### Переменные окружения

Скрипты автоматически определяют пути на основе расположения проекта. При необходимости можно задать:

**Windows:**
```powershell
$env:PROJECT_ROOT = "D:\GitHub-Ai\IP-CSS"
```

**Linux/macOS:**
```bash
export PROJECT_ROOT="/path/to/project"
```

### Конфигурационные файлы

По умолчанию используется:
- `native/video-processing/test/test_config.json`

Можно указать другой файл:
```bash
./scripts/run-ffmpeg-tests.sh --config /path/to/custom_config.json
```

---

## 🐛 Решение проблем

### Проблема: Test executable not found

**Решение:**
1. Убедитесь, что тесты собраны:
   ```bash
   cd native/video-processing/test
   mkdir -p build && cd build
   cmake .. -DENABLE_FFMPEG=ON
   cmake --build . --config Release
   ```

2. Проверьте путь к исполняемому файлу:
   - Windows: `native/video-processing/test/build/video_processing_tests.exe`
   - Linux/macOS: `native/video-processing/test/build/video_processing_tests`

### Проблема: Config file not found

**Решение:**
1. Создайте конфигурационный файл:
   ```bash
   ./scripts/setup-test-cameras.sh
   ```

2. Или укажите путь к существующему файлу:
   ```bash
   ./scripts/run-ffmpeg-tests.sh --config /path/to/config.json
   ```

### Проблема: Permission denied (Linux/macOS)

**Решение:**
```bash
chmod +x scripts/run-ffmpeg-tests.sh
chmod +x scripts/setup-test-cameras.sh
```

---

## 📝 Интеграция с CI/CD

### GitHub Actions

Пример workflow для автоматического запуска тестов:

```yaml
name: FFmpeg Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Install dependencies
        run: |
          sudo apt-get update
          sudo apt-get install -y libavformat-dev libavcodec-dev libavutil-dev
      - name: Build tests
        run: |
          cd native/video-processing/test
          mkdir -p build && cd build
          cmake .. -DENABLE_FFMPEG=ON
          cmake --build . --config Release
      - name: Run tests
        run: |
          chmod +x scripts/run-ffmpeg-tests.sh
          ./scripts/run-ffmpeg-tests.sh --unit
      - name: Upload results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: native/video-processing/test/build/test_results_*.txt
```

---

## ✅ Преимущества автоматизации

1. **Упрощение запуска:** Один скрипт вместо множества команд
2. **Автоматическое логирование:** Результаты сохраняются автоматически
3. **Кроссплатформенность:** Работает на Windows, Linux, macOS
4. **Гибкость:** Поддержка различных режимов тестирования
5. **Интеграция:** Легко интегрируется с CI/CD

---

**Последнее обновление:** 27 January 2026
