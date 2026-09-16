# Отчёт: Интеграция аппаратного декодирования

**Дата:** 26 May 2026  
**Приоритет:** 4 Фазы 2  
**Статус:** 🟡 В процессе (интеграция завершена)  
**Время выполнения:** 26 May 2026

---

## 🎯 Цель

Интегрировать аппаратное декодирование видео (DXVA2 для Windows) в RTSP клиент с автоматическим fallback на программное декодирование.

---

## 📊 Выполненные работы

### 1. Базовая архитектура

**Файлы:**
- `hw_decoder.h` — Общий интерфейс
- `hw_decoder_dxva2.h` — DXVA2 заголовки
- `hw_decoder_dxva2.cpp` — DXVA2 реализация
- `hw_decoder_factory.cpp` — Factory для создания декодеров

**Реализовано:**
- ✅ Абстрактный класс `HWDecoder`
- ✅ Фабрика `HWDecoderFactory`
- ✅ DXVA2 базовая структура
- ✅ Автоматическое определение поддержки
- ✅ Fallback на программное декодирование

---

### 2. Интеграция в RTSP клиент

**Изменения в `rtsp_client.cpp`:**

#### Добавлено поле hwDecoder в RTPStream

```cpp
#ifdef ENABLE_FFMPEG
    // Параметры декодера аудио для данного потока
    struct AACDecoder* aacDecoder;
    struct G711Decoder* g711Decoder;
    struct AudioResampler* audioResampler;
    
    // Аппаратный декодер видео (DXVA2/VideoToolbox)
    HWDecoder* hwDecoder;
#endif
```

#### Инициализация hwDecoder в конструкторе

```cpp
RTPStream() : ...
#ifdef ENABLE_FFMPEG
    , aacDecoder(nullptr), g711Decoder(nullptr), 
      audioResampler(nullptr), hwDecoder(nullptr)
#endif
{}
```

#### Очистка hwDecoder

```cpp
static void cleanup_rtp_stream_decoders(RTPStream& stream) {
    if (stream.aacDecoder) {
        free_aac_decoder(stream.aacDecoder);
    }
    if (stream.g711Decoder) {
        free_g711_decoder(stream.g711Decoder);
    }
    if (stream.audioResampler) {
        free_audio_resampler(stream.audioResampler);
    }
#ifdef ENABLE_FFMPEG
    if (stream.hwDecoder) {
        delete stream.hwDecoder;
        stream.hwDecoder = nullptr;
    }
#endif
}
```

#### Инициализация в parse_sdp

```cpp
// Инициализация аппаратного декодера для видео потоков
#ifdef ENABLE_FFMPEG
if (currentStream->type == RTSP_STREAM_VIDEO && 
    (currentStream->codec == "H264" || currentStream->codec == "H265") &&
    !currentStream->hwDecoder) {
    // Пробуем создать аппаратный декодер
    CodecType codecType = stringToCodecType(currentStream->codec);
    currentStream->hwDecoder = HWDecoderFactory::create(
        codecType, 
        currentStream->width, 
        currentStream->height
    );
    
    if (currentStream->hwDecoder && client && client->statusCallback) {
        // Аппаратное декодирование поддерживается
        client->statusCallback(
            client,
            RTSP_STATUS_HW_DECODING_ENABLED,
            ("Аппаратное декодирование: " + 
             currentStream->hwDecoder->getName()).c_str(),
            client->statusUserData
        );
    }
}
#endif
```

#### Использование в process_rtp_payload

```cpp
#ifdef ENABLE_FFMPEG
if (stream.type == RTSP_STREAM_VIDEO &&
    (stream.codec == "H.264" || stream.codec == "H.265")) {
    // Пробуем использовать аппаратное декодирование
    HWDecoder* hwDecoder = stream.hwDecoder;
    
    if (hwDecoder) {
        // Аппаратное декодирование
        DecodedFrame decoded;
        bool decodedOk = hwDecoder->decode(nal.data.data(), nal.data.size(), 
                                           packet.timestamp, decoded);
        
        if (decodedOk && decoded.data) {
            // Использование FramePool для уменьшения аллокаций
            FramePool* pool = getFramePool();
            RTSPFrame* frame = pool->acquire();
            uint8_t* frameData = nullptr;
            
            try {
                // Получаем буфер из пула или выделяем новый
                if (pool->dataPool.size() > 0) {
                    std::lock_guard<std::mutex> lock(pool->poolMutex);
                    if (!pool->dataPool.empty()) {
                        frameData = pool->dataPool.back();
                        pool->dataPool.pop_back();
                    }
                }
                
                if (!frameData) {
                    frameData = new uint8_t[decoded.size];
                }
                
                memcpy(frameData, decoded.data, decoded.size);
                frame->data = frameData;
                frame->size = static_cast<int>(decoded.size);
                frame->timestamp = decoded.timestamp;
                frame->type = RTSP_STREAM_VIDEO;
                frame->width = decoded.width;
                frame->height = decoded.height;
                
                // Освобождаем память декодера
                delete[] decoded.data;
            } catch (const std::bad_alloc&) {
                pool->release(frame, frameData);
                delete[] decoded.data;
                continue;
            }
            
            // Обновляем AV синхронизацию
            client->avSync.videoTimestamp = packet.timestamp;
            client->avSync.videoClockMs =
                (packet.timestamp * 1000LL) / 
                (stream.clockRate > 0 ? stream.clockRate : 90000);
            
            RTSPFrameCallback videoCallback = nullptr;
            void* videoUserData = nullptr;
            {
                std::lock_guard<std::mutex> lock(client->mutex);
                videoCallback = client->videoCallback;
                videoUserData = client->videoUserData;
            }
            
            if (videoCallback && frame) {
                try {
                    videoCallback(frame, videoUserData);
                } catch (...) {
                    delete[] frame->data;
                    delete frame;
                }
            } else {
                pool->release(frame, frameData);
            }
            continue; // Пропускаем программное декодирование
        }
        
        // Если аппаратное декодирование не удалось, продолжаем с программным
    }
}
#endif // ENABLE_FFMPEG
```

---

## 📁 Изменённые файлы

| Файл | Изменения | Статус |
|------|-----------|--------|
| `rtsp_client.cpp` | +include hw_decoder.h<br>+hwDecoder поле<br>+инициализация<br>+очистка<br>+использование | ✅ Готово |
| `hw_decoder.h` | Общий интерфейс | ✅ Готово |
| `hw_decoder_dxva2.h` | DXVA2 заголовки | ✅ Готово |
| `hw_decoder_dxva2.cpp` | DXVA2 реализация | 🟡 Заглушка |
| `hw_decoder_factory.cpp` | Factory | ✅ Готово |

---

## 📈 Прогресс Приоритета 4

| Задача | Прогресс | Статус |
|--------|----------|--------|
| Frame Pool | 100% | ✅ |
| Базовая архитектура | 100% | ✅ |
| DXVA2 заглушка | 100% | ✅ |
| Интеграция с RTSP | 100% | ✅ |
| DXVA2 полная реализация | 0% | ⏳ |
| VideoToolbox | 0% | ⏳ |
| Тестирование | 0% | ⏳ |

**Прогресс Приоритета 4:** 35% → **55%**

---

## ⚠️ Известные ограничения

### DXVA2

1. **Заглушка декодирования:**
   - Текущая реализация `decode()` выделяет память и копирует данные
   - Полная реализация требует:
     - Обработка NAL units
     - Создание decoder input buffers
     - Вызов `DecodePicture()`
     - Конвертация из DXVA2 формата в RGB/YUV

2. **Требуются тесты:**
   - Реальное оборудование с DXVA2 поддержкой
   - Тесты с H.264 и H.265 потоками
   - Проверка memory leaks

---

## 🚀 План дальнейшей разработки

### Сессия 2: Полная реализация DXVA2 (27 May)

**Задачи:**
1. [ ] Реализовать обработку NAL units
2. [ ] Добавить создание decoder input buffers
3. [ ] Реализовать вызов `DecodePicture()`
4. [ ] Конвертация из DXVA2 в системную память
5. [ ] Тестирование с реальным оборудованием

### Сессия 3: VideoToolbox для macOS (28 May)

**Задачи:**
1. [ ] Реализовать `HWDecoderVideoToolbox`
2. [ ] Создать VTDecompressionSession
3. [ ] Обработка CMVideoFormatDescription
4. [ ] Интеграция с RTSP клиентом

### Сессия 4: Тестирование (29-30 May)

**Задачи:**
1. [ ] Базовое тестирование
2. [ ] Сравнение CPU usage
3. [ ] Проверка memory leaks
4. [ ] Создание отчёта

---

## 🎯 Ожидаемые результаты

### CPU usage

| Платформа | Программное | Аппаратное | Цель |
|-----------|-------------|------------|------|
| Windows (H.264) | ~10% | ~3% | <5% |
| Windows (H.265) | ~25% | ~3% | <5% |

### Video latency

| Метрика | Программное | Аппаратное | Цель |
|---------|-------------|------------|------|
| Average latency | ~200ms | ~100ms | <100ms |

---

**Автор отчёта:** AI Assistant  
**Дата:** 26 May 2026  
**Статус:** 🟡 В процессе (интеграция завершена)  
**Следующая сессия:** Полная реализация DXVA2 (27 May 2026)
