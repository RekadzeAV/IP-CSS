# Интеграция видеоплеера с HLS завершена

**Дата:** Январь 2026
**Статус:** ✅ Завершено

## Выполненные задачи

### 1. Исправлена интеграция HLS генератора с VideoStreamService ✅

**Проблемы:**
- Неправильный формат URL для HLS плейлиста
- Отсутствовал endpoint для обслуживания HLS сегментов (.ts файлов)

**Исправления:**

1. **HlsGeneratorService.getPlaylistUrl()** - исправлен формат URL
   - Было: `/api/v1/cameras/streams/$streamId/hls/playlist.m3u8`
   - Стало: `/api/v1/cameras/$cameraId/stream/hls/playlist.m3u8`

2. **VideoStreamService.getHlsUrl()** - улучшена логика получения URL
   - Проверка наличия активного стрима
   - Правильное формирование URL через HlsGeneratorService

### 2. Добавлен endpoint для обслуживания HLS сегментов ✅

**Новый endpoint:** `GET /api/v1/cameras/{id}/stream/hls/{segment}.ts`

**Функциональность:**
- Обслуживание .ts сегментов из файловой системы
- Правильные HTTP заголовки для кэширования
- CORS заголовки для кросс-доменных запросов
- Проверка существования файла перед отправкой

### 3. Улучшен endpoint для HLS плейлиста ✅

**Endpoint:** `GET /api/v1/cameras/{id}/stream/hls/playlist.m3u8`

**Улучшения:**
- Автоматический запуск стрима если не активен
- Задержка для генерации первого сегмента
- Правильная замена путей к сегментам в плейлисте
- Правильные HTTP заголовки (Cache-Control, CORS)
- Улучшенная обработка ошибок

## Архитектура HLS потока

```
RTSP Camera → VideoStreamService → HlsGeneratorService (FFmpeg) → HLS Segments
                                                                    ↓
                                                              Web Browser (HLS.js)
```

### Поток данных:

1. **Клиент запрашивает HLS плейлист:**
   ```
   GET /api/v1/cameras/{id}/stream/hls/playlist.m3u8
   ```

2. **Сервер:**
   - Проверяет активность стрима
   - Если не активен - запускает стрим через VideoStreamService
   - VideoStreamService запускает RTSP клиент и HlsGeneratorService
   - HlsGeneratorService запускает FFmpeg для генерации HLS сегментов
   - Возвращает плейлист с правильными путями к сегментам

3. **Клиент запрашивает сегменты:**
   ```
   GET /api/v1/cameras/{id}/stream/hls/segment_001.ts
   GET /api/v1/cameras/{id}/stream/hls/segment_002.ts
   ...
   ```

4. **Сервер:**
   - Обслуживает сегменты из файловой системы
   - Устанавливает правильные заголовки для кэширования

## Использование в веб-интерфейсе

### VideoPlayer компонент

```typescript
// Автоматически использует HLS для RTSP потоков
<VideoPlayer
  camera={camera}
  streamType="hls"  // или "rtsp" (автоматически конвертируется в HLS)
  autoPlay={true}
/>
```

### streamService

```typescript
// Получить HLS URL
const hlsUrl = streamService.getHlsUrl(cameraId);
// Возвращает: /api/v1/cameras/{cameraId}/stream/hls/playlist.m3u8

// Запустить стрим
await streamService.startStream(cameraId);

// Получить статус стрима
const status = await streamService.getStreamStatus(cameraId);
// status.hlsUrl содержит URL к HLS плейлисту
```

## Структура файлов HLS

```
streams/hls/
├── {streamId}/
│   ├── playlist.m3u8          # HLS плейлист
│   ├── segment_001.ts         # Сегменты видео
│   ├── segment_002.ts
│   └── ...
```

## Настройки качества

Поддерживаются 4 уровня качества:

- **LOW:** 640x360, 500kbps, 15fps
- **MEDIUM:** 1280x720, 1500kbps, 25fps (по умолчанию)
- **HIGH:** 1920x1080, 3000kbps, 30fps
- **ULTRA:** 1920x1080, 6000kbps, 30fps, fast preset

Изменение качества:
```typescript
await streamService.setStreamQuality(cameraId, 'high');
```

## HTTP заголовки

### Плейлист (.m3u8):
- `Content-Type: application/vnd.apple.mpegurl`
- `Cache-Control: no-cache, no-store, must-revalidate`
- `Access-Control-Allow-Origin: *`

### Сегменты (.ts):
- `Content-Type: video/mp2t`
- `Cache-Control: public, max-age=3600`
- `Access-Control-Allow-Origin: *`

## Проверка работы

### 1. Запустить стрим:
```bash
curl -X POST http://localhost:8080/api/v1/cameras/{cameraId}/stream/start \
  -H "Authorization: Bearer {token}"
```

### 2. Получить HLS плейлист:
```bash
curl http://localhost:8080/api/v1/cameras/{cameraId}/stream/hls/playlist.m3u8 \
  -H "Authorization: Bearer {token}"
```

### 3. Проверить сегменты:
```bash
curl http://localhost:8080/api/v1/cameras/{cameraId}/stream/hls/segment_001.ts \
  -H "Authorization: Bearer {token}"
```

## Известные ограничения

1. **FFmpeg обязателен:** Для генерации HLS требуется установленный FFmpeg
2. **Задержка:** Первый сегмент генерируется через 4 секунды (hls_time)
3. **Дисковое пространство:** Сегменты хранятся на диске до удаления
4. **Одновременные стримы:** Каждый стрим требует отдельный процесс FFmpeg

## Следующие шаги

1. ✅ Интеграция HLS завершена
2. ⚠️ Оптимизация производительности (батчинг сегментов)
3. ⚠️ Поддержка адаптивного битрейта (HLS с несколькими вариантами качества)
4. ⚠️ WebRTC для низкой задержки (альтернатива HLS)

---

**Статус:** Готово к использованию
**Тестирование:** Требуется тестирование с реальными камерами

