# ONVIF UPnP Integration

**Версия:** 1.0
**Дата:** 26 January 2026
**Статус:** 📋 Запланировано

> **📚 Связанные документы:**
> - [ONVIF_CLIENT.md](ONVIF_CLIENT.md) - Основная документация ONVIF клиента
> - [ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](ONVIF_CLIENT_IMPLEMENTATION_PLAN.md) - План реализации

---

## Обзор

UPnP (Universal Plug and Play) - это альтернативный метод обнаружения сетевых устройств, включая IP-камеры. Интеграция UPnP в ONVIF клиент позволит обнаруживать камеры в сетях, где WS-Discovery не работает или ограничен.

## Проблема

Текущая реализация использует только WS-Discovery для обнаружения камер. В некоторых сетевых конфигурациях (виртуальные сети, некоторые роутеры, корпоративные сети) WS-Discovery может не работать из-за ограничений multicast.

### Ограничения WS-Discovery

- ❌ Требует поддержку UDP multicast
- ❌ Может блокироваться файрволами
- ❌ Не работает в некоторых виртуальных сетях
- ❌ Ограниченный TTL для multicast пакетов

## Решение

Добавление поддержки UPnP как альтернативного метода обнаружения устройств.

### Преимущества UPnP

- ✅ Широко поддерживается сетевыми устройствами
- ✅ Работает в большинстве домашних сетей
- ✅ Не требует специальных настроек сети
- ✅ Поддерживается многими производителями камер

### Недостатки UPnP

- ⚠️ Менее безопасен (по умолчанию)
- ⚠️ Может быть отключен в корпоративных сетях
- ⚠️ Требует парсинг XML device description

## Как работает UPnP Discovery

### SSDP (Simple Service Discovery Protocol)

1. **Клиент отправляет M-SEARCH запрос**
   ```
   M-SEARCH * HTTP/1.1
   HOST: 239.255.255.250:1900
   MAN: "ssdp:discover"
   ST: urn:schemas-upnp-org:device:MediaServer:1
   MX: 3
   ```

2. **Устройства отвечают NOTIFY**
   ```
   HTTP/1.1 200 OK
   CACHE-CONTROL: max-age=1800
   LOCATION: http://192.168.1.100:49152/description.xml
   ST: urn:schemas-upnp-org:device:MediaServer:1
   USN: uuid:12345678-1234-1234-1234-123456789012::urn:schemas-upnp-org:device:MediaServer:1
   ```

3. **Клиент получает device description**
   ```
   GET http://192.168.1.100:49152/description.xml
   ```

4. **Парсинг XML и определение ONVIF совместимости**

## Архитектура реализации

### Структура классов

```
UPnPDiscovery (expect)
├── UPnPDiscovery.jvm.kt (actual)
├── UPnPDiscovery.android.kt (actual)
├── UPnPDiscovery.ios.kt (actual)
└── UPnPDiscovery.native.kt (actual)

UPnPDeviceParser
└── parseDeviceDescription(xml: String): UPnPDevice?

OnvifClient
└── discoverCameras(useUpnp: Boolean = true): List<DiscoveredCamera>
```

### Поток данных

```
OnvifClient.discoverCameras()
    ├── WSDiscovery.discover() (параллельно)
    └── UPnPDiscovery.discover() (параллельно)
        ├── SSDP M-SEARCH запрос
        ├── Получение NOTIFY ответов
        ├── Получение device description
        └── UPnPDeviceParser.parse()
    └── Объединение результатов с дедупликацией
```

## Примеры использования (после реализации)

### Обнаружение через WS-Discovery и UPnP

```kotlin
val onvifClient = OnvifClient(engine)

// Автоматическое использование обоих методов
val cameras = onvifClient.discoverCameras(
    timeoutMillis = 5000,
    useUpnp = true // По умолчанию true
)
```

### Только WS-Discovery

```kotlin
val cameras = onvifClient.discoverCameras(
    timeoutMillis = 5000,
    useUpnp = false // Только WS-Discovery
)
```

### Только UPnP

```kotlin
// Прямое использование UPnPDiscovery
val upnpDiscovery = UPnPDiscovery()
val devices = upnpDiscovery.discover(timeoutMillis = 5000)

// Фильтрация ONVIF устройств
val onvifDevices = devices.filter { it.isOnvifCompatible }
```

## Парсинг Device Description

### Пример XML

```xml
<?xml version="1.0"?>
<root xmlns="urn:schemas-upnp-org:device-1-0">
  <specVersion>
    <major>1</major>
    <minor>0</minor>
  </specVersion>
  <device>
    <deviceType>urn:schemas-upnp-org:device:MediaServer:1</deviceType>
    <friendlyName>IP Camera</friendlyName>
    <manufacturer>Hikvision</manufacturer>
    <modelName>DS-2CD2342WD-I</modelName>
    <serviceList>
      <service>
        <serviceType>urn:schemas-onvif-org:service:Device:1</serviceType>
        <serviceId>urn:uuid:device-service</serviceId>
        <SCPDURL>/onvif/device_service</SCPDURL>
      </service>
    </serviceList>
  </device>
</root>
```

### Определение ONVIF совместимости

Устройство считается ONVIF-совместимым, если:

1. В `serviceList` есть сервис с `serviceType`, содержащим `onvif`
2. В `deviceType` указан тип медиа-устройства
3. В `manufacturer` или `modelName` есть упоминание ONVIF

## Интеграция с OnvifClient

### Модификация discoverCameras()

```kotlin
suspend fun discoverCameras(
    timeoutMillis: Long = 5000,
    useUpnp: Boolean = true
): List<DiscoveredCamera> = withContext(Dispatchers.IO) {
    val wsDiscoveryResults = async {
        val wsDiscovery = WSDiscovery()
        try {
            wsDiscovery.discover(timeoutMillis)
        } finally {
            wsDiscovery.close()
        }
    }

    val upnpResults = async {
        if (useUpnp) {
            val upnpDiscovery = UPnPDiscovery()
            try {
                upnpDiscovery.discover(timeoutMillis)
            } finally {
                upnpDiscovery.close()
            }
        } else {
            emptyList()
        }
    }

    // Объединение результатов
    val wsDevices = wsDiscoveryResults.await()
    val upnpDevices = upnpResults.await()

    // Дедупликация по URL
    val allDevices = (wsDevices + upnpDevices).distinctBy { it.xAddrs.firstOrNull() }

    // Преобразование в DiscoveredCamera
    // ...
}
```

## Производители камер с UPnP поддержкой

- ✅ **Sony** - полная поддержка UPnP
- ✅ **Panasonic** - поддержка UPnP
- ✅ **Samsung** - поддержка UPnP
- ⚠️ **Hikvision** - ограниченная поддержка
- ⚠️ **Dahua** - ограниченная поддержка
- ⚠️ **Axis** - ограниченная поддержка

## Тестирование

После реализации будут доступны тесты:

```kotlin
// Unit тесты
class UPnPDiscoveryTest {
    @Test
    fun testMSearchRequest() { ... }

    @Test
    fun testParseNotifyResponse() { ... }
}

class UPnPDeviceParserTest {
    @Test
    fun testParseDeviceDescription() { ... }

    @Test
    fun testDetectOnvifCompatibility() { ... }
}

// Integration тесты
class OnvifClientUpnpTest {
    @Test
    fun testCombinedDiscovery() { ... }

    @Test
    fun testUpnpOnlyDiscovery() { ... }
}
```

## Спецификации

- **UPnP Device Architecture 1.0/1.1** - основная спецификация
- **SSDP (Simple Service Discovery Protocol)** - протокол обнаружения
- **UPnP MediaServer** - спецификация для медиа-устройств

## Статус реализации

| Компонент | Статус | Приоритет |
|-----------|--------|-----------|
| UPnPDiscovery (expect) | ❌ Не начато | 🟡 Средний |
| UPnPDiscovery.jvm.kt | ❌ Не начато | 🟡 Средний |
| UPnPDiscovery.android.kt | ❌ Не начато | 🟡 Средний |
| UPnPDiscovery.ios.kt | ❌ Не начато | 🟡 Средний |
| UPnPDiscovery.native.kt | ❌ Не начато | 🟡 Средний |
| UPnPDeviceParser | ❌ Не начато | 🟡 Средний |
| Интеграция в OnvifClient | ❌ Не начато | 🟡 Средний |
| Тесты | ❌ Не начато | 🟡 Средний |

**Оценка времени:** 5-7 дней

---

**Последнее обновление:** 26 January 2026
**Следующий пересмотр:** После начала реализации
