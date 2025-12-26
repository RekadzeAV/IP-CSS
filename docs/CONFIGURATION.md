# Руководство по настройке и конфигурации IP-CSS

**Версия проекта:** Alfa-0.0.1  
**Последнее обновление:** Декабрь 2025

> **📚 Связанные документы:**
> - [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - Руководство по развертыванию
> - [ADMINISTRATOR_GUIDE.md](ADMINISTRATOR_GUIDE.md) - Руководство для администраторов
> - [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md) - Переменные окружения

---

## 📋 Содержание

1. [Обзор конфигурации](#обзор-конфигурации)
2. [Конфигурационные файлы](#конфигурационные-файлы)
3. [Переменные окружения](#переменные-окружения)
4. [Настройки записи](#настройки-записи)
5. [Настройки безопасности](#настройки-безопасности)
6. [Настройки уведомлений](#настройки-уведомлений)
7. [Настройки сети](#настройки-сети)
8. [Настройки производительности](#настройки-производительности)

---

## Обзор конфигурации

Система IP-CSS использует многоуровневую систему конфигурации:

1. **Переменные окружения** - для чувствительных данных и окружения
2. **Конфигурационные файлы** - для основных настроек
3. **База данных** - для пользовательских настроек
4. **API** - для динамической конфигурации

**Приоритет конфигурации (от высшего к низшему):**
1. Переменные окружения
2. Конфигурационные файлы
3. Настройки в базе данных
4. Значения по умолчанию

---

## Конфигурационные файлы

### Структура конфигурации

```
ip-css/
├── config/
│   ├── application.yml          # Основная конфигурация сервера
│   ├── application-dev.yml      # Конфигурация для разработки
│   ├── application-prod.yml     # Конфигурация для продакшена
│   └── logback.xml              # Конфигурация логирования
├── server/
│   └── api/
│       └── src/main/resources/
│           └── application.conf # Ktor конфигурация
└── server/
    └── web/
        └── .env                 # Переменные окружения для веб-интерфейса
```

### Основные конфигурационные файлы

#### 1. application.yml (Сервер API)

```yaml
server:
  port: 8080
  host: 0.0.0.0
  
database:
  type: sqlite  # sqlite, postgresql, mysql
  path: ./data/ipcss.db
  # Для PostgreSQL:
  # host: localhost
  # port: 5432
  # name: ipcss
  # username: ipcss
  # password: ${DB_PASSWORD}
  
security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 3600  # секунды
    refreshExpiration: 604800  # 7 дней
  
cors:
  allowedOrigins:
    - http://localhost:3000
    - https://yourdomain.com
  
logging:
  level: INFO
  file: ./logs/ipcss.log
  maxSize: 10MB
  maxFiles: 10
```

#### 2. application.conf (Ktor)

```hocon
ktor {
  deployment {
    port = 8080
    host = "0.0.0.0"
  }
  
  application {
    modules = [com.company.ipcamera.server.ApplicationKt.module]
  }
  
  security {
    jwt {
      secret = ${JWT_SECRET}
      issuer = "ip-css"
      audience = "ip-css-users"
      realm = "IP-CSS"
    }
  }
}
```

#### 3. .env (Веб-интерфейс)

```env
# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/api/v1/ws

# Environment
NODE_ENV=development

# Feature Flags
NEXT_PUBLIC_ENABLE_ANALYTICS=true
NEXT_PUBLIC_ENABLE_CLOUD_SYNC=false
```

---

## Переменные окружения

### Обязательные переменные

```bash
# Безопасность
JWT_SECRET=your-secret-key-here-min-32-chars
DB_PASSWORD=your-database-password

# Лицензирование
LICENSE_SERVER_URL=https://license.company.com
LICENSE_SERVER_API_KEY=your-api-key

# Облачная синхронизация (опционально)
CLOUD_SYNC_ENABLED=false
CLOUD_SYNC_URL=https://sync.company.com
CLOUD_SYNC_API_KEY=your-api-key
```

### Опциональные переменные

```bash
# База данных
DB_TYPE=sqlite  # sqlite, postgresql, mysql
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ipcss
DB_USERNAME=ipcss

# Сервер
SERVER_PORT=8080
SERVER_HOST=0.0.0.0

# Логирование
LOG_LEVEL=INFO
LOG_FILE=./logs/ipcss.log

# Хранилище
STORAGE_PATH=./data/recordings
MAX_STORAGE_SIZE=1000000000000  # 1TB в байтах

# Уведомления
EMAIL_SMTP_HOST=smtp.gmail.com
EMAIL_SMTP_PORT=587
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-email-password
```

**См. детали:** [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md)

---

## Настройки записи

### Конфигурация записи видео

```yaml
recording:
  # Качество записи по умолчанию
  defaultQuality: HIGH  # LOW, MEDIUM, HIGH, ULTRA
  
  # Формат записи
  defaultFormat: mp4  # mp4, mkv, avi
  
  # Максимальная длительность записи (секунды)
  maxDuration: 3600  # 1 час
  
  # Автоматическое удаление старых записей
  autoDelete: true
  retentionDays: 30
  
  # Путь для хранения записей
  storagePath: ./data/recordings
  
  # Максимальный размер хранилища (байты)
  maxStorageSize: 1000000000000  # 1TB
  
  # Автоматическая очистка при достижении лимита
  autoCleanup: true
  
  # Настройки кодирования
  encoding:
    codec: H.264
    bitrate: 4096  # kbps
    fps: 25
    resolution:
      width: 1920
      height: 1080
```

### Настройки через API

```http
PUT /api/v1/settings/recording
Content-Type: application/json

{
  "defaultQuality": "ULTRA",
  "retentionDays": 60,
  "maxStorageSize": 2000000000000
}
```

---

## Настройки безопасности

### Конфигурация аутентификации

```yaml
security:
  # JWT настройки
  jwt:
    secret: ${JWT_SECRET}
    expiration: 3600  # 1 час
    refreshExpiration: 604800  # 7 дней
    algorithm: HS256
  
  # Политика паролей
  passwordPolicy:
    minLength: 8
    requireUppercase: true
    requireLowercase: true
    requireNumbers: true
    requireSpecialChars: false
  
  # Сессии
  session:
    timeout: 3600  # секунды
    maxConcurrentSessions: 5
  
  # Блокировка после неудачных попыток
  lockout:
    enabled: true
    maxAttempts: 5
    lockoutDuration: 900  # 15 минут
  
  # LDAP/Active Directory (опционально)
  ldap:
    enabled: false
    url: ldap://ldap.company.com:389
    baseDN: dc=company,dc=com
    bindDN: cn=admin,dc=company,dc=com
    bindPassword: ${LDAP_PASSWORD}
  
  # SSO (опционально)
  sso:
    enabled: false
    provider: saml  # saml, oidc
    saml:
      entityId: https://ipcss.company.com
      ssoUrl: https://sso.company.com/saml/sso
      certificate: ${SAML_CERTIFICATE}
```

### Настройки HTTPS

```yaml
security:
  tls:
    enabled: true
    certificate: ./certs/server.crt
    privateKey: ./certs/server.key
    certificateChain: ./certs/chain.crt
    requireClientCert: false
```

---

## Настройки уведомлений

### Конфигурация уведомлений

```yaml
notifications:
  # Email уведомления
  email:
    enabled: false
    smtp:
      host: smtp.gmail.com
      port: 587
      username: ${EMAIL_USERNAME}
      password: ${EMAIL_PASSWORD}
      tls: true
    from: ipcss@company.com
    to: admin@company.com
  
  # SMS уведомления (опционально)
  sms:
    enabled: false
    provider: twilio  # twilio, nexmo
    apiKey: ${SMS_API_KEY}
    apiSecret: ${SMS_API_SECRET}
    from: +1234567890
  
  # Push уведомления
  push:
    enabled: true
    fcm:
      serverKey: ${FCM_SERVER_KEY}
  
  # Webhook уведомления
  webhook:
    enabled: false
    url: https://your-webhook-url.com/events
    secret: ${WEBHOOK_SECRET}
  
  # Типы событий для уведомлений
  eventTypes:
    - motion_detected
    - object_detected
    - face_detected
    - camera_offline
    - recording_failed
```

---

## Настройки сети

### Конфигурация сети

```yaml
network:
  # API сервер
  api:
    port: 8080
    host: 0.0.0.0
    allowRemoteAccess: false
  
  # WebSocket сервер
  websocket:
    port: 8081
    host: 0.0.0.0
    path: /api/v1/ws
  
  # RTSP прокси (опционально)
  rtsp:
    enabled: true
    port: 8554
    host: 0.0.0.0
  
  # CORS настройки
  cors:
    allowedOrigins:
      - http://localhost:3000
      - https://yourdomain.com
    allowedMethods:
      - GET
      - POST
      - PUT
      - DELETE
      - OPTIONS
    allowedHeaders:
      - Content-Type
      - Authorization
    allowCredentials: true
  
  # Таймауты
  timeouts:
    connect: 5000  # мс
    read: 30000    # мс
    write: 30000   # мс
```

---

## Настройки производительности

### Оптимизация производительности

```yaml
performance:
  # Обработка видео
  video:
    # Количество потоков для обработки
    processingThreads: 4
    
    # Использование аппаратного ускорения
    hardwareAcceleration: true
    
    # Кэширование кадров
    frameCache:
      enabled: true
      maxSize: 100  # количество кадров
  
  # База данных
  database:
    # Connection pool
    pool:
      minSize: 5
      maxSize: 20
      timeout: 30000  # мс
    
    # Кэширование запросов
    cache:
      enabled: true
      maxSize: 1000  # количество записей
      ttl: 300  # секунды
  
  # Память
  memory:
    # Максимальное использование памяти (MB)
    maxHeapSize: 2048
    
    # Очистка неиспользуемых ресурсов
    gc:
      enabled: true
      interval: 300  # секунды
```

---

## Примеры конфигурации

### Разработка (Development)

```yaml
# config/application-dev.yml
server:
  port: 8080

database:
  type: sqlite
  path: ./data/dev.db

logging:
  level: DEBUG

security:
  jwt:
    expiration: 86400  # 24 часа для разработки
```

### Продакшен (Production)

```yaml
# config/application-prod.yml
server:
  port: 8080
  host: 0.0.0.0

database:
  type: postgresql
  host: ${DB_HOST}
  port: 5432
  name: ${DB_NAME}
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD}

logging:
  level: INFO
  file: /var/log/ipcss/ipcss.log

security:
  jwt:
    expiration: 3600  # 1 час
    secret: ${JWT_SECRET}
```

---

## Проверка конфигурации

### Валидация конфигурации

```bash
# Проверить конфигурацию сервера
./gradlew :server:api:run --args="--check-config"

# Проверить переменные окружения
./scripts/check-env.sh
```

### Тестирование подключений

```bash
# Проверить подключение к базе данных
./scripts/test-db-connection.sh

# Проверить подключение к лицензионному серверу
./scripts/test-license-server.sh
```

---

## Резервное копирование конфигурации

### Экспорт конфигурации

```bash
# Экспортировать все настройки
curl -X GET http://localhost:8080/api/v1/settings/export \
  -H "Authorization: Bearer $TOKEN" \
  > config-backup.json
```

### Импорт конфигурации

```bash
# Импортировать настройки
curl -X POST http://localhost:8080/api/v1/settings/import \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @config-backup.json
```

---

## Устранение неполадок

### Частые проблемы

1. **Ошибка подключения к базе данных**
   - Проверьте переменные окружения DB_*
   - Убедитесь, что база данных запущена
   - Проверьте права доступа

2. **Ошибка JWT токенов**
   - Убедитесь, что JWT_SECRET установлен
   - Проверьте формат секрета (минимум 32 символа)

3. **Проблемы с хранилищем**
   - Проверьте права доступа к директории записи
   - Убедитесь, что достаточно места на диске

**См. детали:** [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

---

## Связанные документы

- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - Руководство по развертыванию
- [ADMINISTRATOR_GUIDE.md](ADMINISTRATOR_GUIDE.md) - Руководство для администраторов
- [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md) - Переменные окружения
- [API.md](API.md) - API документация (раздел Settings)
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Устранение неполадок

---

**Последнее обновление:** Декабрь 2025

