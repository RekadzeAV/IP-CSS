# Видеоплеер — интеграция с RTSP/HLS

**Дата создания:** 26 January 2026
**Версия:** 1.0
**Статус:** ✅ Реализовано

## Обзор

Реализована полная интеграция видеоплеера с RTSP и HLS потоками для всех платформ проекта IP-CSS. Система поддерживает автоматический выбор протокола, управление качеством потока, обработку ошибок и переподключение.

## Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                    IP Camera                            │
│                  (RTSP Stream)                          │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              VideoStreamService                          │
│  • Управление RTSP подключениями                        │
│  • Генерация HLS потоков через FFmpeg                   │
│  • Управление качеством                                 │
└──────────────┬──────────────────────┬───────────────────┘
               │                      │
               ▼                      ▼
┌──────────────────────┐  ┌──────────────────────────────┐
│   Web Interface      │  │    Mobile Applications       │
│   (HLS.js)           │  │    (ExoPlayer)               │
│                      │  │                              │
│ • HLS потоки         │  │ • RTSP потоки (низкая        │
│ • Автоматическое     │  │   задержка)                  │
│   переподключение    │  │ • HLS потоки (стабильность)  │
│ • Управление         │  │ • Автоматический выбор       │
│   качеством          │  │   протокола                  │
└──────────────────────┘  └──────────────────────────────┘
```

## Компоненты системы

### 1. Серверная часть

#### VideoStreamService
Управляет жизненным циклом видеопотоков:
- Подключение к RTSP камерам
- Генерация HLS потоков через HlsGeneratorService
- Управление качеством потока
- Автоматическая очистка неактивных стримов

**Основные методы:**
```kotlin
suspend fun startStream(cameraId: String): Result<String>
suspend fun stopStream(cameraId: String): Result<Unit>
suspend fun getRtspUrl(cameraId: String): String?
fun getHlsUrl(cameraId: String): String?
suspend fun setStreamQuality(cameraId: String, quality: StreamQuality): Result<Unit>
```

#### HlsGeneratorService
Генерирует HLS сегменты из RTSP потоков через FFmpeg:
- Конвертация RTSP → HLS
- Управление качеством (LOW, MEDIUM, HIGH, ULTRA)
- Автоматическая очистка старых сегментов

**Поддерживаемые качества:**
- **LOW:** 640x360, 500kbps, 15fps
- **MEDIUM:** 1280x720, 1500kbps, 25fps (по умолчанию)
- **HIGH:** 1920x1080, 3000kbps, 30fps
- **ULTRA:** 1920x1080, 6000kbps, 30fps

### 2. Веб-интерфейс

#### VideoPlayer Component
React компонент для воспроизведения HLS потоков в браузере.

**Особенности:**
- Автоматическое определение поддержки HLS (HLS.js или нативная)
- Обработка ошибок с автоматическим переподключением
- Управление качеством потока
- Полноэкранный режим
- Создание снимков экрана

**Использование:**
```typescript
<VideoPlayer
  camera={camera}
  streamType="hls"  // или "rtsp" (автоматически конвертируется в HLS)
  autoPlay={true}
  controls={true}
/>
```

**Поддерживаемые типы потоков:**
- `hls` - HLS поток (по умолчанию для веб)
- `rtsp` - RTSP поток (автоматически конвертируется в HLS)
- `webrtc` - WebRTC поток (экспериментально)

#### streamService
API клиент для работы с видеопотоками.

**Методы:**
```typescript
async startStream(cameraId: string): Promise<string>
async stopStream(cameraId: string): Promise<void>
async getStreamStatus(cameraId: string): Promise<StreamStatus>
async getRtspUrl(cameraId: string): Promise<string>
getHlsUrl(cameraId: string): string
async captureScreenshot(cameraId: string): Promise<string>
async setStreamQuality(cameraId: string, quality: 'low' | 'medium' | 'high' | 'ultra'): Promise<void>
```

### 3. Android приложение

#### ExoVideoPlayer Component
Compose компонент для воспроизведения RTSP и HLS потоков на Android.

**Особенности:**
- Автоматическое определение типа потока по URL
- Оптимизация для RTSP (низкая задержка) и HLS (стабильность)
- Автоматическое переподключение при ошибках
- Поддержка управления качеством

**Использование:**
```kotlin
ExoVideoPlayer(
    videoUrl = viewModel.getCurrentStreamUrl(),
    streamType = StreamType.RTSP, // или StreamType.HLS
    autoPlay = true,
    enableLowLatency = true,
    onError = { error ->
        // Обработка ошибок
    },
    onRetry = {
        viewModel.reconnect()
    }
)
```

**Оптимизация для RTSP:**
- Минимальный буфер: 1000ms
- Максимальный буфер: 2000ms
- Буфер перед воспроизведением: 500ms

**Оптимизация для HLS:**
- Минимальный буфер: 5000ms
- Максимальный буфер: 15000ms
- Буфер перед воспроизведением: 2000ms

#### VideoViewViewModel
ViewModel для управления видеопотоком в Android приложении.

**Состояние:**
```kotlin
data class VideoViewUiState(
    val camera: Camera? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false,
    val rtspUrl: String? = null,
    val hlsUrl: String? = null,
    val streamActive: Boolean = false,
    val streamProtocol: StreamProtocol = StreamProtocol.RTSP,
    val streamQuality: StreamQuality = StreamQuality.MEDIUM,
    val retryCount: Int = 0
)
```

**Основные методы:**
```kotlin
fun startStream()
fun stopStream()
fun setStreamQuality(quality: StreamQuality)
fun setStreamProtocol(protocol: StreamProtocol)
fun getCurrentStreamUrl(): String?
fun reconnect()
```

## API Endpoints

### Управление потоками

#### POST `/api/v1/cameras/{id}/stream/start`
Запустить трансляцию для камеры.

**Ответ:**
```json
{
  "success": true,
  "data": "stream-id-uuid",
  "message": "Stream started successfully"
}
```

#### POST `/api/v1/cameras/{id}/stream/stop`
Остановить трансляцию для камеры.

#### GET `/api/v1/cameras/{id}/stream/status`
Получить статус трансляции.

**Ответ:**
```json
{
  "success": true,
  "data": {
    "active": true,
    "streamId": "stream-id-uuid",
    "hlsUrl": "/api/v1/cameras/{id}/stream/hls/playlist.m3u8",
    "rtspUrl": "rtsp://username:password@host:port/path"
  }
}
```

#### GET `/api/v1/cameras/{id}/stream/rtsp`
Получить RTSP URL для прямой трансляции (для ExoPlayer).

**Ответ:**
```json
{
  "success": true,
  "data": {
    "rtspUrl": "rtsp://username:password@host:port/path"
  }
}
```

#### POST `/api/v1/cameras/{id}/stream/quality?quality={quality}`
Изменить качество потока.

**Параметры:**
- `quality`: `low`, `medium`, `high`, `ultra`

### HLS потоки

#### GET `/api/v1/cameras/{id}/stream/hls/playlist.m3u8`
Получить HLS плейлист.

**Особенности:**
- Автоматически запускает стрим, если не активен
- Правильные HTTP заголовки для кэширования
- CORS заголовки для кросс-доменных запросов

#### GET `/api/v1/cameras/{id}/stream/hls/{segment}.ts`
Получить HLS сегмент.

**Особенности:**
- Обслуживание .ts файлов из файловой системы
- Правильные HTTP заголовки для кэширования
- CORS заголовки

### Снимки

#### POST `/api/v1/cameras/{id}/stream/screenshot`
Создать снимок текущего кадра.

**Ответ:**
```json
{
  "success": true,
  "data": "/api/v1/screenshots/camera-id_timestamp.jpg",
  "message": "Screenshot captured successfully"
}
```

## Поток работы

### Веб-интерфейс

1. **Инициализация:**
   - Компонент VideoPlayer монтируется
   - Вызывается `streamService.startStream(cameraId)`
   - Сервер запускает RTSP подключение и генерацию HLS

2. **Воспроизведение:**
   - Получается HLS URL через `streamService.getHlsUrl(cameraId)`
   - Инициализируется HLS.js или используется нативная поддержка HLS
   - Плеер подключается к HLS плейлисту

3. **Обработка ошибок:**
   - При сетевых ошибках автоматически переподключается
   - Максимум 3 попытки переподключения
   - Экспоненциальная задержка между попытками

### Android приложение

1. **Инициализация:**
   - ViewModel загружает информацию о камере
   - Получает RTSP и HLS URL через API
   - Выбирает протокол (по умолчанию RTSP)

2. **Воспроизведение:**
   - Вызывается `viewModel.startStream()`
   - Сервер запускает RTSP подключение
   - ExoPlayer подключается к RTSP URL или HLS URL

3. **Обработка ошибок:**
   - При ошибках ExoPlayer вызывает `onError`
   - ViewModel автоматически переподключается
   - Максимум 3 попытки с экспоненциальной задержкой

## Выбор протокола

### RTSP (Рекомендуется для Android)
**Преимущества:**
- Низкая задержка (< 1 секунда)
- Прямое подключение к камере
- Меньше нагрузка на сервер

**Недостатки:**
- Требует прямой доступ к камере
- Может не работать через NAT/firewall

**Использование:**
- Локальные сети
- Мобильные приложения
- Когда важна низкая задержка

### HLS (Рекомендуется для веб)
**Преимущества:**
- Работает через HTTP/HTTPS
- Проходит через NAT/firewall
- Адаптивное качество (можно расширить)
- Стабильность

**Недостатки:**
- Задержка выше (4+ секунды)
- Требует FFmpeg на сервере
- Больше нагрузка на сервер

**Использование:**
- Веб-браузеры
- Мобильные сети
- Когда важна стабильность

## Управление качеством

### Изменение качества в веб-интерфейсе

```typescript
await streamService.setStreamQuality(cameraId, 'high');
// Перезапускает поток с новым качеством
```

### Изменение качества в Android

```kotlin
viewModel.setStreamQuality(StreamQuality.HIGH)
// Автоматически перезапускает поток
```

### Рекомендации по качеству

- **LOW:** Мобильные сети, низкая пропускная способность
- **MEDIUM:** Стандартное качество, большинство случаев
- **HIGH:** WiFi, локальные сети, высокое качество
- **ULTRA:** Локальные сети, максимальное качество

## Обработка ошибок

### Автоматическое переподключение

Система автоматически переподключается при:
- Сетевых ошибках
- Потере соединения
- Ошибках воспроизведения

**Параметры:**
- Максимум попыток: 3
- Задержка: экспоненциальная (2s, 4s, 6s)

### Ручное переподключение

**Веб:**
```typescript
<VideoPlayer
  camera={camera}
  onReconnect={() => {
    // Дополнительная логика
  }}
/>
```

**Android:**
```kotlin
viewModel.reconnect()
```

## Производительность

### Оптимизация для RTSP

- Минимальный буфер для низкой задержки
- Быстрое переключение между кадрами
- Оптимизированные настройки ExoPlayer

### Оптимизация для HLS

- Увеличенный буфер для стабильности
- Кэширование сегментов
- Оптимизированные настройки HLS.js

## Ограничения

1. **FFmpeg обязателен:** Для генерации HLS требуется установленный FFmpeg
2. **Задержка HLS:** Первый сегмент генерируется через 4 секунды
3. **Дисковое пространство:** Сегменты хранятся на диске до удаления
4. **Одновременные стримы:** Каждый стрим требует отдельный процесс FFmpeg

## Следующие шаги

1. ✅ Интеграция RTSP/HLS завершена
2. ⚠️ Оптимизация производительности (батчинг сегментов)
3. ⚠️ Поддержка адаптивного битрейта (HLS с несколькими вариантами качества)
4. ⚠️ WebRTC для низкой задержки (альтернатива HLS)
5. ⚠️ Поддержка iOS видеоплеера

## Связанные документы

- [RTSP_CLIENT.md](RTSP_CLIENT.md) - Документация RTSP клиента
- [VIDEO_PLAYER_IMPLEMENTATION.md](../server/web/VIDEO_PLAYER_IMPLEMENTATION.md) - Детали реализации веб-видеоплеера
- [VIDEO_PLAYER_HLS_INTEGRATION_COMPLETE.md](implementation/VIDEO_PLAYER_HLS_INTEGRATION_COMPLETE.md) - Завершение интеграции HLS

---

**Статус:** Готово к использованию
**Тестирование:** Требуется тестирование с реальными камерами
**Последнее обновление:** 26 January 2026
