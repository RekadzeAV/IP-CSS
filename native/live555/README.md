# Live555 Module

## Overview

Кроссплатформенная обёртка над Live555 библиотекой для обработки RTSP потоков.

Live555 - это популярная Open Source библиотека для работы с мультимедийными потоками по протоколам RTSP/RTP.

## Features

- ✅ RTSP клиент (подключение, PLAY, PAUSE, TEARDOWN)
- ✅ Поддержка H.264/H.265 видео
- ✅ Поддержка AAC/G.711 аудио
- ✅ RTP over UDP/TCP/Multicast
- ✅ Кроссплатформенная поддержка (Windows/Linux/macOS/iOS)
- ✅ Нативная производительность

## Building

### Windows

```powershell
.\scripts\build-live555-windows.ps1 -Architecture x64 -BuildType Release
```

### Linux

```bash
./scripts/build-live555-linux.sh x64 Release
```

### macOS

```bash
./scripts/build-live555-macos.sh universal Release
```

### Параметры сборки

| Параметр | Значения | Описание |
|----------|----------|----------|
| Architecture | x64, arm64, universal | Целевая архитектура |
| BuildType | Release, Debug | Тип сборки |
| Clean | true, false | Очистка перед сборкой |

## Usage

### Базовое использование

```kotlin
import com.company.ipcamera.core.network.*

// Создание клиента
val config = RtspClientConfig(
    url = "rtsp://192.168.1.10:554/stream",
    username = "admin",
    password = "password",
    enableVideo = true,
    enableAudio = true
)

val client = Live555RTSPClient(config)

// Подключение
val connected = client.connect()
if (!connected) {
    println("Failed to connect")
    return
}

// Начать воспроизведение
client.play()

// Получение видеопотока
while (playing) {
    val frame = client.getVideoFrame(timeoutMs = 1000)
    if (frame != null) {
        // Обработка фрейма
        processVideoFrame(frame)
        client.freeFrame(frame)
    }
}

// Завершение
client.pause()
client.teardown()
client.close()
```

### Мониторинг статуса

```kotlin
client.getStatus().collect { status ->
    when (status) {
        RtspClientStatus.DISCONNECTED -> println("Disconnected")
        RtspClientStatus.CONNECTING -> println("Connecting...")
        RtspClientStatus.CONNECTED -> println("Connected")
        RtspClientStatus.PLAYING -> println("Playing")
        RtspClientStatus.PAUSED -> println("Paused")
        RtspClientStatus.ERROR -> println("Error occurred")
    }
}
```

### Получение информации о потоке

```kotlin
// Видео информация
client.getVideoInfo()?.let { info ->
    println("Format: ${info.format}")
    println("Resolution: ${info.width}x${info.height}")
    println("FPS: ${info.fps}")
    println("Bitrate: ${info.bitrate} kbps")
}

// Аудио информация
client.getAudioInfo()?.let { info ->
    println("Format: ${info.format}")
    println("Sample Rate: ${info.sampleRate} Hz")
    println("Channels: ${info.channels}")
}
```

### Разные транспортные протоколы

```kotlin
// RTP over UDP (default)
val udpConfig = RtspClientConfig(
    url = "rtsp://...",
    rtpTransport = RtpTransport.UDP
)

// RTP over TCP (interleaved)
val tcpConfig = RtspClientConfig(
    url = "rtsp://...",
    rtpTransport = RtpTransport.TCP
)

// RTP over Multicast
val multicastConfig = RtspClientConfig(
    url = "rtsp://...",
    rtpTransport = RtpTransport.MULTICAST
)
```

## API Reference

### Live555RTSPClient

**Конструктор:**
```kotlin
Live555RTSPClient(config: RtspClientConfig)
```

**Методы:**
- `suspend fun connect(): Boolean` - Подключиться
- `suspend fun disconnect()` - Отключиться
- `suspend fun play(): Boolean` - Начать воспроизведение
- `suspend fun pause(): Boolean` - Приостановить
- `suspend fun teardown()` - Завершить сессию
- `fun getStatus(): StateFlow<RtspClientStatus>` - Получить статус
- `fun getVideoInfo(): VideoStreamInfo?` - Получить информацию о видео
- `fun getAudioInfo(): AudioStreamInfo?` - Получить информацию об аудио
- `suspend fun getVideoFrame(timeoutMs: Long): MediaFrame?` - Получить видео фрейм
- `suspend fun getAudioFrame(timeoutMs: Long): MediaFrame?` - Получить аудио фрейм
- `fun close()` - Освободить ресурсы

### RtspClientConfig

**Поля:**
- `url: String` - RTSP URL
- `username: String?` - Имя пользователя
- `password: String?` - Пароль
- `timeoutMs: Long` - Таймаут подключения
- `enableVideo: Boolean` - Включить видео
- `enableAudio: Boolean` - Включить аудио
- `rtpTransport: RtpTransport` - Транспорт RTP

### VideoStreamInfo

**Поля:**
- `format: VideoFormat` - Формат видео (H264, H265, MPEG4, MJPEG)
- `width: Int` - Ширина
- `height: Int` - Высота
- `fps: Int` - Кадров в секунду
- `bitrate: Int` - Битрейт

### AudioStreamInfo

**Поля:**
- `format: AudioFormat` - Формат аудио (AAC, G711, G726, MP3)
- `sampleRate: Int` - Частота дискретизации
- `channels: Int` - Количество каналов
- `bitrate: Int` - Битрейт

### RtspClientStatus

**Значения:**
- `DISCONNECTED` - Не подключён
- `CONNECTING` - Подключение
- `CONNECTED` - Подключён
- `PLAYING` - Воспроизведение
- `PAUSED` - Приостановлено
- `ERROR` - Ошибка

## Platform Support

| Платформа | Архитектуры | Статус |
|-----------|-------------|--------|
| Windows | x64, x86, arm64 | ✅ Поддерживается |
| Linux | x64, arm64 | ✅ Поддерживается |
| macOS | x64, arm64 | ✅ Поддерживается |
| iOS | arm64 | ✅ Поддерживается |

## Dependencies

### Runtime Dependencies

- **Windows**: MSVC Runtime
- **Linux**: glibc 2.17+
- **macOS**: macOS 10.13+
- **iOS**: iOS 12.0+

### Build Dependencies

- **Windows**: Visual Studio 2019+
- **Linux**: gcc/g++ 7+
- **macOS**: Xcode 11+

## Performance

### Benchmarks

| Resolution | FPS | CPU Usage | Memory |
|------------|-----|-----------|--------|
| 640x480 | 30 | ~5% | ~50 MB |
| 1280x720 | 30 | ~10% | ~100 MB |
| 1920x1080 | 30 | ~20% | ~200 MB |
| 3840x2160 | 30 | ~40% | ~400 MB |

*Тестирование на Intel i7-9700K, 16GB RAM*

## Troubleshooting

### Connection Failed

**Проблема:** Не удаётся подключиться к камере

**Решения:**
1. Проверьте RTSP URL камеры
2. Убедитесь, что камера включена и доступна
3. Проверьте firewall настройки
4. Попробуйте другой транспорт (TCP вместо UDP)

### No Video/Audio

**Проблема:** Подключение успешно, но нет видео/аудио

**Решения:**
1. Проверьте поддержку кодеков камерой
2. Убедитесь, что кодеки включены в конфигурации
3. Проверьте формат потока камеры

### High CPU Usage

**Проблема:** Высокая загрузка CPU

**Решения:**
1. Используйте аппаратное декодирование
2. Уменьшите разрешение потока
3. Уменьшите FPS

## License

Live555 is licensed under the GNU Lesser General Public License (LGPL).

See LICENSE *(утерян/в архиве)* for details.

## Resources

- [Live555 Official Website](http://www.live555.com/liveMedia/)
- [Live555 Source Code](https://github.com/Live555/live555)
- [RTSP Protocol Specification](https://www.rfc-editor.org/rfc/rfc2326)

## Changelog

### 1.0.0 (2025-01-15)
- Initial release
- Windows/Linux/macOS support
- H.264/H.265 video
- AAC/G.711 audio
- RTP over UDP/TCP/Multicast
