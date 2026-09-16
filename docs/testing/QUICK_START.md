# Быстрый старт тестирования FFmpeg декодирования

**Дата:** 27 January 2026

---

## 🚀 За 5 минут

### Шаг 1: Настройка среды (один раз)

```bash
# Windows
.\scripts\build-test-environment.ps1

# Linux/macOS
./scripts/build-test-environment.sh
```

### Шаг 2: Настройка камер

```bash
# Windows
.\scripts\setup-test-cameras.ps1

# Linux/macOS
./scripts/setup-test-cameras.sh
```

Отредактируйте `native/video-processing/test/test_config.json` с параметрами ваших камер.

### Шаг 3: Запуск тестов

```bash
# Windows
.\scripts\run-ffmpeg-tests.ps1 --integration

# Linux/macOS
./scripts/run-ffmpeg-tests.sh --integration
```

### Шаг 4: Анализ результатов

```bash
# Windows
.\scripts\analyze-test-results.ps1 test_results.txt --Html

# Linux/macOS
./scripts/analyze-test-results.sh test_results.txt --html
```

---

## 📋 Что тестируется

### Unit тесты:
- ✅ Видео декодер (H.264/H.265)
- ✅ Аудио декодер (AAC/PCMU/PCMA)
- ✅ RTP обработка (FU-A, STAP-A)

### Интеграционные тесты:
- ✅ Подключение к RTSP камерам
- ✅ Декодирование видеокадров
- ✅ Метрики производительности (FPS)

---

## 🎯 Результаты

После выполнения вы получите:
- ✅ Логи тестирования
- ✅ HTML/Markdown отчеты
- ✅ Метрики производительности
- ✅ Список проблем (если есть)

---

## 📚 Дополнительная документация

- `docs/testing/FFMPEG_TESTING_SETUP.md` - полная настройка
- `docs/testing/INTEGRATION_TEST_GUIDE.md` - интеграционные тесты
- `docs/testing/AUTOMATION_GUIDE.md` - автоматизация
- `docs/testing/UTILITIES_GUIDE.md` - утилиты

---

**Последнее обновление:** 27 January 2026
