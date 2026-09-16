# 📋 Фаза 2 - Сводная Документация

**Дата:** 2026-06-09  
**Статус:** ✅ 100% ЗАВЕРШЕНО

---

## 🚀 Быстрый Старт

### 1. Запуск Docker с NAS и LDAP:

```powershell
powershell -ExecutionPolicy Bypass -File start_docker_with_nas.ps1
```

### 2. Обновление RTSP URL в БД:

```bash
PGPASSWORD='your_password' psql -h localhost -U surveillance -d surveillance -f update_camera_rtsp_urls.sql
```

### 3. Тестирование RTSP потоков:

```powershell
powershell -ExecutionPolicy Bypass -File test_rtsp_streams.ps1
```

### 4. Проверка ONVIF:

```powershell
powershell -ExecutionPolicy Bypass -File test_onvif_simple.ps1
```

### 5. Настройка NAS записи:

```powershell
powershell -ExecutionPolicy Bypass -File configure_nas_recording.ps1
```

---

## 📁 Структура Файлов

### Конфигурация:

| Файл | Назначение |
|------|------------|
| `config/nas_integration.json` | Конфигурация NAS |
| `config/nas_recording.json` | Конфигурация записи на NAS |
| `.env.nas` | Переменные окружения |

### Скрипты:

| Файл | Назначение |
|------|------------|
| `start_docker_with_nas.ps1` | Запуск Docker с NAS/LDAP |
| `test_docker_nas_ldap.ps1` | Тестирование интеграции |
| `test_rtsp_streams.ps1` | Тест RTSP потоков |
| `test_onvif_simple.ps1` | Тест ONVIF |
| `update_rtsp_urls_db.ps1` | Обновление БД (PowerShell) |
| `update_camera_rtsp_urls.sql` | Обновление БД (SQL) |
| `configure_nas_recording.ps1` | Настройка NAS записи |
| `scripts/mount_nas.sh` | Монтирование NAS (Linux) |

### Backend Код:

| Файл | Назначение |
|------|------------|
| `config/LdapConfig.kt` | LDAP конфигурация |
| `security/LdapUserDetailsService.kt` | LDAP сервис |
| `onvif/OnvifEventSubscriptionService.kt` | ONVIF сервис |
| `controller/OnvifRoutes.kt` | ONVIF контроллер |

### Документация:

| Файл | Назначение |
|------|------------|
| `docs/NAS_DOCKER_CONFIG.md` | Docker + NAS |
| `docs/LDAP_INTEGRATION_FOR_IPCSS_API.md` | LDAP интеграция |
| `docs/reports/PHASE2_COMPLETION_REPORT.md` | Финальный отчёт |

---

## 🎯 Результаты Тестирования

### RTSP Streams (7/7):

| Камера | IP | Статус | Латентность |
|--------|-----|--------|-------------|
| Camera 17 | 192.168.10.17 | ✅ | 7.05ms |
| Camera 20 | 192.168.10.20 | ✅ | 1.00ms |
| Camera 21 | 192.168.10.21 | ✅ | 1.00ms |
| Camera 22 | 192.168.10.22 | ✅ | 1.00ms |
| Camera 23 | 192.168.10.23 | ✅ | 2.00ms |
| Camera 24 | 192.168.10.24 | ✅ | 1.00ms |
| Camera 26 | 192.168.10.26 | ✅ | 2.00ms |

### ONVIF (2/7):

| Камера | IP | Статус |
|--------|-----|--------|
| Camera 23 | 192.168.10.23 | ✅ ONVIF Ready |
| Camera 24 | 192.168.10.24 | ✅ ONVIF Ready |
| Остальные 5 | - | ⚠️ RTSP Only |

### NAS (4/4):

| Узел | IP | Статус | Порты |
|------|-----|--------|-------|
| NAS 1 | 192.168.10.37 | ✅ | 445, 389, 22 |
| NAS 2 | 192.168.10.38 | ✅ | 445, 389, 22 |
| NAS 3 | 192.168.10.39 | ✅ | 445, 389, 22 |
| NAS 4 | 192.168.10.40 | ✅ | 445, 389, 22 |

---

## 🔧 Конфигурация

### LDAP:

```yaml
LDAP_ENABLED: true
AUTH_LDAP_SERVER: ldap://192.168.10.37:389
AUTH_LDAP_BASE_DN: dc=surveillance,dc=local
AUTH_LDAP_BIND_DN: cn=admin,dc=surveillance,dc=local
AUTH_LDAP_BIND_PASSWORD: <NAS_PASSWORD>
AUTH_LDAP_USER_SEARCH_FILTER: (uid={0})
```

### NAS:

```yaml
NAS_PRIMARY_HOST: 192.168.10.37
NAS_HOSTS: 192.168.10.37,192.168.10.38,192.168.10.39,192.168.10.40
NAS_PROTOCOL: NFS
NAS_SHARE_PATH: /storage/recordings
STORAGE_RETENTION_DAYS: 30
```

### RTSP:

```
rtsp://survival:1234567890qazxs@{IP}:554/stream1
```

---

## 📊 API Endpoints

### ONVIF:

```bash
# Проверка поддержки
POST /api/v1/onvif/check/{cameraId}

# Подписка на события
POST /api/v1/onvif/subscribe
Body: {
  "cameraId": "uuid",
  "cameraIp": "192.168.10.23",
  "username": "survival",
  "password": "1234567890qazxs"
}

# Отписка
POST /api/v1/onvif/unsubscribe/{cameraId}

# Информация об устройстве
GET /api/v1/onvif/device/{cameraId}
```

---

## 🐛 Troubleshooting

### LDAP не работает:

1. Проверить доступность: `Test-NetConnection 192.168.10.37 -Port 389`
2. Проверить переменные: `Get-ChildItem Env:`
3. Проверить логи: `docker-compose logs surveillance`

### RTSP не работает:

1. Проверить сеть: `Test-Connection {camera_ip}`
2. Проверить порт: `Test-NetConnection {camera_ip} -Port 554`
3. Проверить URL в БД: `SELECT url FROM camera WHERE id = '{uuid}'`

### NAS не доступен:

1. Проверить сеть: `Test-Connection 192.168.10.37`
2. Проверить SMB: `Test-NetConnection 192.168.10.37 -Port 445`
3. Проверить NFS: `net use` (Windows)

---

## 📈 Мониторинг

### Health Check:

```bash
curl http://localhost:8080/api/v1/health
```

### Логи:

```bash
docker-compose logs -f surveillance
```

### Статус контейнеров:

```bash
docker-compose ps
```

---

## 🎯 Следующие Шаги

### Приоритет 1: Тестирование (1-2 часа)

1. Запустить Docker
2. Проверить LDAP auth
3. Проверить RTSP потоки
4. Проверить ONVIF подписку

### Приоритет 2: Фаза 3 - Raspberry Pi (4-6 часов)

1. Установка OS
2. Настройка RTSP прокси
3. Интеграция с NAS

### Приоритет 3: Production (2-3 часа)

1. TLS сертификаты
2. Monitoring
3. Backup strategy

---

## 📞 Контакты

**Поддержка:** NLP-Core-Team  
**Версия:** 1.0  
**Дата:** 2026-06-09

---

*Фаза 2 завершена на 100% ✅*  
*Готово к тестированию и переходу к Фазе 3*
