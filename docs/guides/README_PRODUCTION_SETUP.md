# Быстрый старт: Настройка Certificate Pinning и HTTPS для Production

Это краткое руководство поможет вам быстро настроить Certificate Pinning и HTTPS для production окружения.

## 🚀 Быстрая настройка (5 минут)

### 1. Получите SHA-256 fingerprints

```bash
# Linux/macOS
chmod +x scripts/get-certificate-fingerprint.sh
./scripts/get-certificate-fingerprint.sh api.example.com

# Windows
.\scripts\get-certificate-fingerprint.ps1 -Hostname api.example.com
```

### 2. Создайте конфигурацию

```bash
cp config/certificate-pins.example.json config/certificate-pins.json
# Отредактируйте config/certificate-pins.json с вашими fingerprints
```

### 3. Настройте HTTPS на сервере

```bash
# Установка Let's Encrypt сертификатов
chmod +x scripts/setup-ssl-letsencrypt.sh
./scripts/setup-ssl-letsencrypt.sh api.example.com admin@example.com

# Настройка nginx
sudo cp config/nginx/nginx.conf.example /etc/nginx/sites-available/ip-css
sudo nano /etc/nginx/sites-available/ip-css  # Отредактируйте под ваш домен
sudo ln -s /etc/nginx/sites-available/ip-css /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 4. Настройте мониторинг

```bash
# Добавьте в cron для проверки сертификатов
0 9 * * * /path/to/scripts/check-certificate-expiry.sh api.example.com 30

# Мониторинг ошибок certificate pinning
0 * * * * /path/to/scripts/monitor-certificate-pinning.sh
```

## 📚 Подробная документация

Для детальных инструкций см. [docs/PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md](../PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md)

## ✅ Чеклист

- [ ] Получены SHA-256 fingerprints для всех доменов
- [ ] Создан и настроен `config/certificate-pins.json`
- [ ] Установлены SSL сертификаты (Let's Encrypt)
- [ ] Настроен nginx для HTTPS
- [ ] HTTPS redirect работает
- [ ] HSTS заголовки установлены
- [ ] Настроен мониторинг сертификатов
- [ ] Настроен мониторинг ошибок certificate pinning
- [ ] Протестировано на staging окружении

## 🔍 Проверка

```bash
# Проверка HTTPS redirect
curl -I http://api.example.com/api/v1/health

# Проверка HSTS
curl -I https://api.example.com/api/v1/health | grep Strict-Transport-Security

# Проверка SSL через SSL Labs
# https://www.ssllabs.com/ssltest/analyze.html?d=api.example.com
```

## ⚠️ Важно

1. Всегда используйте минимум 2 pin для возможности ротации сертификатов
2. Тестируйте в staging перед production
3. Мониторьте ошибки certificate pinning - они могут указывать на MITM атаки
4. Регулярно обновляйте сертификаты (Let's Encrypt автоматически обновляет)

---

**Нужна помощь?** См. [docs/PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md](../PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md)
