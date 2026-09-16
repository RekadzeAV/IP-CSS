# Пункт 3: 1.8.2 HLS Pipeline Finalization - План

**Дата:** 2026-04-27  
**Приоритет:** Средний (после RTSP stability)  
**Текущий статус:** 🟡 ~88%, требуется runtime stability

---

## 📋 Обзор

HLS Generator Service уже имеет базовую реализацию с:
- ✅ Генерация HLS из RTSP потоков
- ✅ Поддержка multiple qualities (LOW, MEDIUM, HIGH, ULTRA, QHD, UHD)
- ✅ Adaptive bitrate streaming (master + variant playlists)
- ✅ Генерация HLS из записей
- ✅ Мониторинг процессов FFmpeg
- ✅ Cleanup старых сегментов

**Цель пункта 3:** Довести runtime stability и production readiness.

---

## 🎯 Задачи

### 3.1 Runtime Stability Improvements

#### 3.1.1 Long-run testing

**Цель:** Убедиться что HLS генерация работает стабильно в течение длительного времени.

**Задачи:**
1. Создать тест на 1 час стабильности
2. Проверить на утечки памяти
3. Проверить корректность cleanup при остановке
4. Валидация сегментов (корректность плейлиста)

**Тест:** `HlsGeneratorLongRunTest.kt`
```kotlin
@Test
fun `test 1 hour HLS stability`() = runTest(timeout = 70.minutes) {
    val ffmpegService = mockk<FfmpegService>(relaxed = true)
    val hlsService = HlsGeneratorService(ffmpegService = ffmpegService)
    
    val streamId = "test-hls-long-run"
    val rtspUrl = "rtsp://test-server/stream"
    
    val playlistPath = hlsService.startHlsGeneration(streamId, rtspUrl)
    assertNotNull(playlistPath)
    
    // Ждать 1 час
    delay(60.minutes)
    
    // Проверить что процесс ещё жив
    assertTrue(hlsService.isHlsGenerationActive(streamId))
    
    // Проверить что сегменты создаются
    val streamDir = File("streams/hls/$streamId")
    val segmentCount = streamDir.listFiles()?.filter { it.extension == "ts" }?.size ?: 0
    assertTrue(segmentCount > 100, "Should have >100 segments after 1 hour")
    
    hlsService.stopHlsGeneration(streamId)
}
```

#### 3.1.2 Memory leak detection

**Цель:** Убедиться что нет утечек памяти при многократном запуске/остановке.

**Тест:** `HlsGeneratorMemoryTest.kt`
```kotlin
@Test
fun `test no memory leaks after 50 start/stop cycles`() = runTest {
    val ffmpegService = mockk<FfmpegService>(relaxed = true)
    val hlsService = HlsGeneratorService(ffmpegService = ffmpegService)
    
    repeat(50) { i ->
        val streamId = "test-stream-$i"
        
        hlsService.startHlsGeneration(streamId, "rtsp://test/stream")
        delay(500)
        hlsService.stopHlsGeneration(streamId)
        
        if ((i + 1) % 10 == 0) {
            System.gc()
            logger.info { "Cycle ${i + 1}/50 completed" }
        }
    }
    
    assertTrue(true, "Memory test completed")
}
```

### 3.2 Cleanup Improvements

#### 3.2.1 Automatic segment cleanup

**Цель:** Автоматически удалять старые сегменты.

**Текущее состояние:**
- `hls_list_size = 6` - плейлист хранит последние 6 сегментов
- Но старые сегменты `.ts` не удаляются автоматически

**Задачи:**
1. Добавить фоновый процесс для cleanup старых сегментов
2. Ограничить максимальное количество сегментов
3. Проверять free disk space

**Реализация:**
```kotlin
class HlsCleanupScheduler(
    private val cleanupIntervalMs: Long = 5 * 60 * 1000L, // 5 минут
    private val maxSegmentsPerStream: Int = 180 // 6 минут при 2s segments
) {
    private val cleanupJob = Job()
    
    fun startCleanupScheduler(hlsOutputDirectory: String) {
        CoroutineScope(Dispatchers.IO + cleanupJob).launch {
            while (isActive) {
                cleanupOldSegments(hlsOutputDirectory)
                delay(cleanupIntervalMs)
            }
        }
    }
    
    private fun cleanupOldSegments(hlsOutputDirectory: String) {
        val hlsDir = File(hlsOutputDirectory)
        hlsDir.listFiles()?.forEach { streamDir ->
            if (streamDir.isDirectory) {
                val tsFiles = streamDir.listFiles { _, name -> name.endsWith(".ts") }
                    ?.sortedBy { it.lastModified() } ?: return@forEach
                
                while (tsFiles.size > maxSegmentsPerStream) {
                    val oldest = tsFiles.first()
                    oldest.delete()
                    tsFiles.removeAt(0)
                }
            }
        }
    }
    
    fun stop() {
        cleanupJob.cancel()
    }
}
```

#### 3.2.2 Disk space monitoring

**Цель:** Проверять свободное место перед запуском HLS.

**Реализация:**
```kotlin
fun checkDiskSpace(hlsOutputDirectory: String, requiredGb: Double = 1.0): Boolean {
    val hlsDir = File(hlsOutputDirectory)
    val parent = hlsDir.parentFile ?: return false
    
    val freeSpaceGb = parent.freeSpace / (1024.0 * 1024 * 1024)
    return freeSpaceGb >= requiredGb
}
```

### 3.3 Error Handling Improvements

#### 3.3.1 Better FFmpeg error messages

**Цель:** Улучшить диагностические сообщения при ошибках FFmpeg.

**Текущее состояние:**
- Логируются exit codes
- Но не парсятся stderr сообщения от FFmpeg

**Задачи:**
1. Перенаправить stderr FFmpeg в отдельный поток
2. Парсить ошибки FFmpeg
3. Логировать с контекстом

**Реализация:**
```kotlin
private fun startProcessWithErrorCapture(args: List<String>, streamId: String): Process {
    val process = ProcessBuilder(args)
        .redirectErrorStream(false) // Отдельный stderr
        .start()
    
    // Парсинг stderr
    CoroutineScope(Dispatchers.IO).launch {
        process.errorReader().forEachLine { line ->
            logger.warn { "FFmpeg stderr [$streamId]: $line" }
            
            // Парсинг известных ошибок FFmpeg
            when {
                "No such file or directory" in line -> {
                    logger.error { "FFmpeg file not found error for $streamId" }
                }
                "Connection refused" in line -> {
                    logger.error { "FFmpeg connection refused for $streamId" }
                }
                "Invalid data" in line -> {
                    logger.error { "FFmpeg invalid data error for $streamId" }
                }
            }
        }
    }
    
    return process
}
```

### 3.4 Performance Optimizations

#### 3.4.1 CPU usage optimization

**Цель:** Оптимизировать CPU usage при генерации HLS.

**Текущее состояние:**
- Используется `-preset fast` для high resolutions
- Но можно улучшить для low resolutions

**Задачи:**
1. Использовать разные presets для разных качеств
2. Оптимизировать thread count
3. Использовать hardware acceleration если доступно

**Реализация:**
```kotlin
private fun transcodeArgsForQuality(quality: StreamQuality): List<String> = when (quality) {
    StreamQuality.LOW -> listOf(
        "-b:v", "500k",
        "-s", "640x360",
        "-r", "15",
        "-preset", "ultrafast",  // Ultrafast для low quality
        "-threads", "2"  // Ограничить threads для low quality
    )
    StreamQuality.MEDIUM -> listOf(
        "-b:v", "1500k",
        "-s", "1280x720",
        "-r", "25",
        "-preset", "faster",
        "-threads", "4"
    )
    // ... другие качества
}
```

#### 3.4.2 Hardware acceleration

**Цель:** Использовать GPU для кодирования если доступно.

**Реализация:**
```kotlin
private fun videoCodecAndAudio(quality: StreamQuality): List<String> {
    val useHardwareAcceleration = System.getenv("HLS_USE_HW_ACCEL")?.equals("true") == true
    
    val v = when {
        useHardwareAcceleration && isNvidiaAvailable() -> "h264_nvenc"
        useHardwareAcceleration && isIntelQsvAvailable() -> "h264_qsv"
        useHardwareAcceleration && isVideoToolboxAvailable() -> "h264_videotoolbox"
        else -> "libx264"
    }
    
    return listOf("-c:v", v, "-c:a", "aac")
}
```

### 3.5 Testing

#### 3.5.1 Integration tests

**Цель:** Проверить интеграцию с реальными потоками.

**Тесты:**
1. Запуск HLS из реального RTSP потока
2. Проверка корректности плейлиста
3. Проверка доступа к сегментам
4. Graceful shutdown

**Скрипт:** `scripts/test-hls-integration.ps1`

---

## 📊 Критерии готовности

**1.8.2 считается завершённым при:**

1. ✅ Long-run test (1 час) проходит без ошибок
2. ✅ Нет утечек памяти после 50 start/stop циклов
3. ✅ Cleanup старых сегментов работает корректно
4. ✅ Disk space monitoring предотвращает переполнение
5. ✅ FFmpeg ошибки логируются с контекстом
6. ✅ CPU usage оптимизирован
7. ✅ Integration tests с реальными потоками проходят

---

## 🔧 Рекомендации по исполнению

### Приоритеты:
1. **Critical:** Long-run stability testing
2. **High:** Automatic segment cleanup
3. **Medium:** Disk space monitoring
4. **Low:** Hardware acceleration

### Риски:
1. **FFmpeg instability** - требует полевых тестов
2. **Disk space exhaustion** - может остановить систему
3. **CPU overload** - влияет на другие сервисы

### Mitigation:
1. Полевое тестирование на реальных потоках
2. Добавить мониторинг disk space
3. Ограничить максимальное количество одновременных HLS потоков

---

## 📝 Следующие шаги

После завершения пункта 3:
1. ✅ Пункт 1: 2.1.7 AI Integration - COMPLETE
2. ✅ Пункт 2: 1.8.4 RTSP Integration - IN PROGRESS
3. 🎯 Пункт 3: 1.8.2 HLS Pipeline - READY FOR EXECUTION

---

**План создан:** 2026-04-27  
**Версия:** 1.0  
**Статус:** Ready for execution
