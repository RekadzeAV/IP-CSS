# Использование Certificate Pinning в IP-CSS

**Дата создания:** 26 January 2026
**Версия проекта:** Alfa-0.0.1

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

---

## Обзор

Certificate Pinning интегрирован в основные компоненты сети:
- ✅ ApiClient
- ✅ OnvifClient (через OnvifClientFactory)
- ⚠️ WebSocketClient (не требует pinning, так как использует WSS напрямую)

---

## Использование в ApiClient

Certificate pinning автоматически применяется при создании ApiClient:

```kotlin
import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.security.CertificatePinningManager

// Автоматическая загрузка конфигурации
val pinningConfig = CertificatePinningManager.loadConfig()

val apiConfig = ApiClientConfig(
    baseUrl = "https://api.example.com",
    certificatePinningConfig = pinningConfig
)

val apiClient = ApiClient.create(apiConfig)
```

---

## Использование в OnvifClient

### Вариант 1: С автоматической загрузкой конфигурации

```kotlin
import com.company.ipcamera.core.network.OnvifClientFactory

// Автоматически загружает конфигурацию из файла или переменных окружения
val onvifClient = OnvifClientFactory.createWithAutoConfig()
```

### Вариант 2: С явной конфигурацией

```kotlin
import com.company.ipcamera.core.network.OnvifClientFactory
import com.company.ipcamera.core.network.security.CertificatePinningConfig

val pinningConfig = CertificatePinningConfig.create(
    certificates = mapOf(
        "192.168.1.100" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        )
    )
)

val onvifClient = OnvifClientFactory.create(pinningConfig = pinningConfig)
```

### Вариант 3: С кастомным engine

```kotlin
import com.company.ipcamera.core.network.OnvifClientFactory
import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.security.CertificatePinningConfig

val pinningConfig = CertificatePinningManager.loadConfig()
val engine = ApiClient.createEngineWithPinning(pinningConfig)

val onvifClient = OnvifClientFactory.create(engine = engine)
```

---

## WebSocketClient

WebSocketClient использует WebSocket протокол (ws:// или wss://). Certificate pinning для WebSocket соединений:

- **WSS (WebSocket Secure)**: Использует TLS, но pinning должен быть настроен на уровне платформы
- **WS (WebSocket)**: Не использует TLS, pinning не применим

Для WSS соединений certificate pinning работает автоматически через системные настройки платформы:
- **Android**: Через network_security_config.xml
- **iOS**: Через NSURLSession настройки
- **JVM**: Через системные настройки SSL

---

## Примеры использования

### Пример 1: Базовое использование

```kotlin
// 1. Загрузить конфигурацию
val pinningConfig = CertificatePinningManager.loadConfig()

// 2. Создать ApiClient с pinning
val apiClient = ApiClient.create(
    ApiClientConfig(
        baseUrl = "https://api.example.com",
        certificatePinningConfig = pinningConfig
    )
)

// 3. Использовать клиент
val result = apiClient.get<ResponseDto>("/endpoint")
```

### Пример 2: OnvifClient с pinning

```kotlin
// Создать OnvifClient с автоматической конфигурацией
val onvifClient = OnvifClientFactory.createWithAutoConfig()

// Обнаружить камеры
val cameras = onvifClient.discoverCameras()

// Получить информацию об устройстве
val deviceInfo = onvifClient.getDeviceInformation(
    url = "https://192.168.1.100",
    username = "admin",
    password = "password"
)
```

### Пример 3: Программная конфигурация

```kotlin
import com.company.ipcamera.core.network.security.CertificatePinningManager

// Создать конфигурацию программно
val pinningConfig = CertificatePinningManager.createFromMap(
    certificates = mapOf(
        "api.example.com" to listOf(
            "sha256/CURRENT_FINGERPRINT=",
            "sha256/BACKUP_FINGERPRINT="
        ),
        "192.168.1.100" to listOf(
            "sha256/CAMERA_FINGERPRINT="
        )
    ),
    enablePinning = true,
    enforcePinning = true
)

// Использовать в ApiClient
val apiClient = ApiClient.create(
    ApiClientConfig(
        baseUrl = "https://api.example.com",
        certificatePinningConfig = pinningConfig
    )
)
```

---

## Отладка

### Включение детального логирования

Certificate pinning логирует важные события:
- Загрузка конфигурации
- Валидация pins
- Успешные/неуспешные проверки

Для включения детального логирования установите уровень логирования:

```kotlin
// В development
System.setProperty("kotlin.logging.level", "DEBUG")
```

### Проверка загруженной конфигурации

```kotlin
val config = CertificatePinningManager.loadConfig()
println("Pinning enabled: ${config.enablePinning}")
println("Enforce pinning: ${config.enforcePinning}")
println("Hosts: ${config.pinnedCertificates.keys}")
```

---

## Best Practices

1. **Всегда используйте backup pins** - на случай обновления сертификата
2. **Тестируйте на всех платформах** - pinning работает по-разному
3. **Отключайте enforcePinning в development** - для упрощения отладки
4. **Регулярно обновляйте pins** - при обновлении сертификатов на сервере
5. **Используйте централизованную конфигурацию** - через CertificatePinningManager

---

## Troubleshooting

### Проблема: Соединение отклоняется

**Решение:**
1. Проверьте текущий fingerprint сервера
2. Обновите pins в конфигурации
3. Убедитесь, что используете fingerprint leaf сертификата

### Проблема: Pinning не работает

**Решение:**
1. Проверьте логи на наличие сообщений о загрузке конфигурации
2. Убедитесь, что `enablePinning=true`
3. Проверьте формат pin (должен начинаться с `sha256/`)

---

**Последнее обновление:** 26 January 2026
