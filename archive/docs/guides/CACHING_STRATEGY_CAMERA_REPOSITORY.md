# Стратегия кэширования CameraRepository (V2)

Краткое описание кэшей в `CameraRepositoryImplV2` для поддержки и отладки.

## Кэши

| Кэш | Ключ | TTL | Назначение |
|-----|------|-----|------------|
| **Discovery** | `"discovered_cameras"` | 6 минут | Результаты `discoverCameras()` (ONVIF WS-Discovery). |
| **Status** | `"status_$id"` | 20 секунд | Статус камеры по ID для `getCameraStatus(id)`. |
| **Cameras** | `"all_cameras"` и по `id` | 5 минут | Списки камер и камера по ID (как и раньше). |

## Инвалидация

- **Status cache:** при `addCamera`, `updateCamera`, `removeCamera` для соответствующего `id` вызывается `statusCache.remove("status_$id")`.
- **Discovery cache:** при явном действии «обновить список обнаруженных» вызывается `discoverCameras(forceRefresh = true)` — перед новым обнаружением кэш discovery очищается для ключа `"discovered_cameras"`.
- **Cameras cache:** при add/update/remove по-прежнему инвалидируется `all_cameras` и запись по `id`.

## Поведение по TTL

- В пределах TTL повторные вызовы возвращают данные из кэша (без запроса к источнику).
- После истечения TTL запись считается устаревшей, при следующем запросе выполняется обращение к источнику и кэш обновляется.

## Файлы

- `shared/.../CameraRepositoryImplV2.kt` — реализация (внутренний класс `CameraCache`).
- Интерфейс: `discoverCameras(forceRefresh: Boolean = false)` в `CameraRepository`.
