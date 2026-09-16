# RTSP URL Configurations for Popular Cameras

Готовые конфигурации RTSP URL для популярных брендов IP-камер.

## 📋 Содержание

- [Hikvision](#hikvision)
- [Dahua](#dahua)
- [Reolink](#reolink)
- [Axis](#axis)
- [Hikvision](#hikvision)
- [Ubiquiti UniFi](#ubiquiti-unifi)
- [Generic/ONVIF](#genericonvif)

---

## Hikvision

### Форматы URL

| Канал | Субпоток | High Stream (Main) | Low Stream (Sub) |
|-------|----------|-------------------|------------------|
| 1 | Main | `rtsp://admin:pass@ip:554/Streaming/Channels/101` | `rtsp://admin:pass@ip:554/Streaming/Channels/102` |
| 1 | Audio | `rtsp://admin:pass@ip:554/Streaming/Channels/103` | - |
| 2 | Main | `rtsp://admin:pass@ip:554/Streaming/Channels/201` | `rtsp://admin:pass@ip:554/Streaming/Channels/202` |
| 3 | Main | `rtsp://admin:pass@ip:554/Streaming/Channels/301` | `rtsp://admin:pass@ip:554/Streaming/Channels/302` |

### Паттерн
```
rtsp://[user]:[password]@[ip]:[port]/Streaming/Channels/[channel][stream]
```

- `channel`: номер камеры (1-16)
- `stream`: тип потока
  - `01` = Main (H.264/H.265, высокое качество)
  - `02` = Sub (низкое качество)
  - `03` = Audio

### Примеры

```kotlin
// Основная камера, высокое качество
val config = RtspClientConfig(
    url = "rtsp://admin:password123@192.168.1.64:554/Streaming/Channels/101",
    enableVideo = true,
    enableAudio = false
)

// Субпоток для низкой задержки
val config = RtspClientConfig(
    url = "rtsp://admin:password123@192.168.1.64:554/Streaming/Channels/102",
    enableVideo = true
)
```

### Особенности
- ✅ Поддержка H.264 и H.265
- ✅ Multi-stream (основной + субпоток)
- ✅ Digest Authentication
- ⚠️ Требуется включить RTSP в настройках камеры

---

## Dahua

### Форматы URL

| Тип | URL |
|-----|-----|
| Main Stream | `rtsp://admin:pass@ip:554/cam/realmonitor?channel=1&subtype=0` |
| Sub Stream | `rtsp://admin:pass@ip:554/cam/realmonitor?channel=1&subtype=1` |
| Alternative | `rtsp://admin:pass@ip:554/S1` |

### Паттерн
```
rtsp://[user]:[password]@[ip]:[port]/cam/realmonitor?channel=[channel]&subtype=[subtype]
```

### Примеры

```kotlin
// Основная камера Dahua
val config = RtspClientConfig(
    url = "rtsp://admin:password123@192.168.1.108:554/cam/realmonitor?channel=1&subtype=0",
    enableVideo = true,
    enableAudio = true
)

// Субпоток Dahua
val config = RtspClientConfig(
    url = "rtsp://admin:password123@192.168.1.108:554/cam/realmonitor?channel=1&subtype=1",
    enableVideo = true
)
```

### Особенности
- ✅ H.264/H.265
- ✅ AAC аудио
- ✅ Multiple camera channels

---

## Reolink

### Форматы URL

| Модель | URL Pattern |
|--------|-------------|
| Старые модели | `rtsp://admin:pass@ip:554/h264Preview_01_main` |
| Новые модели | `rtsp://admin:pass@ip:554/flv?port=1935&appid=live&streamname=channel_1_main` |
| RTSP over TCP | `rtsp://admin:pass@ip:554/tcp/101` |

### Паттерн
```
rtsp://[user]:[password]@[ip]:[port]/h264Preview_[channel]_[stream]
```

### Примеры

```kotlin
// Reolink камера (старые модели)
val config = RtspClientConfig(
    url = "rtsp://admin:password123@192.168.1.202:554/h264Preview_01_main",
    enableVideo = true
)

// Reolink с TCP транспортом (меньше задержка)
val config = RtspClientConfig(
    url = "rtsp://admin:password123@192.168.1.202:554/tcp/101",
    enableVideo = true
)
```

### Особенности
- ⚠️ Разные URL для разных моделей
- ✅ H.264
- ⚠️ Может требовать включения RTSP в веб-интерфейсе

---

## Axis

### Форматы URL

| Тип | URL |
|-----|-----|
| RTSP | `rtsp://ip/axis-media/media.amp` |
| With credentials | `rtsp://user:pass@ip/axis-media/media.amp` |
| Specific stream | `rtsp://ip/axis-media/media.amp?videocodec=h264` |

### Паттерн
```
rtsp://[user]:[password]@[ip]:[port]/axis-media/media.amp[?options]
```

### Примеры

```kotlin
// Axis камера
val config = RtspClientConfig(
    url = "rtsp://192.168.1.90/axis-media/media.amp",
    enableVideo = true,
    enableAudio = false
)

// С аутентификацией
val config = RtspClientConfig(
    url = "rtsp://admin:password123@192.168.1.90:554/axis-media/media.amp",
    enableVideo = true
)
```

### Особенности
- ✅ RTSP over HTTP
- ✅ ONVIF совместимость
- ✅ H.264/H.265

---

## Ubiquiti UniFi

### Форматы URL

| Модель | URL |
|--------|-----|
| UniFi Protect | `rtsp://ip:7447/[device_id]` |
| G3/G4 Bullet | `rtsp://ip:7447/UhB1bGxldA==` |

### Паттерн
```
rtsp://[user]:[password]@[ip]:7447/[stream_id]
```

### Примеры

```kotlin
// UniFi Camera
val config = RtspClientConfig(
    url = "rtsp://admin:password123@192.168.1.50:7447/UhB1bGxldA==",
    enableVideo = true,
    enableAudio = true
)
```

### Особенности
- ✅ H.264
- ✅ AAC аудио
- ⚠️ Порт 7447 (не стандартный 554)

---

## Generic / ONVIF

### Стандартные URL

| Тип | URL |
|-----|-----|
| ONVIF Profile S | `rtsp://ip:554/onvif1` |
| ONVIF Profile G | `rtsp://ip:554/onvif2` |
| Generic RTSP | `rtsp://ip:554/stream1` |

### Паттерн
```
rtsp://[user]:[password]@[ip]:[port]/[stream_path]
```

### Примеры

```kotlin
// ONVIF камера
val config = RtspClientConfig(
    url = "rtsp://admin:password123@192.168.1.150:554/onvif1",
    enableVideo = true
)

// Generic RTSP
val config = RtspClientConfig(
    url = "rtsp://admin:password123@192.168.1.150:554/stream1",
    enableVideo = true
)
```

---

## 🛠️ Утилита для генерации URL

```kotlin
object CameraUrlGenerator {
    
    enum class CameraBrand {
        HIKVISION, DAHUA, REOLINK, AXIS, UNIFI, GENERIC
    }
    
    data class CameraConfig(
        val brand: CameraBrand,
        val host: String,
        val port: Int = 554,
        val username: String,
        val password: String,
        val channel: Int = 1,
        val streamType: StreamType = StreamType.MAIN
    )
    
    enum class StreamType {
        MAIN, SUB, AUDIO
    }
    
    fun generateUrl(config: CameraConfig): String {
        val auth = "${config.username}:${config.password}@"
        val hostPort = "${config.host}:${config.port}"
        
        return when (config.brand) {
            CameraBrand.HIKVISION -> {
                val streamCode = when (config.streamType) {
                    StreamType.MAIN -> "101"
                    StreamType.SUB -> "102"
                    StreamType.AUDIO -> "103"
                }
                "rtsp://${auth}${hostHost}/Streaming/Channels/${config.channel}${streamCode}"
            }
            CameraBrand.DAHUA -> {
                val subtype = when (config.streamType) {
                    StreamType.MAIN -> "0"
                    StreamType.SUB -> "1"
                    else -> "0"
                }
                "rtsp://${auth}${hostPort}/cam/realmonitor?channel=${config.channel}&subtype=$subtype"
            }
            CameraBrand.REOLINK -> {
                val streamCode = when (config.streamType) {
                    StreamType.MAIN -> "main"
                    StreamType.SUB -> "sub"
                    else -> "main"
                }
                "rtsp://${auth}${hostPort}/h264Preview_${config.channel.toString().padStart(2, '0')}_$streamCode"
            }
            CameraBrand.AXIS -> {
                "rtsp://${auth}${hostPort}/axis-media/media.amp"
            }
            CameraBrand.UNIFI -> {
                "rtsp://${auth}${config.host}:7447/UhB1bGxldA=="
            }
            CameraBrand.GENERIC -> {
                "rtsp://${auth}${hostPort}/stream1"
            }
        }
    }
}

// Использование
fun main() {
    val config = CameraUrlGenerator.CameraConfig(
        brand = CameraUrlGenerator.CameraBrand.HIKVISION,
        host = "192.168.1.64",
        username = "admin",
        password = "password123",
        channel = 1,
        streamType = CameraUrlGenerator.StreamType.MAIN
    )
    
    val url = CameraUrlGenerator.generateUrl(config)
    println("RTSP URL: $url")
    // Вывод: rtsp://admin:password123@192.168.1.64:554/Streaming/Channels/101
}
```

---

## 🔧 Диагностика URL

### Проверка доступности

```bash
# FFmpeg
ffprobe rtsp://admin:password@192.168.1.100:554/stream

# VLC
vlc rtsp://admin:password@192.168.1.100:554/stream

# ffplay (простой просмотр)
ffplay -rtsp_transport tcp rtsp://admin:password@192.168.1.100:554/stream
```

### Проверка аутентификации

```bash
# Basic auth
curl -v rtsp://admin:password@192.168.1.100:554/stream

# Digest auth
curl -v --digest -u admin:password rtsp://192.168.1.100:554/stream
```

---

## ⚠️ Общие проблемы

| Проблема | Решение |
|----------|---------|
| "401 Unauthorized" | Проверьте username/password, включите RTSP в камере |
| "404 Not Found" | Проверьте правильность URL для вашей модели камеры |
| "Connection timeout" | Проверьте сетевую доступность: `ping camera_ip` |
| "Codec not supported" | Убедитесь, что камера использует H.264/H.265 |
| "No video frames" | Попробуйте субпоток или измените транспорт на TCP |

---

**Версия:** 1.0  
**Обновлено:** 24 мая 2026
