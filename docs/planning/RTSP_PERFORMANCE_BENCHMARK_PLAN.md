# RTSP Performance Benchmark Plan

**Дата:** 2026-04-27  
**Статус:** 🟡 STARTED  
**Оценка:** 2 недели

---

## 📊 Цели

1. Измерить производительность RTSP подключения
2. Сравнить hardware vs software декодирование
3. Оптимизировать FPS, CPU, Memory usage
4. Протестировать стабильность long-run (24+ часа)
5. Определить bottleneck в pipeline

---

## 🎯 Метрики

### Primary Metrics:
- **FPS (Frames Per Second)** - целевое значение: ≥25 fps
- **Latency** - целевое значение: ≤200ms
- **CPU Usage** - целевое значение: ≤30% per stream
- **Memory Usage** - целевое значение: ≤200MB per stream
- **Bandwidth** - измерение потребления сети

### Secondary Metrics:
- **Jitter** - вариация latency
- **Packet Loss** - процент потерянных пакетов
- **Reconnection Time** - время восстановления после разрыва
- **Startup Time** - время до первого кадра

---

## 🧪 Test Scenarios

### Scenario 1: Single Stream Baseline
**Цель:** Базовая производительность одного потока

**Конфигурация:**
- 1 камера, 1080p, 30fps, H.264
- Hardware decoding: DISABLED
- Buffer size: 1000ms

**Метрики:**
- FPS (target: 30)
- CPU (target: ≤10%)
- Memory (target: ≤100MB)
- Latency (target: ≤150ms)

**Время:** 10 минут

---

### Scenario 2: Hardware Acceleration Comparison
**Цель:** Сравнить hardware vs software декодирование

**Конфигурация:**
- 1 камера, 1080p, 30fps, H.264
- Hardware decoding: ENABLED vs DISABLED
- GPU: NVENC (NVIDIA), QSV (Intel), VideoToolbox (Apple)

**Метрики:**
- CPU usage comparison
- FPS comparison
- Memory usage comparison
- Power consumption (ноутбук)

**Время:** 10 минут на каждую конфигурацию

---

### Scenario 3: Multi-Camera Load
**Цель:** Измерить масштабирование с несколькими камерами

**Конфигурация:**
- 1, 2, 4, 8, 16 камер одновременно
- 1080p, 30fps, H.264
- Hardware decoding: ENABLED

**Метрики:**
- Total CPU usage
- Total Memory usage
- FPS per camera
- Total bandwidth

**Время:** 5 минут на каждую конфигурацию

---

### Scenario 4: Long-Run Stability
**Цель:** Проверить стабильность при длительной работе

**Конфигурация:**
- 4 камеры, 1080p, 30fps
- Hardware decoding: ENABLED
- Continuous recording

**Метрики:**
- Memory leak detection
- FPS stability over time
- Reconnection events
- Error rate

**Время:** 24 часа

---

### Scenario 5: Network Conditions
**Цель:** Тестирование при различных условиях сети

**Конфигурация:**
- 1 камера, 1080p, 30fps
- Network simulation:
  - Perfect: 0% packet loss, <10ms latency
  - Good: 0.1% packet loss, 50ms latency
  - Poor: 1% packet loss, 100ms latency
  - Bad: 5% packet loss, 200ms latency

**Метрики:**
- FPS under stress
- Reconnection frequency
- Buffer underrun events
- Quality degradation

**Время:** 5 минут на каждую сеть

---

### Scenario 6: Resolution Scaling
**Цель:** Влияние разрешения на производительность

**Конфигурация:**
- Разные разрешения: 720p, 1080p, 2K, 4K
- Hardware decoding: ENABLED
- 1 камера

**Метрики:**
- CPU usage vs resolution
- Memory usage vs resolution
- Max achievable FPS
- Bandwidth consumption

**Время:** 5 минут на каждое разрешение

---

## 🛠️ Инструменты

### 1. FFmpeg для генерации тестовых потоков:
```bash
# Генерация тестового H.264 потока
ffmpeg -re -f lavfi -i testsrc=duration=3600:size=1920x1080:rate=30 \
  -c:v libx264 -preset ultrafast -tune zerolatency \
  -f rtsp rtsp://localhost:8554/test
```

### 2. Wireshark для анализа сети:
```bash
# Capture RTSP traffic
tshark -i any -f "port 554" -w rtsp_capture.pcap
```

### 3. Performance Monitoring:
- **Linux:** `top`, `htop`, `vmstat`, `iostat`
- **Windows:** Task Manager, Performance Monitor
- **Cross-platform:** `psutil` (Python)

### 4. Custom Benchmark Tool:
```kotlin
// Создадим в проекте
class RtspBenchmarkTool {
    suspend fun runBenchmark(config: BenchmarkConfig)
    fun measureFps(): Double
    fun measureCpuUsage(): Double
    fun measureMemoryUsage(): Long
    fun measureLatency(): Long
}
```

---

## 📁 Структура тестов

```
core/network/src/jvmTest/kotlin/com/company/ipcamera/core/network/rtsp/
├── RtspBenchmarkConfig.kt
├── RtspBenchmarkRunner.kt
├── RtspPerformanceTest.kt
├── RtspHardwareAccelerationTest.kt
├── RtspMultiStreamTest.kt
├── RtspLongRunStabilityTest.kt
├── RtspNetworkStressTest.kt
└── RtspResolutionScalingTest.kt
```

---

## 📋 Benchmark Configuration

### RtspBenchmarkConfig.kt:
```kotlin
data class RtspBenchmarkConfig(
    val rtspUrl: String,
    val duration: Duration = 10.minutes,
    val hardwareDecoding: Boolean = true,
    val bufferSize: Long = 1000, // ms
    val resolution: VideoResolution = VideoResolution.FHD,
    val expectedFps: Int = 30,
    val metricsCollectionInterval: Duration = 1.seconds
)

enum class VideoResolution {
    HD, FHD, QHD, UHD
}
```

---

## 📊 Сбор метрик

### MetricsCollector.kt:
```kotlin
class MetricsCollector {
    suspend fun collect(): BenchmarkMetrics
    
    data class BenchmarkMetrics(
        val timestamp: Long,
        val fps: Double,
        val cpuUsage: Double,
        val memoryUsageBytes: Long,
        val latencyMs: Long,
        val droppedFrames: Long,
        val reconnections: Long,
        val errors: List<String>
    )
}
```

### Визуализация:
```kotlin
// Генерация отчета
class BenchmarkReportGenerator {
    fun generateHtmlReport(metrics: List<BenchmarkMetrics>): String
    fun generateChart(metrics: List<BenchmarkMetrics>, metric: String): String
}
```

---

## 🎯 Критерии успеха

### PASS Criteria:
1. ✅ Single stream 1080p: FPS ≥ 25, CPU ≤ 10%, Memory ≤ 100MB
2. ✅ Hardware decoding: CPU reduction ≥ 50% vs software
3. ✅ 8 streams: Total CPU ≤ 60%, Memory ≤ 800MB
4. ✅ 24h stability: No memory leaks, FPS variance ≤ 5%
5. ✅ Network stress: Graceful degradation, auto-reconnection
6. ✅ 4K support: FPS ≥ 20, CPU ≤ 40%

### FAIL Criteria:
1. ❌ Memory leak > 10MB/hour
2. ❌ FPS drops below 15 consistently
3. ❌ CPU usage > 80% for single stream
4. ❌ Latency > 500ms consistently
5. ❌ More than 5 reconnections per hour

---

## 📅 План выполнения

### Week 1:
- **Day 1-2:** Setup benchmark infrastructure
  - Создать RtspBenchmarkTool
  - Настроить метрики collection
  - Создать тестовые RTSP серверы

- **Day 3-4:** Run baseline tests
  - Scenario 1: Single Stream
  - Scenario 2: Hardware vs Software
  - Scenario 6: Resolution Scaling

- **Day 5:** Analysis and initial optimization

### Week 2:
- **Day 1-2:** Run stress tests
  - Scenario 3: Multi-Camera Load
  - Scenario 5: Network Conditions

- **Day 3-4:** Long-run stability test
  - Scenario 4: 24-hour test
  - Continuous monitoring

- **Day 5:** Final analysis and report

---

## 📊 Ожидаемые результаты

### Hypothesis:
1. **Hardware decoding:** 60-80% CPU reduction
2. **Multi-stream scaling:** Linear up to 8 streams, then diminishing returns
3. **4K support:** 2x CPU usage vs 1080p
4. **Memory:** Stable around 50-100MB per stream
5. **Latency:** 100-200ms with default buffer

---

## 🔧 Оптимизации (если нужно)

### CPU Optimization:
- [ ] Tune FFmpeg codec parameters
- [ ] Optimize buffer sizes
- [ ] Enable multi-threaded decoding
- [ ] Use GPU memory for frames

### Memory Optimization:
- [ ] Frame pooling
- [ ] Reduce buffer sizes
- [ ] Early frame disposal
- [ ] GC tuning

### Network Optimization:
- [ ] UDP vs TCP comparison
- [ ] Adaptive bitrate
- [ ] Packet loss recovery
- [ ] Jitter buffer optimization

---

## 📄 Отчет

### Формат отчета:
```markdown
# RTSP Performance Benchmark Report

**Дата:** 2026-04-XX
**Environment:** Windows 11, Intel i7, 32GB RAM, NVIDIA RTX 3070

## Summary
- Total Tests: 6
- Passed: X
- Failed: Y
- Average FPS: Z

## Detailed Results
### Scenario 1: Single Stream Baseline
- FPS: 29.8 (target: 30) ✅
- CPU: 8% (target: ≤10%) ✅
- Memory: 95MB (target: ≤100MB) ✅
- Latency: 120ms (target: ≤150ms) ✅

...

## Recommendations
1. Enable hardware decoding by default
2. Optimize buffer size for 4K streams
3. Add connection pooling for multi-camera
```

---

## ✅ Checklist

- [ ] Создать RtspBenchmarkConfig
- [ ] Создать RtspBenchmarkRunner
- [ ] Создать MetricsCollector
- [ ] Создать BenchmarkReportGenerator
- [ ] Настроить тестовые RTSP серверы
- [ ] Run Scenario 1: Single Stream
- [ ] Run Scenario 2: Hardware vs Software
- [ ] Run Scenario 3: Multi-Camera
- [ ] Run Scenario 4: Long-Run Stability
- [ ] Run Scenario 5: Network Conditions
- [ ] Run Scenario 6: Resolution Scaling
- [ ] Generate final report
- [ ] Apply optimizations

---

**Status:** 🟡 STARTED  
**Next:** Создать инфраструктуру для бенчмарков  
**ETA:** 2026-05-11
