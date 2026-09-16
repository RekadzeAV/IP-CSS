# Интеграция Janus Media Server для WebRTC

## Обзор

Janus Gateway - это легковесный WebRTC медиа-сервер с открытым исходным кодом, который используется для преобразования RTSP потоков в WebRTC для низкой задержки видеотрансляции в веб-интерфейсе.

## Архитектура

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Browser   │◄────────┤ Janus Gateway│◄────────┤  RTSP       │
│  (WebRTC)   │         │  (Media      │         │  Camera     │
│             │         │   Server)    │         │             │
└─────────────┘         └──────────────┘         └─────────────┘
                              ▲
                              │
                              │ HTTP API
                              │
                       ┌──────┴──────┐
                       │   Backend   │
                       │   (Ktor)    │
                       └─────────────┘
```

## Компоненты

### 1. JanusGatewayService

Сервис для взаимодействия с Janus Gateway через HTTP API.

**Основные методы:**
- `createSession()` - создание сессии в Janus
- `attachToRtspPlugin()` - присоединение к плагину RTSP-to-WebRTC
- `createRtspStream()` - создание RTSP потока в Janus
- `startStream()` - запуск воспроизведения потока
- `handleOffer()` - обработка WebRTC offer и создание answer
- `stopStream()` - остановка потока
- `destroyStream()` - удаление потока
- `destroySession()` - уничтожение сессии

**Расположение:** `server/api/src/main/kotlin/com/company/ipcamera/server/service/JanusGatewayService.kt`

### 2. WebRtcService

Обновленный сервис для работы с WebRTC соединениями, интегрированный с Janus Gateway.

**Основные методы:**
- `handleOffer()` - обработка WebRTC offer от клиента через Janus
- `closeConnection()` - закрытие соединения с очисткой ресурсов Janus
- `closeConnectionsForCamera()` - закрытие всех соединений для камеры

**Расположение:** `server/api/src/main/kotlin/com/company/ipcamera/server/service/WebRtcService.kt`

### 3. API Endpoints

#### POST `/api/v1/cameras/{id}/stream/webrtc/offer`

Обработка WebRTC offer от клиента.

**Request:**
```json
{
  "offer": {
    "type": "offer",
    "sdp": "v=0\r\no=- ..."
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "answer": {
      "type": "answer",
      "sdp": "v=0\r\no=- ..."
    },
    "iceCandidates": [
      {
        "candidate": "candidate:...",
        "sdpMid": "0",
        "sdpMLineIndex": 0
      }
    ]
  },
  "message": "WebRTC offer processed successfully"
}
```

## Установка и настройка Janus Gateway

### Вариант 1: Docker (рекомендуется)

```bash
# Запуск Janus Gateway через Docker
docker run -d \
  --name janus-gateway \
  -p 8088:8088 \
  -p 8089:8089 \
  -p 8188:8188 \
  -p 8189:8189 \
  -p 20000-20010:20000-20010/udp \
  canyan/janus-gateway:latest
```

### Вариант 2: Установка из исходников

#### Требования

- Ubuntu/Debian:
  ```bash
  sudo apt-get update
  sudo apt-get install libmicrohttpd-dev libjansson-dev \
    libssl-dev libglib2.0-dev libopus-dev libogg-dev \
    libcurl4-openssl-dev liblua5.3-dev libconfig-dev \
    pkg-config libtool automake libsrtp2-dev
  ```

#### Компиляция и установка

```bash
# Клонирование репозитория
git clone https://github.com/meetecho/janus-gateway.git
cd janus-gateway

# Компиляция
sh autogen.sh
./configure --prefix=/opt/janus --enable-post-processing
make
sudo make install
```

#### Конфигурация

Основной конфигурационный файл: `/opt/janus/etc/janus/janus.jcfg`

```json
{
  "general": {
    "configs_folder": "/opt/janus/etc/janus",
    "plugins_folder": "/opt/janus/lib/janus/plugins",
    "transports_folder": "/opt/janus/lib/janus/transports",
    "events_folder": "/opt/janus/lib/janus/events",
    "loggers_folder": "/opt/janus/lib/janus/loggers",
    "debug_level": 4,
    "debug_timestamps": true,
    "interface": "0.0.0.0",
    "port": 8088,
    "https": false,
    "secure_port": 8089,
    "server_name": "Janus WebRTC Server"
  },
  "plugins": {
    "disable": "libjanus_voicemail.so,libjanus_recordplay.so,libjanus_videocall.so,libjanus_audiobridge.so,libjanus_videoroom.so,libjanus_textroom.so,libjanus_sip.so,libjanus_nosip.so,libjanus_echotest.so,libjanus_streaming.so"
  }
}
```

Конфигурация плагина streaming: `/opt/janus/etc/janus/janus.plugin.streaming.jcfg`

```json
{
  "streaming": {
    "general": {
      "enabled": true,
      "rtsp_port": 8554
    },
    "rtsp": {
      "enabled": true,
      "rtp_port_range": "20000-20010"
    }
  }
}
```

#### Запуск

```bash
# Запуск Janus Gateway
/opt/janus/bin/janus --configs-folder=/opt/janus/etc/janus
```

### Вариант 3: Использование готового Docker Compose

Создайте файл `docker-compose.janus.yml`:

```yaml
version: '3.8'

services:
  janus:
    image: canyan/janus-gateway:latest
    container_name: janus-gateway
    ports:
      - "8088:8088"   # HTTP API
      - "8089:8089"   # HTTPS API
      - "8188:8188"   # WebSocket
      - "8189:8189"   # Secure WebSocket
      - "20000-20010:20000-20010/udp"  # RTP порты
    environment:
      - JANUS_CONFIG_PATH=/opt/janus/etc/janus
    volumes:
      - ./janus-config:/opt/janus/etc/janus
    restart: unless-stopped
```

Запуск:
```bash
docker-compose -f docker-compose.janus.yml up -d
```

## Настройка Backend

### Переменные окружения

Добавьте следующие переменные окружения для включения Janus Gateway:

```bash
# Включить Janus Gateway
JANUS_ENABLED=true

# URL Janus Gateway API
JANUS_URL=http://localhost:8088/janus
```

### Пример конфигурации для production

```bash
# .env файл
JANUS_ENABLED=true
JANUS_URL=http://janus-gateway:8088/janus
```

## Проверка работы

### 1. Проверка доступности Janus Gateway

```bash
curl http://localhost:8088/janus/info
```

Ожидаемый ответ:
```json
{
  "name": "Janus WebRTC Server",
  "version": 1,
  "version_string": "1.0.0",
  "author": "Meetecho",
  "server_name": "Janus WebRTC Server",
  "data_structures": {
    "session_id": 64,
    "handle_id": 64,
    "session_timeout": 60,
    "candidates_timeout": 45,
    "transports": {},
    "events": {},
    "plugins": {}
  }
}
```

### 2. Тестирование WebRTC соединения

Используйте веб-интерфейс приложения для тестирования WebRTC потока. При выборе WebRTC в видеоплеере:

1. Откройте DevTools → Network
2. Найдите запрос к `/api/v1/cameras/{id}/stream/webrtc/offer`
3. Проверьте, что ответ содержит `answer` с SDP

### 3. Проверка логов

**Backend логи:**
```
INFO  - Created Janus session: 1234567890
INFO  - Attached to RTSP plugin: handle=9876543210, session=1234567890
INFO  - Created RTSP stream in Janus: streamId=1, rtspUrl=rtsp://...
INFO  - Started RTSP stream in Janus: streamId=1
INFO  - Created WebRTC answer via Janus for session=1234567890, handle=9876543210
```

**Janus логи:**
```
[INFO] Creating new session: 1234567890
[INFO] Creating new handle: 9876543210
[INFO] RTSP stream created: streamId=1
[INFO] WebRTC offer processed, answer created
```

## Troubleshooting

### Проблема: Janus Gateway недоступен

**Решение:**
1. Проверьте, что Janus запущен: `docker ps | grep janus`
2. Проверьте логи: `docker logs janus-gateway`
3. Проверьте переменную окружения `JANUS_URL`

### Проблема: WebRTC соединение не устанавливается

**Решение:**
1. Проверьте, что RTP порты открыты (20000-20010 UDP)
2. Проверьте firewall настройки
3. Убедитесь, что STUN/TURN серверы настроены правильно

### Проблема: RTSP поток не создается в Janus

**Решение:**
1. Проверьте доступность RTSP URL камеры
2. Проверьте логи Janus для ошибок RTSP
3. Убедитесь, что плагин `streaming` включен в конфигурации

### Проблема: Высокая задержка

**Решение:**
1. Используйте TURN сервер для NAT traversal
2. Настройте оптимальные параметры кодека в Janus
3. Проверьте сетевую задержку между компонентами

## Производительность

### Рекомендации

1. **Масштабирование:** Используйте несколько экземпляров Janus Gateway с балансировщиком нагрузки
2. **Ресурсы:** Каждое WebRTC соединение требует ~10-50 MB RAM
3. **CPU:** Кодирование/декодирование видео требует значительных ресурсов CPU
4. **Сеть:** Используйте выделенную сеть для RTP трафика

### Мониторинг

- **Метрики Janus:** Используйте `/janus/info` endpoint для получения статистики
- **Логи:** Включите debug логи для диагностики проблем
- **Метрики Backend:** Отслеживайте количество активных WebRTC соединений

## Безопасность

1. **HTTPS:** Используйте HTTPS для Janus API в production
2. **Аутентификация:** Настройте аутентификацию для Janus API
3. **Firewall:** Ограничьте доступ к RTP портам только необходимым IP
4. **TLS:** Используйте DTLS для WebRTC соединений

## Альтернативы

Если Janus Gateway не подходит для вашего случая использования, рассмотрите:

1. **Kurento Media Server** - более функциональный, но более тяжелый
2. **MediaSoup** - Node.js медиа-сервер
3. **Pion WebRTC** - Go библиотека для создания собственного медиа-сервера
4. **GStreamer WebRTC** - низкоуровневое решение на основе GStreamer

## Дополнительные ресурсы

- [Janus Gateway Documentation](https://janus.conf.meetecho.com/docs/)
- [Janus Gateway GitHub](https://github.com/meetecho/janus-gateway)
- [WebRTC API Documentation](https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API)
