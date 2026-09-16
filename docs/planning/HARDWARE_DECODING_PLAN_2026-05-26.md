# План: Аппаратное декодирование (DXVA2/VideoToolbox)

**Дата:** 26 May 2026  
**Приоритет:** 4 Фазы 2  
**Статус:** 🟡 Планирование  
**Срок:** 27-30 May 2026 (4 дня)

---

## 🎯 Цель

Интегрировать аппаратное декодирование видео для снижения CPU usage с ~10% до ~3% на один поток.

---

## 📋 План реализации

### Сессия 1: DXVA2 для Windows (27 May)

**Задачи:**
1. [ ] Создать структуру данных для DXVA2
2. [ ] Реализовать инициализацию Direct3D 11
3. [ ] Реализовать создание DXVA2 decoder
4. [ ] Добавить обработку поверхностей декодирования
5. [ ] Интегрировать в RTSP клиент

**Файлы:**
- `native/video-processing/src/hw_decoder_dxva2.h`
- `native/video-processing/src/hw_decoder_dxva2.cpp`

### Сессия 2: VideoToolbox для macOS (28 May)

**Задачи:**
1. [ ] Создать структуру данных для VideoToolbox
2. [ ] Реализовать VTDecompressionSession
3. [ ] Обработка CMVideoFormatDescription
4. [ ] Интеграция в RTSP клиент

**Файлы:**
- `native/video-processing/src/hw_decoder_vt.h`
- `native/video-processing/src/hw_decoder_vt.cpp`

### Сессия 3: Автоматическое определение поддержки (29 May)

**Задачи:**
1. [ ] Проверка поддержки DXVA2 (Direct3D 11)
2. [ ] Проверка поддержки VideoToolbox
3. [ ] Fallback на программное декодирование
4. [ ] Логирование результатов

**Файлы:**
- `native/video-processing/src/hw_decoder_factory.h`
- `native/video-processing/src/hw_decoder_factory.cpp`

### Сессия 4: Тестирование и отчёт (30 May)

**Задачи:**
1. [ ] Базовое тестирование (подключение)
2. [ ] Тестирование производительности
3. [ ] Сравнение CPU usage
4. [ ] Создание отчёта

**Выходные данные:**
- `docs/reports/HARDWARE_DECODING_TEST_RESULTS_2026-05-30.md`

---

## 📊 Метрики успеха

### DXVA2 (Windows)

| Метрика | Цель | Статус |
|---------|------|--------|
| CPU usage (H.264) | <5% | ⏳ |
| CPU usage (H.265) | <3% | ⏳ |
| Video latency | <100ms | ⏳ |
| Memory usage | <100MB | ⏳ |

### VideoToolbox (macOS)

| Метрика | Цель | Статус |
|---------|------|--------|
| CPU usage (H.264) | <5% | ⏳ |
| CPU usage (H.265) | <3% | ⏳ |
| Video latency | <100ms | ⏳ |
| Memory usage | <100MB | ⏳ |

### Fallback

| Метрика | Цель | Статус |
|---------|------|--------|
| Fallback time | <1 sec | ⏳ |
| No frame loss | ✅ | ⏳ |
| Seamless transition | ✅ | ⏳ |

---

## 🔧 Техническая реализация

### DXVA2 Architecture

```cpp
// Псевдокод архитектуры
class HWDecoderDXVA2 {
    ID3D11Device* d3dDevice;
    ID3D11DeviceContext* d3dContext;
    ID3D11VideoDevice* videoDevice;
    ID3D11VideoContext* videoContext;
    ID3D11VideoDecoder* decoder;
    ID3D11VideoDecoderOutputView* decoderOutput;
    
    bool init();
    bool decode(const uint8_t* nal, size_t size, uint8_t* output);
    void release();
};
```

### VideoToolbox Architecture

```cpp
// Псевдокод архитектуры
class HWDecoderVideoToolbox {
    VTDecompressionSessionRef decompressionSession;
    CMVideoFormatDescriptionRef formatDescription;
    
    bool init();
    bool decode(const uint8_t* nal, size_t size, uint8_t* output);
    void release();
};
```

### Factory Pattern

```cpp
class HWDecoderFactory {
    static std::unique_ptr<HWDecoder> create();
    
    // Проверка поддержки
    static bool isDXVA2Supported();
    static bool isVideoToolboxSupported();
    
    // Создание декодера
    static std::unique_ptr<HWDecoder> createDecoder(Codec codec);
};
```

---

## 📁 Структура файлов

```
native/video-processing/src/
├── hw_decoder.h              # Общий интерфейс
├── hw_decoder.cpp            # Общая реализация
├── hw_decoder_dxva2.h        # DXVA2 заголовки
├── hw_decoder_dxva2.cpp      # DXVA2 реализация
├── hw_decoder_vt.h           # VideoToolbox заголовки
├── hw_decoder_vt.cpp         # VideoToolbox реализация
├── hw_decoder_factory.h      # Factory заголовки
└── hw_decoder_factory.cpp    # Factory реализация
```

---

## ⚠️ Зависимости

### Windows (DXVA2)

```cmake
find_package(D3D11 REQUIRED)
find_package(D3D12)  # Опционально
include_directories(${D3D11_INCLUDE_DIRS})
target_link_libraries(video-processing ${D3D11_LIBRARIES} d3d11.lib)
```

### macOS (VideoToolbox)

```cmake
find_library(VIDEOTOOLBOX VideoToolbox REQUIRED)
find_library(COREMEDIA CoreMedia REQUIRED)
find_library(COREVIDEO CoreVideo REQUIRED)
target_link_libraries(video-processing ${VIDEOTOOLBOX} ${COREMEDIA} ${COREVIDEO})
```

---

## 🎯 Критерии завершения Приоритета 4

### MVP Ready

- [ ] DXVA2 реализован
- [ ] VideoToolbox реализован
- [ ] Fallback на программное декодирование
- [ ] CPU usage <15% подтверждено

### Production Ready

- [ ] DXVA2 + VideoToolbox протестированы
- [ ] CPU usage <10% подтверждено
- [ ] Video latency <100ms подтверждено
- [ ] Отчёт по производительности создан

---

**План утверждён:** ________________  
**Дата начала:** 27 May 2026  
**Дата окончания:** 30 May 2026  
**Ответственный:** AI Assistant
