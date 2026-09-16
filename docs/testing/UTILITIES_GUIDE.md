# Руководство по утилитам тестирования

**Дата:** 27 January 2026

---

## 📋 Обзор

Дополнительные утилиты для упрощения тестирования и анализа результатов.

---

## 🛠️ Утилиты

### test_utils.h / test_utils.cpp

**Назначение:** Вспомогательные функции для создания тестовых данных и работы с RTP.

**Функции:**

#### Работа с файлами:
- `load_file()` - Загрузка файла в память
- `save_file()` - Сохранение данных в файл

#### Создание тестовых данных H.264:
- `create_test_h264_sps()` - Создание SPS
- `create_test_h264_pps()` - Создание PPS
- `create_test_h264_idr_frame()` - Создание IDR кадра

#### Создание тестовых данных H.265:
- `create_test_h265_vps()` - Создание VPS
- `create_test_h265_sps()` - Создание SPS
- `create_test_h265_pps()` - Создание PPS
- `create_test_h265_idr_frame()` - Создание IDR кадра

#### Работа с RTP:
- `create_rtp_packet()` - Создание RTP пакета
- `parse_rtp_header()` - Парсинг RTP заголовка
- `extract_rtp_payload()` - Извлечение payload
- `create_fua_fragment()` - Создание FU-A фрагмента
- `create_stap_a_packet()` - Создание STAP-A пакета

#### Валидация:
- `is_valid_h264_nal()` - Проверка валидности H.264 NAL
- `is_valid_h265_nal()` - Проверка валидности H.265 NAL
- `get_h264_nal_type()` - Получение типа H.264 NAL
- `get_h265_nal_type()` - Получение типа H.265 NAL

#### Утилиты:
- `format_time_ms()` - Форматирование времени
- `format_size()` - Форматирование размера
- `is_ffmpeg_available()` - Проверка доступности FFmpeg
- `get_ffmpeg_version()` - Получение версии FFmpeg

**Использование:**
```cpp
#include "test_utils.h"

// Создание тестового H.264 SPS
auto sps = test_utils::create_test_h264_sps(1920, 1080, 25);

// Создание RTP пакета
auto rtp_packet = test_utils::create_rtp_packet(
    96,  // payload type
    100, // sequence
    1000, // timestamp
    0x12345678, // SSRC
    sps.data(),
    sps.size()
);
```

---

### test_analyzer.cpp

**Назначение:** Анализ результатов тестов и генерация отчетов.

**Функции:**
- Парсинг вывода тестов
- Подсчет статистики (passed/failed)
- Генерация HTML отчетов
- Генерация Markdown отчетов

**Использование:**

#### Командная строка:
```bash
# Базовый анализ
./test_analyzer test_results.txt

# С HTML отчетом
./test_analyzer test_results.txt --html

# С Markdown отчетом
./test_analyzer test_results.txt --markdown

# Оба формата
./test_analyzer test_results.txt --html --markdown
```

#### Через скрипты:

**Windows:**
```powershell
.\scripts\analyze-test-results.ps1 test_results.txt --Html
.\scripts\analyze-test-results.ps1 test_results.txt --Markdown
```

**Linux/macOS:**
```bash
./scripts/analyze-test-results.sh test_results.txt --html
./scripts/analyze-test-results.sh test_results.txt --markdown
```

---

## 📊 Форматы отчетов

### HTML отчет

Создает визуально приятный HTML файл с:
- Сводной статистикой
- Таблицей результатов
- Цветовой индикацией (зеленый/красный)

**Пример:**
```html
<!DOCTYPE html>
<html>
<head>
    <title>Test Results</title>
    <style>...</style>
</head>
<body>
    <h1>Test Results Report</h1>
    <div class="summary">
        <h2>Summary</h2>
        <p>Total: 10</p>
        <p class="passed">Passed: 8</p>
        <p class="failed">Failed: 2</p>
        <p>Success Rate: 80.00%</p>
    </div>
    <table>...</table>
</body>
</html>
```

### Markdown отчет

Создает Markdown файл для документации:

**Пример:**
```markdown
# Test Results Report

## Summary

- **Total:** 10
- **Passed:** 8
- **Failed:** 2
- **Success Rate:** 80.00%

## Test Details

| Test Name | Status | Message |
|-----------|--------|---------|
| test_1 | ✅ PASSED | |
| test_2 | ❌ FAILED | Error message |
```

---

## 🔧 Интеграция

### В тестах

Использование утилит в тестах:

```cpp
#include "test_utils.h"

TEST(video_decoder_test) {
    // Создание тестовых данных
    auto sps = test_utils::create_test_h264_sps(1920, 1080, 25);
    auto pps = test_utils::create_test_h264_pps();
    auto idr = test_utils::create_test_h264_idr_frame(1920, 1080);

    // Валидация
    ASSERT_TRUE(test_utils::is_valid_h264_nal(sps.data(), sps.size()));

    // Тестирование
    // ...
}
```

### В CI/CD

Автоматический анализ результатов:

```yaml
- name: Run tests
  run: ./scripts/run-ffmpeg-tests.sh --unit

- name: Analyze results
  run: |
    ./scripts/analyze-test-results.sh \
      native/video-processing/test/build/test_results_*.txt \
      --html --markdown

- name: Upload reports
  uses: actions/upload-artifact@v3
  with:
    name: test-reports
    path: |
      native/video-processing/test/build/*.html
      native/video-processing/test/build/*.md
```

---

## 📝 Примеры использования

### Создание тестовых данных для файлов

```cpp
// Создание и сохранение тестовых данных
auto sps = test_utils::create_test_h264_sps(1920, 1080, 25);
test_utils::save_file("test_data/h264/sps.bin", sps.data(), sps.size());
```

### Анализ результатов после тестирования

```bash
# Запуск тестов
./scripts/run-ffmpeg-tests.sh --integration > results.txt

# Анализ результатов
./scripts/analyze-test-results.sh results.txt --html --markdown

# Просмотр HTML отчета
open results.txt.html  # macOS
xdg-open results.txt.html  # Linux
start results.txt.html  # Windows
```

---

## ✅ Преимущества

1. **Упрощение тестирования:** Готовые функции для создания тестовых данных
2. **Автоматизация анализа:** Автоматический парсинг и генерация отчетов
3. **Визуализация:** HTML отчеты для удобного просмотра
4. **Документация:** Markdown отчеты для включения в документацию
5. **Интеграция:** Легко интегрируется с CI/CD

---

**Последнее обновление:** 27 January 2026
