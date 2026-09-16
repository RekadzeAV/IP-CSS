# Настройка HTTPS для IP-CSS

**Дата создания:** 26 January 2026
**Версия проекта:** Alfa-0.0.1

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

---

## Обзор

IP-CSS поддерживает HTTPS через nginx reverse proxy. Ktor сервер работает на HTTP (порт 8080), а nginx терминирует SSL соединения и проксирует запросы на Ktor.

---

## Варианты настройки

### Вариант 1: Nginx с Let's Encrypt (Рекомендуется для production)

#### Шаг 1: Установка nginx и certbot

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install nginx certbot python3-certbot-nginx

# CentOS/RHEL
sudo yum install nginx certbot python3-certbot-nginx
```

#### Шаг 2: Настройка nginx

1. Скопируйте `nginx/nginx.conf` в `/etc/nginx/sites-available/ip-css`
2. Обновите `server_name` на ваш домен
3. Создайте симлинк:

```bash
sudo ln -s /etc/nginx/sites-available/ip-css /etc/nginx/sites-enabled/
```

#### Шаг 3: Получение SSL сертификата от Let's Encrypt

```bash
sudo certbot --nginx -d your-domain.com -d www.your-domain.com
```

Certbot автоматически:
- Получит SSL сертификат
- Обновит nginx конфигурацию
- Настроит автоматическое обновление

#### Шаг 4: Проверка конфигурации

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

### Вариант 2: Docker Compose с nginx

#### Шаг 1: Обновление docker-compose.yml

Добавьте nginx сервис:

```yaml
services:
  nginx:
    image: nginx:alpine
    container_name: ip-css-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./nginx/letsencrypt:/etc/letsencrypt:ro
    depends_on:
      - surveillance
    networks:
      - surveillance-network
    restart: unless-stopped
```

#### Шаг 2: Создание SSL сертификатов

Для Let's Encrypt в Docker:

```bash
# Используйте certbot в отдельном контейнере
docker run -it --rm \
  -v ./nginx/letsencrypt:/etc/letsencrypt \
  -v ./nginx/ssl:/var/www/certbot \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d your-domain.com
```

---

### Вариант 3: Самоподписанные сертификаты (только для разработки)

#### Создание самоподписанного сертификата

```bash
mkdir -p nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/key.pem \
  -out nginx/ssl/cert.pem \
  -subj "/C=RU/ST=Moscow/L=Moscow/O=Company/CN=localhost"
```

**⚠️ ВНИМАНИЕ:** Самоподписанные сертификаты не должны использоваться в production!

---

## Переменные окружения

### Ktor сервер

```bash
# Принудительный редирект HTTP → HTTPS
FORCE_HTTPS=true

# Production режим (автоматически включает FORCE_HTTPS)
NODE_ENV=production

# HTTP порт (по умолчанию 8080)
HTTP_PORT=8080
```

### Nginx

Nginx конфигурация находится в `nginx/nginx.conf`. Основные настройки:

- `listen 443 ssl http2` - HTTPS порт
- `ssl_certificate` - путь к SSL сертификату
- `ssl_certificate_key` - путь к приватному ключу
- `proxy_pass http://ktor_backend` - проксирование на Ktor

---

## Проверка работы HTTPS

### 1. Проверка редиректа HTTP → HTTPS

```bash
curl -I http://your-domain.com
# Должен вернуть: HTTP/1.1 301 Moved Permanently
# Location: https://your-domain.com
```

### 2. Проверка HTTPS соединения

```bash
curl -I https://your-domain.com
# Должен вернуть: HTTP/2 200
```

### 3. Проверка SSL сертификата

```bash
openssl s_client -connect your-domain.com:443 -servername your-domain.com
```

### 4. Проверка HSTS заголовка

```bash
curl -I https://your-domain.com | grep Strict-Transport-Security
# Должен вернуть: Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
```

---

## Автоматическое обновление Let's Encrypt сертификатов

Let's Encrypt сертификаты действительны 90 дней. Certbot автоматически обновляет их, но можно проверить вручную:

```bash
# Проверка статуса
sudo certbot certificates

# Ручное обновление
sudo certbot renew

# Тестовое обновление (dry-run)
sudo certbot renew --dry-run
```

Для автоматического обновления добавьте в crontab:

```bash
0 0 * * * certbot renew --quiet
```

---

## Troubleshooting

### Проблема: nginx не запускается

**Решение:**
```bash
# Проверьте конфигурацию
sudo nginx -t

# Проверьте логи
sudo tail -f /var/log/nginx/error.log
```

### Проблема: 502 Bad Gateway

**Решение:**
- Убедитесь, что Ktor сервер запущен на порту 8080
- Проверьте, что nginx может подключиться к Ktor (проверьте сеть Docker)
- Проверьте логи nginx: `sudo tail -f /var/log/nginx/error.log`

### Проблема: SSL сертификат не работает

**Решение:**
- Проверьте права доступа к файлам сертификатов
- Убедитесь, что пути в nginx.conf правильные
- Проверьте, что сертификат не истек: `openssl x509 -in cert.pem -noout -dates`

### Проблема: Редирект не работает

**Решение:**
- Убедитесь, что `FORCE_HTTPS=true` установлена
- Проверьте, что middleware SecurityHeadersMiddleware установлен
- Проверьте логи Ktor сервера

---

## Безопасность

### Рекомендации для production:

1. **Используйте только TLS 1.2+**
   - Уже настроено в nginx.conf: `ssl_protocols TLSv1.2 TLSv1.3;`

2. **Используйте сильные cipher suites**
   - Уже настроено в nginx.conf

3. **Включите HSTS**
   - Уже настроено в nginx.conf

4. **Регулярно обновляйте сертификаты**
   - Настроено автоматически через certbot

5. **Используйте только валидные сертификаты**
   - Не используйте самоподписанные сертификаты в production

---

## Дополнительные ресурсы

- [Let's Encrypt документация](https://letsencrypt.org/docs/)
- [Nginx SSL настройка](https://nginx.org/en/docs/http/configuring_https_servers.html)
- [Ktor SSL настройка](https://ktor.io/docs/ssl.html)

---

**Последнее обновление:** 26 January 2026
