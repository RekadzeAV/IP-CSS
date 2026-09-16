# HTTPS и Certificate Pins для продакшена — краткая инструкция

Краткое руководство по задаче **2.2.3** MVP: как включить HTTPS на сервере и как сгенерировать/добавить pins для продакшена.

Полная документация: **[PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md](PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md)**.

---

## 1. Включение HTTPS на сервере

### Вариант A: Nginx перед приложением (рекомендуется)

1. Установите nginx и получите SSL-сертификат (например, Let's Encrypt):
   ```bash
   sudo apt-get install -y nginx certbot python3-certbot-nginx
   sudo certbot --nginx -d api.example.com --email admin@example.com --agree-tos --non-interactive
   ```
2. Настройте nginx: проксирование на Ktor (порт 8080), редирект HTTP→HTTPS на 443.
3. Пример конфига: **[config/nginx/nginx.conf.example](../config/nginx/nginx.conf.example)**; полное описание — [PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md § Настройка HTTPS](PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md#-настройка-https-на-сервере).

### Вариант B: Встроенный редирект в приложении

Сервер уже поддерживает редирект HTTP → HTTPS через `HttpsRedirectMiddleware`. В production он включается автоматически (`installHttpsRedirect(enabled = isProduction)` в `Application.kt`).

Переменные окружения для приложения:

| Переменная    | Описание |
|---------------|----------|
| `FORCE_HTTPS` | Включить принудительный редирект HTTP→HTTPS (например `true` в production). |
| `USE_HTTPS`   | Включить встроенный SSL на Ktor (обычно не для production). |
| `HTTPS_PORT`  | Порт HTTPS при `USE_HTTPS=true` (по умолчанию 8443). |

Для production обычно: перед приложением стоит nginx с SSL (порт 443), приложение слушает только HTTP (например 8080); редирект при необходимости настраивается в nginx или через `FORCE_HTTPS` при прямом доступе к приложению.

---

## 2. Генерация и добавление pins для продакшена

### Шаг 1: Получить SHA-256 fingerprint сертификата

**Linux/macOS:**
```bash
./scripts/get-certificate-fingerprint.sh api.example.com 443
```

**Windows (PowerShell):**
```powershell
.\scripts\get-certificate-fingerprint.ps1 -Hostname api.example.com -Port 443
```

**Вручную (OpenSSL):**
```bash
echo | openssl s_client -connect api.example.com:443 -showcerts | \
  openssl x509 -fingerprint -sha256 -noout | cut -d'=' -f2
```
Результат нужно представить в формате `sha256/<base64>` (см. вывод скриптов).

### Шаг 2: Добавить pins в конфиг

1. Скопировать пример:
   ```bash
   cp config/certificate-pins.example.json config/certificate-pins.json
   ```
2. Подставить свои домены и fingerprints в `certificate-pins.json`:
   ```json
   {
     "enabled": true,
     "enforce": true,
     "certificates": {
       "api.example.com": [
         "sha256/PRIMARY_SHA256_BASE64",
         "sha256/BACKUP_SHA256_BASE64"
       ]
     }
   }
   ```
3. Рекомендуется **минимум 2 pin на домен** (основной + резервный) для ротации сертификатов.

### Шаг 3: Использование в клиентах

- **Android/iOS/Desktop:** загрузка конфига через `CertificatePinningManager.loadConfig("config/certificate-pins.json")` и передача в `ApiClientConfig.certificatePinningConfig`.
- **Переменные окружения (альтернатива):**
  ```bash
  export CERTIFICATE_PINNING_ENABLED=true
  export CERTIFICATE_PINNING_ENFORCE=true
  export CERTIFICATE_PINS="api.example.com:sha256/AAA...,sha256/BBB..."
  ```

---

## 3. Проверка

- **HTTPS-редирект:** `curl -I http://api.example.com/api/v1/health` → ожидается `301` и `Location: https://...`
- **Pinning:** подключение к API с валидным pin должно проходить; с неверным pin — отклоняться (см. [PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md § Проверка](PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md#-проверка-и-тестирование)).

---

## Ссылки

- [PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md](PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md) — полная настройка, мониторинг, ротация сертификатов.
- [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md) — переменные окружения (в т.ч. безопасность).
- [MVP_PHASED_IMPLEMENTATION_PLAN.md](MVP_PHASED_IMPLEMENTATION_PLAN.md) — Фаза 2, этап 2.2.
