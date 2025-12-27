# SSL/TLS Certificate Pinning Implementation

## Обзор

Реализована поддержка SSL/TLS certificate pinning для всех платформ (Android, iOS, JVM/Desktop). Certificate pinning защищает от MITM (Man-In-The-Middle) атак путем проверки SHA-256 fingerprints сертификатов сервера.

## Структура реализации

### Common (общая часть)

- **`CertificatePinningConfig`**: Конфигурация для certificate pinning
  - `pinnedCertificates`: Map хостов и их SHA-256 fingerprints
  - `enablePinning`: Включить/выключить pinning
  - `enforcePinning`: Отклонять соединения при несовпадении (по умолчанию `true`)

- **`CertificatePinner`**: Expect класс для platform-specific реализаций

### Platform-specific реализации

#### Android
- **Файл**: `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.android.kt`
- **Использует**: OkHttp `CertificatePinner`
- **Статус**: Базовая структура реализована
- **Примечание**: Для полной интеграции с Ktor Android engine рекомендуется использовать Android Network Security Config (XML файл)

#### iOS
- **Файл**: `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.ios.kt`
- **Использует**: NSURLSession (требует дополнительной реализации через URLSessionDelegate)
- **Статус**: Базовая структура реализована
- **Примечание**: Полная реализация требует использования URLSessionDelegate методов

#### JVM/Desktop
- **Файл**: `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.jvm.kt`
- **Использует**: Кастомный `TrustManager` с проверкой SHA-256 fingerprints
- **Статус**: Полностью реализовано
- **Особенности**: Использует кастомный `X509TrustManager`, который проверяет certificate pins перед принятием соединения

## Использование

### Пример создания ApiClient с certificate pinning

```kotlin
import com.company.ipcamera.core.network.*
import com.company.ipcamera.core.network.security.CertificatePinningConfig

// Настройка certificate pinning
val pinningConfig = CertificatePinningConfig.create(
    certificates = mapOf(
        "api.example.com" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        ),
        "another-api.example.com" to listOf(
            "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="
        )
    )
)

// Создание ApiClient с certificate pinning
val config = ApiClientConfig(
    baseUrl = "https://api.example.com",
    certificatePinningConfig = pinningConfig
)

val apiClient = ApiClient.create(config)
```

### Отключение certificate pinning

```kotlin
val config = ApiClientConfig(
    baseUrl = "https://api.example.com",
    certificatePinningConfig = CertificatePinningConfig.disabled()
)
```

## Получение SHA-256 fingerprint сертификата

### Через OpenSSL
```bash
openssl s_client -servername api.example.com -connect api.example.com:443 < /dev/null | \
  openssl x509 -pubkey -noout | \
  openssl pkey -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64
```

### Через Java keytool
```bash
keytool -printcert -sslserver api.example.com:443 | grep -A 1 "Certificate fingerprints"
```

### Через Python
```python
import ssl
import hashlib
import base64

cert = ssl.get_server_certificate(('api.example.com', 443))
cert_der = ssl.PEM_cert_to_DER_cert(cert)
fingerprint = hashlib.sha256(cert_der).digest()
pin = base64.b64encode(fingerprint).decode()
print(f"sha256/{pin}")
```

## Важные замечания

1. **Обновление сертификатов**: При обновлении сертификатов сервера необходимо обновить pins в конфигурации. Рекомендуется использовать несколько pins (основной + резервный) для плавного перехода.

2. **Android**: Для полной реализации на Android рекомендуется использовать [Network Security Config](https://developer.android.com/training/articles/security-config), так как Ktor Android engine не предоставляет прямого способа передать предварительно настроенный OkHttpClient.

3. **iOS**: Полная реализация на iOS требует использования URLSessionDelegate методов для проверки сертификатов, что требует дополнительной интеграции с Ktor Darwin engine.

4. **JVM/Desktop**: Реализация полностью функциональна и использует кастомный TrustManager для проверки certificate pins.

5. **TLS версии**: Все реализации принудительно используют только TLS 1.2 и выше (TLS 1.3, если поддерживается).

## Безопасность

- ✅ Защита от MITM атак
- ✅ Проверка цепочки сертификатов
- ✅ Использование только TLS 1.2+
- ✅ SHA-256 fingerprints (рекомендуемый стандарт)

## Статус реализации

- ✅ Общая структура и конфигурация
- ✅ JVM/Desktop реализация (полностью функциональна)
- ⚠️ Android реализация (базовая структура, рекомендуется Network Security Config)
- ⚠️ iOS реализация (базовая структура, требует дополнительной работы)

## Дальнейшие улучшения

1. Полная интеграция certificate pinning с Ktor Android engine
2. Реализация certificate pinning через URLSessionDelegate для iOS
3. Добавление поддержки обновления pins через remote configuration
4. Интеграция с Android Network Security Config
5. Добавление unit и integration тестов


