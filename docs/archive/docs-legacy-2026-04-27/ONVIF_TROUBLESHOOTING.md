# ONVIF Troubleshooting Guide

**Версия:** 1.0
**Дата:** 26 January 2026

> **📚 Связанные документы:**
> - [ONVIF_CLIENT.md](ONVIF_CLIENT.md) - Основная документация ONVIF клиента
> - [ONVIF_DIGEST_AUTH.md](ONVIF_DIGEST_AUTH.md) - Digest Authentication
> - [ONVIF_UPNP.md](ONVIF_UPNP.md) - UPnP интеграция

---

## Обзор

Этот документ содержит решения распространенных проблем при работе с ONVIF клиентом.

---

## Проблемы с подключением

### Проблема: 401 Unauthorized

**Симптомы:**
```
Exception: HTTP 401 Unauthorized
Error: Authentication failed
```

**Причины:**
1. Неправильные учетные данные
2. Камера требует Digest Authentication (не реализовано)
3. Учетная запись заблокирована

**Решения:**

#### Решение 1: Проверка учетных данных
```kotlin
// Убедитесь, что username и password правильные
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin", // Проверьте в веб-интерфейсе камеры
    password = "password123" // Проверьте в веб-интерфейсе камеры
)
```

#### Решение 2: Digest Authentication
Если камера требует Digest Authentication (Hikvision, Dahua, Bosch):

**Временное решение:**
- Создайте пользователя с поддержкой Basic Authentication в веб-интерфейсе камеры
- Или дождитесь реализации Digest Authentication (см. [ONVIF_DIGEST_AUTH.md](ONVIF_DIGEST_AUTH.md))

**Статус:** Digest Authentication в разработке

#### Решение 3: Проверка блокировки учетной записи
- Проверьте веб-интерфейс камеры на наличие блокировки
- Сбросьте пароль через веб-интерфейс

---

### Проблема: Connection Timeout

**Симптомы:**
```
Exception: Connection timeout
Error: SocketTimeoutException
```

**Причины:**
1. Камера недоступна в сети
2. Неправильный IP адрес
3. Файрвол блокирует подключение
4. Камера выключена

**Решения:**

#### Решение 1: Проверка доступности камеры
```bash
# Проверьте ping
ping 192.168.1.100

# Проверьте порт
telnet 192.168.1.100 80
```

#### Решение 2: Проверка IP адреса
```kotlin
// Убедитесь, что URL правильный
val url = "http://192.168.1.100" // Без /onvif/device_service
// Клиент автоматически добавит правильный путь
```

#### Решение 3: Увеличение таймаута
```kotlin
// Используйте более длинный таймаут для медленных сетей
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)
// Таймаут настраивается в HttpClientEngine
```

#### Решение 4: Проверка файрвола
- Убедитесь, что порт 80 (HTTP) открыт
- Проверьте настройки файрвола камеры
- Проверьте настройки роутера

---

### Проблема: Unknown Host

**Симптомы:**
```
Exception: UnknownHostException
Error: Could not resolve host
```

**Причины:**
1. Неправильное имя хоста или IP адрес
2. Проблемы с DNS
3. Камера не в той же сети

**Решения:**

#### Решение 1: Использование IP адреса вместо имени
```kotlin
// Используйте IP адрес вместо имени хоста
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100", // IP адрес
    username = "admin",
    password = "password123"
)
```

#### Решение 2: Проверка сети
```bash
# Проверьте, что камера в той же сети
ipconfig # Windows
ifconfig # Linux/Mac

# Проверьте доступность
ping 192.168.1.100
```

---

## Проблемы с обнаружением камер

### Проблема: discoverCameras() возвращает пустой список

**Симптомы:**
```kotlin
val cameras = onvifClient.discoverCameras()
println(cameras.size) // 0
```

**Причины:**
1. WS-Discovery не работает в сети (multicast заблокирован)
2. Камеры не поддерживают WS-Discovery
3. Камеры в другой подсети
4. Файрвол блокирует UDP multicast

**Решения:**

#### Решение 1: Увеличение таймаута
```kotlin
val cameras = onvifClient.discoverCameras(
    timeoutMillis = 10000 // Увеличьте до 10 секунд
)
```

#### Решение 2: Проверка multicast
```bash
# Проверьте, что multicast работает
# На Linux:
ip maddr show

# На Windows:
netsh interface ip show joins
```

#### Решение 3: Ручное добавление камер
```kotlin
// Если обнаружение не работает, добавьте камеру вручную
val camera = Camera(
    id = generateId(),
    name = "Camera 1",
    url = "http://192.168.1.100",
    // ...
)
```

#### Решение 4: Ожидание UPnP поддержки
UPnP поддержка планируется и позволит обнаруживать камеры в сетях, где WS-Discovery не работает. См. [ONVIF_UPNP.md](ONVIF_UPNP.md)

---

### Проблема: Обнаружены не все камеры

**Симптомы:**
```kotlin
val cameras = onvifClient.discoverCameras()
// Ожидается 5 камер, но найдено только 2
```

**Причины:**
1. Некоторые камеры отвечают медленнее
2. Камеры в разных подсетях
3. Различные версии ONVIF

**Решения:**

#### Решение 1: Увеличение таймаута
```kotlin
val cameras = onvifClient.discoverCameras(
    timeoutMillis = 15000 // Увеличьте до 15 секунд
)
```

#### Решение 2: Множественные попытки
```kotlin
var cameras = emptyList<DiscoveredCamera>()
repeat(3) { attempt ->
    val discovered = onvifClient.discoverCameras(timeoutMillis = 5000)
    cameras = (cameras + discovered).distinctBy { it.url }
    delay(2000) // Пауза между попытками
}
```

---

## Проблемы с PTZ управлением

### Проблема: PTZ не работает

**Симптомы:**
```kotlin
val result = onvifClient.movePtz(
    url = "http://192.168.1.100",
    direction = PtzDirection.RIGHT,
    speed = 0.5f
)
// result = false
```

**Причины:**
1. Камера не поддерживает PTZ
2. Неправильный profileToken
3. PTZ заблокирован в настройках камеры

**Решения:**

#### Решение 1: Проверка поддержки PTZ
```kotlin
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)

if (capabilities?.ptzServiceUrl == null) {
    println("Камера не поддерживает PTZ")
}
```

#### Решение 2: Использование правильного profileToken
```kotlin
// Получите список профилей
val profiles = onvifClient.getProfiles(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)

// Используйте правильный profileToken
val profileToken = profiles.firstOrNull()?.token ?: "Profile1"

onvifClient.movePtz(
    url = "http://192.168.1.100",
    direction = PtzDirection.RIGHT,
    speed = 0.5f,
    profileToken = profileToken
)
```

#### Решение 3: Проверка настроек камеры
- Проверьте веб-интерфейс камеры
- Убедитесь, что PTZ не заблокирован
- Проверьте права доступа пользователя

---

## Проблемы с получением потоков

### Проблема: getStreamUri() возвращает null

**Симптомы:**
```kotlin
val streamUri = onvifClient.getStreamUri(
    url = "http://192.168.1.100",
    profileToken = "Profile1"
)
// streamUri = null
```

**Причины:**
1. Неправильный profileToken
2. Камера не поддерживает потоки
3. Ошибка аутентификации

**Решения:**

#### Решение 1: Использование правильного profileToken
```kotlin
// Получите список профилей
val profiles = onvifClient.getProfiles(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)

// Используйте правильный profileToken
for (profile in profiles) {
    val streamUri = onvifClient.getStreamUri(
        url = "http://192.168.1.100",
        profileToken = profile.token,
        username = "admin",
        password = "password123"
    )
    if (streamUri != null) {
        println("Stream URI: $streamUri")
        break
    }
}
```

#### Решение 2: Проверка поддержки потоков
```kotlin
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)

if (capabilities?.mediaServiceUrl == null) {
    println("Камера не поддерживает Media Service")
}
```

---

## Проблемы с XML парсингом

### Проблема: Ошибки парсинга XML

**Симптомы:**
```
Exception: SerializationException
Error: XML parsing failed
```

**Причины:**
1. Нестандартный формат XML от камеры
2. Неподдерживаемые namespace
3. Поврежденный XML ответ

**Решения:**

#### Решение 1: Клиент использует fallback парсинг
Клиент автоматически использует fallback парсинг через regex при неудаче DOM парсинга. Ошибка может быть временной.

#### Решение 2: Проверка логов
```kotlin
// Включите детальное логирование
// Клиент автоматически логирует ошибки парсинга
```

#### Решение 3: Сообщите о проблеме
Если проблема повторяется, сообщите разработчикам с:
- Моделью камеры
- Версией прошивки
- Примером XML ответа (из логов)

---

## Проблемы с производительностью

### Проблема: Медленное обнаружение камер

**Симптомы:**
```kotlin
val cameras = onvifClient.discoverCameras()
// Занимает более 10 секунд
```

**Решения:**

#### Решение 1: Уменьшение таймаута
```kotlin
val cameras = onvifClient.discoverCameras(
    timeoutMillis = 3000 // Уменьшите до 3 секунд
)
```

#### Решение 2: Кэширование (планируется)
Кэширование capabilities и profiles планируется для улучшения производительности.

---

## Часто задаваемые вопросы

### Q: Почему камера не обнаруживается?

**A:** Возможные причины:
1. Камера не поддерживает WS-Discovery
2. Multicast заблокирован в сети
3. Камера в другой подсети
4. Файрвол блокирует UDP

**Решение:** Добавьте камеру вручную или дождитесь UPnP поддержки.

---

### Q: Поддерживаются ли все производители камер?

**A:** Клиент поддерживает стандартный ONVIF протокол, но:
- Некоторые производители имеют нестандартные реализации
- Некоторые камеры требуют Digest Authentication (в разработке)
- Некоторые функции могут отличаться

**Рекомендация:** Тестируйте с вашей конкретной моделью камеры.

---

### Q: Как проверить, поддерживает ли камера ONVIF?

**A:** Используйте testConnection():

```kotlin
val result = onvifClient.testConnection(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)

when (result) {
    is ConnectionTestResult.Success -> {
        println("Камера поддерживает ONVIF")
        println("Capabilities: ${result.capabilities}")
    }
    is ConnectionTestResult.Failure -> {
        println("Камера не поддерживает ONVIF или недоступна")
        println("Error: ${result.error}")
    }
}
```

---

### Q: Можно ли использовать HTTPS вместо HTTP?

**A:** Да, если камера поддерживает HTTPS:

```kotlin
val capabilities = onvifClient.getCapabilities(
    url = "https://192.168.1.100", // Используйте https://
    username = "admin",
    password = "password123"
)
```

**Примечание:** Могут потребоваться настройки SSL/TLS для самоподписанных сертификатов.

---

## Получение помощи

Если проблема не решена:

1. **Проверьте логи** - клиент логирует детальную информацию
2. **Проверьте документацию** - [ONVIF_CLIENT.md](ONVIF_CLIENT.md)
3. **Сообщите о проблеме** - создайте issue с:
   - Моделью камеры
   - Версией прошивки
   - Логами ошибок
   - Шагами воспроизведения

---

**Последнее обновление:** 26 January 2026
