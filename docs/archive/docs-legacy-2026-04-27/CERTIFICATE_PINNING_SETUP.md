# Настройка Certificate Pinning

**Дата создания:** 26 January 2026
**Версия проекта:** Alfa-0.0.1

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

---

## Обзор

Certificate Pinning защищает приложение от MITM (Man-In-The-Middle) атак путем проверки SHA-256 fingerprints SSL/TLS сертификатов сервера.

---

## Получение SHA-256 Fingerprint сертификата

### Метод 1: Использование OpenSSL

```bash
# Для HTTPS сервера
openssl s_client -connect api.example.com:443 -servername api.example.com < /dev/null 2>/dev/null | \
  openssl x509 -fingerprint -sha256 -noout -in /dev/stdin | \
  cut -d'=' -f2 | \
  sed 's/://g' | \
  awk '{print "sha256/" $0}'
```

### Метод 2: Использование curl

```bash
echo | openssl s_client -connect api.example.com:443 -servername api.example.com 2>/dev/null | \
  openssl x509 -pubkey -noout | \
  openssl pkey -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64 | \
  awk '{print "sha256/" $0}'
```

### Метод 3: Онлайн инструменты

Используйте [SSL Labs SSL Test](https://www.ssllabs.com/ssltest/) для получения информации о сертификате.

---

## Настройка Certificate Pinning

### Вариант 1: JSON файл

1. Скопируйте `core/network/certificate-pins.example.json` в `certificate-pins.json`
2. Обновите hosts и pins:

```json
{
  "hosts": {
    "api.example.com": [
      "sha256/YOUR_ACTUAL_FINGERPRINT_HERE=",
      "sha256/BACKUP_FINGERPRINT_HERE="
    ]
  },
  "enablePinning": true,
  "enforcePinning": true
}
```

3. Укажите путь к файлу через переменную окружения:

```bash
export CERTIFICATE_PINS_FILE=/path/to/certificate-pins.json
```

### Вариант 2: Переменные окружения

```bash
# Включить pinning
export CERTIFICATE_PINNING_ENABLED=true

# Принудительное отклонение при несовпадении
export CERTIFICATE_PINNING_ENFORCE=true

# Pins для хостов (формат: CERTIFICATE_PINS_<HOST>=sha256/...,sha256/...)
export CERTIFICATE_PINS_API_EXAMPLE_COM="sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=,sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
```

**Примечание:** Имя хоста в переменной окружения должно быть в верхнем регистре, с подчеркиваниями вместо точек и дефисов.

### Вариант 3: Программная конфигурация

```kotlin
import com.company.ipcamera.core.network.security.CertificatePinningManager
import com.company.ipcamera.core.network.security.CertificatePinningConfig

val config = CertificatePinningManager.createFromMap(
    certificates = mapOf(
        "api.example.com" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        )
    ),
    enablePinning = true,
    enforcePinning = true
)
```

---

## Использование в ApiClient

Certificate pinning автоматически применяется при создании ApiClient, если конфигурация загружена:

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

```kotlin
import com.company.ipcamera.core.network.OnvifClient
import com.company.ipcamera.core.network.security.CertificatePinningManager
import com.company.ipcamera.core.network.ApiClient

val pinningConfig = CertificatePinningManager.loadConfig()
val engine = ApiClient.createEngineWithPinning(pinningConfig)
val onvifClient = OnvifClient(engine)
```

---

## Best Practices

### 1. Используйте несколько pins (backup pins)

Всегда добавляйте backup pins на случай обновления сертификата:

```json
{
  "hosts": {
    "api.example.com": [
      "sha256/CURRENT_CERTIFICATE_FINGERPRINT=",
      "sha256/BACKUP_CERTIFICATE_FINGERPRINT="
    ]
  }
}
```

### 2. Не используйте enforcePinning в development

В development режиме можно отключить enforcePinning для упрощения отладки:

```json
{
  "enablePinning": true,
  "enforcePinning": false
}
```

### 3. Регулярно обновляйте pins

При обновлении SSL сертификата на сервере обновите pins в конфигурации.

### 4. Тестируйте на всех платформах

Certificate pinning работает по-разному на разных платформах:
- Android: Использует OkHttp CertificatePinner
- iOS: Использует NSURLSession с custom delegate
- JVM: Использует кастомный TrustManager

---

## Troubleshooting

### Проблема: Соединение отклоняется с ошибкой "Certificate pinning validation failed"

**Причины:**
1. Сертификат сервера изменился
2. Неправильный fingerprint в конфигурации
3. Используется промежуточный сертификат вместо leaf сертификата

**Решение:**
1. Проверьте текущий fingerprint сервера
2. Обновите pins в конфигурации
3. Убедитесь, что используете fingerprint leaf сертификата

### Проблема: Pinning не работает

**Причины:**
1. Конфигурация не загружена
2. Pinning отключен
3. Неправильный формат pin

**Решение:**
1. Проверьте логи на наличие сообщений о загрузке конфигурации
2. Убедитесь, что `enablePinning=true`
3. Проверьте формат pin (должен начинаться с `sha256/`)

---

## Формат Pin

Pin должен быть в формате:
```
sha256/<Base64-encoded SHA-256 hash>
```

Где:
- `sha256/` - префикс алгоритма
- `<Base64-encoded SHA-256 hash>` - Base64-encoded SHA-256 hash сертификата (обычно 44 символа)

Пример:
```
sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
```

---

## Дополнительные ресурсы

- [OWASP Certificate Pinning](https://owasp.org/www-community/controls/Certificate_and_Public_Key_Pinning)
- [Android Network Security Config](https://developer.android.com/training/articles/security-config)
- [iOS Certificate Pinning](https://developer.apple.com/documentation/foundation/url_loading_system/handling_an_authentication_challenge)

---

**Последнее обновление:** 26 January 2026
