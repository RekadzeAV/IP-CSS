# Адаптивный битрейт для HLS и WebRTC - Реализация

**Дата создания:** 26 January 2026
**Версия:** 1.0
**Статус:** ✅ Реализовано (частично)

## Обзор

Реализована поддержка адаптивного битрейта для HLS потоков и базовая инфраструктура для WebRTC соединений. Система автоматически выбирает оптимальное качество на основе пропускной способности сети.

## Адаптивный битрейт для HLS

### Архитектура

```
RTSP Camera → FFmpeg (4 процесса) → HLS Variants → Master Playlist → HLS.js → Video Player
                                    ├─ Low (640x360)
                                    ├─ Medium (1280x720)
                                    ├─ High (1920x1080)
                                    └─ Ultra (1920x1080)
```

### Реализация

#### 1. HlsGeneratorService

Добавлен метод `startAdaptiveHlsGeneration()` для генерации нескольких вариантов качества одновременно:

```kotlin
fun startAdaptiveHlsGeneration(
    streamId: String,
    rtspUrl: String,
    qualities: List<StreamQuality> = listOf(LOW, MEDIUM, HIGH, ULTRA)
): String?
```

**Особенности:**
- Генерирует 4 варианта качества параллельно
- Создает master playlist с ссылками на все варианты
- Каждый вариант имеет свою директорию и плейлист

#### 2. Master Playlist

Создается файл `master.m3u8` со следующей структурой:

```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-STREAM-INF:BANDWIDTH=564000,RESOLUTION=640x360,CODECS="avc1.42e01e,mp4a.40.2"
/api/v1/cameras/{cameraId}/stream/hls/low/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=1644000,RESOLUTION=1280x720,CODECS="avc1.4d001f,mp4a.40.2"
/api/v1/cameras/{cameraId}/stream/hls/medium/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=3144000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2"
/api/v1/cameras/{cameraId}/stream/hls/high/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=6144000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2"
/api/v1/cameras/{cameraId}/stream/hls/ultra/playlist.m3u8
```

#### 3. Варианты качества

| Качество | Разрешение | Битрейт видео | Битрейт аудио | FPS | Bandwidth |
|----------|------------|---------------|---------------|-----|-----------|
| LOW      | 640x360    | 500k          | 64k           | 15  | 564k      |
| MEDIUM   | 1280x720   | 1500k         | 128k          | 25  | 1644k     |
| HIGH     | 1920x1080  | 3000k         | 192k          | 30  | 3144k     |
| ULTRA    | 1920x1080  | 6000k         | 256k          | 30  | 6144k     |

### API Endpoints

#### GET `/api/v1/cameras/{id}/stream/hls/master.m3u8`
Получить master playlist с вариантами качества.

**Ответ:** Master playlist в формате M3U8

#### GET `/api/v1/cameras/{id}/stream/hls/{quality}/playlist.m3u8`
Получить плейлист для конкретного качества.

**Параметры:**
- `quality`: `low`, `medium`, `high`, `ultra`

#### GET `/api/v1/cameras/{id}/stream/hls/{quality}/{segment}.ts`
Получить сегмент для конкретного качества.

### Использование в веб-интерфейсе

VideoPlayer автоматически использует master playlist для адаптивного битрейта:

```typescript
// Автоматически используется master playlist
<VideoPlayer
  camera={camera}
  streamType="hls"
  autoPlay={true}
/>
```

HLS.js автоматически выбирает оптимальное качество на основе:
- Пропускной способности сети
- Размера буфера
- Производительности устройства

### Преимущества

1. **Автоматический выбор качества:** Плеер сам выбирает оптимальное качество
2. **Плавное переключение:** Переключение между качествами происходит без прерываний
3. **Экономия трафика:** Используется только необходимое качество
4. **Лучший UX:** Видео воспроизводится даже при медленном интернете

## WebRTC для низкой задержки

### Архитектура

```
RTSP Camera → Media Server → WebRTC → Browser (PeerConnection)
```

**Примечание:** Требуется медиа-сервер (Kurento, Janus, или GStreamer-based) для полноценной реализации.

### Реализация

#### 1. WebRtcService

Создан сервис для обработки WebRTC соединений:

```kotlin
class WebRtcService(
    private val videoStreamService: VideoStreamService
) {
    suspend fun handleOffer(
        cameraId: String,
        offer: String
    ): Result<WebRtcAnswer>
}
```

**Текущий статус:** Базовая инфраструктура готова, требуется интеграция с медиа-сервером.

#### 2. WebRTC Endpoint

**POST `/api/v1/cameras/{id}/stream/webrtc/offer`**

Обработать WebRTC offer от клиента.

**Запрос:**
```json
{
  "offer": {
    "type": "offer",
    "sdp": "v=0\r\no=..."
  }
}
```

**Ответ (заглушка):**
```json
{
  "success": false,
  "message": "WebRTC support requires media server. Not implemented yet."
}
```

#### 3. Клиентская часть

WebRTC клиент уже реализован в `server/web/src/utils/webrtc.ts`:

```typescript
const stream = await initWebRTCConnection(cameraId, apiUrl, {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
  ],
  onTrack: (event) => {
    video.srcObject = event.streams[0];
  },
});
```

### Использование в VideoPlayer

```typescript
<VideoPlayer
  camera={camera}
  streamType="webrtc"  // Использует WebRTC для низкой задержки
  autoPlay={true}
/>
```

### Требования для полноценной реализации

1. **Медиа-сервер:** Требуется установка и настройка медиа-сервера:
   - **Kurento Media Server** - популярный выбор для WebRTC
   - **Janus Gateway** - легковесный WebRTC сервер
   - **GStreamer-based server** - кастомное решение

2. **Signaling:** Реализация WebRTC signaling через WebSocket или HTTP

3. **ICE/TURN серверы:** Для работы через NAT/firewall

4. **Интеграция с RTSP:** Конвертация RTSP потоков в WebRTC через медиа-сервер

### Преимущества WebRTC

1. **Низкая задержка:** < 500ms (vs 4+ секунд для HLS)
2. **Прямое соединение:** P2P соединение между сервером и клиентом
3. **Адаптивность:** Автоматическая адаптация к условиям сети
4. **Низкая нагрузка:** Меньше нагрузка на сервер по сравнению с HLS

### Ограничения

1. **Требует медиа-сервер:** Не может работать без медиа-сервера
2. **Сложность настройки:** Требует настройки STUN/TURN серверов
3. **Ограничения браузеров:** Некоторые браузеры имеют ограничения на WebRTC

## Сравнение протоколов

| Параметр | HLS (адаптивный) | WebRTC |
|----------|------------------|--------|
| Задержка | 4-10 секунд | < 500ms |
| Качество | Автоматическое | Автоматическое |
| Совместимость | Все браузеры | Современные браузеры |
| Нагрузка на сервер | Средняя | Низкая |
| Требования | FFmpeg | Медиа-сервер |
| Стабильность | Высокая | Средняя |

## Рекомендации по использованию

### Использовать HLS когда:
- Важна стабильность и совместимость
- Задержка не критична
- Нет возможности установить медиа-сервер
- Нужна поддержка старых браузеров

### Использовать WebRTC когда:
- Требуется низкая задержка (< 1 секунда)
- Есть медиа-сервер
- Работа в современных браузерах
- Прямое управление камерой (PTZ)

## Следующие шаги

1. ✅ Адаптивный битрейт для HLS - реализовано
2. ⚠️ WebRTC - требуется интеграция с медиа-сервером
3. ⚠️ Оптимизация производительности адаптивного битрейта
4. ⚠️ Мониторинг качества соединения для автоматического переключения

## Связанные документы

- [VIDEO_PLAYER_RTSP_HLS_INTEGRATION.md](VIDEO_PLAYER_RTSP_HLS_INTEGRATION.md) - Полная документация видеоплеера
- [RTSP_CLIENT.md](RTSP_CLIENT.md) - Документация RTSP клиента

---

**Статус:** Адаптивный битрейт готов к использованию, WebRTC требует медиа-сервер
**Последнее обновление:** 26 January 2026
