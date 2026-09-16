# Настройка Certificate Pinning и HTTPS для Production

**Дата создания:** Январь 2026
**Статус:** Готово к использованию

---

## 📋 Содержание

1. [Получение SHA-256 Fingerprints](#получение-sha-256-fingerprints)
2. [Настройка Certificate Pinning](#настройка-certificate-pinning)
3. [Настройка HTTPS на сервере](#настройка-https-на-сервере)
4. [Мониторинг и алертинг](#мониторинг-и-алертинг)
5. [Проверка и тестирование](#проверка-и-тестирование)

---

## 🔐 Получение SHA-256 Fingerprints

### Linux/macOS

Используйте готовый скрипт:

```bash
chmod +x scripts/get-certificate-fingerprint.sh
./scripts/get-certificate-fingerprint.sh api.example.com 443
```

Или вручную:

```bash
echo | openssl s_client -connect api.example.com:443 -showcerts | \
  openssl x509 -fingerprint -sha256 -noout | \
  cut -d'=' -f2
```

### Windows (PowerShell)

Используйте готовый скрипт:

```powershell
.\scripts\get-certificate-fingerprint.ps1 -Hostname api.example.com -Port 443
```

Или вручную через PowerShell:

```powershell
$tcpClient = New-Object System.Net.Sockets.TcpClient("api.example.com", 443)
$sslStream = New-Object System.Net.Security.SslStream($tcpClient.GetStream(), $false, {$true})
$sslStream.AuthenticateAsClient("api.example.com")
$cert = [System.Security.Cryptography.X509Certificates.X509Certificate2]$sslStream.RemoteCertificate
$hash = $cert.GetCertHashString("SHA256")
$bytes = [System.Convert]::FromHexString($hash)
$base64 = [System.Convert]::ToBase64String($bytes)
Write-Host "sha256/$base64"
```

### Онлайн инструменты

- **SSL Labs**: https://www.ssllabs.com/ssltest/analyze.html?d=api.example.com
- **Certificate Transparency**: https://crt.sh/?q=api.example.com

---

## ⚙️ Настройка Certificate Pinning

### Шаг 1: Получите fingerprints для всех доменов

Для каждого production домена получите SHA-256 fingerprints:

```bash
# Основной домен API
./scripts/get-certificate-fingerprint.sh api.example.com

# Backup домен (если есть)
./scripts/get-certificate-fingerprint.sh api-backup.example.com
```

**Важно:** Всегда получайте fingerprints для:
- Основного сертификата
- Backup сертификата (для ротации)
- Сертификата промежуточного CA (опционально)

### Шаг 2: Создайте конфигурационный файл

Скопируйте пример конфигурации:

```bash
cp config/certificate-pins.example.json config/certificate-pins.json
```

Отредактируйте `config/certificate-pins.json`:

```json
{
  "enabled": true,
  "enforce": true,
  "certificates": {
    "api.example.com": [
      "sha256/YOUR_PRIMARY_CERTIFICATE_SHA256_FINGERPRINT",
      "sha256/YOUR_BACKUP_CERTIFICATE_SHA256_FINGERPRINT"
    ],
    "api2.example.com": [
      "sha256/YOUR_CERTIFICATE_SHA256_FINGERPRINT"
    ]
  }
}
```

### Шаг 3: Настройка через переменные окружения

Альтернативно, можно использовать переменные окружения:

```bash
export CERTIFICATE_PINNING_ENABLED=true
export CERTIFICATE_PINNING_ENFORCE=true
export CERTIFICATE_PINS="api.example.com:sha256/AAAA...,sha256/BBBB...|api2.example.com:sha256/CCCC..."
```

### Шаг 4: Интеграция в приложение

#### Android/iOS/Desktop (Kotlin Multiplatform)

```kotlin
import com.company.ipcamera.core.network.security.CertificatePinningManager
import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiClientConfig

// Загрузка конфигурации
val pinningConfig = CertificatePinningManager.loadConfig("config/certificate-pins.json")

// Создание ApiClient с certificate pinning
val apiConfig = ApiClientConfig(
    baseUrl = "https://api.example.com",
    certificatePinningConfig = pinningConfig
)

val apiClient = ApiClient.create(apiConfig)
```

#### Android (XML конфигурация - дополнительно)

Отредактируйте `android/app/src/main/res/xml/network_security_config.xml`:

```xml
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">api.example.com</domain>
    <pin-set expiration="2026-12-31">
        <pin digest="SHA-256">YOUR_PRIMARY_CERTIFICATE_SHA256_FINGERPRINT</pin>
        <pin digest="SHA-256">YOUR_BACKUP_CERTIFICATE_SHA256_FINGERPRINT</pin>
    </pin-set>
</domain-config>
```

---

## 🔒 Настройка HTTPS на сервере

### Вариант 1: Nginx (рекомендуется)

#### Шаг 1: Установка nginx

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y nginx

# CentOS/RHEL
sudo yum install -y nginx
```

#### Шаг 2: Получение SSL сертификатов (Let's Encrypt)

Используйте готовый скрипт:

```bash
chmod +x scripts/setup-ssl-letsencrypt.sh
./scripts/setup-ssl-letsencrypt.sh api.example.com admin@example.com
```

Или вручную:

```bash
# Установка certbot
sudo apt-get install -y certbot python3-certbot-nginx

# Получение сертификата
sudo certbot --nginx -d api.example.com --email admin@example.com --agree-tos --non-interactive
```

#### Шаг 3: Настройка nginx

Скопируйте пример конфигурации:

```bash
sudo cp config/nginx/nginx.conf.example /etc/nginx/sites-available/ip-css
sudo ln -s /etc/nginx/sites-available/ip-css /etc/nginx/sites-enabled/
```

Отредактируйте конфигурацию:

```bash
sudo nano /etc/nginx/sites-available/ip-css
```

Обновите:
- `server_name` - ваш домен
- `ssl_certificate` - путь к сертификату
- `ssl_certificate_key` - путь к ключу

#### Шаг 4: Проверка и перезагрузка

```bash
# Проверка конфигурации
sudo nginx -t

# Перезагрузка nginx
sudo systemctl reload nginx
```

#### Шаг 5: Автоматическое обновление сертификатов

Certbot автоматически создает cron job для обновления. Проверьте:

```bash
sudo crontab -l | grep certbot
```

### Вариант 2: Ktor с встроенным SSL (не рекомендуется для production)

Для development можно использовать встроенный SSL в Ktor:

```kotlin
embeddedServer(Netty, applicationEngineEnvironment {
    connector {
        host = "0.0.0.0"
        port = 8080
    }
    sslConnector(
        keyStore = KeyStore.getInstance("PKCS12").apply {
            load(FileInputStream("keystore.p12"), "password".toCharArray())
        },
        keyAlias = "alias",
        keyStorePassword = { "password".toCharArray() },
        privateKeyPassword = { "password".toCharArray() }
    ) {
        host = "0.0.0.0"
        port = 8443
    }
    module(Application::module)
}).start(wait = true)
```

**Примечание:** Для production рекомендуется использовать nginx для HTTPS терминирования.

---

## 📊 Мониторинг и алертинг

### Логирование ошибок Certificate Pinning

Ошибки certificate pinning автоматически логируются через `SecurityLogger`:

```kotlin
// На сервере (если обнаружена попытка MITM)
SecurityLogger.logCertificatePinningFailure(
    ipAddress = call.request.origin.remoteHost,
    host = "api.example.com",
    reason = "Certificate pin mismatch"
)
```

### Настройка алертов

#### 1. Мониторинг логов

Настройте мониторинг логов на наличие ошибок certificate pinning:

```bash
# Поиск ошибок certificate pinning в логах
grep "CERTIFICATE_PINNING_FAILURE" /var/log/ip-css/security.log

# Или через journalctl (systemd)
journalctl -u ip-css-api -g "CERTIFICATE_PINNING_FAILURE"
```

#### 2. Настройка алертов (Prometheus + Alertmanager)

Пример alert rule:

```yaml
groups:
  - name: security_alerts
    rules:
      - alert: CertificatePinningFailure
        expr: increase(security_events_total{type="CERTIFICATE_PINNING_FAILURE"}[5m]) > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Certificate pinning validation failed"
          description: "Possible MITM attack detected on {{ $labels.host }}"
```

#### 3. Настройка алертов (ELK Stack)

Создайте правило в Kibana:

```json
{
  "query": {
    "match": {
      "type": "CERTIFICATE_PINNING_FAILURE"
    }
  },
  "actions": [
    {
      "type": "email",
      "email": {
        "to": ["security@example.com"],
        "subject": "Certificate Pinning Failure Alert"
      }
    }
  ]
}
```

### Мониторинг SSL/TLS сертификатов

#### Проверка срока действия сертификатов

Создайте скрипт для мониторинга:

```bash
#!/bin/bash
# scripts/check-certificate-expiry.sh

DOMAIN="api.example.com"
DAYS_BEFORE_EXPIRY=30

EXPIRY_DATE=$(echo | openssl s_client -connect $DOMAIN:443 2>/dev/null | \
    openssl x509 -noout -enddate | cut -d= -f2)

EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
CURRENT_EPOCH=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( ($EXPIRY_EPOCH - $CURRENT_EPOCH) / 86400 ))

if [ $DAYS_UNTIL_EXPIRY -lt $DAYS_BEFORE_EXPIRY ]; then
    echo "⚠️  ВНИМАНИЕ: Сертификат для $DOMAIN истекает через $DAYS_UNTIL_EXPIRY дней!"
    # Отправить алерт
fi
```

Добавьте в cron:

```bash
# Проверка каждый день в 9:00
0 9 * * * /path/to/scripts/check-certificate-expiry.sh
```

---

## ✅ Проверка и тестирование

### Тест 1: Проверка Certificate Pinning

#### Android/iOS/Desktop

Создайте тестовую конфигурацию с неверным pin:

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

// Должна быть ошибка: SSLPeerUnverifiedException
val result = apiClient.get<ResponseData>("/endpoint")
```

#### Ожидаемый результат:
- Соединение должно быть отклонено
- В логах должна быть запись об ошибке certificate pinning

### Тест 2: Проверка HTTPS Redirect

```bash
# HTTP запрос должен перенаправляться на HTTPS
curl -I http://api.example.com/api/v1/health

# Ожидаемый ответ:
# HTTP/1.1 301 Moved Permanently
# Location: https://api.example.com/api/v1/health
```

### Тест 3: Проверка HSTS Header

```bash
curl -I https://api.example.com/api/v1/health

# Ожидаемый заголовок:
# Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
```

### Тест 4: Проверка SSL/TLS версии

```bash
# Проверка поддерживаемых версий TLS
openssl s_client -connect api.example.com:443 -tls1_2
openssl s_client -connect api.example.com:443 -tls1_3

# TLS 1.0 и 1.1 должны быть отклонены
openssl s_client -connect api.example.com:443 -tls1
openssl s_client -connect api.example.com:443 -tls1_1
```

### Тест 5: SSL Labs Test

Проверьте конфигурацию через SSL Labs:

1. Перейдите на https://www.ssllabs.com/ssltest/
2. Введите ваш домен
3. Проверьте оценку (должна быть A или A+)
4. Убедитесь, что:
   - Поддерживаются только TLS 1.2 и 1.3
   - HSTS включен
   - Нет слабых шифров

---

## 🔄 Ротация сертификатов

### Подготовка к ротации

1. **Получите новый сертификат:**
   ```bash
   sudo certbot certonly --nginx -d api.example.com
   ```

2. **Получите SHA-256 fingerprint нового сертификата:**
   ```bash
   ./scripts/get-certificate-fingerprint.sh api.example.com
   ```

3. **Добавьте новый pin в конфигурацию:**
   ```json
   {
     "certificates": {
       "api.example.com": [
         "sha256/OLD_CERTIFICATE_PIN",  // Старый (для плавной миграции)
         "sha256/NEW_CERTIFICATE_PIN"   // Новый
       ]
     }
   }
   ```

4. **Обновите приложения:**
   - Обновите конфигурационный файл
   - Разверните обновление приложений
   - Подождите 24-48 часов для распространения обновлений

5. **Удалите старый pin:**
   ```json
   {
     "certificates": {
       "api.example.com": [
         "sha256/NEW_CERTIFICATE_PIN"  // Только новый
       ]
     }
   }
   ```

---

## 📚 Дополнительные ресурсы

- [Certificate Pinning Best Practices](https://owasp.org/www-community/controls/Certificate_and_Public_Key_Pinning)
- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Nginx SSL Configuration](https://nginx.org/en/docs/http/configuring_https_servers.html)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)

---

## ⚠️ Важные замечания

1. **Всегда используйте минимум 2 pin** для возможности ротации сертификатов
2. **Не включайте enforce: true** до тщательного тестирования
3. **Мониторьте ошибки certificate pinning** - они могут указывать на MITM атаки
4. **Регулярно обновляйте сертификаты** - Let's Encrypt сертификаты действительны 90 дней
5. **Тестируйте в staging** перед развертыванием в production

---

**Статус:** ✅ Готово к использованию
