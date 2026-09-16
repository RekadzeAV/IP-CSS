# Стратегия кэширования

Краткое описание TTL и правил инвалидации кэшей для поддержки и расширения.

---

## 1. CameraRepository (shared)

### 1.1 Кэш списка камер (`getCameras()` / `getCameraById()`)

| Параметр    | Значение        |
|------------|-----------------|
| **TTL**    | 5 минут         |
| **Ключ**   | `allCamerasCacheKey` — список; `id` — по камере |
| **Инвалидация** | При `addCamera`, `updateCamera`, `removeCamera`: сброс `allCamerasCacheKey` и записей по `id`. |

### 1.2 Discovery cache (`discoverCameras()`)

| Параметр    | Значение        |
|------------|-----------------|
| **TTL**    | 5–7 минут (рекомендуется 7 мин, как в текущем Impl) |
| **Ключ**   | `"discovered_cameras"` (один ключ на весь список) |
| **Инвалидация** | По явному действию пользователя «Обновить список обнаруженных» — вызов очистки discovery cache. Не инвалидируется при add/update/remove камеры. |

### 1.3 Status cache (`getCameraStatus(id)`)

| Параметр    | Значение        |
|------------|-----------------|
| **TTL**    | 15–30 секунд (рекомендуется 20 сек для согласованности с Impl) |
| **Ключ**   | `"status_$id"` по id камеры |
| **Инвалидация** | При `updateCamera(id, …)` и `removeCamera(id)`: удаление `"status_$id"`. При добавлении камеры — при необходимости сброс `status_${newCamera.id}`. |

### 1.4 Общие правила (CameraRepository)

- **Реализация:** общий TTL-кэш (например, `CameraCache`) с `maxSize`, `expirationTime`, методами `get`/`put`/`remove`/`clear`.
- **Поведение при переполнении:** вытеснение по давности (LRU или по времени последнего доступа).
- **Потокобезопасность:** доступ к кэшу под мьютексом (или аналог для KMP).

---

## 2. Сетевой слой (core/network)

### 2.1 ApiClient — ResponseCache

| Параметр    | Значение        |
|------------|-----------------|
| **TTL**    | 5 минут (настраивается) |
| **Ключ**   | `path` + query-параметры (без учёта авторизации — см. аудит безопасности) |
| **Инвалидация** | По TTL; ручной вызов `clearCache()` при необходимости. |

### 2.2 OnvifClient

| Кэш            | TTL      | Инвалидация |
|----------------|----------|-------------|
| Capabilities   | 5 мин    | `clearCapabilitiesCache(url)`, `invalidateCache(url)`, `clearAllCaches()` |
| Device Info   | 10 мин   | `clearDeviceInfoCache(url)`, `invalidateCache(url)`, `clearAllCaches()` |
| Profiles      | 5 мин    | `invalidateCache(url)`, `clearAllCaches()` |
| Digest Auth   | по сессии| При stale nonce или по `invalidateCache(url)` |

---

## 3. Рекомендации для поддержки

1. **Не уменьшать TTL без обоснования:** более частые запросы к discovery/ONVIF увеличивают нагрузку на сеть и устройства.
2. **При добавлении новых кэшей:** зафиксировать TTL и правила инвалидации в этом документе.
3. **Тесты:** проверять cache hit в пределах TTL и обращение к источнику после истечения TTL или после инвалидации.
4. **CameraRepository V2:** при переносе логики из `CameraRepositoryImpl` сохранять те же TTL и правила инвалидации (см. п. 1.1–1.3), чтобы поведение было предсказуемым.

---

**Связанные документы:** `docs/MVP_PHASED_IMPLEMENTATION_PLAN.md` (Фаза 4), `docs/VARIABLE_AND_SECURITY_AUDIT_REPORT.md` (кэши и ключи).
