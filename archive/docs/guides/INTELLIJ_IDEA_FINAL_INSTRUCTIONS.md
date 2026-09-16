# Финальные инструкции по запуску тестов в IntelliJ IDEA

## ✅ Готово к запуску

Создан простой демо-класс, который можно запустить прямо сейчас:

### Файл: `VideoDecoderDemoMain.kt`
**Путь:** `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/video/VideoDecoderDemoMain.kt`

## 🚀 Запуск (3 шага)

### Шаг 1: Открыть IntelliJ IDEA
1. Запустите IntelliJ IDEA
2. File → Open → Выберите папку проекта `IP-CSS`
3. Дождитесь индексации проекта

### Шаг 2: Найти файл
В Project Explorer перейдите к:
```
core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/video/VideoDecoderDemoMain.kt
```

### Шаг 3: Запустить
1. Откройте файл `VideoDecoderDemoMain.kt`
2. Найдите функцию `main()` (строка 18)
3. Рядом с функцией появится зеленая стрелка ▶️
4. Кликните на стрелку
5. Выберите **"Run 'VideoDecoderDemoMainKt'"**

## 📊 Ожидаемый результат

В консоли Run вы увидите:

```
=== VideoDecoder Demo ===
Creating VideoDecoder for H264, 1920x1080
Starting frame generation and decoding...
Decoded frame 1: 1920x1080
Decoded frame 2: 1920x1080
...
=== Demo Results ===
Frames decoded: 50
Total time: 2000ms
Average decode time: 40.00ms
Average FPS: 25.00
Total decode operations: 50
Demo completed successfully!
```

## 🔧 Если не работает

### Проблема: Нет зеленой стрелки
**Решение:**
1. File → Invalidate Caches / Restart
2. Build → Rebuild Project
3. File → Sync Project with Gradle Files

### Проблема: Ошибка компиляции
**Решение:**
1. Проверьте, что JavaCV зависимости установлены
2. Build → Rebuild Project
3. Проверьте логи ошибок

### Проблема: "Class not found"
**Решение:**
1. Убедитесь, что проект синхронизирован с Gradle
2. File → Sync Project with Gradle Files
3. Build → Rebuild Project

## 📝 Альтернатива: Запуск тестов

Если хотите запустить полные JUnit тесты:

1. Перейдите к `core/network/src/jvmTest/.../VideoDecoderDemoTest.kt`
2. Кликните на зеленую стрелку рядом с классом `VideoDecoderDemoTest`
3. Выберите "Run 'VideoDecoderDemoTest'"

**Примечание:** Тесты могут требовать исправления ошибок компиляции в других файлах проекта.

## ✅ Что демонстрирует

- ✅ Создание VideoDecoder
- ✅ Генерация тестовых кадров
- ✅ Декодирование H.264
- ✅ Сбор метрик производительности
- ✅ Освобождение ресурсов

## 📚 Дополнительная документация

- `docs/INTELLIJ_IDEA_TESTING_GUIDE.md` - Подробное руководство
- `docs/INTELLIJ_IDEA_QUICK_START.md` - Быстрый старт
- `docs/INTELLIJ_IDEA_RUN_DEMO.md` - Инструкции по демо

## 🎯 Следующие шаги

После успешного запуска:
1. Проанализируйте результаты
2. Настройте реальные камеры (если доступны)
3. Запустите полные тесты
4. Примените оптимизации

---

**Готово!** Откройте IntelliJ IDEA и запустите `VideoDecoderDemoMain.kt` 🚀
