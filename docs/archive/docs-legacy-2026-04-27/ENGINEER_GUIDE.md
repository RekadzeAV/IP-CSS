# Руководство инженера по развертыванию и первичной настройке IP-CSS

**Версия проекта:** Alfa-0.0.1
**Последнее обновление:** 28 January 2026

---

## Оглавление

1. [Введение](#введение)
2. [Требования к системе](#требования-к-системе)
3. [Подготовка окружения](#подготовка-окружения)
4. [Развертывание системы](#развертывание-системы)
5. [Первичная настройка](#первичная-настройка)
6. [Проверка работоспособности](#проверка-работоспособности)
7. [Настройка безопасности](#настройка-безопасности)
8. [Мониторинг и логирование](#мониторинг-и-логирование)
9. [Устранение неполадок](#устранение-неполадок)
10. [Дополнительные ресурсы](#дополнительные-ресурсы)

---

## Введение

Данное руководство предназначено для инженеров-разработчиков и DevOps специалистов, ответственных за развертывание и первичную настройку системы видеонаблюдения IP-CSS в production или staging окружении.

### Целевая аудитория

- DevOps инженеры
- Системные администраторы
- Инженеры по развертыванию
- Разработчики, выполняющие первичную настройку

### Что покрывает это руководство

- Полная установка и настройка всех компонентов системы
- Развертывание на различных платформах (Docker, Linux, Windows, NAS)
- Первичная конфигурация системы
- Настройка безопасности и производительности
- Мониторинг и диагностика

---

## Требования к системе

### Минимальные требования для сервера

#### Для небольших установок (до 8 камер):
- **CPU:** 4 ядра (Intel i5 / AMD Ryzen 5 или эквивалент)
- **RAM:** 8 ГБ
- **Диск:** 100 ГБ SSD + место под записи (рекомендуется 500 ГБ+)
- **Сеть:** 1 Гбит/с

#### Для средних установок (8-32 камеры):
- **CPU:** 8 ядер (Intel i7 / AMD Ryzen 7 или эквивалент)
- **RAM:** 16 ГБ
- **Диск:** 500 ГБ SSD + RAID массив для записей (2+ ТБ)
- **Сеть:** 1 Гбит/с (рекомендуется 10 Гбит/с)

#### Для крупных установок (32+ камеры):
- **CPU:** 16+ ядер (Intel Xeon / AMD EPYC или эквивалент)
- **RAM:** 32+ ГБ
- **Диск:** 1 ТБ SSD + RAID массив для записей (10+ ТБ)
- **Сеть:** 10 Гбит/с
- **GPU:** Опционально для аппаратного ускорения видео (NVIDIA GPU с NVENC)

### Требования к программному обеспечению

#### Обязательные компоненты:
- **Docker:** 20.10+ (для Docker развертывания)
- **Docker Compose:** 2.0+ (для Docker развертывания)
- **Java JDK:** 17+ (для локальной сборки)
- **Node.js:** 20+ LTS (для веб-интерфейса)
- **PostgreSQL:** 13+ (рекомендуется для production) или SQLite (для небольших установок)
- **Redis:** 6.0+ (для rate limiting и кэширования)

#### Опциональные компоненты:
- **FFmpeg:** 5.0+ (для обработки видео)
- **CMake:** 3.15+ (для сборки нативных библиотек)
- **Nginx:** 1.20+ (для reverse proxy и HTTPS)

---

## Подготовка окружения

### 1. Установка Docker и Docker Compose

#### Linux (Ubuntu/Debian):
```bash
# Обновление системы
sudo apt update && sudo apt upgrade -y

# Установка Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Добавление пользователя в группу docker
sudo usermod -aG docker $USER

# Установка Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Проверка установки
docker --version
docker-compose --version
```

#### Windows:
1. Скачайте Docker Desktop: https://www.docker.com/products/docker-desktop
2. Установите и перезапустите компьютер
3. Проверьте установку: `docker --version`

#### macOS:
```bash
# Через Homebrew
brew install --cask docker

# Или скачайте Docker Desktop: https://www.docker.com/products/docker-desktop
```

### 2. Установка PostgreSQL (рекомендуется для production)

#### Linux:
```bash
# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib -y

# Запуск и настройка
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Создание базы данных
sudo -u postgres psql
CREATE DATABASE ipcss;
CREATE USER ipcss_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE ipcss TO ipcss_user;
\q
```

#### Docker:
```bash
docker run -d \
  --name postgres-ipcss \
  -e POSTGRES_DB=ipcss \
  -e POSTGRES_USER=ipcss_user \
  -e POSTGRES_PASSWORD=secure_password \
  -v postgres-data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:15
```

### 3. Установка Redis

#### Linux:
```bash
# Ubuntu/Debian
sudo apt install redis-server -y

# Запуск и настройка
sudo systemctl start redis-server
sudo systemctl enable redis-server

# Проверка
redis-cli ping
```

#### Docker:
```bash
docker run -d \
  --name redis-ipcss \
  -p 6379:6379 \
  redis:7-alpine
```

### 4. Установка Nginx (опционально, для reverse proxy)

```bash
# Ubuntu/Debian
sudo apt install nginx -y

# Запуск
sudo systemctl start nginx
sudo systemctl enable nginx
```

---

## Развертывание системы

### Вариант 1: Развертывание через Docker Compose (рекомендуется)

#### Шаг 1: Клонирование репозитория

```bash
git clone https://github.com/company/ip-css.git
cd ip-css
```

#### Шаг 2: Настройка переменных окружения

Создайте файл `.env` на основе `.env.example`:

```bash
cp .env.example .env
nano .env  # или используйте любой редактор
```

Минимальная конфигурация `.env`:

```env
# База данных
DATABASE_URL=jdbc:postgresql://postgres:5432/ipcss
DATABASE_USER=ipcss_user
DATABASE_PASSWORD=secure_password

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Сервер
SERVER_HOST=0.0.0.0
SERVER_PORT=8080
HTTPS_ENABLED=false

# JWT
JWT_SECRET=your-secret-key-change-in-production
JWT_ISSUER=ip-css
JWT_AUDIENCE=ip-css-users

# Хранилище
STORAGE_PATH=/app/recordings
STORAGE_MAX_SIZE_GB=1000
STORAGE_RETENTION_DAYS=30

# Безопасность
CORS_ALLOWED_ORIGINS=http://localhost:3000
CSRF_ENABLED=true
RATE_LIMIT_ENABLED=true
```

#### Шаг 3: Запуск системы

```bash
# Запуск всех сервисов
docker-compose up -d

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f
```

#### Шаг 4: Проверка доступности

```bash
# Проверка API
curl http://localhost:8080/api/v1/health

# Проверка веб-интерфейса
curl http://localhost:3000
```

### Вариант 2: Развертывание на Linux сервере

#### Шаг 1: Установка зависимостей

```bash
# Java JDK 17
sudo apt install openjdk-17-jdk -y

# Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# PostgreSQL и Redis (см. раздел "Подготовка окружения")
```

#### Шаг 2: Сборка проекта

```bash
# Клонирование репозитория
git clone https://github.com/company/ip-css.git
cd ip-css

# Сборка Kotlin модулей
./gradlew build

# Сборка веб-интерфейса
cd server/web
npm install
npm run build
cd ../..
```

#### Шаг 3: Настройка systemd сервиса

Создайте файл `/etc/systemd/system/ip-css.service`:

```ini
[Unit]
Description=IP-CSS Surveillance System
After=network.target postgresql.service redis.service

[Service]
Type=simple
User=ipcss
WorkingDirectory=/opt/ip-css
Environment="DATABASE_URL=jdbc:postgresql://localhost:5432/ipcss"
Environment="DATABASE_USER=ipcss_user"
Environment="DATABASE_PASSWORD=secure_password"
Environment="REDIS_HOST=localhost"
Environment="REDIS_PORT=6379"
ExecStart=/usr/bin/java -jar /opt/ip-css/server/api/build/libs/server-api.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Запуск сервиса:

```bash
sudo systemctl daemon-reload
sudo systemctl enable ip-css
sudo systemctl start ip-css
sudo systemctl status ip-css
```

### Вариант 3: Развертывание на NAS устройствах

> **📚 Подробнее:** См. [NAS_PLATFORMS_ANALYSIS.md](NAS_PLATFORMS_ANALYSIS.md) для детальной информации о NAS платформах.

#### Synology DSM:
1. Скачайте `.spk` пакет для вашей архитектуры
2. Откройте **Панель управления** → **Пакеты** → **Установка вручную**
3. Выберите `.spk` файл и следуйте инструкциям

#### QNAP QTS:
1. Скачайте `.qpkg` пакет для вашей архитектуры
2. Откройте **App Center** → **Установка из файла**
3. Выберите `.qpkg` файл и установите

---

## Первичная настройка

### 1. Первый вход в систему

1. Откройте веб-интерфейс: `http://<IP-адрес>:8080` или `http://localhost:8080`
2. Войдите с учетными данными по умолчанию:
   - **Логин:** `admin`
   - **Пароль:** `admin`
3. **⚠️ ВАЖНО:** Сразу смените пароль администратора!

### 2. Настройка базовых параметров

#### Настройка сети

Перейдите в **Настройки** → **Сеть**:

- **Порт HTTP:** 8080 (по умолчанию)
- **Порт HTTPS:** 8443 (по умолчанию)
- **Внешний URL:** для доступа извне (например, `https://surveillance.company.com`)
- **SSL сертификат:** для HTTPS (рекомендуется)

#### Настройка хранилища

Перейдите в **Настройки** → **Хранилище**:

- **Путь к записям:** `/var/lib/ip-css/recordings` (Linux) или `C:\ProgramData\IP-CSS\recordings` (Windows)
- **Максимальный размер:** укажите лимит или оставьте без ограничений
- **Срок хранения:** количество дней (по умолчанию: 30)
- **Автоматическая очистка:** включить/выключить

#### Настройка базы данных

Для production рекомендуется PostgreSQL:

```yaml
# config.yaml или через переменные окружения
database:
  type: postgresql
  host: localhost
  port: 5432
  name: ipcss
  username: ipcss_user
  password: secure_password
  pool_size: 10
  max_lifetime: 1800000
```

### 3. Настройка первой камеры

1. Перейдите в **Камеры** → **Добавить камеру**
2. Выберите способ:
   - **Автообнаружение** (ONVIF WS-Discovery) - система найдет камеры в сети
   - **Вручную** - введите параметры камеры
3. Заполните параметры:
   - **Название:** отображаемое имя камеры
   - **RTSP URL:** `rtsp://username:password@ip:port/stream`
   - **ONVIF URL:** `http://ip:port/onvif/device_service` (опционально)
   - **Учетные данные:** логин и пароль
4. Нажмите **Тест подключения** для проверки
5. Нажмите **Сохранить**

### 4. Настройка записи

Для каждой камеры настройте режим записи:

- **Непрерывная запись:** запись 24/7
- **Запись по событиям:** только при обнаружении движения/объектов
- **Запись по расписанию:** в указанное время

### 5. Настройка уведомлений

Перейдите в **Настройки** → **Уведомления**:

- **Email:** SMTP настройки
- **SMS:** интеграция с SMS-шлюзом
- **Push:** настройка push-уведомлений
- **Telegram:** токен бота
- **Webhook:** URL для отправки событий

---

## Проверка работоспособности

### 1. Проверка API

```bash
# Health check
curl http://localhost:8080/api/v1/health

# Ожидаемый ответ:
# {"status":"ok","timestamp":"2026-01-28T12:00:00Z"}
```

### 2. Проверка базы данных

```bash
# Подключение к PostgreSQL
psql -h localhost -U ipcss_user -d ipcss

# Проверка таблиц
\dt

# Проверка данных
SELECT COUNT(*) FROM cameras;
```

### 3. Проверка Redis

```bash
# Подключение к Redis
redis-cli

# Проверка
PING
# Ожидаемый ответ: PONG
```

### 4. Проверка веб-интерфейса

1. Откройте браузер: `http://localhost:3000` или `http://localhost:8080`
2. Войдите в систему
3. Проверьте доступность всех разделов:
   - Dashboard
   - Cameras
   - Events
   - Recordings
   - Settings

### 5. Проверка видеопотоков

1. Откройте камеру в веб-интерфейсе
2. Проверьте, что видеопоток воспроизводится
3. Проверьте запись (если включена)

---

## Настройка безопасности

### 1. Настройка HTTPS

#### Получение SSL сертификата (Let's Encrypt)

```bash
# Установка Certbot
sudo apt install certbot -y

# Получение сертификата
sudo certbot certonly --standalone -d surveillance.company.com

# Сертификаты будут в:
# /etc/letsencrypt/live/surveillance.company.com/fullchain.pem
# /etc/letsencrypt/live/surveillance.company.com/privkey.pem
```

#### Настройка в системе

Обновите `.env`:

```env
HTTPS_ENABLED=true
SSL_CERT_PATH=/etc/letsencrypt/live/surveillance.company.com/fullchain.pem
SSL_KEY_PATH=/etc/letsencrypt/live/surveillance.company.com/privkey.pem
```

### 2. Настройка Nginx как reverse proxy

Создайте файл `/etc/nginx/sites-available/ip-css`:

```nginx
server {
    listen 80;
    server_name surveillance.company.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name surveillance.company.com;

    ssl_certificate /etc/letsencrypt/live/surveillance.company.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/surveillance.company.com/privkey.pem;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # API proxy
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Web interface
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Активация:

```bash
sudo ln -s /etc/nginx/sites-available/ip-css /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 3. Настройка файрвола

#### Linux (iptables/ufw):

```bash
# Разрешить HTTP и HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Разрешить SSH (если используется)
sudo ufw allow 22/tcp

# Включить файрвол
sudo ufw enable
```

### 4. Настройка JWT секрета

**⚠️ ВАЖНО:** Используйте надежный секретный ключ для JWT!

```bash
# Генерация случайного секрета
openssl rand -base64 32

# Обновите .env
JWT_SECRET=<сгенерированный_секрет>
```

### 5. Настройка rate limiting

Система использует Redis для rate limiting. Убедитесь, что Redis запущен и доступен.

Настройки в `.env`:

```env
RATE_LIMIT_ENABLED=true
RATE_LIMIT_REQUESTS_PER_MINUTE=60
RATE_LIMIT_BURST=10
```

---

## Мониторинг и логирование

### 1. Просмотр логов

#### Docker:

```bash
# Все логи
docker-compose logs -f

# Логи конкретного сервиса
docker-compose logs -f api
docker-compose logs -f web
```

#### Systemd:

```bash
# Логи сервиса
sudo journalctl -u ip-css -f

# Логи за последний час
sudo journalctl -u ip-css --since "1 hour ago"
```

### 2. Мониторинг ресурсов

```bash
# Использование CPU и памяти
docker stats

# Использование диска
df -h

# Использование сети
iftop  # или nethogs
```

### 3. Настройка мониторинга (опционально)

#### Prometheus + Grafana:

```yaml
# docker-compose.monitoring.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
```

---

## Устранение неполадок

### Проблема: Сервис не запускается

**Диагностика:**
```bash
# Проверка логов
docker-compose logs api

# Проверка статуса контейнеров
docker-compose ps

# Проверка подключения к базе данных
docker-compose exec api ping postgres
```

**Решения:**
- Убедитесь, что все зависимости (PostgreSQL, Redis) запущены
- Проверьте переменные окружения в `.env`
- Проверьте доступность портов

### Проблема: База данных недоступна

**Диагностика:**
```bash
# Проверка подключения
psql -h localhost -U ipcss_user -d ipcss

# Проверка статуса PostgreSQL
sudo systemctl status postgresql
```

**Решения:**
- Убедитесь, что PostgreSQL запущен
- Проверьте учетные данные в `.env`
- Проверьте настройки доступа в `pg_hba.conf`

### Проблема: Видеопотоки не воспроизводятся

**Диагностика:**
- Проверьте RTSP URL камеры
- Проверьте учетные данные камеры
- Проверьте сетевую доступность камеры

**Решения:**
- Используйте **Тест подключения** в интерфейсе
- Проверьте файрвол на камере
- Убедитесь, что порт RTSP (обычно 554) открыт

### Проблема: Высокое использование ресурсов

**Решения:**
- Уменьшите количество одновременных потоков
- Снизьте качество записи
- Используйте аппаратное ускорение (если доступно)
- Рассмотрите масштабирование системы

---

## Дополнительные ресурсы

- **[ADMINISTRATOR_GUIDE.md](ADMINISTRATOR_GUIDE.md)** - Руководство для администраторов
- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Общее руководство по развертыванию
- **[CONFIGURATION.md](CONFIGURATION.md)** - Детальная конфигурация системы
- **[API.md](API.md)** - REST API документация
- **[SECURITY_BEST_PRACTICES.md](SECURITY_BEST_PRACTICES.md)** - Лучшие практики безопасности
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Расширенное руководство по устранению неполадок
- **[installation/INSTALL_INSTRUCTIONS.md](installation/INSTALL_INSTRUCTIONS.md)** - Установка компонентов для сборки
- **[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)** - Статус реализации всех компонентов

---

**Версия документа:** Alfa-0.0.1
**Последнее обновление:** 28 January 2026
