# Тестовые данные для unit тестов

Этот каталог содержит тестовые данные для unit тестов декодирования.

## Структура

```
test_data/
├── h264/
│   ├── sps.bin          # H.264 SPS (Sequence Parameter Set)
│   ├── pps.bin          # H.264 PPS (Picture Parameter Set)
│   ├── idr_frame.bin    # H.264 IDR frame
│   └── p_frame.bin      # H.264 P frame
├── h265/
│   ├── vps.bin          # H.265 VPS (Video Parameter Set)
│   ├── sps.bin          # H.265 SPS
│   ├── pps.bin          # H.265 PPS
│   ├── idr_frame.bin    # H.265 IDR frame
│   └── p_frame.bin      # H.265 P frame
└── audio/
    ├── aac_frame.bin    # AAC audio frame
    ├── pcmu_frame.bin   # G.711 PCMU frame
    └── pcma_frame.bin   # G.711 PCMA frame
```

## Генерация тестовых данных

Тестовые данные можно сгенерировать с помощью FFmpeg или создать вручную.

### Генерация H.264 тестовых данных

```bash
# Создать тестовый H.264 файл
ffmpeg -f lavfi -i testsrc=duration=1:size=640x480:rate=1 -c:v libx264 -preset ultrafast -tune zerolatency -f h264 test.h264

# Извлечь SPS/PPS
ffmpeg -i test.h264 -c:v copy -bsf:v h264_mp4toannexb -f h264 test_annexb.h264
```

### Генерация H.265 тестовых данных

```bash
# Создать тестовый H.265 файл
ffmpeg -f lavfi -i testsrc=duration=1:size=640x480:rate=1 -c:v libx265 -preset ultrafast -tune zerolatency -f hevc test.hevc
```

## Формат данных

### H.264 NAL Units

- **SPS**: Начинается с `0x67` (NAL type 7)
- **PPS**: Начинается с `0x68` (NAL type 8)
- **IDR**: Начинается с `0x65` (NAL type 5)
- **P frame**: Начинается с `0x41` (NAL type 1)

### H.265 NAL Units

- **VPS**: Начинается с `0x40` (NAL type 32)
- **SPS**: Начинается с `0x42` (NAL type 33)
- **PPS**: Начинается с `0x44` (NAL type 34)
- **IDR**: Начинается с `0x26` (NAL type 19 или 20)

## Использование в тестах

Тестовые данные загружаются в тестах через функции чтения файлов:

```cpp
std::vector<uint8_t> load_test_data(const std::string& filename) {
    std::ifstream file(filename, std::ios::binary);
    // ...
}
```
