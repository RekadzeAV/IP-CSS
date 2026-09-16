# Переменные окружения

**Версия проекта:** Alfa-0.0.1
**Последнее обновление:** 24 April 2026

> **📚 Связанные документы:**
> - [CONFIGURATION.md](CONFIGURATION.md) - Руководство по настройке
> - [DEPLOYMENT_GUIDE.md](../archive/docs/deployment/DEPLOYMENT_GUIDE.md) - Руководство по развертыванию
> - [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Устранение неполадок

---

## 📋 Содержание

1. [Обзор](#обзор)
2. [Обязательные переменные](#обязательные-переменные)
3. [Опциональные переменные](#опциональные-переменные)
4. [Переменные для разработки](#переменные-для-разработки)
5. [Примеры конфигурации](#примеры-конфигурации)
6. [Безопасность](#безопасность)

---

## Обзор

Система IP-CSS использует переменные окружения для конфигурации, которая зависит от окружения развертывания. Это позволяет:

- Хранить секреты отдельно от кода
- Легко менять конфигурацию для разных окружений
- Использовать один и тот же код для разных сред

**Приоритет конфигурации:**
1. Переменные окружения (наивысший приоритет)
2. Конфигурационные файлы
3. Значения по умолчанию

---

## Обязательные переменные

### JWT_SECRET

**Описание:** Секретный ключ для подписи JWT токенов

**Тип:** String
**Обязательно:** Да
**Пример:**
```bash
JWT_SECRET=your-super-secret-key-min-32-chars-long
```

**Рекомендации:**
- Минимум 32 символа
- Используйте случайную строку
- Не используйте один и тот же ключ для разных окружений
- Храните в секретном хранилище

**Генерация:**
```bash
# Linux/macOS
openssl rand -base64 32

# Или используйте онлайн генератор
```

---

### ADMIN_PASSWORD_HASH

**Описание:** Хеш пароля администратора (BCrypt)

**Тип:** String
**Обязательно:** Да (для первого запуска)
**Пример:**
```bash
ADMIN_PASSWORD_HASH=$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

**Генерация:**
```bash
# Используйте утилиту для генерации BCrypt хеша
# Или через API при первом запуске
```

**Безопасность:**
- Никогда не храните в Git
- Используйте сильный пароль
- Регулярно меняйте пароль

---

### DB_PASSWORD

**Описание:** Пароль для базы данных PostgreSQL

**Тип:** String
**Обязательно:** Да (если используется PostgreSQL)
**Пример:**
```bash
DB_PASSWORD=secure-database-password-123
```

**Рекомендации:**
- Минимум 12 символов
- Используйте буквы, цифры и специальные символы
- Не используйте простые пароли

---

### REDIS_PASSWORD

**Описание:** Пароль для Redis

**Тип:** String
**Обязательно:** Да (если используется Redis)
**Пример:**
```bash
REDIS_PASSWORD=secure-redis-password-123
```

---

## Опциональные переменные

### Сервер

#### HTTP_PORT

**Описание:** Порт HTTP сервера

**Тип:** Integer
**По умолчанию:** `8080`
**Пример:**
```bash
HTTP_PORT=8080
```

**Примечание:** `SERVER_PORT` в старых конфигурациях считается устаревшим именем; используйте `HTTP_PORT`.

---

#### SERVER_HOST / HOST

**Описание:** Хост для привязки сервера

**Тип:** String
**По умолчанию:** `0.0.0.0`
**Пример:**
```bash
HOST=0.0.0.0
```

---

#### HTTP_PORT

**Описание:** Порт, на котором сервер принимает HTTP-запросы (при использовании редиректа — обычно 80).

**Тип:** Integer
**По умолчанию:** `8080`
**Пример:**
```bash
HTTP_PORT=80
```

---

#### HTTPS_PORT

**Описание:** Целевой порт HTTPS для редиректа HTTP → HTTPS (куда перенаправлять клиента при опции «только HTTPS»). Обычно 443.

**Тип:** Integer
**По умолчанию:** `8443`
**Пример:**
```bash
HTTPS_PORT=443
```

---

#### FORCE_HTTPS (только HTTPS)

**Описание:** Включить принудительный редирект HTTP → HTTPS. Все запросы по HTTP перенаправляются на HTTPS с портом из `HTTPS_PORT`. Рекомендуется в production за nginx (порт 80 → 443).

**Тип:** Boolean
**По умолчанию:** `false` (или `true`, если задан `NODE_ENV=production`)
**Пример:**
```bash
FORCE_HTTPS=true
```

---

#### LOG_LEVEL

**Описание:** Уровень логирования

**Тип:** String
**По умолчанию:** `INFO`
**Возможные значения:** `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`
**Пример:**
```bash
LOG_LEVEL=DEBUG
```

---

### База данных

#### DATABASE_URL

**Описание:** URL подключения к базе данных

**Тип:** String
**По умолчанию:** `jdbc:sqlite:data/database/surveillance.db`
**Примеры:**

SQLite:
```bash
DATABASE_URL=jdbc:sqlite:data/database/surveillance.db
```

PostgreSQL:
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/surveillance?user=surveillance&password=password
```

---

#### DATABASE_MAX_POOL_SIZE

**Описание:** Размер пула соединений с базой данных

**Тип:** Integer
**По умолчанию:** `10`
**Пример:**
```bash
DATABASE_MAX_POOL_SIZE=20
```

---

### Redis

#### REDIS_HOST

**Описание:** Хост Redis сервера

**Тип:** String
**По умолчанию:** `localhost`
**Пример:**
```bash
REDIS_HOST=redis
```

---

#### REDIS_PORT

**Описание:** Порт Redis сервера

**Тип:** Integer
**По умолчанию:** `6379`
**Пример:**
```bash
REDIS_PORT=6379
```

---

#### REDIS_DATABASE

**Описание:** Номер базы данных Redis

**Тип:** Integer
**По умолчанию:** `0`
**Пример:**
```bash
REDIS_DATABASE=0
```

---

### CORS

#### CORS_ALLOWED_ORIGINS

**Описание:** Разрешенные источники для CORS (через запятую)

**Тип:** String
**По умолчанию:** `http://localhost:3000,http://localhost:8080`
**Пример:**
```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://example.com
```

---

### Лицензирование

#### LICENSE_KEY

**Описание:** Ключ лицензии

**Тип:** String
**Обязательно:** Нет
**Пример:**
```bash
LICENSE_KEY=your-license-key-here
```

---

### Хранилище

#### STORAGE_PATH

**Описание:** Путь к директории для хранения записей

**Тип:** String
**По умолчанию:** `data/recordings`
**Пример:**
```bash
STORAGE_PATH=/mnt/storage/recordings
```

---

#### STORAGE_MAX_SIZE_GB

**Описание:** Максимальный размер хранилища в GB

**Тип:** Integer
**По умолчанию:** `100`
**Пример:**
```bash
STORAGE_MAX_SIZE_GB=500
```

---

#### STORAGE_RETENTION_DAYS

**Описание:** Количество дней хранения записей

**Тип:** Integer
**По умолчанию:** `30`
**Пример:**
```bash
STORAGE_RETENTION_DAYS=60
```

---

### Видео

#### VIDEO_CODEC

**Описание:** Кодек для записи видео

**Тип:** String
**По умолчанию:** `h264`
**Возможные значения:** `h264`, `h265`, `mjpeg`
**Пример:**
```bash
VIDEO_CODEC=h264
```

---

#### VIDEO_QUALITY

**Описание:** Качество видео

**Тип:** String
**По умолчанию:** `high`
**Возможные значения:** `low`, `medium`, `high`
**Пример:**
```bash
VIDEO_QUALITY=medium
```

---

#### VIDEO_FPS

**Описание:** Частота кадров

**Тип:** Integer
**По умолчанию:** `30`
**Пример:**
```bash
VIDEO_FPS=15
```

---

### AI-аналитика

#### AI_ENABLED

**Описание:** Включить AI-аналитику

**Тип:** Boolean
**По умолчанию:** `false`
**Пример:**
```bash
AI_ENABLED=true
```

---

#### AI_MODEL_PATH

**Описание:** Путь к модели AI

**Тип:** String
**По умолчанию:** `data/models/yolov8n.onnx`
**Пример:**
```bash
AI_MODEL_PATH=/opt/models/yolov8n.onnx
```

---

#### AI_USE_GPU

**Описание:** Использовать GPU для AI-аналитики

**Тип:** Boolean
**По умолчанию:** `false`
**Пример:**
```bash
AI_USE_GPU=true
```

---

### Уведомления

#### NOTIFICATION_EMAIL_ENABLED

**Описание:** Включить email уведомления

**Тип:** Boolean
**По умолчанию:** `false`
**Пример:**
```bash
NOTIFICATION_EMAIL_ENABLED=true
```

---

#### SMTP_HOST

**Описание:** SMTP сервер для email

**Тип:** String
**Пример:**
```bash
SMTP_HOST=smtp.gmail.com
```

---

#### SMTP_PORT

**Описание:** Порт SMTP сервера

**Тип:** Integer
**По умолчанию:** `587`
**Пример:**
```bash
SMTP_PORT=587
```

---

#### SMTP_USERNAME

**Описание:** Имя пользователя SMTP

**Тип:** String
**Пример:**
```bash
SMTP_USERNAME=your-email@gmail.com
```

---

#### SMTP_PASSWORD

**Описание:** Пароль SMTP

**Тип:** String
**Пример:**
```bash
SMTP_PASSWORD=your-email-password
```

---

#### SMTP_FROM_ADDRESS

**Описание:** Адрес отправителя (From) для email-уведомлений (B.1).

**Тип:** String
**По умолчанию:** `noreply@localhost`
**Пример:**
```bash
SMTP_FROM_ADDRESS=noreply@example.com
```

---

#### SMTP_FROM_NAME

**Описание:** Имя отправителя (отображаемое имя).

**Тип:** String
**Пример:**
```bash
SMTP_FROM_NAME=IP Camera System
```

---

#### SMTP_USE_TLS

**Описание:** Использовать STARTTLS для SMTP.

**Тип:** Boolean
**По умолчанию:** `true`
**Пример:**
```bash
SMTP_USE_TLS=true
```

---

#### SMTP_ENABLED

**Описание:** Включить отправку уведомлений по email. Если задан `SMTP_HOST`, отправка включается по умолчанию.

**Тип:** Boolean
**По умолчанию:** `true` (при наличии SMTP_HOST)
**Пример:**
```bash
SMTP_ENABLED=true
```

---

### Telegram уведомления

#### TELEGRAM_BOT_TOKEN

**Описание:** Токен Telegram бота

**Тип:** String
**Пример:**
```bash
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
```

---

#### TELEGRAM_CHAT_ID

**Описание:** ID чата для уведомлений

**Тип:** String
**Пример:**
```bash
TELEGRAM_CHAT_ID=123456789
```

---

#### TELEGRAM_CHAT_IDS

**Описание:** Список ID чатов через запятую для отправки в несколько чатов.

**Тип:** String
**Пример:**
```bash
TELEGRAM_CHAT_IDS=123456789,-1001234567890
```

---

#### TELEGRAM_ENABLED

**Описание:** Включает/выключает отправку уведомлений в Telegram при наличии токена и chat ID.

**Тип:** Boolean
**По умолчанию:** `true`
**Пример:**
```bash
TELEGRAM_ENABLED=true
```

---

### ONVIF Events (подписки на события камер)

Конфигурация авто-подписок на ONVIF Event Service при старте API и при добавлении/обновлении камер. Читается сервером из `OnvifEventsConfig.fromEnvironment()`.

#### ONVIF_EVENTS_ENABLED

**Описание:** Включить авто-подписку на события ONVIF при старте API и при добавлении камеры.

**Тип:** Boolean  
**По умолчанию:** `true`  
**Пример:**
```bash
ONVIF_EVENTS_ENABLED=true
```

**Примечание:** При `false` подписки при старте и при add/update не создаются; отписка при delete всё равно выполняется.

---

#### ONVIF_SUBSCRIPTION_TIME_SEC

**Описание:** Время жизни подписки в секундах (используется для CreatePullPointSubscription и для продления подписки Renew).

**Тип:** Integer  
**По умолчанию:** `3600` (1 час)  
**Минимум:** `60`  
**Пример:**
```bash
ONVIF_SUBSCRIPTION_TIME_SEC=3600
```

---

#### ONVIF_PULL_INTERVAL_MS

**Описание:** Интервал опроса PullPoint в миллисекундах (как часто сервер запрашивает новые события у камеры).

**Тип:** Long  
**По умолчанию:** `5000` (5 секунд)  
**Минимум:** `1000`  
**Пример:**
```bash
ONVIF_PULL_INTERVAL_MS=5000
```

---

#### ONVIF_PULL_TIMEOUT_MS

**Описание:** Таймаут одного запроса PullMessages в миллисекундах (ожидание ответа от камеры).

**Тип:** Integer  
**По умолчанию:** `500`  
**Допустимый диапазон:** 100–30000  
**Пример:**
```bash
ONVIF_PULL_TIMEOUT_MS=500
```

---

#### ONVIF_USE_PULL_POINT

**Описание:** Использовать PullPoint по умолчанию для подписок (рекомендуется для надёжности; альтернатива — Basic Notification push).

**Тип:** Boolean  
**По умолчанию:** `true`  
**Пример:**
```bash
ONVIF_USE_PULL_POINT=true
```

---

### Enterprise: LDAP/AD, 2FA, аудит, шифрование (Фаза 4)

Переменные для расширенной аутентификации и безопасности (см. [TODO.md](../archive/docs-duplicates-2026-08-08/TODO.md)).

| Переменная | Описание |
|------------|----------|
| `LDAP_ENABLED` | Включить LDAP/AD: `true` / `false` |
| `LDAP_URL` | URL LDAP, напр. `ldap://dc.example.com:389` |
| `LDAP_BIND_DN`, `LDAP_BIND_PASSWORD` | Учётка для поиска (опционально) |
| `LDAP_USER_BASE_DN` | Base DN пользователей |
| `LDAP_USER_SEARCH_FILTER` | Фильтр, `{0}` = логин, напр. `(uid={0})` |
| `LDAP_ROLE_MAPPING` | Группы → роли: `OU=Admins:ADMIN,...` |
| `TOTP_ISSUER` | Название в приложении-аутентификаторе |
| `TWO_FACTOR_REQUIRED` | Обязательная 2FA для всех |
| `AUDIT_PERSIST_ENABLED` | Сохранять события аудита |
| `DATA_ENCRYPTION_KEY` | Ключ шифрования (32/64 hex символов) |
| `OAUTH2_FRONTEND_SUCCESS_URL` | URL фронтенда после успешного OAuth2 входа |
| `OAUTH2_FRONTEND_ERROR_URL` | URL фронтенда при ошибке OAuth2 |
| `OAUTH2_STATE_TTL_SEC` | TTL state в Redis (60–900 сек) |
| `OAUTH2_*`, `KERBEROS_*` | SSO и Kerberos (см. EnterpriseAuthConfig) |

### Enterprise: облачная синхронизация и хранилище (4.1)

| Переменная | Описание |
|------------|----------|
| `CLOUD_SYNC_ENABLED` | Включить синхронизацию между устройствами |
| `CLOUD_SYNC_CONFLICT_STRATEGY` | LAST_WRITE_WINS \| MERGE \| MANUAL |
| `CLOUD_SYNC_INTERVAL_SEC` | Интервал фоновой синхронизации (0 = выкл.) |
| `CLOUD_STORAGE_ENABLED` | Включить облачное/локальное хранилище записей |
| `CLOUD_STORAGE_PROVIDER` | s3 \| minio \| gcs \| azure (пока используется файловый провайдер) |
| `CLOUD_STORAGE_LOCAL_PATH` | Локальная папка при файловом провайдере |
| `BACKUP_SCHEDULER_INTERVAL_MIN` | Интервал автобэка в минутах (0 = выкл.) |

#### 4.2 Кластеризация и репликация

| Переменная | Описание |
|------------|----------|
| `CLUSTER_ENABLED` | Включить режим кластера (true/false) |
| `NODE_ID` | Уникальный идентификатор узла (для LB и координации) |
| `NODE_URL` | Публичный URL узла (для discovery) |
| `CLUSTER_HEARTBEAT_SEC` | Интервал heartbeat в Redis (секунды, 5–120) |
| `DATABASE_READ_REPLICA_URL` | JDBC URL read replica PostgreSQL (опционально) |
| `DATABASE_READ_REPLICA_POOL_SIZE` | Размер пула соединений к реплике (по умолчанию 5) |

---

### Временная зона

#### TZ

**Описание:** Временная зона

**Тип:** String
**По умолчанию:** `UTC`
**Пример:**
```bash
TZ=Europe/Moscow
```

---

## Переменные для разработки

### DEBUG

**Описание:** Включить режим отладки

**Тип:** Boolean
**По умолчанию:** `false`
**Пример:**
```bash
DEBUG=true
```

---

### DEV_MODE

**Описание:** Режим разработки

**Тип:** Boolean
**По умолчанию:** `false`
**Пример:**
```bash
DEV_MODE=true
```

---

### API_DOCS_ENABLED

**Описание:** Включить Swagger документацию

**Тип:** Boolean
**По умолчанию:** `false`
**Пример:**
```bash
API_DOCS_ENABLED=true
```

---

## Примеры конфигурации

### Минимальная конфигурация (.env)

```bash
# Обязательные переменные
JWT_SECRET=your-super-secret-key-min-32-chars-long
ADMIN_PASSWORD_HASH=$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
DB_PASSWORD=secure-database-password-123
REDIS_PASSWORD=secure-redis-password-123

# Опциональные
LOG_LEVEL=INFO
HTTP_PORT=8080
TZ=Europe/Moscow
```

---

### Production конфигурация (.env.production)

```bash
# Обязательные
JWT_SECRET=<generated-secret>
ADMIN_PASSWORD_HASH=<generated-hash>
DB_PASSWORD=<secure-password>
REDIS_PASSWORD=<secure-password>

# Сервер
HTTP_PORT=8080
SERVER_HOST=0.0.0.0
LOG_LEVEL=WARN

# База данных
DATABASE_URL=jdbc:postgresql://db:5432/surveillance
DATABASE_MAX_POOL_SIZE=20

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<secure-password>

# CORS
CORS_ALLOWED_ORIGINS=https://your-domain.com

# ONVIF Events
ONVIF_EVENTS_ENABLED=true
ONVIF_SUBSCRIPTION_TIME_SEC=3600
ONVIF_PULL_INTERVAL_MS=5000
ONVIF_PULL_TIMEOUT_MS=500
ONVIF_USE_PULL_POINT=true

# Хранилище
STORAGE_PATH=/mnt/storage/recordings
STORAGE_MAX_SIZE_GB=1000
STORAGE_RETENTION_DAYS=90

# Видео
VIDEO_CODEC=h264
VIDEO_QUALITY=high
VIDEO_FPS=30

# AI-аналитика
AI_ENABLED=true
AI_USE_GPU=true
AI_MODEL_PATH=/opt/models/yolov8n.onnx

# Уведомления
NOTIFICATION_EMAIL_ENABLED=true
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=notifications@your-domain.com
SMTP_PASSWORD=<email-password>

# Временная зона
TZ=Europe/Moscow
```

---

### Development конфигурация (.env.development)

```bash
# Обязательные
JWT_SECRET=dev-secret-key-not-for-production
ADMIN_PASSWORD_HASH=$2a$10$dev-hash-here
DB_PASSWORD=dev-password
REDIS_PASSWORD=dev-password

# Режим разработки
DEBUG=true
DEV_MODE=true
LOG_LEVEL=DEBUG
API_DOCS_ENABLED=true

# Сервер
HTTP_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080

# База данных
DATABASE_URL=jdbc:sqlite:data/database/surveillance.db

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# ONVIF Events (подписки на события камер)
ONVIF_EVENTS_ENABLED=true
ONVIF_PULL_INTERVAL_MS=5000

# Временная зона
TZ=UTC
```

---

## Безопасность

### Рекомендации

1. **Никогда не коммитьте .env файлы в Git**
   ```bash
   # Убедитесь, что .env в .gitignore
   echo ".env" >> .gitignore
   echo ".env.*" >> .gitignore
   ```

2. **Используйте .env.example как шаблон**
   ```bash
   # Создайте .env.example с примерами (без реальных значений)
   cp .env.example .env
   # Заполните реальными значениями
   ```

3. **Используйте секретные хранилища для production**
   - Docker Secrets
   - Kubernetes Secrets
   - HashiCorp Vault
   - AWS Secrets Manager

4. **Регулярно меняйте секреты**
   - JWT_SECRET
   - Пароли баз данных
   - API ключи

5. **Используйте разные секреты для разных окружений**
   - Development
   - Staging
   - Production

6. **Ограничьте доступ к .env файлам**
   ```bash
   # Установите правильные права
   chmod 600 .env
   ```

7. **HTTPS и certificate pinning в production**
   - Включение HTTPS на сервере и генерация/добавление pins: см. [HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md](../archive/docs/guides/HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md) и [PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md](PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md).

---

## Использование в Docker

### Docker Compose

```yaml
services:
  surveillance:
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - ADMIN_PASSWORD_HASH=${ADMIN_PASSWORD_HASH}
      - DB_PASSWORD=${DB_PASSWORD}
    env_file:
      - .env
```

### Docker run

```bash
docker run --env-file .env \
  -p 8080:8080 \
  company/ip-camera-surveillance:latest
```

---

## Использование в Kubernetes

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: surveillance-config
data:
  LOG_LEVEL: "INFO"
  SERVER_PORT: "8080"
```

### Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: surveillance-secrets
type: Opaque
stringData:
  JWT_SECRET: "your-secret"
  DB_PASSWORD: "your-password"
```

---

## Видео: HLS и запись (сервер JVM)

### `HLS_HEVC_FOR_2K4K`

**Описание:** если `true`, рендции **qhd1440** и **uhd4k** в HLS транскодируются через **libx265** (HEVC). Параллельно в **адаптивный** плейлист добавляются дубли **qhd1440_h264** и **uhd4k_h264** (тот же размер кадра, **libx264**) для клиентов без HEVC. Если флаг **выключен**, уровни `qhd1440` / `uhd4k` остаются на **libx264**; отдельные строки `*_h264` по-прежнему валидны для явного выбора H.264 на 2K/4K.

**Тип:** boolean (`true` / `false`, регистр не важен)  
**По умолчанию:** не задано (= выключено)

### `RECORDING_HEVC_DISABLE`

**Описание:** если `true`, запись с камеры **не** переключается на HEVC автоматически при высоте кадра > 1080 (ручной `useH265` в API по-прежнему может включить HEVC, если поддерживается цепочкой FFmpeg).

**По умолчанию:** не задано (= авто-HEVC для >1080 разрешён)

См. также: [planning/HLS_TRANSCODE_PROFILES.md](planning/HLS_TRANSCODE_PROFILES.md), [planning/VIDEO_SURVEILLANCE_COMPLIANCE_AUDIT_AND_TZ_2026.md](planning/VIDEO_SURVEILLANCE_COMPLIANCE_AUDIT_AND_TZ_2026.md).

---

**Последнее обновление:** 24 April 2026

