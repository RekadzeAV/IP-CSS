# План реализации 4.4 ONVIF клиент

**Версия:** 1.1
**Дата:** 26 January 2026
**Текущий прогресс:** ~75% 🟡

---

## 📋 Обзор текущего состояния

### ✅ Реализовано (75%)
- ✅ Базовые методы ONVIF (getCapabilities, getDeviceInformation, getProfiles, getStreamUri)
- ✅ PTZ управление (movePtz, stopPtz, zoomIn, zoomOut)
- ✅ testConnection() с детальной диагностикой
- ✅ WS-Discovery улучшен (улучшенный XML парсинг, множественные Probe запросы)
- ✅ Полноценный XML парсинг (улучшенный с поддержкой разных namespace)
- ✅ Fallback механизмы для парсинга XML
- ✅ Обработка ошибок и retry логика
- ✅ Поддержка Basic Authentication
- ✅ Digest Authentication (базовая реализация готова, требуется тестирование)

### 🟡 Требуется доработка (25%)
- 🟡 Digest Authentication (тестирование и edge cases)
- ❌ UPnP как альтернатива WS-Discovery
- ❌ Дополнительные ONVIF методы (Events, Analytics, Imaging)
- ❌ Улучшение обработки SOAP Fault
- ❌ Кэширование capabilities и профилей
- ❌ Поддержка ONVIF Profile S/T/M

---

## 🎯 Цели реализации

1. **Завершить базовую функциональность MVP** - Digest Authentication
2. **Улучшить обнаружение устройств** - UPnP поддержка
3. **Повысить надежность** - улучшенная обработка ошибок
4. **Оптимизировать производительность** - кэширование

---

## 📝 Пошаговый план реализации

### Этап 1: Digest Authentication (КРИТИЧНО для MVP)

**Приоритет:** 🔴 Высокий
**Оценка времени:** 3-5 дней
**Зависимости:** Нет

#### Шаг 1.1: Изучение спецификации Digest Authentication
- [ ] Изучить RFC 2617 (HTTP Digest Authentication)
- [ ] Изучить спецификаю ONVIF Digest Authentication
- [ ] Проанализировать примеры реализации в других библиотеках
- [ ] Определить алгоритмы (MD5, SHA-256)

**Результат:** Документ с требованиями и алгоритмами

#### Шаг 1.2: Создание DigestAuthHelper класса
- [ ] Создать `DigestAuthHelper.kt` в `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/auth/`
- [ ] Реализовать метод `generateDigestAuthHeader()`:
  - [ ] Парсинг WWW-Authenticate заголовка
  - [ ] Извлечение realm, nonce, qop, algorithm
  - [ ] Генерация HA1 (MD5(username:realm:password))
  - [ ] Генерация HA2 (MD5(method:uri))
  - [ ] Генерация response (MD5(HA1:nonce:nc:cnonce:qop:HA2))
  - [ ] Формирование Authorization заголовка
- [ ] Реализовать поддержку MD5 и SHA-256
- [ ] Реализовать поддержку qop (auth, auth-int)
- [ ] Добавить обработку stale nonce

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/auth/DigestAuthHelper.kt`

**Результат:** Класс DigestAuthHelper с полной поддержкой Digest Authentication

#### Шаг 1.3: Интеграция Digest Authentication в OnvifClient
- [ ] Модифицировать метод `sendSoapRequest()`:
  - [ ] Обнаружение 401 Unauthorized ответа
  - [ ] Извлечение WWW-Authenticate заголовка
  - [ ] Определение типа аутентификации (Basic/Digest)
  - [ ] Вызов DigestAuthHelper для Digest
  - [ ] Повторная отправка запроса с Authorization заголовком
- [ ] Добавить поддержку nonce counting (nc)
- [ ] Добавить поддержку client nonce (cnonce)
- [ ] Реализовать кэширование digest параметров для сессии

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt`

**Результат:** OnvifClient поддерживает Digest Authentication

#### Шаг 1.4: Тестирование Digest Authentication
- [ ] Создать unit тесты для DigestAuthHelper:
  - [ ] Тест генерации HA1
  - [ ] Тест генерации HA2
  - [ ] Тест генерации response
  - [ ] Тест полного цикла Digest Authentication
- [ ] Создать integration тесты:
  - [ ] Тест с реальной камерой, требующей Digest Auth
  - [ ] Тест с различными алгоритмами (MD5, SHA-256)
  - [ ] Тест с qop=auth и qop=auth-int
- [ ] Проверить совместимость с различными производителями камер

**Файлы:**
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/auth/DigestAuthHelperTest.kt`
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/OnvifClientDigestAuthTest.kt`

**Результат:** Полное покрытие тестами Digest Authentication

---

### Этап 2: UPnP поддержка для обнаружения устройств

**Приоритет:** 🟡 Средний
**Оценка времени:** 5-7 дней
**Зависимости:** Нет

#### Шаг 2.1: Изучение UPnP спецификации
- [ ] Изучить UPnP Device Architecture 1.0/1.1
- [ ] Изучить UPnP MediaServer спецификацию
- [ ] Проанализировать SSDP (Simple Service Discovery Protocol)
- [ ] Определить интеграцию с существующим WS-Discovery

**Результат:** Документ с требованиями UPnP интеграции

#### Шаг 2.2: Создание UPnPDiscovery класса
- [ ] Создать `UPnPDiscovery.kt` в `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/`
- [ ] Реализовать expect/actual для платформ:
  - [ ] `UPnPDiscovery.common.kt` (expect)
  - [ ] `UPnPDiscovery.jvm.kt` (actual для JVM)
  - [ ] `UPnPDiscovery.android.kt` (actual для Android)
  - [ ] `UPnPDiscovery.ios.kt` (actual для iOS)
  - [ ] `UPnPDiscovery.native.kt` (actual для Native)
- [ ] Реализовать SSDP M-SEARCH запросы:
  - [ ] UDP multicast на 239.255.255.250:1900
  - [ ] ST заголовок для поиска устройств
  - [ ] MX заголовок для таймаута
  - [ ] MAN заголовок для SOAP action
- [ ] Реализовать парсинг SSDP NOTIFY ответов
- [ ] Реализовать получение device description (XML)

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/UPnPDiscovery.kt`
- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/UPnPDiscovery.jvm.kt`
- `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/UPnPDiscovery.android.kt`
- `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/UPnPDiscovery.ios.kt`
- `core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/UPnPDiscovery.native.kt`

**Результат:** UPnPDiscovery класс с базовой функциональностью

#### Шаг 2.3: Парсинг UPnP Device Description
- [ ] Создать `UPnPDeviceParser.kt`
- [ ] Реализовать парсинг XML device description:
  - [ ] Извлечение deviceType
  - [ ] Извлечение friendlyName
  - [ ] Извлечение manufacturer
  - [ ] Извлечение modelName
  - [ ] Извлечение serviceList
  - [ ] Определение ONVIF совместимости
- [ ] Реализовать фильтрацию ONVIF устройств
- [ ] Реализовать преобразование в DiscoveredCamera

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/UPnPDeviceParser.kt`

**Результат:** Парсер UPnP устройств с поддержкой ONVIF

#### Шаг 2.4: Интеграция UPnP в OnvifClient
- [ ] Модифицировать метод `discoverCameras()`:
  - [ ] Добавить параметр `useUpnp: Boolean = true`
  - [ ] Параллельный запуск WS-Discovery и UPnP
  - [ ] Объединение результатов с дедупликацией
  - [ ] Приоритизация WS-Discovery результатов
- [ ] Добавить fallback на UPnP при неудаче WS-Discovery
- [ ] Добавить логирование источника обнаружения

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt`

**Результат:** OnvifClient поддерживает UPnP обнаружение

#### Шаг 2.5: Тестирование UPnP
- [ ] Создать unit тесты для UPnPDiscovery:
  - [ ] Тест M-SEARCH запроса
  - [ ] Тест парсинга NOTIFY ответов
  - [ ] Тест парсинга device description
- [ ] Создать integration тесты:
  - [ ] Тест с реальными UPnP устройствами
  - [ ] Тест комбинированного обнаружения (WS-Discovery + UPnP)
- [ ] Проверить производительность параллельного обнаружения

**Файлы:**
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/UPnPDiscoveryTest.kt`
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/UPnPDeviceParserTest.kt`

**Результат:** Полное покрытие тестами UPnP функциональности

---

### Этап 3: Улучшение обработки ошибок и SOAP Fault

**Приоритет:** 🟡 Средний
**Оценка времени:** 2-3 дня
**Зависимости:** Нет

#### Шаг 3.1: Расширенная обработка SOAP Fault
- [ ] Улучшить парсинг SOAP Fault:
  - [ ] Извлечение fault code (s:Code/s:Value)
  - [ ] Извлечение fault subcode (s:Code/s:Subcode/s:Value)
  - [ ] Извлечение fault reason (s:Reason/s:Text)
  - [ ] Извлечение fault detail (s:Detail)
  - [ ] Извлечение ONVIF специфичных ошибок
- [ ] Создать иерархию исключений:
  - [ ] `OnvifException` (базовый)
  - [ ] `OnvifFaultException` (SOAP Fault)
  - [ ] `OnvifAuthenticationException` (401/403)
  - [ ] `OnvifNotSupportedException` (метод не поддерживается)
  - [ ] `OnvifTimeoutException` (таймаут)
- [ ] Добавить маппинг ONVIF кодов ошибок в человекочитаемые сообщения

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifExceptions.kt`
- Модификация `OnvifClient.kt`

**Результат:** Улучшенная обработка ошибок с детальными сообщениями

#### Шаг 3.2: Retry стратегия
- [ ] Реализовать умную retry стратегию:
  - [ ] Exponential backoff
  - [ ] Retry только для временных ошибок (timeout, network)
  - [ ] Не retry для постоянных ошибок (401, 404, 501)
  - [ ] Максимальное количество попыток (configurable)
- [ ] Добавить circuit breaker для неработающих камер
- [ ] Добавить логирование всех retry попыток

**Файлы:**
- Модификация `OnvifClient.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/RetryStrategy.kt`

**Результат:** Надежная retry стратегия с логированием

---

### Этап 4: Кэширование capabilities и профилей

**Приоритет:** 🟢 Низкий
**Оценка времени:** 2-3 дня
**Зависимости:** Нет

#### Шаг 4.1: Создание кэша
- [ ] Создать `OnvifCache.kt`:
  - [ ] In-memory кэш для capabilities
  - [ ] In-memory кэш для profiles
  - [ ] TTL для записей кэша (configurable)
  - [ ] Thread-safe операции
- [ ] Реализовать методы:
  - [ ] `getCapabilities(url: String): OnvifCapabilities?`
  - [ ] `putCapabilities(url: String, capabilities: OnvifCapabilities)`
  - [ ] `getProfiles(url: String): List<OnvifProfile>?`
  - [ ] `putProfiles(url: String, profiles: List<OnvifProfile>)`
  - [ ] `clear(url: String?)` (очистка для URL или всего кэша)
  - [ ] `invalidate(url: String)` (принудительная инвалидация)

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifCache.kt`

**Результат:** Кэш для capabilities и profiles

#### Шаг 4.2: Интеграция кэша в OnvifClient
- [ ] Добавить кэш как опциональный параметр конструктора
- [ ] Модифицировать `getCapabilities()`:
  - [ ] Проверка кэша перед запросом
  - [ ] Сохранение в кэш после успешного запроса
- [ ] Модифицировать `getProfiles()`:
  - [ ] Проверка кэша перед запросом
  - [ ] Сохранение в кэш после успешного запроса
- [ ] Добавить метод `invalidateCache(url: String)` для принудительного обновления
- [ ] Добавить автоматическую инвалидацию при ошибках аутентификации

**Файлы:**
- Модификация `OnvifClient.kt`

**Результат:** OnvifClient использует кэш для оптимизации

#### Шаг 4.3: Тестирование кэша
- [ ] Создать unit тесты:
  - [ ] Тест сохранения и получения из кэша
  - [ ] Тест TTL истечения
  - [ ] Тест thread-safety
  - [ ] Тест очистки кэша
- [ ] Создать integration тесты:
  - [ ] Тест производительности с кэшем и без
  - [ ] Тест инвалидации кэша

**Файлы:**
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/OnvifCacheTest.kt`

**Результат:** Полное покрытие тестами кэша

---

### Этап 5: Дополнительные ONVIF методы (Опционально)

**Приоритет:** 🟢 Низкий (не критично для MVP)
**Оценка времени:** 5-7 дней
**Зависимости:** Нет

#### Шаг 5.1: Events Service
- [ ] Реализовать `subscribeToEvents()`:
  - [ ] Создание подписки на события
  - [ ] Получение событий через PullPoint
  - [ ] Парсинг событий (MotionDetection, Tampering, etc.)
- [ ] Реализовать `unsubscribeFromEvents()`
- [ ] Реализовать `getEventProperties()`

**Файлы:**
- Модификация `OnvifClient.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifEvents.kt`

#### Шаг 5.2: Imaging Service
- [ ] Реализовать `getImagingSettings()`
- [ ] Реализовать `setImagingSettings()`:
  - [ ] Яркость (Brightness)
  - [ ] Контрастность (Contrast)
  - [ ] Насыщенность (ColorSaturation)
  - [ ] Резкость (Sharpness)
- [ ] Реализовать `getOptions()` для получения доступных настроек

**Файлы:**
- Модификация `OnvifClient.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifImaging.kt`

#### Шаг 5.3: Analytics Service (если поддерживается)
- [ ] Реализовать `getAnalyticsModules()`
- [ ] Реализовать `getSupportedAnalyticsRules()`
- [ ] Реализовать `configureAnalyticsRule()`

**Файлы:**
- Модификация `OnvifClient.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifAnalytics.kt`

---

## 📊 Приоритизация задач

### Критично для MVP (Этап 1)
1. ✅ **Digest Authentication** - 3-5 дней
   - Блокирует работу с камерами, требующими Digest Auth
   - Высокий приоритет

### Желательно для MVP (Этап 2)
2. 🟡 **UPnP поддержка** - 5-7 дней
   - Улучшает обнаружение устройств
   - Средний приоритет

### Улучшения (Этапы 3-4)
3. 🟡 **Улучшение обработки ошибок** - 2-3 дня
   - Повышает надежность
   - Средний приоритет

4. 🟢 **Кэширование** - 2-3 дня
   - Оптимизирует производительность
   - Низкий приоритет

### Опционально (Этап 5)
5. 🟢 **Дополнительные методы** - 5-7 дней
   - Расширяет функциональность
   - Низкий приоритет (не для MVP)

---

## 🧪 Тестирование

### Unit тесты
- [ ] DigestAuthHelper - все методы
- [ ] UPnPDiscovery - парсинг и запросы
- [ ] OnvifCache - все операции
- [ ] OnvifExceptions - все типы исключений
- [ ] RetryStrategy - логика retry

### Integration тесты
- [ ] Digest Authentication с реальными камерами
- [ ] UPnP обнаружение с реальными устройствами
- [ ] Комбинированное обнаружение (WS-Discovery + UPnP)
- [ ] Кэширование в реальных сценариях
- [ ] Обработка ошибок с различными камерами

### Тестовые камеры
- [ ] Hikvision (Digest Auth)
- [ ] Axis (Basic Auth)
- [ ] Dahua (Digest Auth)
- [ ] Sony (UPnP)
- [ ] Bosch (Digest Auth)

---

## 📚 Документация

### Обновить существующую документацию
- [ ] `docs/ONVIF_CLIENT.md` - добавить Digest Auth примеры
- [ ] `docs/ONVIF_CLIENT.md` - добавить UPnP примеры
- [ ] `docs/ONVIF_CLIENT.md` - добавить обработку ошибок

### Создать новую документацию
- [ ] `docs/ONVIF_DIGEST_AUTH.md` - детальное описание Digest Auth
- [ ] `docs/ONVIF_UPNP.md` - описание UPnP интеграции
- [ ] `docs/ONVIF_TROUBLESHOOTING.md` - решение проблем

---

## 🔄 Миграция и обратная совместимость

### Обратная совместимость
- ✅ Все существующие методы остаются без изменений
- ✅ Basic Authentication продолжает работать
- ✅ WS-Discovery остается основным методом обнаружения
- ✅ Новые методы опциональны

### Миграция
- [ ] Обновить примеры использования
- [ ] Обновить тесты
- [ ] Обновить документацию

---

## 📈 Метрики успеха

### Функциональные метрики
- ✅ Поддержка Digest Authentication для всех основных производителей
- ✅ Обнаружение устройств через WS-Discovery и UPnP
- ✅ Улучшенная обработка ошибок с детальными сообщениями
- ✅ Кэширование снижает количество запросов на 50%+

### Производительность
- ✅ Время обнаружения устройств < 5 секунд
- ✅ Время получения capabilities < 1 секунда (с кэшем)
- ✅ Успешность подключения > 95%

### Качество кода
- ✅ Покрытие тестами > 80%
- ✅ Нет критических багов
- ✅ Документация актуальна

---

## 🗓️ Временная оценка

| Этап | Время | Приоритет |
|------|-------|-----------|
| Этап 1: Digest Authentication | 3-5 дней | 🔴 Высокий |
| Этап 2: UPnP поддержка | 5-7 дней | 🟡 Средний |
| Этап 3: Обработка ошибок | 2-3 дня | 🟡 Средний |
| Этап 4: Кэширование | 2-3 дня | 🟢 Низкий |
| Этап 5: Дополнительные методы | 5-7 дней | 🟢 Низкий |
| **ИТОГО (MVP)** | **10-15 дней** | |
| **ИТОГО (Полный)** | **17-25 дней** | |

---

## ✅ Чеклист готовности MVP

### Критичные требования
- [x] Digest Authentication реализован и протестирован
- [ ] UPnP поддержка реализована (опционально)
- [ ] Обработка ошибок улучшена
- [ ] Документация обновлена
- [ ] Тесты написаны и проходят

### Желательные требования
- [ ] Кэширование реализовано
- [ ] Дополнительные методы реализованы (опционально)
- [ ] Производительность оптимизирована

---

**Последнее обновление:** 26 January 2026
**Следующий пересмотр:** После завершения Этапа 1
