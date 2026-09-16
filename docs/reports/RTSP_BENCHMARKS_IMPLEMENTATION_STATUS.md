# RTSP Performance Benchmarks - Implementation Status

**Дата:** 2026-04-27  
**Статус:** 🟡 60% COMPLETE

---

## ✅ Выполненные задачи

### 1. План бенчмарков - COMPLETE
**Файл:** `docs/planning/RTSP_PERFORMANCE_BENCHMARK_PLAN.md`

**Содержит:**
- 6 тестовых сценариев
- Детальные метрики (FPS, CPU, Memory, Latency)
- Критерии прохождения тестов
- План на 2 недели

**Сценарии:**
1. Single Stream Baseline
2. Hardware Acceleration Comparison
3. Multi-Camera Load
4. Long-Run Stability (24h)
5. Network Conditions
6. Resolution Scaling

---

### 2. Базовая структура данных - COMPLETE
**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/rtsp/RtspBenchmarkConfig.kt`

**Классы:**
- `RtspBenchmarkConfig` - конфигурация теста
- `VideoResolution` - enum разрешений (HD, FHD, QHD, UHD)
- `BenchmarkMetrics` - метрики в момент времени
- `BenchmarkSummary` - сводка результатов
- `BenchmarkResult` - полный результат теста
- `PassCriteria` - критерии прохождения
- `BenchmarkReportGenerator` - HTML генератор отчетов

**Пример использования:**
```kotlin
val config = RtspBenchmarkConfig(
    rtspUrl = "rtsp://camera.local/stream",
    duration = 10.minutes,
    hardwareDecoding = true,
    resolution = VideoResolution.FHD,
    expectedFps = 30
)
```

---

### 3. JVM системные метрики - COMPLETE

**Файл:** `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/rtsp/RtspBenchmarkRunner.jvm.kt`
```kotlin
actual class DefaultCpuUsageCalculator : CpuUsageCalculator {
    actual override fun getCurrentCpuUsage(): Double {
        // Использование ManagementFactory для получения CPU usage
    }
}
```

**Файл:** `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/rtsp/RtspBenchmarkRunner.memory.jvm.kt`
```kotlin
actual class DefaultMemoryUsageCalculator : MemoryUsageCalculator {
    actual override fun getCurrentMemoryUsage(): Long {
        // Использование MemoryMXBean для получения памяти
    }
}
```

---

### 4. Unit тесты - COMPLETE
**Файл:** `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/rtsp/RtspBenchmarkConfigTest.kt`

**Тесты:**
- `test default config values`
- `test custom config values`
- `test video resolution enum`
- `test HTML report generation`
- `test HTML report with failures`

---

## 📋 Ожидаемая архитектура

### Компоненты:

```
RtspBenchmarkRunner
├── RtspBenchmarkConfig (config)
├── CpuUsageCalculator (interface)
│   └── DefaultCpuUsageCalculator (JVM)
├── MemoryUsageCalculator (interface)
│   └── DefaultMemoryUsageCalculator (JVM)
├── BenchmarkReportGenerator
└── BenchmarkResult
```

### Интеграция:

```
RtspClient + VideoDecoder
         ↓
    Benchmark Runner
         ↓
   Metrics Collection
         ↓
  Summary Generation
         ↓
    HTML Report
```

---

## 🎯 Метрики

### Primary Metrics:
| Метрика | Target | Описание |
|---------|--------|----------|
| FPS | ≥25 fps | Frames per second |
| CPU | ≤30% | CPU usage per stream |
| Memory | ≤200MB | Memory usage per stream |
| Latency | ≤200ms | Time to first frame |

### Secondary Metrics:
- Jitter
- Packet Loss
- Reconnection Time
- Startup Time
- Dropped Frames

---

## 📊 Пример отчета

```html
<html>
<head><title>RTSP Benchmark Report</title></head>
<body>
<h1>RTSP Benchmark Report</h1>
<p><strong>RTSP URL:</strong> rtsp://camera.local/stream</p>
<p><strong>Duration:</strong> 10 minutes</p>
<p><strong>Hardware Decoding:</strong> true</p>

<div class='summary'>
<h2>Summary</h2>
<div class='metric'><strong>Avg FPS:</strong> <span class='pass'>29.8</span></div>
<div class='metric'><strong>Avg CPU:</strong> 8.5%</div>
<div class='metric'><strong>Avg Memory:</strong> 95.2 MB</div>
<div class='metric'><strong>Pass Criteria:</strong> <span class='pass'>PASS</span></div>
</div>
</body>
</html>
```

---

## ⏳ Следующие шаги

### Immediate (эта неделя):
1. ⏳ Создать интеграцию с RtspClient
2. ⏳ Создать интеграцию с VideoDecoder
3. ⏳ Запустить baseline тест на тестовой камере

### Short-term (следующая неделя):
4. ⏳ Hardware Acceleration comparison
5. ⏳ Multi-camera load testing
6. ⏳ Generate HTML reports

### Long-term (2-3 неделя):
7. ⏳ Long-run stability test (24h)
8. ⏳ Network stress testing
9. ⏳ Final optimization

---

## 📁 Файлы

### Созданные (5 файлов):
1. `docs/planning/RTSP_PERFORMANCE_BENCHMARK_PLAN.md`
2. `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/rtsp/RtspBenchmarkConfig.kt`
3. `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/rtsp/RtspBenchmarkRunner.jvm.kt`
4. `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/rtsp/RtspBenchmarkRunner.memory.jvm.kt`
5. `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/rtsp/RtspBenchmarkConfigTest.kt`

### Удаленные:
- `RtspBenchmarkRunner.kt` (проблемы с компиляцией)
- `SimpleRtspBenchmarkRunner.kt` (API mismatch)

---

## 🧪 Тестирование

### Unit Tests (работают):
```bash
./gradlew :core:network:test --tests "*RtspBenchmarkConfigTest*"
```

### Integration Tests (требуется):
```bash
# Требуется тестовая RTSP камера
export TEST_CAMERA_URL="rtsp://test-camera.local/stream"
./gradlew :core:network:test --tests "*RtspBenchmarkIntegrationTest*"
```

---

## 📝 Комментарии к реализации

### Проблемы, выявленные:
1. **API mismatch**: `RtspClient.connect()` не принимает callback для frames
2. **Complexity**: Полная реализация с фоновой коллекцией метрик слишком сложна для KMP
3. **Dependencies**: VideoDecoder требует специфичных JVM зависимостей

### Решение:
- Создать упрощенную версию без фоновой коллекции
- Использовать Integration тесты для реальных замеров
- Документация и структура готовы для будущей реализации

---

## 🎯 Рекомендации

### Для запуска бенчмарков:

1. **Подготовить тестовую среду:**
   - Тестовая RTSP камера или сервер (например, VLC)
   - Настроенный Redis для метрик
   - Мониторинг CPU/Memory

2. **Запустить baseline:**
   ```kotlin
   val runner = RtspBenchmarkRunner(...)
   val config = RtspBenchmarkConfig(
       rtspUrl = "rtsp://test-camera/stream",
       duration = 10.minutes
   )
   val result = runner.runBenchmark(config)
   println("FPS: ${result.summary.avgFps}")
   ```

3. **Сгенерировать отчет:**
   ```kotlin
   val generator = BenchmarkReportGenerator()
   val html = generator.generateHtmlReport(result)
   File("benchmark-report.html").writeText(html)
   ```

---

**Статус:** 🟡 **60% COMPLETE**  
**Создано:** 5 файлов  
**Следующий шаг:** Integration с RtspClient/VideoDecoder  
**ETA:** 1 неделя
