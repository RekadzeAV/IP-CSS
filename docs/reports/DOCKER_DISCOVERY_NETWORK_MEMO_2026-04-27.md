# Docker Discovery Network Memo (2026-04-27)

## Почему `discover` может быть пустым в Docker

В стандартной bridge-сети Docker multicast/broadcast discovery (ONVIF WS-Discovery, SSDP/UPnP) нередко ограничен. Это приводит к ситуации:

- сервис жив и отвечает;
- ручные подключения к известным IP работают;
- `GET /api/v1/cameras/discover` через multicast дает пустой список.

## Принятая стратегия

В manual-стенде применяем двухступенчатую модель:

1. ONVIF discover (как основной путь).
2. Fallback по known hosts при пустом ONVIF результате:
   - источник: `config/test-cameras.local.json`;
   - active probe TCP (RTSP и HTTP порты);
   - в ответ включаются только reachable камеры.

Маркер fallback-записей в ответе:

- `model = known-host-fallback`
- `manufacturer = configured`

## Настройки в Docker manual профиле

В `docker-compose.manual.yml`:

- `DISCOVERY_FALLBACK_ENABLED=true`
- `DISCOVERY_KNOWN_HOSTS_CONFIG=/app/discovery-config/test-cameras.local.json`
- volume: `./config:/app/discovery-config:ro`

Опционально:

- `DISCOVERY_CONNECT_TIMEOUT_MS` (таймаут active probe в миллисекундах)

## Операционная проверка

1. Перезапустить стек:
   - `docker compose --env-file .env.docker-manual -f docker-compose.yml -f docker-compose.manual.yml up --build -d`
2. Запустить smoke:
   - `.\scripts\server-auth-discovery-smoke.ps1`
3. Проверить, что:
   - login/refresh/ws-token успешны;
   - `discover.count > 0` (если known hosts reachable).
4. Для диагностики динамики проверить:
   - `GET /api/v1/health/metrics` (admin JWT),
   - рост счётчиков `discoverRequests`, `discoverFallbackUsed`, `auth*`.

## Что делать, если снова пусто

- Проверить доступность IP/портов из `config/test-cameras.local.json`.
- Проверить, что файл смонтирован в контейнер.
- Проверить логи `discover_result ... usedFallback=...`.
- Если в окружении multicast доступен, оставлять ONVIF как первичный путь; fallback нужен как гарантированный путь для Docker/CI/manual стендов.
