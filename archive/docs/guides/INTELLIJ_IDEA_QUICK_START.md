# Быстрый старт: Запуск тестов VideoDecoder в IntelliJ IDEA

## За 3 шага

### Шаг 1: Открыть файл теста
В IntelliJ IDEA перейдите к:
```
core/network/src/jvmTest/kotlin/com/company/ipcamera/core/network/video/VideoDecoderDemoTest.kt
```

### Шаг 2: Найти зеленую стрелку
Рядом с классом `VideoDecoderDemoTest` или методом теста появится зеленая стрелка ▶️

### Шаг 3: Кликнуть "Run"
Кликните на стрелку и выберите "Run 'VideoDecoderDemoTest'"

## Готово! 🎉

Тесты запустятся и вы увидите результаты в консоли.

## Что вы увидите

```
=== Demo: H.264 Decoding with Simulator ===
Decoded frame 1: 1920x1080
Decoded frame 2: 1920x1080
...
Demo Results:
  Frames decoded: 100
  Average decode time: 40.00ms
  Average FPS: 25.00
```

## Если что-то не работает

1. **File → Invalidate Caches / Restart**
2. **Build → Rebuild Project**
3. Попробуйте снова

Подробная инструкция: `docs/INTELLIJ_IDEA_TESTING_GUIDE.md`
