# Реализация видеоплеера с интеграцией RTSP потоков

## Обзор

Видеоплеер (`VideoPlayer`) реализован для воспроизведения RTSP потоков в веб-браузере. Поскольку браузеры не поддерживают RTSP напрямую, серверная часть конвертирует RTSP потоки в HLS (HTTP Live Streaming) формат, который затем воспроизводится через библиотеку HLS.js.

## Архитектура

```
RTSP Camera → Server (FFmpeg) → HLS Segments → HLS.js → HTML5 Video Element
```

### Компоненты

1. **VideoPlayer Component** (`src/components/VideoPlayer/VideoPlayer.tsx`)
   - React компонент для воспроизведения видео
   - Использует HLS.js для HLS потоков
   - Управляет жизненным циклом потока
   - Обрабатывает ошибки и переподключение

2. **Stream Service** (`src/services/streamService.ts`)
   - API клиент для работы с видеопотоками
   - Методы для запуска/остановки потоков
   - Получение HLS URL
   - Создание снимков экрана

3. **Server API** (Kotlin)
   - `VideoStreamService` - управление RTSP потоками
   - `HlsGeneratorService` - конвертация RTSP в HLS через FFmpeg
   - API endpoints для управления потоками

## Основные функции

### 1. Автоматический запуск потока

При монтировании компонента автоматически:
- Вызывается API endpoint для запуска RTSP потока
- Сервер начинает конвертацию RTSP → HLS
- Плеер подключается к HLS плейлисту

```typescript
// Автоматический запуск потока через API
await streamService.startStream(camera.id);
const hlsUrl = streamService.getHlsUrl(camera.id);
```

### 2. Воспроизведение HLS

Используется HLS.js для браузеров без нативной поддержки HLS:
- Chrome, Firefox, Edge - через HLS.js
- Safari - нативная поддержка HLS

```typescript
if (Hls.isSupported()) {
  hls = new Hls({
    enableWorker: true,
    lowLatencyMode: true, // Низкая задержка для RTSP
    backBufferLength: 90,
    maxBufferLength: 30,
  });
  hls.loadSource(hlsUrl);
  hls.attachMedia(video);
}
```

### 3. Обработка ошибок и восстановление

- Автоматическое восстановление при сетевых ошибках
- Переподключение с экспоненциальной задержкой
- Максимум 3 попытки переподключения
- Индикация статуса подключения

```typescript
hls.on(Hls.Events.ERROR, (event, data) => {
  if (data.fatal) {
    switch (data.type) {
      case Hls.ErrorTypes.NETWORK_ERROR:
        hls?.startLoad(); // Попытка восстановления
        break;
      case Hls.ErrorTypes.MEDIA_ERROR:
        hls?.recoverMediaError();
        break;
    }
  }
});
```

### 4. Управление качеством

Поддержка различных уровней качества:
- Low (640x360)
- Medium (1280x720) - по умолчанию
- High (1920x1080)
- Ultra (1920x1080)

> **Примечание:** Изменение качества требует перезапуска потока на сервере (TODO: реализовать API endpoint)

### 5. Дополнительные функции

- **Снимок экрана** - создание скриншота текущего кадра
- **Полноэкранный режим** - поддержка нативного fullscreen API
- **Статус подключения** - визуальная индикация состояния потока
- **Управление воспроизведением** - play, pause, stop

## Использование

### Базовое использование

```tsx
import VideoPlayer from '@/components/VideoPlayer/VideoPlayer';

<VideoPlayer
  camera={camera}
  autoPlay={true}
  controls={true}
/>
```

### С параметрами

```tsx
<VideoPlayer
  camera={camera}
  autoPlay={false}
  controls={false} // Кастомные контролы
  width="100%"
  height="auto"
  streamType="hls" // По умолчанию
/>
```

## API Интеграция

### Endpoints

#### Запуск потока
```typescript
POST /api/v1/cameras/{id}/stream/start
Response: { success: true, data: "stream-id", message: "..." }
```

#### Остановка потока
```typescript
POST /api/v1/cameras/{id}/stream/stop
Response: { success: true, message: "..." }
```

#### Статус потока
```typescript
GET /api/v1/cameras/{id}/stream/status
Response: {
  success: true,
  data: {
    active: boolean,
    streamId: string | null,
    hlsUrl: string | null,
    rtspUrl: string | null
  }
}
```

#### HLS Плейлист
```typescript
GET /api/v1/cameras/{id}/stream/hls/playlist.m3u8
Response: application/vnd.apple.mpegurl
```

#### Снимок экрана
```typescript
POST /api/v1/cameras/{id}/stream/screenshot
Response: { success: true, data: "url-to-screenshot", message: "..." }
```

## Статусы потока

Компонент отображает следующие статусы:

- **idle** - Ожидание
- **starting** - Запуск трансляции
- **connected** - Подключено (поток активен, но не воспроизводится)
- **playing** - Воспроизведение
- **error** - Ошибка подключения/воспроизведения
- **stopped** - Остановлено

## Обработка ошибок

### Типы ошибок

1. **Сетевые ошибки** (NETWORK_ERROR)
   - Автоматическое восстановление через `hls.startLoad()`
   - До 3 попыток восстановления
   - При неудаче - переподключение

2. **Ошибки медиа** (MEDIA_ERROR)
   - Восстановление через `hls.recoverMediaError()`
   - Перезагрузка сегментов

3. **Критические ошибки**
   - Остановка текущего потока
   - Очистка ресурсов
   - Инициализация нового подключения

### Переподключение

При критических ошибках компонент:
1. Останавливает текущий поток через API
2. Очищает HLS инстанс
3. Сбрасывает состояние
4. Перезапускает поток через триггер переподключения

## Производительность

### Настройки HLS.js для низкой задержки

```typescript
{
  enableWorker: true,           // Использование Web Workers
  lowLatencyMode: true,         // Режим низкой задержки
  backBufferLength: 90,         // Размер буфера (секунды)
  maxBufferLength: 30,          // Максимальный буфер
  maxMaxBufferLength: 60,       // Абсолютный максимум
}
```

### Рекомендации

- Используйте адаптивный битрейт (ABR) для оптимального качества
- Настройте размер буфера в зависимости от сетевых условий
- Для еще более низкой задержки рассмотрите WebRTC (планируется)

## Будущие улучшения

### WebRTC поддержка

Для еще более низкой задержки планируется добавить поддержку WebRTC:

```tsx
<VideoPlayer
  camera={camera}
  streamType="webrtc" // WebRTC для низкой задержки
/>
```

Преимущества WebRTC:
- Задержка < 500ms (vs ~2-5s для HLS)
- Прямая передача данных без сегментации
- Лучшая производительность на мобильных устройствах

### Адаптивное качество

- Автоматическое переключение качества в зависимости от пропускной способности
- Интеграция с HLS.js уровней качества
- API для изменения качества в реальном времени

### Статистика потока

- FPS кадров
- Битрейт
- Задержка
- Потери пакетов

## Отладка

### Проверка HLS потока

1. Откройте DevTools → Network
2. Найдите запросы к `.m3u8` и `.ts` файлам
3. Проверьте статус ответов (должны быть 200 OK)

### Логирование

Компонент логирует все важные события:
- Запуск потока
- Ошибки HLS
- Переподключения
- Статусы воспроизведения

```typescript
console.log('Stream started:', streamId);
console.error('HLS error:', error);
```

### Типичные проблемы

1. **Поток не запускается**
   - Проверьте доступность RTSP камеры
   - Проверьте логи сервера
   - Убедитесь, что FFmpeg установлен на сервере

2. **Видео не воспроизводится**
   - Проверьте поддержку HLS в браузере
   - Проверьте CORS настройки
   - Проверьте доступность HLS плейлиста

3. **Высокая задержка**
   - Уменьшите `maxBufferLength`
   - Используйте `lowLatencyMode: true`
   - Рассмотрите WebRTC для критичных случаев

## Зависимости

```json
{
  "hls.js": "^1.5.12",
  "video.js": "^8.10.1",  // Опционально, для расширенных функций
  "@mui/material": "^5.16.7",
  "@mui/icons-material": "^5.16.7"
}
```

## Лицензия

См. основной файл LICENSE в корне проекта.


