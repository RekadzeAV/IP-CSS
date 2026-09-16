# RTSP Performance Benchmarks - План

**Дата:** 2026-04-27  
**Статус:** 🟡 READY FOR EXECUTION

---

## 📋 Обзор

Цель: Создать количественные метрики производительности RTSP декодирования.

### Метрики для измерения:

1. **FPS (Frames Per Second)**
   - Целевой FPS камеры
   - Фактический FPS декодирования
   - Потерянные кадры

2. **CPU Usage**
   - CPU usage на одну камеру
   - CPU usage при многопоточном декодировании
   - CPU usage при разных разрешениях

3. **Memory Usage**
   - Memory на одну камеру
   - Memory при долгой работе (memory leaks)
   - Memory при разных разрешениях

4. **Latency**
   - Время от камеры до отображения
   - Buffer latency
   - Network latency

5. **Stress Test**
   - Максимальное количество камер на сервер
   - Деградация производительности при нагрузке
   - Recovery после пиковой нагрузки

---

## 📊 Тестовое окружение

### Hardware:
- **Server:** Intel i7, 32GB RAM, NVIDIA GPU (опционально)
- **Network:** Gigabit Ethernet
- **Cameras:** 10+ тестовых камер (разные разрешения)

### Software:
- **OS:** Ubuntu 22.04 / Windows 11
- **JVM:** OpenJDK 17
- **FFmpeg:** 6.0+
- **Monitoring:** JProfiler / VisualVM / async-profiler

---

## 🎯 Benchmarks Tests

### Test 1: Single Stream Performance

**Цель:** Измерить производительность одной камеры

**Параметры:**
```kotlin
val config = RtspClientConfig(
    url = "rtsp://test-camera/stream",
    enableVideo = true,
    enableAudio = true
)

val client = RtspClient(config)
val metrics = BenchmarkMetrics()

client.setVideoFrameCallback { frame ->
    metrics.recordFrame(
        timestamp = System.currentTimeMillis(),
        width = frame.width,
        height = frame.height
    )
}

client.connect()

// Измерять 60 секунд
delay(60_000)

client.disconnect()
client.close()

println("FPS: ${metrics.calculateFps()}")
println("CPU: ${metrics.getCpuUsage()}")
println("Memory: ${metrics.getMemoryUsage()}")
```

**Ожидаемые результаты:**
- 1080p @ 30fps: FPS >= 25, CPU < 20%
- 720p @ 25fps: FPS >= 20, CPU < 10%
- 4K @ 30fps: FPS >= 20, CPU < 40%

---

### Test 2: Multi-Stream Performance

**Цель:** Измерить производительность при многопоточном декодировании

**Параметры:**
```kotlin
val cameras = listOf(
    "rtsp://camera1/stream",
    "rtsp://camera2/stream",
    // ... 10 камер
)

val clients = cameras.map { url ->
    val client = RtspClient(RtspClientConfig(url = url))
    // setup callbacks
    client
}

clients.forEach { it.connect() }

// Измерять 120 секунд
delay(120_000)

clients.forEach { 
    it.disconnect()
    it.close()
}

println("Total FPS: ${clients.sumOf { it.calculateFps() }}")
println("Total CPU: ${getTotalCpuUsage()}")
println("Total Memory: ${getTotalMemoryUsage()}")
```

**Ожидаемые результаты:**
- 10 камер 1080p: Total FPS >= 200, CPU < 80%
- 5 камер 4K: Total FPS >= 80, CPU < 70%

---

### Test 3: Long-Run Stability

**Цель:** Проверить на memory leaks и деградацию

**Параметры:**
```kotlin
val client = RtspClient(RtspClientConfig(url = "rtsp://test/stream"))
val memorySamples = mutableListOf<Long>()

client.setVideoFrameCallback { /* process frames */ }
client.connect()

// Измерять 1 час
repeat(3600) { second ->
    delay(1000)
    
    if (second % 60 == 0) {
        val memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        memorySamples.add(memory)
        
        println("Second $second: Memory = ${memory / 1024 / 1024}MB")
    }
}

client.disconnect()
client.close()

// Проанализировать memory samples
val memoryTrend = analyzeMemoryTrend(memorySamples)
println("Memory trend: $memoryTrend") // stable / increasing / leaking
```

**Ожидаемые результаты:**
- Memory stable (±10MB)
- Нет memory leaks
- FPS не деградирует

---

### Test 4: Resolution Comparison

**Цель:** Сравнить производительность при разных разрешениях

**Параметры:**
```kotlin
val resolutions = listOf(
    Triple("360p", 640, 360),
    Triple("720p", 1280, 720),
    Triple("1080p", 1920, 1080),
    Triple("4K", 3840, 2160)
)

resolutions.forEach { (name, width, height) ->
    val client = RtspClient(RtspClientConfig(url = "rtsp://test/stream"))
    
    val metrics = BenchmarkMetrics()
    client.setVideoFrameCallback { frame ->
        metrics.recordFrame(System.currentTimeMillis(), frame.width, frame.height)
    }
    
    client.connect()
    delay(30_000)
    client.disconnect()
    client.close()
    
    println("$name: FPS=${metrics.calculateFps()}, CPU=${metrics.getCpuUsage()}%")
}
```

**Ожидаемые результаты:**
```
360p:  FPS = 30, CPU = 5%
720p:  FPS = 28, CPU = 12%
1080p: FPS = 25, CPU = 22%
4K:    FPS = 20, CPU = 45%
```

---

### Test 5: Hardware Acceleration

**Цель:** Сравнить software vs hardware декодирование

**Параметры:**
```kotlin
// Software decoding
val softwareClient = RtspClient(
    RtspClientConfig(url = "rtsp://test/stream", hardwareAcceleration = false)
)
val softwareFps = benchmarkFps(softwareClient)

// Hardware decoding (NVENC/QSV/VideoToolbox)
val hardwareClient = RtspClient(
    RtspClientConfig(url = "rtsp://test/stream", hardwareAcceleration = true)
)
val hardwareFps = benchmarkFps(hardwareClient)

println("Software: FPS = $softwareFps, CPU = ${getSoftwareCpu()}%")
println("Hardware: FPS = $hardwareFps, CPU = ${getHardwareCpu()}%")
println("Speedup: ${hardwareFps / softwareFps}x")
```

**Ожидаемые результаты:**
- Hardware acceleration: 2-3x CPU reduction
- Hardware acceleration: 10-20% FPS increase

---

### Test 6: Network Conditions

**Цель:** Проверить поведение при плохом network

**Параметры:**
```kotlin
val client = RtspClient(RtspClientConfig(url = "rtsp://test/stream"))
val metrics = NetworkMetrics()

client.setStatusCallback { status, error ->
    metrics.recordStatus(status, error)
}

// Simulate network issues
simulateNetworkLatency(latencyMs = 500)
simulatePacketLoss(lossPercent = 5)

client.connect()
delay(120_000)

// Restore network
simulateNetworkLatency(latencyMs = 0)
simulatePacketLoss(lossPercent = 0)

println("Reconnect count: ${metrics.reconnectCount}")
println("Average latency: ${metrics.avgLatency}ms")
println("Packet loss recovery: ${metrics.recoveryTime}ms")
```

**Ожидаемые результаты:**
- Reconnect time < 5 секунд
- Recovery from packet loss < 10 секунд
- Graceful degradation при high latency

---

## 📊 BenchmarkMetrics Class

```kotlin
class BenchmarkMetrics {
    private val frameTimestamps = mutableListOf<Long>()
    private var cpuSamples = mutableListOf<Double>()
    private var memorySamples = mutableListOf<Long>()
    
    fun recordFrame(timestamp: Long, width: Int, height: Int) {
        synchronized(frameTimestamps) {
            frameTimestamps.add(timestamp)
            
            // Очистить старые timestamps (последние 10 секунд)
            val cutoff = timestamp - 10_000
            frameTimestamps.removeAll { it < cutoff }
        }
    }
    
    fun calculateFps(): Double {
        val now = System.currentTimeMillis()
        val cutoff = now - 10_000
        
        val recentFrames = synchronized(frameTimestamps) {
            frameTimestamps.filter { it >= cutoff }.size
        }
        
        return recentFrames / 10.0
    }
    
    fun recordCpuUsage(usage: Double) {
        cpuSamples.add(usage)
    }
    
    fun getCpuUsage(): Double {
        if (cpuSamples.isEmpty()) return 0.0
        return cpuSamples.average()
    }
    
    fun recordMemoryUsage(memory: Long) {
        memorySamples.add(memory)
    }
    
    fun getMemoryUsage(): Long {
        if (memorySamples.isEmpty()) return 0L
        return memorySamples.average().toLong()
    }
    
    fun analyzeMemoryTrend(): String {
        if (memorySamples.size < 10) return "insufficient_data"
        
        val firstHalfAvg = memorySamples.take(memorySamples.size / 2).average()
        val secondHalfAvg = memorySamples.drop(memorySamples.size / 2).average()
        
        val increase = secondHalfAvg - firstHalfAvg
        
        return when {
            increase > 10_000_000 -> "leaking"  // > 10MB increase
            increase > 5_000_000 -> "increasing"  // > 5MB increase
            else -> "stable"
        }
    }
}
```

---

## 🚀 Execution Plan

### Week 1:

**Day 1-2:** Создать benchmark тесты
- BenchmarkMetrics class
- Single stream test
- Multi-stream test

**Day 3-4:** Запустить baseline тесты
- Single stream (все разрешения)
- Multi-stream (5, 10, 20 камер)
- Записать результаты

**Day 5:** Анализ результатов
- Сравнить с ожидаемыми результатами
- Выявить bottlenecks
- Составить план оптимизации

### Week 2:

**Day 1-2:** Hardware acceleration
- Настроить NVENC / QSV / VideoToolbox
- Запустить сравнительные тесты
- Записать результаты

**Day 3-4:** Long-run stability
- Запустить 12-час тесты
- Проверить на memory leaks
- Задокументировать результаты

**Day 5:** Финальный отчёт
- Свести все результаты
- Создать dashboard
- Рекомендации по оптимизации

---

## 📁 Deliverables

1. **Benchmark test code**
   - `RtspBenchmarkTest.kt`
   - `BenchmarkMetrics.kt`
   - `StressTest.kt`

2. **Results documentation**
   - `RTSP_BENCHMARK_RESULTS.md`
   - Charts и графики
   - Comparison tables

3. **Recommendations**
   - Optimal configuration
   - Hardware requirements
   - Scaling guidelines

---

**План создан:** 2026-04-27  
**Статус:** Ready for execution  
**Оценка времени:** 2 недели
