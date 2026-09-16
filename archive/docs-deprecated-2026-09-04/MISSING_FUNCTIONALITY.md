# Детальный анализ нереализованного функционала

**Дата анализа:** 26 January 2026
**Версия проекта:** Alfa-0.0.1
**Последнее обновление:** 26 January 2026

---

## 📋 Содержание

1. [OnvifClient](#onvifclient)
2. [LicenseManager](#licensemanager)
3. [WebSocketClient](#websocketclient)
4. [RtspClient](#rtspclient)
5. [Связи с другими документами](#связи-с-другими-документами)

---

## OnvifClient

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt`
**Статус:** ⚠️ Частично реализовано (~40%)
**Документация:** [ONVIF_CLIENT.md](ONVIF_CLIENT.md)
**Интеграция:** [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#1-xml-парсинг-для-onvif)

### ❌ Критически важный нереализованный функционал

#### 1. WS-Discovery (Обнаружение камер)

**Текущее состояние:** Метод `discoverCameras()` возвращает пустой список (заглушка)

**Что нужно реализовать:**

1. **UDP Multicast отправка**
   - Отправка SOAP Probe запроса на `239.255.255.250:3702`
   - Поддержка IPv4 и IPv6
   - Обработка сетевых интерфейсов
   - Таймауты и retry логика

2. **SOAP Probe запрос**
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
               xmlns:a="http://schemas.xmlsoap.org/ws/2004/08/addressing"
               xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery">
       <s:Header>
           <a:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</a:Action>
           <a:MessageID>uuid:...</a:MessageID>
           <a:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</a:To>
       </s:Header>
       <s:Body>
           <d:Probe>
               <d:Types>dn:NetworkVideoTransmitter</d:Types>
           </d:Probe>
       </s:Body>
   </s:Envelope>
   ```

3. **Обработка ProbeMatches ответов**
   - Прием UDP ответов от камер
   - Парсинг ProbeMatches XML
   - Извлечение информации о камерах (XAddrs, Types, Scopes)
   - Агрегация результатов с таймаутом

4. **Фильтрация устройств**
   - Фильтрация по типам устройств (NetworkVideoTransmitter, NetworkVideoDisplay)
   - Фильтрация по производителям
   - Исключение дубликатов

**Зависимости:**
- UDP socket поддержка в Kotlin Multiplatform
- XML парсинг для SOAP ответов
- UUID генерация для MessageID

**Связанные файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt` (строки 45-71)
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/CameraRepositoryImpl.kt` (строка 117-120)

**Приоритет:** 🔴 КРИТИЧЕСКИЙ

---

#### 2. XML парсинг

**Текущее состояние:** Используется упрощенный парсинг через регулярные выражения

**Что нужно реализовать:**

1. **Полноценный XML парсер**
   - Использовать `kotlinx.serialization.xml` или `ktor-serialization-kotlinx-xml`
   - Поддержка SOAP namespaces
   - Обработка вложенных структур

2. **Data классы для SOAP ответов**
   ```kotlin
   @Serializable
   @XmlSerialName("Envelope", namespace = "http://www.w3.org/2003/05/soap-envelope")
   data class SoapEnvelope(
       @XmlElement(true) val header: SoapHeader?,
       @XmlElement(true) val body: SoapBody
   )

   @Serializable
   @XmlSerialName("Body", namespace = "http://www.w3.org/2003/05/soap-envelope")
   data class SoapBody(
       @XmlElement(true) val capabilities: CapabilitiesResponse?,
       @XmlElement(true) val deviceInformation: DeviceInformationResponse?,
       @XmlElement(true) val fault: SoapFault?
   )
   ```

3. **Обработка SOAP Fault**
   - Парсинг ошибок от сервера
   - Детальные сообщения об ошибках
   - Коды ошибок

**Зависимости:**
- `io.ktor:ktor-serialization-kotlinx-xml:2.3.5`
- `org.jetbrains.kotlinx:kotlinx-serialization-xml:1.6.0`

**Связанные файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt` (строки 396-444, 520-533)

**Приоритет:** 🔴 КРИТИЧЕСКИЙ

---

#### 3. Digest Authentication

**Текущее состояние:** Только Basic Authentication

**Что нужно реализовать:**

1. **Обработка WWW-Authenticate заголовка**
   - Парсинг realm, nonce, qop, algorithm
   - Генерация response hash
   - Поддержка MD5 и SHA-256

2. **Digest Auth реализация**
   ```kotlin
   private fun createDigestAuthHeader(
       username: String,
       password: String,
       method: String,
       uri: String,
       realm: String,
       nonce: String,
       qop: String? = null
   ): String {
       // Реализация Digest Authentication
   }
   ```

3. **Автоматическое переключение**
   - Определение типа аутентификации из ответа сервера
   - Fallback с Digest на Basic при необходимости

**Приоритет:** 🟡 ВЫСОКИЙ

---

#### 4. Дополнительные ONVIF функции

**Что нужно реализовать:**

1. **PTZ функции:**
   - `AbsoluteMove` - абсолютное позиционирование
   - `RelativeMove` - относительное движение
   - `GetPresets` - получение пресетов
   - `SetPreset` - установка пресета
   - `GotoPreset` - переход к пресету
   - `GetStatus` - получение статуса PTZ

2. **Imaging функции:**
   - `GetImagingSettings` - получение настроек изображения
   - `SetImagingSettings` - установка настроек
   - `GetOptions` - получение опций

3. **Events функции:**
   - `Subscribe` - подписка на события
   - `PullMessages` - получение сообщений
   - `GetEventProperties` - получение свойств событий

**Приоритет:** 🟢 СРЕДНИЙ

---

## LicenseManager

**Файл:** `core/license/src/commonMain/kotlin/com/company/ipcamera/core/license/LicenseManager.kt`
**Статус:** ⏸️ ОТЛОЖЕНО - Вынесено за рамки проекта (January 2026)
**Документация:** [LICENSE_SYSTEM.md](LICENSE_SYSTEM.md)
**Примечание:** Функционал лицензирования требует полной переработки и будет реализован в отдельной доработке.

### ❌ Критически важный нереализованный функционал

#### 1. Онлайн активация

**Текущее состояние:** Метод `activateOnlineLicense()` возвращает ошибку (TODO)

**Что нужно реализовать:**

1. **HTTP запрос к лицензионному серверу**
   - Endpoint: `/api/v1/license/activate` (см. [API.md](API.md#лицензии-apiv1license))
   - Отправка license XML и activation code
   - Обработка ответа сервера

2. **Парсинг license XML**
   - Валидация XML структуры
   - Извлечение данных лицензии (features, limitations, validity)
   - Проверка формата

3. **Проверка цифровой подписи**
   - Валидация подписи лицензии
   - Проверка сертификата
   - Проверка цепочки сертификатов

4. **Валидация лицензии**
   - Проверка срока действия
   - Проверка поддерживаемых платформ
   - Проверка функций и лимитов

5. **Сохранение активированной лицензии**
   - Использование `LicenseRepository.saveLicense()`
   - Шифрование данных лицензии
   - Сохранение метаданных активации

**Связанные файлы:**
- `core/license/src/commonMain/kotlin/com/company/ipcamera/core/license/LicenseManager.kt` (строки 110-117)
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/api/LicenseApiService.kt`

**Приоритет:** 🔴 КРИТИЧЕСКИЙ

---

#### 2. Офлайн активация

**Текущее состояние:** Метод `activateOfflineLicense()` возвращает ошибку (TODO)

**Что нужно реализовать:**

1. **Расшифровка offline code**
   - Использование `PlatformCrypto.decryptOfflineCode()`
   - Валидация формата кода (OFA-XXXX-XXXX-XXXX)
   - Проверка срока действия кода

2. **Валидация offline code**
   - Проверка подписи кода
   - Проверка привязки к устройству (опционально)
   - Проверка лимита активаций

3. **Создание offline лицензии**
   - Генерация `ActivatedLicense` с флагом `isOffline = true`
   - Установка `offlineValidUntil` (обычно 30 дней)
   - Установка `requiresOnlineValidation = true`

4. **Сохранение offline лицензии**
   - Использование `LicenseRepository.saveLicense()`
   - Шифрование данных

**Связанные файлы:**
- `core/license/src/commonMain/kotlin/com/company/ipcamera/core/license/LicenseManager.kt` (строки 119-125)

**Приоритет:** 🟡 ВЫСОКИЙ

---

#### 3. Проверка целостности лицензии

**Текущее состояние:** Метод `checkLicenseIntegrity()` всегда возвращает `true` (TODO)

**Что нужно реализовать:**

1. **Проверка цифровой подписи**
   - Валидация подписи лицензии
   - Проверка сертификата
   - Проверка цепочки сертификатов до корневого CA

2. **Проверка хеша**
   - Вычисление хеша данных лицензии
   - Сравнение с сохраненным хешем
   - Обнаружение изменений

3. **Проверка привязки к устройству**
   - Сравнение `deviceFingerprint` с текущим
   - Обработка изменений железа (HARDWARE_CHANGED)

**Связанные файлы:**
- `core/license/src/commonMain/kotlin/com/company/ipcamera/core/license/LicenseManager.kt` (строки 239-243)

**Приоритет:** 🔴 КРИТИЧЕСКИЙ

---

#### 4. Платформо-специфичные реализации

**Android:**

**Файл:** `core/license/src/androidMain/kotlin/com/company/ipcamera/core/license/LicenseManager.android.kt`

**Что нужно реализовать:**

1. **PlatformCrypto.getSecureDeviceFingerprint()**
   - Использование Android Keystore
   - Генерация уникального идентификатора устройства
   - Устойчивость к сбросу настроек

2. **PlatformCrypto.decryptOfflineCode()**
   - Расшифровка через Android Keystore
   - Использование EncryptedSharedPreferences

3. **PlatformCrypto.schedulePeriodicCheck()**
   - Использование WorkManager
   - Периодическая проверка лицензии (раз в день)

4. **LicenseRepository.saveLicense() / loadLicense()**
   - Использование EncryptedSharedPreferences
   - Или Android Keystore для критичных данных

**iOS:**

**Файл:** `core/license/src/iosMain/kotlin/com/company/ipcamera/core/license/LicenseManager.ios.kt`

**Что нужно реализовать:**

1. **PlatformCrypto.getSecureDeviceFingerprint()**
   - Использование iOS Keychain
   - Генерация уникального идентификатора устройства

2. **PlatformCrypto.decryptOfflineCode()**
   - Расшифровка через iOS Keychain
   - Использование Security framework

3. **PlatformCrypto.schedulePeriodicCheck()**
   - Использование Background Tasks
   - Периодическая проверка лицензии

4. **LicenseRepository.saveLicense() / loadLicense()**
   - Использование iOS Keychain
   - Безопасное хранение данных

**Desktop (Windows/Linux/macOS):**

**Что нужно реализовать:**

1. **PlatformCrypto** - полностью отсутствует
2. **LicenseRepository** - полностью отсутствует
3. Использование системных хранилищ:
   - Windows: Registry или Encrypted File
   - Linux: ~/.config или Encrypted File
   - macOS: Keychain

**Приоритет:** 🔴 КРИТИЧЕСКИЙ

**Связанные документы:**
- [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#5-криптография-для-лицензирования)

---

## WebSocketClient

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/WebSocketClient.kt`
**Статус:** ✅ Частично реализовано (~80%)
**Документация:** [WEBSOCKET_CLIENT.md](WEBSOCKET_CLIENT.md)

### ⚠️ Недостающий функционал

#### 1. Обработка бинарных сообщений

**Текущее состояние:** Бинарные сообщения игнорируются (строка 196)

**Что нужно реализовать:**

1. **Обработка Frame.Binary**
   ```kotlin
   is Frame.Binary -> {
       val data = frame.readBytes()
       // Обработка бинарных данных
       // Например: изображения, файлы
   }
   ```

2. **Поддержка различных форматов**
   - JSON в бинарном формате
   - Protobuf
   - Изображения (JPEG, PNG)

**Приоритет:** 🟢 СРЕДНИЙ

---

#### 2. Очередь сообщений

**Что нужно реализовать:**

1. **Очередь при отключении**
   - Сохранение сообщений при разрыве соединения
   - Отправка при переподключении

2. **Приоритизация**
   - Приоритетные сообщения отправляются первыми
   - Обычные сообщения в порядке очереди

**Приоритет:** 🟢 СРЕДНИЙ

---

#### 3. Rate limiting

**Что нужно реализовать:**

1. **Ограничение частоты отправки**
   - Максимальное количество сообщений в секунду
   - Очередь при превышении лимита

**Приоритет:** 🟢 НИЗКИЙ

---

#### 4. Метрики и мониторинг

**Что нужно реализовать:**

1. **Метрики подключения**
   - Latency (задержка)
   - Количество переподключений
   - Время работы соединения

2. **Статистика сообщений**
   - Количество отправленных/полученных сообщений
   - Размер сообщений

**Приоритет:** 🟢 НИЗКИЙ

---

## RtspClient

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/RtspClient.kt`
**Нативная библиотека:** `native/video-processing/src/rtsp_client.cpp`
**Статус:** ⚠️ Частично реализовано (~10%)
**Документация:** [RTSP_CLIENT.md](RTSP_CLIENT.md)
**Интеграция:** [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#2-rtsp-клиент---интеграция-live555)

### ❌ Критически важный нереализованный функционал

#### 1. Kotlin обертка - интеграция с нативной библиотекой

**Текущее состояние:** Все методы содержат TODO комментарии, используются заглушки

**Что нужно реализовать:**

1. **FFI биндинги для Kotlin/Native**
   - Создать `.def` файл для cinterop
   - Настроить компиляцию нативной библиотеки
   - Интеграция с Kotlin кодом

2. **Реализация connect()**
   - Вызов `rtsp_client_connect()` из нативной библиотеки
   - Обработка результатов
   - Инициализация потоков

3. **Реализация play() / stop() / pause()**
   - Вызов соответствующих нативных функций
   - Обновление статуса

4. **Получение кадров**
   - Настройка callback для получения кадров
   - Конвертация нативных кадров в Kotlin объекты
   - Эмиссия в Flow

**Связанные файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/RtspClient.kt` (строки 129-173, 184-230, 289-343)

**Приоритет:** 🔴 КРИТИЧЕСКИЙ

---

#### 2. Нативная C++ библиотека - RTSP протокол

**Текущее состояние:** Все функции содержат TODO, используются заглушки

**Что нужно реализовать:**

1. **RTSP подключение (rtsp_client_connect)**
   - Парсинг URL (rtsp://host:port/path)
   - TCP подключение к серверу
   - RTSP DESCRIBE запрос
   - Парсинг SDP ответа
   - RTSP SETUP для каждого потока
   - RTSP PLAY запрос

2. **RTP/RTCP обработка**
   - Прием RTP пакетов
   - Демультиплексирование потоков
   - Обработка RTCP (SR, RR, BYE)
   - Восстановление последовательности пакетов
   - Обработка потерь пакетов

3. **Декодирование**
   - H.264 декодирование
   - H.265 декодирование
   - AAC декодирование
   - PCM обработка

4. **Аутентификация**
   - Basic Authentication
   - Digest Authentication

5. **Обработка ошибок**
   - RTSP ошибки (4xx, 5xx)
   - Сетевые ошибки
   - Retry логика

**Рекомендация:** Использовать библиотеку Live555 или FFmpeg

**Связанные файлы:**
- `native/video-processing/src/rtsp_client.cpp` (строки 65-108, 129-174)
- `native/video-processing/include/rtsp_client.h`

**Приоритет:** 🔴 КРИТИЧЕСКИЙ

**Связанные документы:**
- [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#2-rtsp-клиент---интеграция-live555)

---

## Связи с другими документами

### Документы, которые ссылаются на этот анализ:

1. **[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)** - общий статус реализации
2. **[DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md)** - план разработки
3. **[INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)** - руководство по интеграции библиотек
4. **[PLATFORMS.md](PLATFORMS.md)** - разделение разработки по платформам
5. **[ONVIF_CLIENT.md](ONVIF_CLIENT.md)** - документация ONVIF клиента
6. **[RTSP_CLIENT.md](RTSP_CLIENT.md)** - документация RTSP клиента
7. **[WEBSOCKET_CLIENT.md](WEBSOCKET_CLIENT.md)** - документация WebSocket клиента
8. **[LICENSE_SYSTEM.md](LICENSE_SYSTEM.md)** - документация системы лицензирования

### Приоритеты реализации:

- 🔴 **КРИТИЧЕСКИЙ** - блокирует MVP, должен быть реализован в первую очередь
- 🟡 **ВЫСОКИЙ** - важен для полноценной работы, реализовать после критических
- 🟢 **СРЕДНИЙ** - улучшает функциональность, реализовать по возможности
- 🔵 **НИЗКИЙ** - nice-to-have, реализовать в последнюю очередь

---

**Последнее обновление:** 26 January 2026

