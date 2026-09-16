# Завершение безопасности: Certificate Pinning и HTTPS

**Дата завершения:** Январь 2026
**Статус:** ✅ Завершено

## Резюме

Реализованы и завершены все компоненты безопасности, связанные с Certificate Pinning и принудительным использованием HTTPS.

---

## ✅ Реализованные компоненты

### 1. Certificate Pinning

#### Android
- ✅ Реализовано через OkHttp `CertificatePinner`
- ✅ Интегрировано в Ktor Android engine через `AndroidEngineConfig.preconfigured`
- ✅ Поддержка TLS 1.2+ только
- ✅ Файл: `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.android.kt`

#### iOS
- ✅ Реализовано через `CertificatePinningDelegate` и `CertificatePinningEngineWrapper`
- ✅ Полная интеграция с NSURLSession
- ✅ Файлы:
  - `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.ios.kt`
  - `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinningDelegate.ios.kt`
  - `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinningEngineWrapper.ios.kt`

#### JVM/Desktop
- ✅ Реализовано через кастомный `TrustManager`
- ✅ Проверка SHA-256 fingerprints сертификатов
- ✅ Поддержка TLS 1.2+ только
- ✅ Файл: `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.jvm.kt`

#### Интеграция
- ✅ Автоматическая интеграция в `ApiClient` через `ApiClientConfig.certificatePinningConfig`
- ✅ Поддержка в `OnvifClientFactory`
- ✅ Файлы:
  - `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt`
  - `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClientFactory.kt`

### 2. HTTPS принудительно

#### Android
- ✅ `android:usesCleartextTraffic="false"` в `AndroidManifest.xml`
- ✅ `network_security_config.xml` настроен правильно
- ✅ Cleartext traffic разрешен только для localhost в debug режиме
- ✅ Файлы:
  - `android/app/src/main/AndroidManifest.xml`
  - `android/app/src/main/res/xml/network_security_config.xml`

#### Сервер (Ktor)
- ✅ HTTPS Redirect middleware реализован
- ✅ HSTS (HTTP Strict Transport Security) middleware реализован
- ✅ Автоматическое перенаправление HTTP → HTTPS в production
- ✅ Файлы:
  - `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/HttpsRedirectMiddleware.kt`
  - `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/HstsMiddleware.kt`
  - `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt`

#### Next.js (Web)
- ✅ Security headers настроены
- ✅ HTTPS redirect в production
- ✅ Файл: `server/web/next.config.js`

---

## 📋 Использование

### Certificate Pinning

#### 1. Создание конфигурации

```kotlin
import com.company.ipcamera.core.network.security.CertificatePinningConfig

// Создание конфигурации с сертификатами
val pinningConfig = CertificatePinningConfig.create(
    certificates = mapOf(
        "api.example.com" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // Основной сертификат
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="  // Backup сертификат
        )
    )
)

// Или отключить pinning
val disabledConfig = CertificatePinningConfig.disabled()
```

#### 2. Использование с ApiClient

```kotlin
import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiClientConfig

val apiConfig = ApiClientConfig(
    baseUrl = "https://api.example.com",
    certificatePinningConfig = pinningConfig
)

val apiClient = ApiClient.create(apiConfig)
```

#### 3. Использование с OnvifClient

```kotlin
import com.company.ipcamera.core.network.OnvifClientFactory

val onvifClient = OnvifClientFactory.create(
    pinningConfig = pinningConfig
)
```

#### 4. Автоматическая загрузка конфигурации

```kotlin
import com.company.ipcamera.core.network.security.CertificatePinningManager

// Загрузка из файла
val config = CertificatePinningManager.loadFromFile("certificate-pins.json")

// Загрузка из переменных окружения
val envConfig = CertificatePinningManager.loadFromEnvironment()

// Автоматическая загрузка (файл или переменные окружения)
val autoConfig = CertificatePinningManager.loadConfig()
```

### HTTPS

#### Android

HTTPS принудительно включен по умолчанию через:
- `AndroidManifest.xml`: `android:usesCleartextTraffic="false"`
- `network_security_config.xml`: базовая конфигурация запрещает cleartext

Для настройки certificate pinning в XML (опционально, дополнительно к программному):

```xml
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">api.example.com</domain>
    <pin-set expiration="2026-12-31">
        <pin digest="SHA-256">YOUR_PRIMARY_CERTIFICATE_SHA256_FINGERPRINT</pin>
        <pin digest="SHA-256">YOUR_BACKUP_CERTIFICATE_SHA256_FINGERPRINT</pin>
    </pin-set>
</domain-config>
```

#### Сервер

HTTPS redirect и HSTS автоматически активируются в production:

```kotlin
// В Application.kt
val isProduction = System.getenv("ENVIRONMENT") == "production" ||
                   System.getenv("ENVIRONMENT") == "prod"

// HTTPS Redirect
installHttpsRedirect(enabled = isProduction)

// HSTS
installHsts(
    maxAge = 31536000, // 1 год
    includeSubDomains = true,
    preload = false,
    enabled = isProduction
)
```

---

## 🔧 Получение SHA-256 Fingerprint сертификата

### Через OpenSSL

```bash
# Получить fingerprint сертификата
openssl s_client -connect api.example.com:443 -showcerts | \
  openssl x509 -fingerprint -sha256 -noout

# Или получить в формате Base64 (для pinning)
openssl s_client -connect api.example.com:443 -showcerts | \
  openssl x509 -pubkey -noout | \
  openssl pkey -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64
```

### Через онлайн инструменты

- SSL Labs: https://www.ssllabs.com/ssltest/
- Certificate Transparency Logs: https://crt.sh/

---

## 📝 Конфигурация через переменные окружения

### Формат переменных

```bash
# Включить certificate pinning
CERTIFICATE_PINNING_ENABLED=true

# Формат: HOST1:PIN1,PIN2|HOST2:PIN3,PIN4
CERTIFICATE_PINS=api.example.com:sha256/AAAA...,sha256/BBBB...|api2.example.com:sha256/CCCC...

# Enforce pinning (отклонять соединения при несовпадении)
CERTIFICATE_PINNING_ENFORCE=true
```

### Пример

```bash
export CERTIFICATE_PINNING_ENABLED=true
export CERTIFICATE_PINNING_ENFORCE=true
export CERTIFICATE_PINS="api.example.com:sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=,sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=|api2.example.com:sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="
```

---

## 📄 Конфигурация через JSON файл

### Формат файла `certificate-pins.json`

```json
{
  "enabled": true,
  "enforce": true,
  "certificates": {
    "api.example.com": [
      "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
    ],
    "api2.example.com": [
      "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="
    ]
  }
}
```

---

## ✅ Критерии приемки

- [x] Certificate Pinning реализован для всех платформ (Android, iOS, JVM)
- [x] Certificate Pinning интегрирован в ApiClient
- [x] Certificate Pinning интегрирован в OnvifClient
- [x] Android: cleartext traffic отключен
- [x] Android: network_security_config.xml настроен
- [x] Сервер: HTTPS redirect реализован
- [x] Сервер: HSTS реализован
- [x] Сервер: HTTPS redirect и HSTS активируются в production
- [x] Web: Security headers настроены
- [x] Web: HTTPS redirect в production
- [x] Документация создана

---

## 🔍 Тестирование

### Тест Certificate Pinning

1. **Создать тестовую конфигурацию:**
```kotlin
val testConfig = CertificatePinningConfig.create(
    certificates = mapOf(
        "api.example.com" to listOf("sha256/INVALID_PIN")
    )
)

val apiClient = ApiClient.create(
    ApiClientConfig(
        baseUrl = "https://api.example.com",
        certificatePinningConfig = testConfig
    )
)
```

2. **Выполнить запрос:**
```kotlin
val result = apiClient.get<ResponseData>("/endpoint")
// Должна быть ошибка: SSLPeerUnverifiedException
```

### Тест HTTPS Redirect

1. **Отправить HTTP запрос:**
```bash
curl -I http://api.example.com/api/v1/health
# Должен вернуть: 301 Moved Permanently
# Location: https://api.example.com/api/v1/health
```

2. **Проверить HSTS header:**
```bash
curl -I https://api.example.com/api/v1/health
# Должен вернуть: Strict-Transport-Security: max-age=31536000; includeSubDomains
```

---

## 📚 Связанные документы

- CERTIFICATE_PINNING_USAGE.md *(утерян/в архиве)* - Детальное руководство по использованию
- [CERTIFICATE_PINNING_SETUP.md](../archive/docs/guides/HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md) - Инструкции по настройке
- [MOBILE_SECURITY.md](MOBILE_SECURITY.md) - Безопасность мобильных приложений
- [SECURITY_AUDIT_REPORT.md](../archive/docs-duplicates-2026-08-08/SECURITY_AUDIT_REPORT.md) - Отчет об аудите безопасности

---

## 🎯 Следующие шаги

1. **Настроить certificate pinning для production доменов:**
   - Получить SHA-256 fingerprints сертификатов
   - Добавить в конфигурацию (JSON файл или переменные окружения)
   - Протестировать на staging окружении

2. **Настроить HTTPS на сервере:**
   - Получить SSL сертификаты (Let's Encrypt, коммерческий CA)
   - Настроить nginx или другой reverse proxy
   - Настроить автоматическое обновление сертификатов

3. **Мониторинг:**
   - Настроить мониторинг ошибок certificate pinning
   - Настроить алерты при обнаружении MITM атак
   - Логировать все ошибки SSL/TLS

---

**Статус:** ✅ Все компоненты реализованы и готовы к использованию
