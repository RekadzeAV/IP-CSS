# Детальный план устранения критических блокеров ФАЗЫ 1: MVP

**Дата создания:** 26 January 2026
**Версия:** 1.0
**Статус:** В работе

> **📚 Связанные документы:**
> - [DETAILED_DEVELOPMENT_PLAN.md](DETAILED_DEVELOPMENT_PLAN.md)
> - [ROADMAP_2026.md](../ROADMAP_2026.md)

---

## 🎯 Обзор критических блокеров

### Список критических блокеров MVP:

1. **ONVIF Event service (0%)** - 🔴 КРИТИЧЕСКИЙ БЛОКЕР для событий
2. **Certificate Pinning (50%)** - 🔴 КРИТИЧЕСКИЙ БЛОКЕР безопасности
3. **Принудительный HTTPS (⚠️)** - 🔴 КРИТИЧЕСКИЙ БЛОКЕР безопасности
4. **Безопасное хранение JWT токенов (⚠️)** - 🔴 КРИТИЧЕСКИЙ БЛОКЕР безопасности

**Общая оценка времени:** 4-6 недель
**Приоритет:** 🔴 КРИТИЧЕСКИЙ (блокирует MVP)

---

## 1. ONVIF Event Service (0% → 100%)

### 1.1 Обзор задачи

**Проблема:**
ONVIF Event service полностью отсутствует. Без него невозможно получать события от камер (детекция движения, вторжения, тревоги) в реальном времени.

**Текущее состояние:**
- ❌ Подписка на события отсутствует
- ❌ Обработка событий отсутствует
- ❌ Интеграция с системой событий отсутствует

**Целевое состояние:**
- ✅ Полная реализация ONVIF Event service
- ✅ Подписка на события камер
- ✅ Обработка и маппинг событий
- ✅ Интеграция с EventService и EventRepository

**Оценка времени:** 2-3 недели

---

### 1.2 Детальный план реализации

#### Этап 1: Изучение ONVIF Event Service спецификации (2 дня)

**Задачи:**
1. Изучить ONVIF Core Specification, раздел Event Service
2. Изучить WS-Notification стандарт (W3C)
3. Проанализировать примеры реализации от производителей камер
4. Составить техническую спецификацию

**Результат:**
- Документ с технической спецификацией ONVIF Event Service
- Список поддерживаемых типов событий
- Схема подписки и обработки событий

**Файлы:**
- `docs/ONVIF_EVENT_SERVICE_SPEC.md` (создать)

---

#### Этап 2: Реализация базовой структуры (3 дня)

**Задачи:**

1. **Создать модели данных для событий ONVIF:**
   - `OnvifEvent.kt` - модель события ONVIF
   - `OnvifEventType.kt` - типы событий ONVIF
   - `OnvifEventSubscription.kt` - модель подписки
   - `OnvifEventFilter.kt` - фильтры событий

2. **Создать интерфейс OnvifEventService:**
   ```kotlin
   interface OnvifEventService {
       suspend fun subscribeToEvents(
           cameraUrl: String,
           username: String?,
           password: String?,
           filter: OnvifEventFilter? = null
       ): Result<OnvifEventSubscription>

       suspend fun unsubscribe(subscriptionId: String): Result<Unit>

       suspend fun renewSubscription(subscriptionId: String): Result<Unit>

       suspend fun getEventProperties(cameraUrl: String): Result<OnvifEventProperties>
   }
   ```

3. **Создать XML парсеры для событий:**
   - Парсинг NotificationMessage (WS-Notification)
   - Парсинг TopicExpression
   - Парсинг MessageContent

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEvent.kt` (создать)
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventService.kt` (создать)
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventParser.kt` (создать)

**Результат:**
- Базовая структура классов и интерфейсов
- Модели данных для событий

---

#### Этап 3: Реализация WS-Notification протокола (5 дней)

**Задачи:**

1. **Реализовать Subscribe операцию:**
   - Создание SOAP запроса для Subscribe
   - Обработка ответа с SubscriptionReference
   - Сохранение информации о подписке

2. **Реализовать Unsubscribe операцию:**
   - Отправка Unsubscribe запроса
   - Очистка подписки

3. **Реализовать Renew операцию:**
   - Продление подписки перед истечением

4. **Реализовать GetCurrentMessage операцию:**
   - Получение последнего сообщения (опционально)

**Код реализации:**

```kotlin
class OnvifEventServiceImpl(
    private val client: HttpClient,
    private val digestAuthHelper: DigestAuthHelper
) : OnvifEventService {

    private val activeSubscriptions = mutableMapOf<String, OnvifEventSubscription>()

    override suspend fun subscribeToEvents(
        cameraUrl: String,
        username: String?,
        password: String?,
        filter: OnvifEventFilter?
    ): Result<OnvifEventSubscription> {
        // 1. Получить Event Service URL через GetCapabilities
        // 2. Создать Subscribe SOAP запрос
        // 3. Отправить запрос с Digest Authentication
        // 4. Парсить ответ с SubscriptionReference
        // 5. Сохранить подписку
    }

    override suspend fun unsubscribe(subscriptionId: String): Result<Unit> {
        // 1. Найти подписку
        // 2. Отправить Unsubscribe запрос
        // 3. Удалить подписку
    }

    override suspend fun renewSubscription(subscriptionId: String): Result<Unit> {
        // 1. Найти подписку
        // 2. Отправить Renew запрос
        // 3. Обновить срок действия
    }
}
```

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventServiceImpl.kt` (создать)
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventSoapBuilder.kt` (создать)

**Результат:**
- Работающие операции Subscribe, Unsubscribe, Renew

---

#### Этап 4: Реализация обработки событий (4 дня)

**Задачи:**

1. **Реализовать Notification Receiver:**
   - HTTP endpoint для приема событий от камер
   - Парсинг NotificationMessage
   - Валидация подписки

2. **Реализовать маппинг ONVIF событий в доменные события:**
   - Маппинг типов событий (MotionDetected → MOTION_DETECTION)
   - Извлечение метаданных
   - Создание Event объектов

3. **Реализовать автоматическое продление подписок:**
   - Фоновая задача для продления подписок
   - Обработка ошибок продления

**Код реализации:**

```kotlin
class OnvifEventNotificationReceiver(
    private val eventService: EventService,
    private val subscriptionManager: OnvifEventSubscriptionManager
) {
    suspend fun handleNotification(
        notificationMessage: NotificationMessage,
        subscriptionId: String
    ): Result<Unit> {
        // 1. Валидировать подписку
        // 2. Парсить событие
        // 3. Маппить в доменное событие
        // 4. Создать событие через EventService
    }
}

class OnvifEventMapper {
    fun mapToDomainEvent(onvifEvent: OnvifEvent, cameraId: String): Event {
        val eventType = when (onvifEvent.topic) {
            "tns1:VideoSource/MotionAlarm" -> EventType.MOTION_DETECTION
            "tns1:RuleEngine/LineDetector/Crossed" -> EventType.OBJECT_DETECTION
            "tns1:Device/IO/PortState" -> EventType.OTHER
            else -> EventType.OTHER
        }

        return Event(
            id = generateId(),
            cameraId = cameraId,
            type = eventType,
            timestamp = onvifEvent.timestamp,
            description = onvifEvent.message,
            metadata = onvifEvent.properties
        )
    }
}
```

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventNotificationReceiver.kt` (создать)
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventMapper.kt` (создать)
- `server/api/src/main/kotlin/com/company/ipcamera/server/service/OnvifEventSubscriptionManager.kt` (создать)

**Результат:**
- Работающая обработка событий от камер
- Автоматическое создание событий в системе

---

#### Этап 5: Интеграция с OnvifClient (2 дня)

**Задачи:**

1. **Добавить методы в OnvifClient:**
   ```kotlin
   suspend fun subscribeToEvents(
       filter: OnvifEventFilter? = null
   ): Result<OnvifEventSubscription>

   suspend fun unsubscribeFromEvents(subscriptionId: String): Result<Unit>
   ```

2. **Интегрировать с CameraRepository:**
   - Автоматическая подписка при добавлении камеры
   - Автоматическая отписка при удалении камеры

3. **Добавить конфигурацию подписок:**
   - Настройки подписки в CameraSettings
   - Фильтры событий

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt` (обновить)
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/model/Camera.kt` (обновить)

**Результат:**
- Интеграция Event Service в OnvifClient
- Автоматическое управление подписками

---

#### Этап 6: Интеграция с сервером (3 дня)

**Задачи:**

1. **Создать серверный сервис для управления подписками:**
   ```kotlin
   class OnvifEventSubscriptionService(
       private val onvifClient: OnvifClient,
       private val eventService: EventService,
       private val cameraRepository: CameraRepository
   ) {
       suspend fun subscribeToCameraEvents(cameraId: String): Result<Unit>
       suspend fun unsubscribeFromCameraEvents(cameraId: String): Result<Unit>
       suspend fun handleIncomingEvent(notification: NotificationMessage): Result<Unit>
   }
   ```

2. **Создать HTTP endpoint для приема событий:**
   - `POST /api/v1/onvif/events/notification` - прием событий от камер
   - Валидация подписки
   - Обработка и создание событий

3. **Интегрировать с EventService:**
   - Автоматическое создание событий при получении уведомлений
   - Отправка через WebSocket

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/service/OnvifEventSubscriptionService.kt` (создать)
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/OnvifEventRoutes.kt` (создать)
- `server/api/src/main/kotlin/com/company/ipcamera/server/di/AppModule.kt` (обновить)

**Результат:**
- Работающий серверный endpoint для событий
- Автоматическая обработка событий от камер

---

#### Этап 7: Тестирование (3 дня)

**Задачи:**

1. **Unit тесты:**
   - Тесты парсинга событий
   - Тесты маппинга событий
   - Тесты подписки/отписки

2. **Integration тесты:**
   - Тесты с реальными камерами (если доступны)
   - Тесты с mock камерами
   - Тесты обработки событий

3. **E2E тесты:**
   - Полный цикл: подписка → получение события → создание события в системе

**Файлы:**
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventServiceTest.kt` (создать)
- `server/api/src/test/kotlin/com/company/ipcamera/server/service/OnvifEventSubscriptionServiceTest.kt` (создать)

**Результат:**
- Покрытие тестами >70%
- Проверенная работа с реальными камерами

---

### 1.3 Чеклист завершения

- [ ] Техническая спецификация ONVIF Event Service
- [ ] Модели данных для событий
- [ ] Интерфейс OnvifEventService
- [ ] Реализация Subscribe/Unsubscribe/Renew
- [ ] Notification Receiver
- [ ] Маппинг ONVIF → доменные события
- [ ] Интеграция с OnvifClient
- [ ] Серверный endpoint для событий
- [ ] Интеграция с EventService
- [ ] Unit тесты (>70% покрытие)
- [ ] Integration тесты
- [ ] Документация

---

## 2. Certificate Pinning (50% → 100%)

### 2.1 Обзор задачи

**Проблема:**
Certificate Pinning частично реализован. Требуется завершение интеграции и тестирование на всех платформах.

**Текущее состояние:**
- ✅ Базовая структура реализована
- ✅ JVM/Desktop реализация завершена
- ⚠️ Android реализация частична (~50%)
- ⚠️ iOS реализация частична (~50%)
- ❌ Интеграция с ApiClient не завершена
- ❌ Тестирование отсутствует

**Целевое состояние:**
- ✅ Полная реализация на всех платформах
- ✅ Интеграция с ApiClient
- ✅ Автоматическое получение fingerprints
- ✅ Тестирование

**Оценка времени:** 1-2 недели

---

### 2.2 Детальный план реализации

#### Этап 1: Завершение Android реализации (3 дня)

**Задачи:**

1. **Интегрировать с Ktor Android engine:**
   - Использовать OkHttp CertificatePinner
   - Настроить через HttpClientEngineFactory

2. **Создать Network Security Config (опционально):**
   - `android/app/src/main/res/xml/network_security_config.xml`
   - Настройка certificate pinning через XML

3. **Обновить CertificatePinner.android.kt:**
   ```kotlin
   actual class CertificatePinner actual constructor(
       private val config: CertificatePinningConfig
   ) {
       private val okHttpPinner = CertificatePinner.Builder().apply {
           config.pinnedCertificates.forEach { (host, pins) ->
               pins.forEach { pin ->
                   add(host, pin)
               }
           }
       }.build()

       actual fun configureHttpClient(builder: HttpClientConfig<*>) {
           // Настройка через OkHttp engine
       }
   }
   ```

**Файлы:**
- `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.android.kt` (обновить)
- `android/app/src/main/res/xml/network_security_config.xml` (создать, опционально)

**Результат:**
- Работающий certificate pinning на Android

---

#### Этап 2: Завершение iOS реализации (3 дня)

**Задачи:**

1. **Завершить CertificatePinningDelegate:**
   - Убедиться, что все методы реализованы
   - Проверить обработку ошибок

2. **Интегрировать с Ktor Darwin engine:**
   - Использовать CertificatePinningEngineWrapper
   - Настроить NSURLSession с delegate

3. **Обновить CertificatePinner.ios.kt:**
   ```kotlin
   actual class CertificatePinner actual constructor(
       private val config: CertificatePinningConfig
   ) {
       private val delegate = CertificatePinningDelegate(config)

       actual fun configureHttpClient(builder: HttpClientConfig<*>) {
           // Настройка через Darwin engine с delegate
       }
   }
   ```

**Файлы:**
- `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinningDelegate.ios.kt` (проверить/обновить)
- `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.ios.kt` (обновить)

**Результат:**
- Работающий certificate pinning на iOS

---

#### Этап 3: Интеграция с ApiClient (2 дня)

**Задачи:**

1. **Обновить ApiClientConfig:**
   ```kotlin
   data class ApiClientConfig(
       val baseUrl: String,
       val certificatePinningConfig: CertificatePinningConfig? = null,
       // ... другие параметры
   )
   ```

2. **Обновить ApiClient.create():**
   ```kotlin
   fun create(config: ApiClientConfig): ApiClient {
       val engine = createEngine(config)
       val pinner = config.certificatePinningConfig?.let {
           CertificatePinner(it)
       }

       pinner?.configureHttpClient(engine)

       return ApiClient(engine, config)
   }
   ```

3. **Создать фабрику для создания engine с pinning:**
   - `createEngineWithPinning()` функция

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt` (обновить)
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClientConfig.kt` (обновить)

**Результат:**
- Автоматическая интеграция certificate pinning в ApiClient

---

#### Этап 4: Автоматическое получение fingerprints (2 дня)

**Задачи:**

1. **Создать утилиту для получения fingerprints:**
   ```kotlin
   object CertificateFingerprintExtractor {
       suspend fun extractFingerprint(host: String, port: Int = 443): Result<String>
       suspend fun extractFingerprints(hosts: List<String>): Map<String, List<String>>
   }
   ```

2. **Создать CLI инструмент:**
   - Скрипт для извлечения fingerprints
   - Интеграция в процесс разработки

3. **Документация:**
   - Инструкция по получению fingerprints
   - Примеры для разных платформ

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/security/CertificateFingerprintExtractor.kt` (создать)
- `scripts/extract-certificate-fingerprints.sh` (создать)
- `docs/CERTIFICATE_PINNING.md` (обновить)

**Результат:**
- Упрощенный процесс получения fingerprints

---

#### Этап 5: Конфигурация и настройка (1 день)

**Задачи:**

1. **Создать конфигурационный файл:**
   - `config/certificate-pins.json` или через environment variables
   - Загрузка конфигурации при старте

2. **Добавить в настройки приложения:**
   - Включение/выключение pinning
   - Список закрепленных хостов

3. **Обновить документацию:**
   - Инструкция по настройке
   - Примеры конфигурации

**Файлы:**
- `config/certificate-pins.json` (создать)
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/model/Settings.kt` (обновить)

**Результат:**
- Гибкая конфигурация certificate pinning

---

#### Этап 6: Тестирование (3 дня)

**Задачи:**

1. **Unit тесты:**
   - Тесты проверки fingerprints
   - Тесты обработки ошибок
   - Тесты для каждой платформы

2. **Integration тесты:**
   - Тесты с реальными серверами
   - Тесты с невалидными сертификатами
   - Тесты с MITM атаками (в контролируемой среде)

3. **E2E тесты:**
   - Полный цикл: подключение → проверка pinning → обработка ошибок

**Файлы:**
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/security/CertificatePinnerTest.kt` (создать)
- `core/network/src/androidTest/kotlin/.../CertificatePinnerAndroidTest.kt` (создать)
- `core/network/src/iosTest/kotlin/.../CertificatePinnerIosTest.kt` (создать)

**Результат:**
- Покрытие тестами >80%
- Проверенная защита от MITM

---

### 2.3 Чеклист завершения

- [ ] Android реализация завершена
- [ ] iOS реализация завершена
- [ ] Интеграция с ApiClient
- [ ] Автоматическое получение fingerprints
- [ ] Конфигурация через файлы/env
- [ ] Unit тесты (>80% покрытие)
- [ ] Integration тесты
- [ ] Документация обновлена

---

## 3. Принудительный HTTPS (⚠️ → 100%)

### 3.1 Обзор задачи

**Проблема:**
Требуется принудительное перенаправление HTTP → HTTPS и настройка безопасности.

**Текущее состояние:**
- ⚠️ Частичная реализация
- ❌ Автоматическое перенаправление отсутствует
- ❌ HSTS headers не настроены
- ❌ Проверка HTTPS в production

**Целевое состояние:**
- ✅ Автоматическое перенаправление HTTP → HTTPS
- ✅ HSTS headers настроены
- ✅ Проверка в production окружении
- ✅ Блокировка небезопасных соединений

**Оценка времени:** 1 неделя

---

### 3.2 Детальный план реализации

#### Этап 1: Серверное перенаправление (2 дня)

**Задачи:**

1. **Создать HTTPS Redirect middleware:**
   ```kotlin
   class HttpsRedirectMiddleware {
       fun Application.install() {
           intercept(ApplicationCallPipeline.Call) {
               if (!call.request.origin.scheme.equals("https", ignoreCase = true)) {
                   val httpsUrl = call.request.url.copy(scheme = "https")
                   call.respondRedirect(httpsUrl.buildString(), permanent = true)
               }
           }
       }
   }
   ```

2. **Добавить проверку окружения:**
   - Перенаправление только в production
   - Логирование попыток HTTP соединений

3. **Интегрировать в Application.kt:**
   ```kotlin
   if (environment.config.propertyOrNull("ktor.deployment.ssl") != null) {
       install(HttpsRedirectMiddleware)
   }
   ```

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/HttpsRedirectMiddleware.kt` (создать)
- `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt` (обновить)

**Результат:**
- Автоматическое перенаправление HTTP → HTTPS

---

#### Этап 2: HSTS Headers (1 день)

**Задачи:**

1. **Добавить HSTS middleware:**
   ```kotlin
   class HstsMiddleware {
       fun Application.install() {
           intercept(ApplicationCallPipeline.Call) {
               if (call.request.origin.scheme.equals("https", ignoreCase = true)) {
                   call.response.headers.append(
                       HttpHeaders.StrictTransportSecurity,
                       "max-age=31536000; includeSubDomains; preload"
                   )
               }
           }
       }
   }
   ```

2. **Настроить параметры HSTS:**
   - max-age (рекомендуется 1 год)
   - includeSubDomains
   - preload (опционально)

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/HstsMiddleware.kt` (создать)
- `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt` (обновить)

**Результат:**
- HSTS headers настроены

---

#### Этап 3: Next.js конфигурация (1 день)

**Задачи:**

1. **Обновить next.config.js:**
   ```javascript
   async headers() {
       return [
           {
               source: '/:path*',
               headers: [
                   {
                       key: 'Strict-Transport-Security',
                       value: 'max-age=31536000; includeSubDomains; preload'
                   },
                   {
                       key: 'X-Frame-Options',
                       value: 'DENY'
                   },
                   {
                       key: 'X-Content-Type-Options',
                       value: 'nosniff'
                   }
               ]
           }
       ]
   },
   async redirects() {
       return [
           {
               source: '/:path*',
               has: [
                   {
                       type: 'header',
                       key: 'x-forwarded-proto',
                       value: 'http',
                   },
               ],
               destination: 'https://:path*',
               permanent: true,
           },
       ]
   }
   ```

2. **Проверить работу в production:**
   - Тестирование перенаправлений
   - Проверка headers

**Файлы:**
- `server/web/next.config.js` (обновить)

**Результат:**
- HTTPS принудительно в Next.js

---

#### Этап 4: Android конфигурация (1 день)

**Задачи:**

1. **Обновить AndroidManifest.xml:**
   ```xml
   <application
       android:usesCleartextTraffic="false"
       android:networkSecurityConfig="@xml/network_security_config">
   ```

2. **Обновить network_security_config.xml:**
   ```xml
   <network-security-config>
       <base-config cleartextTrafficPermitted="false">
           <trust-anchors>
               <certificates src="system" />
           </trust-anchors>
       </base-config>
   </network-security-config>
   ```

**Файлы:**
- `android/app/src/main/AndroidManifest.xml` (обновить)
- `android/app/src/main/res/xml/network_security_config.xml` (создать/обновить)

**Результат:**
- Блокировка HTTP в Android приложении

---

#### Этап 5: Тестирование (2 дня)

**Задачи:**

1. **Тесты перенаправления:**
   - HTTP запросы → HTTPS
   - Проверка статуса 301/308

2. **Тесты HSTS:**
   - Проверка headers
   - Проверка параметров

3. **E2E тесты:**
   - Полный цикл с реальным сервером

**Файлы:**
- `server/api/src/test/kotlin/com/company/ipcamera/server/middleware/HttpsRedirectMiddlewareTest.kt` (создать)
- `server/api/src/test/kotlin/com/company/ipcamera/server/middleware/HstsMiddlewareTest.kt` (создать)

**Результат:**
- Проверенная работа HTTPS принудительно

---

### 3.3 Чеклист завершения

- [ ] HTTPS Redirect middleware
- [ ] HSTS middleware
- [ ] Next.js конфигурация
- [ ] Android конфигурация
- [ ] Тестирование
- [ ] Документация

---

## 4. Безопасное хранение JWT токенов (⚠️ → 100%)

### 4.1 Обзор задачи

**Проблема:**
JWT токены частично реализованы через httpOnly cookies, но требуется проверка полноты реализации и доработка клиентской части.

**Текущее состояние:**
- ✅ Серверная часть реализована (httpOnly cookies)
- ⚠️ Клиентская часть требует проверки
- ❌ CSRF защита отсутствует
- ❌ Проверка работы на всех платформах

**Целевое состояние:**
- ✅ Полная реализация httpOnly cookies
- ✅ CSRF защита
- ✅ Работа на всех платформах
- ✅ Тестирование

**Оценка времени:** 2-3 дня

---

### 4.2 Детальный план реализации

#### Этап 1: Проверка и доработка серверной части (1 день)

**Задачи:**

1. **Проверить текущую реализацию:**
   - ✅ httpOnly cookies установлены (проверено в AuthRoutes.kt)
   - ✅ Secure flag установлен в production
   - ✅ SameSite установлен

2. **Добавить дополнительные проверки:**
   - Валидация cookie domain
   - Проверка cookie path
   - Логирование попыток доступа

3. **Обновить middleware для чтения токенов:**
   ```kotlin
   fun ApplicationCall.getJwtToken(): String? {
       // Сначала проверяем cookie
       val tokenFromCookie = request.cookies["access_token"]
       if (tokenFromCookie != null) return tokenFromCookie

       // Fallback на Authorization header (для мобильных приложений)
       val authHeader = request.headers["Authorization"]
       return authHeader?.removePrefix("Bearer ")
   }
   ```

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/getJwtToken.kt` (проверить/обновить)
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt` (проверить)

**Результат:**
- Полностью рабочая серверная часть

---

#### Этап 2: Доработка клиентской части (1 день)

**Задачи:**

1. **Проверить удаление localStorage:**
   - Убедиться, что токены не сохраняются в localStorage
   - Удалить старый код, если есть

2. **Обновить authService.ts:**
   ```typescript
   // Удалить все обращения к localStorage
   // Cookies автоматически отправляются браузером
   export const login = async (username: string, password: string) => {
       const response = await apiClient.post('/auth/login', {
           username,
           password
       });
       // Токены теперь в httpOnly cookies, не нужно сохранять
       return response.data;
   };
   ```

3. **Обновить логику проверки аутентификации:**
   - Убрать проверку localStorage
   - Использовать проверку через API

**Файлы:**
- `server/web/src/services/authService.ts` (проверить/обновить)
- `server/web/src/utils/api.ts` (проверить, уже обновлен)

**Результат:**
- Полностью обновленная клиентская часть

---

#### Этап 3: CSRF защита (1 день)

**Задачи:**

1. **Создать CSRF middleware:**
   ```kotlin
   class CsrfMiddleware {
       fun Application.install() {
           intercept(ApplicationCallPipeline.Call) {
               if (call.request.httpMethod != HttpMethod.Get) {
                   val csrfToken = call.request.headers["X-CSRF-Token"]
                   val cookieToken = call.request.cookies["csrf_token"]

                   if (csrfToken == null || csrfToken != cookieToken) {
                       call.respond(HttpStatusCode.Forbidden, "CSRF token mismatch")
                       return@intercept
                   }
               }
           }
       }
   }
   ```

2. **Генерация CSRF токенов:**
   - Генерация при логине
   - Установка в cookie
   - Возврат в ответе для клиента

3. **Интеграция в клиент:**
   - Получение токена при логине
   - Отправка в заголовке X-CSRF-Token

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/CsrfMiddleware.kt` (создать)
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt` (обновить)
- `server/web/src/utils/api.ts` (обновить)

**Результат:**
- Работающая CSRF защита

---

#### Этап 4: Тестирование (1 день)

**Задачи:**

1. **Тесты серверной части:**
   - Проверка установки cookies
   - Проверка httpOnly флага
   - Проверка CSRF защиты

2. **Тесты клиентской части:**
   - Проверка отсутствия localStorage
   - Проверка автоматической отправки cookies

3. **E2E тесты:**
   - Полный цикл: логин → использование токенов → логаут

**Файлы:**
- `server/api/src/test/kotlin/com/company/ipcamera/server/middleware/CsrfMiddlewareTest.kt` (создать)
- `server/web/src/__tests__/authService.test.ts` (создать/обновить)

**Результат:**
- Проверенная безопасность токенов

---

### 4.3 Чеклист завершения

- [ ] Серверная часть проверена и доработана
- [ ] Клиентская часть обновлена (удален localStorage)
- [ ] CSRF защита реализована
- [ ] Тестирование завершено
- [ ] Документация обновлена

---

## 📊 Общий план выполнения

### Временная шкала

| Неделя | Задачи | Ответственный |
|--------|--------|---------------|
| **Неделя 1** | ONVIF Event Service (Этапы 1-2)<br>Certificate Pinning (Этап 1) | Backend Developer |
| **Неделя 2** | ONVIF Event Service (Этапы 3-4)<br>Certificate Pinning (Этапы 2-3) | Backend Developer |
| **Неделя 3** | ONVIF Event Service (Этапы 5-7)<br>Certificate Pinning (Этапы 4-5) | Backend Developer |
| **Неделя 4** | Certificate Pinning (Этап 6)<br>HTTPS принудительно (Этапы 1-3) | Backend + Frontend Developer |
| **Неделя 5** | HTTPS принудительно (Этапы 4-5)<br>JWT токены (Этапы 1-2) | Backend + Frontend Developer |
| **Неделя 6** | JWT токены (Этапы 3-4)<br>Финальное тестирование | Backend + Frontend Developer |

### Зависимости

1. **ONVIF Event Service** → не зависит от других блокеров
2. **Certificate Pinning** → не зависит от других блокеров
3. **HTTPS принудительно** → может выполняться параллельно
4. **JWT токены** → может выполняться параллельно

### Риски и митигация

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| Сложность ONVIF Event Service | Высокая | Высокое | Изучение спецификации, тестирование с реальными камерами |
| Проблемы с certificate pinning на iOS | Средняя | Среднее | Использование готовых решений, тестирование на реальных устройствах |
| Конфликты с существующим кодом | Средняя | Среднее | Тщательное тестирование, постепенное внедрение |

---

## ✅ Критерии завершения

### ONVIF Event Service
- ✅ Подписка на события работает
- ✅ События обрабатываются и создаются в системе
- ✅ Покрытие тестами >70%
- ✅ Документация готова

### Certificate Pinning
- ✅ Работает на всех платформах (Android, iOS, Desktop)
- ✅ Интегрирован с ApiClient
- ✅ Покрытие тестами >80%
- ✅ Документация готова

### HTTPS принудительно
- ✅ HTTP → HTTPS перенаправление работает
- ✅ HSTS headers настроены
- ✅ Работает на всех платформах
- ✅ Тестирование завершено

### JWT токены
- ✅ httpOnly cookies работают
- ✅ CSRF защита реализована
- ✅ localStorage не используется
- ✅ Тестирование завершено

---

**Последнее обновление:** 26 January 2026
**Следующий пересмотр:** После завершения каждого этапа
