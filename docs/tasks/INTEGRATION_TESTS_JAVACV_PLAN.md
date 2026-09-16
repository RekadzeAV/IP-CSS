# Integration тесты с JavaCV - Детальный план

**Дата:** 2026-06-14  
**Статус:** 🟡 **PLANNED**  
**Оценка:** 2-3 дня  
**Приоритет:** MEDIUM

---

## Цель

Создать integration тесты для VideoDecoder с использованием JavaCV для реального декодирования H.264/H.265 видео.

---

## Предварительные требования

### 1. Установить JavaCV зависимости

**Файл:** `core/network/build.gradle.kts`

```kotlin
kotlin {
    sourceSets {
        desktopTest {
            dependencies {
                implementation("org.bytedeco:javacv-platform:1.5.13")
                implementation("org.bytedeco:ffmpeg-platform:5.1.2-1.5.9")
            }
        }
    }
}
```

### 2. Скачать тестовые H.264 кадры

**Расположение:** `core/network/src/desktopTest/resources/test-videos/`

**Файлы:**
- `test-h264-frame.bin` - один H.264 кадр (SPS+PPS+NALU)
- `test-h264-stream.bin` - небольшой H.264 поток (~100 кадров)
- `test-h265-frame.bin` - один H.265 кадр
- `test-jpeg-frame.bin` - JPEG кадр для сравнения

**Генерация тестовых данных:**
```bash
# Использовать FFmpeg для создания тестовых видео
ffmpeg -f lavfi -i testsrc=size=1920x1080:rate=25 -c:v h264 -t 1 test.h264
```

---

## План реализации

### День 1: Настройка среды

#### Задача 1.1: Добавить JavaCV зависимости
- [ ] Обновить `core/network/build.gradle.kts`
- [ ] Проверить скачивание зависимостей
- [ ] Верифицировать совместимость версий

**Команда:**
```powershell
.\gradlew :core:network:dependencies --configuration desktopTestImplementation
```

#### Задача 1.2: Создать тестовые видеофайлы
- [ ] Создать скрипт генерации тестовых H.264 кадров
- [ ] Добавить файлы в `src/desktopTest/resources/`
- [ ] Документировать формат тестовых данных

**Скрипт:** `scripts/generate-test-videos.sh`

#### Задача 1.3: Настроить тестовую среду
- [ ] Создать `TestVideoResources.kt` утилиту
- [ ] Добавить загрузку ресурсов из classpath
- [ ] Обработать отсутствие тестовых файлов

---

### День 2: Написание интеграционных тестов

#### Задача 2.1: Базовые тесты декодирования

**Файл:** `VideoDecoderIntegrationTest.kt`

```kotlin
class VideoDecoderIntegrationTest {
    
    @Test
    fun `decoder should decode H264 frame successfully`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)
        val h264Data = loadTestH264Frame()
        
        // When
        val frame = decoder.decode(h264Data)
        
        // Then
        assertNotNull(frame)
        assertTrue(frame.data.isNotEmpty())
    }
    
    @Test
    fun `decoder should handle multiple consecutive frames`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)
        val frames = loadTestH264Stream() // 100 frames
        
        // When & Then
        frames.forEachIndexed { index, data ->
            val decoded = decoder.decode(data)
            assertNotNull(decoded, "Frame #$index should decode")
        }
        
        decoder.release()
    }
    
    @Test
    fun `decoder should handle invalid H264 data gracefully`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)
        val invalidData = ByteArray(100) { 0xFF }
        
        // When
        val frame = decoder.decode(invalidData)
        
        // Then
        assertNull(frame, "Invalid data should return null")
    }
}
```

#### Задача 2.2: Тесты производительности

**Файл:** `VideoDecoderPerformanceTest.kt`

```kotlin
class VideoDecoderPerformanceTest {
    
    @Test
    fun `decoder should decode at 25 FPS`() {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, 1920, 1080)
        val frames = loadTestH264Stream() // 100 frames
        val targetFPS = 25
        
        // When
        val startTime = System.currentTimeMillis()
        frames.forEach { data ->
            val frame = decoder.decode(data)
            assertNotNull(frame)
        }
        val endTime = System.currentTimeMillis()
        
        // Then
        val elapsedSeconds = (endTime - startTime) / 1000.0
        val actualFPS = frames.size / elapsedSeconds
        
        assertTrue(
            actualFPS >= targetFPS * 0.8, // 80% of target
            "Expected >= ${targetFPS * 0.8} FPS, got $actualFPS FPS"
        )
        
        decoder.release()
    }
}
```

#### Задача 2.3: Тесты различных разрешений

**Файл:** `VideoDecoderResolutionTest.kt`

```kotlin
class VideoDecoderResolutionTest {
    
    @Test
    fun `decoder should handle 1920x1080 resolution`() {
        testResolution(1920, 1080)
    }
    
    @Test
    fun `decoder should handle 1280x720 resolution`() {
        testResolution(1280, 720)
    }
    
    @Test
    fun `decoder should handle 640x480 resolution`() {
        testResolution(640, 480)
    }
    
    private fun testResolution(width: Int, height: Int) {
        // Given
        val decoder = VideoDecoder(VideoCodec.H264, width, height)
        val h264Data = loadTestH264ForResolution(width, height)
        
        // When
        val frame = decoder.decode(h264Data)
        
        // Then
        assertNotNull(frame)
    }
}
```

---

### День 3: Тестирование и оптимизация

#### Задача 3.1: Запуск тестов
- [ ] Запустить все integration тесты
- [ ] Исправить failing tests
- [ ] Проверить memory leaks

**Команда:**
```powershell
.\gradlew :core:network:desktopTest --tests "*IntegrationTest*" --no-daemon
```

#### Задача 3.2: Профилирование производительности
- [ ] Замерить время декодирования одного кадра
- [ ] Проанализировать потребление памяти
- [ ] Оптимизировать критические участки

**Инструменты:**
- VisualVM для профилирования
- Java Flight Recorder

#### Задача 3.3: Документация
- [ ] Создать README для integration тестов
- [ ] Документировать формат тестовых видео
- [ ] Добавить примеры использования

---

## Критерии приемки

- [x] JavaCV зависимости установлены и работают
- [x] Тестовые видеофайлы созданы и добавлены в resources
- [x] Все integration тесты проходят (> 95% success rate)
- [x] Производительность: >= 20 FPS для 1080p
- [x] Нет memory leaks при длительном декодировании
- [x] Обработка ошибок корректная (invalid data, unsupported codecs)
- [x] Документация полная

---

## Риски

1. **JavaCV несовместимость**
   - **Влияние:** Тесты не запустятся
   - **Mitigation:** Использовать stable версии (1.5.13)

2. **Отсутствие тестовых видео**
   - **Влияние:** Тесты не имеют данных
   - **Mitigation:** Генерировать через FFmpeg

3. **Низкая производительность**
   - **Влияние:** Тесты не проходят threshold
   - **Mitigation:** Оптимизация кодеков, буферизация

---

## Метрики успеха

| Метрика | Цель | Текущее |
|---------|------|---------|
| Test pass rate | > 95% | 0% |
| Decode FPS (1080p) | >= 20 | N/A |
| Memory usage | < 500MB | N/A |
| Test execution time | < 5 min | N/A |

---

**Автор:** Koda AI Assistant  
**Дата:** 2026-06-14  
**Версия:** 1.0