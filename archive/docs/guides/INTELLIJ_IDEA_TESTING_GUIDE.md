# Руководство по запуску тестов VideoDecoder в IntelliJ IDEA

## Быстрый старт

### Шаг 1: Открыть проект
1. Откройте IntelliJ IDEA
2. File → Open → Выберите папку проекта `IP-CSS`

### Шаг 2: Дождаться индексации
- IntelliJ IDEA автоматически обнаружит Gradle проект
- Дождитесь завершения индексации и синхронизации Gradle

### Шаг 3: Найти тесты
Перейдите к файлу:
```
core/network/src/jvmTest/kotlin/com/company/ipcamera/core/network/video/VideoDecoderDemoTest.kt
```

### Шаг 4: Запустить тесты

#### Вариант A: Запустить все тесты в классе
1. Откройте файл `VideoDecoderDemoTest.kt`
2. Найдите класс `VideoDecoderDemoTest`
3. Рядом с объявлением класса появится зеленая стрелка ▶️
4. Кликните на стрелку → "Run 'VideoDecoderDemoTest'"

#### Вариант B: Запустить отдельный тест
1. Найдите метод теста (например, `demoH264DecodingWithSimulator`)
2. Рядом с методом появится зеленая стрелка ▶️
3. Кликните на стрелку → "Run 'demoH264DecodingWithSimulator()'"

#### Вариант C: Через контекстное меню
1. Правой кнопкой мыши на классе или методе
2. Выберите "Run 'VideoDecoderDemoTest'" или "Run 'testMethodName'"

## Ожидаемые результаты

### Успешный запуск
Вы увидите в консоли:
```
=== Demo: H.264 Decoding with Simulator ===
Decoded frame 1: 1920x1080
Decoded frame 2: 1920x1080
...
Demo Results:
  Frames decoded: 100
  Total time: 4000ms
  Average decode time: 40.00ms
  Average FPS: 25.00
```

### Возможные проблемы

#### Проблема: Тесты не найдены
**Решение:**
1. File → Invalidate Caches / Restart
2. Build → Rebuild Project
3. File → Sync Project with Gradle Files

#### Проблема: Ошибка компиляции
**Решение:**
1. Проверьте, что JavaCV зависимости установлены
2. Build → Rebuild Project
3. Проверьте логи ошибок

#### Проблема: Тесты помечены как @Disabled
**Решение:**
В `VideoDecoderDemoTest.kt` тесты НЕ должны быть помечены `@Disabled`.
Если видите `@Disabled`, удалите эту аннотацию.

## Доступные тесты

### 1. demoH264DecodingWithSimulator
- Тестирует декодирование H.264 с симулятором
- Декодирует 100 кадров
- Проверяет производительность

### 2. demoPerformanceDifferentResolutions
- Тестирует различные разрешения (VGA, HD, Full HD)
- Сравнивает производительность

### 3. demoMetricsCollection
- Демонстрирует сбор метрик
- Экспортирует статистику

### 4. demoProfiling
- Демонстрирует профилирование
- Показывает детальные метрики

## Настройка для реальных камер

### Шаг 1: Настроить переменные окружения
1. Run → Edit Configurations
2. Выберите конфигурацию теста
3. Environment variables → Добавьте:
   - `TEST_CAMERA_H264_URL=rtsp://camera-ip:554/stream`
   - `TEST_CAMERA_USERNAME=admin`
   - `TEST_CAMERA_PASSWORD=password`

### Шаг 2: Запустить интеграционные тесты
Перейдите к `VideoDecoderIntegrationTest.kt` и запустите тесты.

## Просмотр результатов

### Консоль
Результаты тестов отображаются в консоли Run.

### Test Results
1. View → Tool Windows → Run (или Alt+4)
2. Выберите выполненный тест
3. Просмотрите результаты и логи

### Метрики
Метрики выводятся в консоль в формате:
```
Performance Metrics Summary:
  Total frames: 100
  Average decode time: 40.00ms
  Average FPS: 25.00
```

## Отладка тестов

### Установка точек останова
1. Кликните слева от номера строки
2. Появится красная точка (breakpoint)
3. Запустите тест в режиме Debug (Shift+F9)

### Просмотр переменных
В режиме Debug:
- Variables - показывает значения переменных
- Watches - позволяет следить за выражениями
- Console - показывает логи

## Советы

1. **Первый запуск может быть медленным** - JavaCV загружает нативные библиотеки
2. **Проверьте логи** - если есть ошибки, они будут в консоли
3. **Используйте Debug режим** - для детального анализа проблем
4. **Проверьте зависимости** - убедитесь, что JavaCV установлен

## Следующие шаги

После успешного запуска демо-тестов:
1. Настройте реальные камеры
2. Запустите интеграционные тесты
3. Проанализируйте результаты
4. Примените оптимизации
