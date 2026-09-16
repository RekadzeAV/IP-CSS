# Ручное выполнение тестов VideoDecoder

## Проблема с Gradle задачами

В Kotlin Multiplatform проектах тесты могут требовать специальной конфигурации. Если автоматический запуск через Gradle не работает, используйте следующие методы:

## Метод 1: IntelliJ IDEA (Рекомендуется)

### Шаги:
1. Откройте проект в IntelliJ IDEA
2. Перейдите к файлу теста:
   - `core/network/src/jvmTest/kotlin/com/company/ipcamera/core/network/video/VideoDecoderDemoTest.kt`
3. Правой кнопкой мыши на классе или методе теста
4. Выберите "Run 'VideoDecoderDemoTest'" или "Run 'testMethodName'"

### Преимущества:
- Автоматическая настройка classpath
- Удобный просмотр результатов
- Отладка тестов

## Метод 2: Настройка переменных окружения вручную

### Windows PowerShell:
```powershell
$env:TEST_CAMERA_H264_URL="rtsp://camera-ip:554/stream"
$env:TEST_CAMERA_H265_URL="rtsp://camera-ip:554/stream"
$env:TEST_CAMERA_MJPEG_URL="rtsp://camera-ip:554/stream"
$env:TEST_CAMERA_USERNAME="admin"
$env:TEST_CAMERA_PASSWORD="password"
```

### Linux/macOS:
```bash
export TEST_CAMERA_H264_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_H265_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_MJPEG_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_USERNAME="admin"
export TEST_CAMERA_PASSWORD="password"
```

## Метод 3: Создание простого main класса

Создайте файл `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/video/VideoDecoderDemoMain.kt`:

```kotlin
package com.company.ipcamera.core.network.video

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("=== VideoDecoder Demo ===")

    // Демо-тест декодирования
    val codec = VideoCodec.H264
    val width = 1920
    val height = 1080

    val decoder = VideoDecoder(codec, width, height)
    val simulator = RtspStreamSimulator(codec, width, height, fps = 25)

    var framesDecoded = 0

    decoder.setCallback { decodedFrame ->
        framesDecoded++
        println("Decoded frame $framesDecoded: ${decodedFrame.width}x${decodedFrame.height}")
    }

    // Генерируем и декодируем 10 кадров
    simulator.generateFrames(10).collect { frame ->
        decoder.decode(frame)
    }

    decoder.release()
    println("Demo complete: $framesDecoded frames decoded")
}
```

Запуск:
```bash
.\gradlew.bat :core:network:run
```

## Метод 4: Использование JUnit Console Launcher

Если JUnit установлен:

```bash
java -jar junit-platform-console-standalone-1.9.3.jar \
  --class-path build/classes/kotlin/jvm/test \
  --select-class com.company.ipcamera.core.network.video.VideoDecoderDemoTest
```

## Проверка зависимостей

Убедитесь, что все зависимости установлены:

```bash
.\gradlew.bat :core:network:dependencies --configuration desktopRuntimeClasspath
```

Проверьте наличие:
- `org.bytedeco:javacv:1.5.9`
- `org.bytedeco:ffmpeg-platform:6.0-1.5.9`

## Альтернатива: Использование существующих тестов

В проекте уже есть тесты в `commonTest`. Попробуйте запустить их:

```bash
.\gradlew.bat :core:network:allTests
```

Или найдите существующие тесты VideoDecoder и запустите их через IntelliJ IDEA.

## Рекомендации

1. **Для разработки**: Используйте IntelliJ IDEA для запуска тестов
2. **Для CI/CD**: Настройте Gradle задачи для тестов
3. **Для демонстрации**: Используйте простой main класс

## Следующие шаги

После успешного запуска тестов:
1. Проанализируйте результаты
2. Заполните отчет используя `docs/TESTING_RESULTS_EXAMPLE.md`
3. Примените оптимизации из `docs/OPTIMIZATION_RECOMMENDATIONS.md`
