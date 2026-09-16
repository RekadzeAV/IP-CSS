# Руководство по интеграционным тестам

**Дата:** 27 January 2026
**Этап:** 2.3 - Интеграционные тесты

---

## 📋 Обзор

Интеграционные тесты проверяют работу FFmpeg декодирования в RTSP клиенте на реальных камерах.

---

## 🚀 Быстрый старт

### 1. Подготовка конфигурации

Создайте файл `test_config.json` на основе примера:

```bash
cp native/video-processing/test/test_config.json.example native/video-processing/test/test_config.json
```

Отредактируйте `test_config.json` и укажите параметры ваших тестовых камер.

### 2. Сборка тестов

```bash
cd native/video-processing/test
mkdir build && cd build

cmake .. \
    -DCMAKE_BUILD_TYPE=Release \
    -DENABLE_FFMPEG=ON

cmake --build . --config Release
```

### 3. Запуск тестов

#### Только unit тесты:
```bash
./video_processing_tests
```

#### Unit тесты + интеграционные тесты:
```bash
./video_processing_tests --integration
```

#### С указанием конфигурационного файла:
```bash
./video_processing_tests --integration --config /path/to/test_config.json
```

---

## 📝 Формат конфигурационного файла

```json
{
  "cameras": [
    {
      "name": "Test Camera 1",
      "url": "rtsp://192.168.1.100:554/stream",
      "username": "admin",
      "password": "password",
      "codec": "H264",
      "resolution": "1920x1080",
      "fps": 25,
      "profile": "Main"
    }
  ],
  "test_settings": {
    "connection_timeout_ms": 5000,
    "frame_receive_timeout_ms": 5000,
    "decoding_test_duration_seconds": 10,
    "min_fps_threshold": 10.0,
    "min_frames_received": 10
  }
}
```

### Поля конфигурации:

**Camera:**
- `name` - Имя камеры (для идентификации в логах)
- `url` - RTSP URL камеры
- `username` - Имя пользователя (опционально, можно оставить пустым)
- `password` - Пароль (опционально)
- `codec` - Кодек: "H264" или "H265"
- `resolution` - Разрешение в формате "WIDTHxHEIGHT"
- `fps` - Частота кадров
- `profile` - Профиль кодек (для H.264: "Baseline", "Main", "High")

**Test Settings:**
- `connection_timeout_ms` - Таймаут подключения (мс)
- `frame_receive_timeout_ms` - Таймаут получения первого кадра (мс)
- `decoding_test_duration_seconds` - Длительность теста декодирования (сек)
- `min_fps_threshold` - Минимальный FPS для успешного теста
- `min_frames_received` - Минимальное количество кадров для успешного теста

---

## 🧪 Типы тестов

### 1. Тест подключения (`test_rtsp_connection`)

**Что проверяет:**
- Успешное подключение к RTSP камере
- Получение первого видеокадра
- Корректность метаданных (разрешение, кодек)

**Критерии успеха:**
- Подключение установлено
- Получен хотя бы один кадр

### 2. Тест декодирования (`test_frame_decoding`)

**Что проверяет:**
- Успешное декодирование видеокадров
- Производительность (FPS)
- Стабильность декодирования

**Критерии успеха:**
- Декодировано минимум 10 кадров
- Средний FPS ≥ 10

---

## 📊 Интерпретация результатов

### Успешный тест:
```
Testing connection to: Test Camera 1
  URL: rtsp://192.168.1.100:554/stream
  ✅ Connected successfully
  ✅ Playing
  ✅ Received 25 frames
PASSED
```

### Неуспешный тест:
```
Testing connection to: Test Camera 1
  URL: rtsp://192.168.1.100:554/stream
  ❌ Connection failed
FAILED: test_rtsp_connection_Test Camera 1 - Connection failed
```

### Метрики производительности:
```
Testing frame decoding: Test Camera 1
  Frames decoded: 250
  Average FPS: 25.0
PASSED
```

---

## 🐛 Решение проблем

### Проблема: Connection failed

**Возможные причины:**
1. Неправильный RTSP URL
2. Камера недоступна в сети
3. Неправильные учетные данные
4. Брандмауэр блокирует соединение

**Решение:**
- Проверьте доступность камеры: `ping <camera_ip>`
- Проверьте RTSP URL в VLC или другом плеере
- Проверьте учетные данные
- Проверьте настройки брандмауэра

### Проблема: No frames received

**Возможные причины:**
1. Камера не отправляет поток
2. Проблемы с RTP/RTCP
3. Неправильный кодек

**Решение:**
- Проверьте, что камера отправляет поток
- Проверьте кодек в SDP ответе
- Проверьте логи RTSP клиента

### Проблема: Low FPS

**Возможные причины:**
1. Медленная сеть
2. Высокая нагрузка на CPU
3. Проблемы с декодированием

**Решение:**
- Проверьте сетевую задержку
- Проверьте использование CPU
- Попробуйте более низкое разрешение

---

## 📈 Метрики производительности

Тесты собирают следующие метрики:

- **Frames Received** - Количество полученных кадров
- **Frames Decoded** - Количество декодированных кадров
- **Average FPS** - Средний FPS декодирования
- **Connection Time** - Время подключения
- **First Frame Time** - Время до первого кадра

---

## 🔧 Расширенные настройки

### Тестирование различных профилей H.264

Создайте несколько записей в конфигурации для разных профилей:

```json
{
  "cameras": [
    {
      "name": "H.264 Baseline 640x480",
      "codec": "H264",
      "resolution": "640x480",
      "fps": 15,
      "profile": "Baseline"
    },
    {
      "name": "H.264 Main 1280x720",
      "codec": "H264",
      "resolution": "1280x720",
      "fps": 25,
      "profile": "Main"
    },
    {
      "name": "H.264 High 1920x1080",
      "codec": "H264",
      "resolution": "1920x1080",
      "fps": 30,
      "profile": "High"
    }
  ]
}
```

### Длительное тестирование стабильности

Для тестирования стабильности увеличьте `decoding_test_duration_seconds`:

```json
{
  "test_settings": {
    "decoding_test_duration_seconds": 3600  // 1 час
  }
}
```

---

## 📝 Запись результатов

Результаты тестирования выводятся в консоль. Для сохранения в файл:

```bash
./video_processing_tests --integration > test_results.txt 2>&1
```

---

## ✅ Критерии приемки

Интеграционные тесты считаются успешными, если:

- [ ] Успешное подключение ко всем тестовым камерам
- [ ] Получение видеокадров от всех камер
- [ ] Успешное декодирование кадров
- [ ] FPS ≥ 10 для всех камер
- [ ] Нет утечек памяти (проверяется вручную)

---

**Последнее обновление:** 27 January 2026
