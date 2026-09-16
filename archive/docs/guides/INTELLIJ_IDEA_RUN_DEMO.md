# Запуск VideoDecoder Demo в IntelliJ IDEA

## Самый простой способ

### Шаг 1: Открыть файл
В IntelliJ IDEA перейдите к:
```
core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/video/VideoDecoderDemoMain.kt
```

### Шаг 2: Запустить
1. Найдите функцию `main()`
2. Рядом появится зеленая стрелка ▶️
3. Кликните на стрелку → "Run 'VideoDecoderDemoMainKt'"

## Готово! 🎉

Вы увидите результаты в консоли:
```
=== VideoDecoder Demo ===
Creating VideoDecoder for H264, 1920x1080
Starting frame generation and decoding...
Decoded frame 1: 1920x1080
...
=== Demo Results ===
Frames decoded: 50
Average decode time: 40.00ms
Average FPS: 25.00
Demo completed successfully!
```

## Альтернатива: Запуск тестов

Если хотите запустить полные тесты:

1. Перейдите к `core/network/src/jvmTest/.../VideoDecoderDemoTest.kt`
2. Кликните на зеленую стрелку рядом с классом
3. Выберите "Run 'VideoDecoderDemoTest'"

## Если что-то не работает

1. **File → Invalidate Caches / Restart**
2. **Build → Rebuild Project**
3. Проверьте, что JavaCV зависимости установлены
4. Проверьте логи на ошибки

## Что демонстрирует

- Создание VideoDecoder для H.264
- Генерация тестовых кадров через симулятор
- Декодирование кадров
- Сбор метрик производительности
- Освобождение ресурсов

Это безопасный способ проверить функциональность без реальных камер!
