# Video Stream Optimization Guide

## Overview

Optimization strategies for RTSP video streaming performance.

**Version:** 1.0  
**Last Updated:** 2026-05-17

---

## Table of Contents

1. [RTSP Performance Metrics](#rtsp-performance-metrics)
2. [Network Optimization](#network-optimization)
3. [Buffer Management](#buffer-management)
4. [Codec Optimization](#codec-optimization)
5. [Resource Management](#resource-management)
6. [Monitoring and Alerts](#monitoring-and-alerts)

---

## RTSP Performance Metrics

### Target Metrics

| Metric | Target | Critical |
|--------|--------|----------|
| Stream Latency | < 500ms | > 2000ms |
| Frame Rate | 30 FPS | < 15 FPS |
| Packet Loss | < 1% | > 5% |
| Buffer Underrun | < 0.1% | > 1% |
| Connection Time | < 2s | > 5s |
| Memory Usage | < 200MB/camera | > 500MB/camera |

---

## Network Optimization

### Connection Pooling

```kotlin
class RtspConnectionPool(
    private val maxSize: Int = 10
) {
    private val pool = ConcurrentHashMap<String, RtspConnection>()
    
    fun getConnection(cameraId: String): RtspConnection {
        return pool.getOrPut(cameraId) {
            createNewConnection(cameraId)
        }
    }
    
    fun releaseConnection(cameraId: String) {
        pool.remove(cameraId)?.close()
    }
    
    fun cleanupIdleConnections(timeout: Long = 30000) {
        val now = System.currentTimeMillis()
        pool.entries.removeIf { (_, conn) ->
            now - conn.lastUsedTime > timeout
        }
    }
}
```

### Adaptive Bitrate

```kotlin
class AdaptiveBitrateController {
    fun adjustBitrate(networkQuality: NetworkQuality): Int {
        return when (networkQuality) {
            NetworkQuality.EXCELLENT -> 8000 // 8 Mbps
            NetworkQuality.GOOD -> 4000       // 4 Mbps
            NetworkQuality.FAIR -> 2000       // 2 Mbps
            NetworkQuality.POOR -> 1000       // 1 Mbps
            NetworkQuality.Poor -> 500        // 500 kbps
        }
    }
}
```

### TCP vs UDP

```kotlin
enum class TransportProtocol {
    UDP,  // Lower latency, may lose packets
    TCP   // Reliable, higher latency
}

class RtspConfig(
    val transport: TransportProtocol = TransportProtocol.TCP,
    val bufferSize: Int = 1024 * 1024 // 1MB
)
```

---

## Buffer Management

### Ring Buffer

```kotlin
class RingBuffer<T>(private val size: Int) {
    private val buffer = Array(size) { null as T? }
    private var readIndex = 0
    private var writeIndex = 0
    
    fun write(item: T): Boolean {
        if (isFull()) return false
        buffer[writeIndex] = item
        writeIndex = (writeIndex + 1) % size
        return true
    }
    
    fun read(): T? {
        if (isEmpty()) return null
        val item = buffer[readIndex]
        buffer[readIndex] = null
        readIndex = (readIndex + 1) % size
        return item
    }
    
    fun isEmpty() = readIndex == writeIndex && buffer[readIndex] == null
    fun isFull() = (writeIndex + 1) % size == readIndex
}
```

### Buffer Sizing

```kotlin
// Optimal buffer size calculation
fun calculateBufferSize(
    bitrate: Int,      // bits per second
    latency: Long,     // milliseconds
    fps: Int
): Int {
    // Buffer = (bitrate * latency) / 8 + (frameSize * fps * bufferDuration)
    val bitrateBuffer = (bitrate * latency) / 8
    val frameBuffer = (bitrate / fps) * 2 // 2 frames
    
    return (bitrateBuffer + frameBuffer).toInt()
}

// Example: 4Mbps, 500ms latency, 30 FPS
// Buffer = (4000000 * 500) / 8 + (133333 * 30 * 2) = 250KB + 8MB = ~8.25MB
```

---

## Codec Optimization

### H.264/H.265 Selection

```kotlin
enum class VideoCodec {
    H264,  // Better compatibility
    H265   // Better compression (50% smaller)
}

class CodecSelector {
    fun selectCodec(camera: Camera): VideoCodec {
        return when {
            camera.supportsH265 && isHardwareDecodingAvailable() -> VideoCodec.H265
            else -> VideoCodec.H264
        }
    }
}
```

### Keyframe Interval

```kotlin
class KeyframeConfig {
    // Request keyframe every 2 seconds
    fun getKeyframeInterval(fps: Int): Int {
        return fps * 2 // 2 seconds worth of frames
    }
    
    // Force keyframe on seek
    fun requestKeyframe(): Boolean {
        return sendRtspCommand("PAUSE") && sendRtspCommand("PLAY")
    }
}
```

### Hardware Acceleration

```kotlin
class HardwareDecoder {
    fun isHardwareSupported(): Boolean {
        return try {
            // Check for hardware decoder availability
            MediaCodec.createDecoderByType("video/avc") != null
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun createDecoder(codec: String): MediaCodec {
        return MediaCodec.createDecoderByType(codec)
    }
}
```

---

## Resource Management

### Stream Lifecycle

```kotlin
class StreamLifecycle {
    private val streams = mutableMapOf<String, StreamInfo>()
    
    fun startStream(cameraId: String) {
        val info = StreamInfo(cameraId, System.currentTimeMillis())
        streams[cameraId] = info
        
        // Auto-stop after 1 hour if inactive
        scheduleAutoStop(cameraId)
    }
    
    fun stopStream(cameraId: String) {
        streams.remove(cameraId)?.let { info ->
            cleanupResources(info)
        }
    }
    
    private fun scheduleAutoStop(cameraId: String) {
        CoroutineScope(Dispatchers.Default).launch {
            delay(3600000) // 1 hour
            if (streams.containsKey(cameraId) && !isStreamActive(cameraId)) {
                stopStream(cameraId)
            }
        }
    }
}
```

### Memory Limits

```kotlin
class MemoryLimiter {
    private val maxMemoryPerStream = 200 * 1024 * 1024L // 200MB
    
    fun checkMemoryUsage(cameraId: String): Boolean {
        val currentUsage = getStreamMemoryUsage(cameraId)
        return currentUsage < maxMemoryPerStream
    }
    
    fun enforceLimits() {
        val streams = getAllStreams()
        val totalMemory = streams.sumOf { getStreamMemoryUsage(it.id) }
        
        if (totalMemory > maxTotalMemory) {
            // Stop least recently used streams
            streams.sortedBy { it.lastUsed }
                .takeWhile { getStreamMemoryUsage(it.id) > maxTotalMemory * 0.8 }
                .forEach { stopStream(it.id) }
        }
    }
}
```

### Connection Limits

```kotlin
class ConnectionLimiter {
    private val maxConcurrentStreams = 10
    
    fun canStartStream(): Boolean {
        return getActiveStreamCount() < maxConcurrentStreams
    }
    
    fun queueStream(cameraId: String): QueueItem {
        return QueueItem(cameraId, System.currentTimeMillis())
    }
    
    fun processQueue() {
        while (canStartStream() && hasQueuedStreams()) {
            val item = dequeueStream()
            startStream(item.cameraId)
        }
    }
}
```

---

## Monitoring and Alerts

### Performance Monitoring

```kotlin
data class StreamMetrics(
    val cameraId: String,
    val fps: Double,
    val bitrate: Int,
    val latency: Long,
    val packetLoss: Double,
    val bufferLevel: Int,
    val memoryUsage: Long
)

class StreamMonitor {
    private val metrics = ConcurrentHashMap<String, StreamMetrics>()
    
    fun recordMetrics(cameraId: String, metrics: StreamMetrics) {
        this.metrics[cameraId] = metrics
    }
    
    fun getAverageFps(): Double {
        return metrics.values.map { it.fps }.average()
    }
    
    fun getAverageLatency(): Long {
        return metrics.values.map { it.latency }.average().toLong()
    }
}
```

### Alerts

```kotlin
class PerformanceAlerts {
    fun checkAlerts(metrics: StreamMetrics) {
        if (metrics.fps < 15) {
            sendAlert("LOW_FPS", "Camera ${metrics.cameraId}: ${metrics.fps} FPS")
        }
        
        if (metrics.latency > 2000) {
            sendAlert("HIGH_LATENCY", "Camera ${metrics.cameraId}: ${metrics.latency}ms")
        }
        
        if (metrics.packetLoss > 5) {
            sendAlert("HIGH_PACKET_LOSS", "Camera ${metrics.cameraId}: ${metrics.packetLoss}%")
        }
        
        if (metrics.memoryUsage > 500 * 1024 * 1024) {
            sendAlert("HIGH_MEMORY", "Camera ${metrics.cameraId}: ${metrics.memoryUsage / (1024 * 1024)}MB")
        }
    }
}
```

### Health Check

```kotlin
class HealthChecker {
    fun checkStreamHealth(cameraId: String): HealthStatus {
        val metrics = getStreamMetrics(cameraId) ?: return HealthStatus.UNHEALTHY
        
        return when {
            metrics.fps < 10 -> HealthStatus.CRITICAL
            metrics.fps < 20 -> HealthStatus.WARNING
            metrics.latency > 2000 -> HealthStatus.WARNING
            metrics.packetLoss > 5 -> HealthStatus.WARNING
            else -> HealthStatus.HEALTHY
        }
    }
}
```

---

## Configuration

### RTSP Client Configuration

```kotlin
data class RtspClientConfig(
    val connectTimeout: Long = 5000,
    val readTimeout: Long = 10000,
    val bufferSize: Int = 1024 * 1024,
    val transportProtocol: TransportProtocol = TransportProtocol.TCP,
    val autoReconnect: Boolean = true,
    val reconnectDelay: Long = 5000,
    val maxReconnectAttempts: Int = 3,
    val enableHardwareDecoding: Boolean = true,
    val keyframeInterval: Int = 60,
    val bufferDuration: Long = 5000
)
```

### Optimized Defaults

```kotlin
object RtspOptimizedConfig {
    val HIGH_QUALITY = RtspClientConfig(
        bufferSize = 2 * 1024 * 1024,     // 2MB
        transportProtocol = TransportProtocol.TCP,
        enableHardwareDecoding = true,
        keyframeInterval = 60
    )
    
    val LOW_LATENCY = RtspClientConfig(
        bufferSize = 512 * 1024,          // 512KB
        transportProtocol = TransportProtocol.UDP,
        connectTimeout = 2000,
        readTimeout = 5000,
        keyframeInterval = 30
    )
    
    val LOW_BANDWIDTH = RtspClientConfig(
        bufferSize = 256 * 1024,          // 256KB
        transportProtocol = TransportProtocol.TCP,
        enableHardwareDecoding = true,
        keyframeInterval = 120
    )
}
```

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| High latency | Large buffer | Reduce buffer size, use UDP |
| Frame drops | Network congestion | Lower bitrate, increase buffer |
| Memory leak | Stream not stopped | Implement auto-stop, cleanup |
| Connection timeout | Network issues | Increase timeout, add retry |
| Black screen | Codec mismatch | Check camera codec support |
| Audio sync issues | Buffer misalignment | Adjust audio/video buffers |

### Debug Commands

```bash
# Check network quality
ping -c 10 camera-ip

# Check RTSP stream
ffplay rtsp://camera-ip:554/stream

# Monitor network traffic
iftop -n -P

# Check memory usage
jstat -gc <pid> 1000

# Analyze RTSP traffic
tcpdump -i any port 554 -w rtsp.pcap
```

---

**End of Video Stream Optimization Guide**
