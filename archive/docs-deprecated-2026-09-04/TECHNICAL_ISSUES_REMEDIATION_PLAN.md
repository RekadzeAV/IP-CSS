# 🔧 Решение Технических Проблем

**Дата:** 2026-06-09  
**Статус:** 🟡 В РАБОТЕ  
**Версия:** 1.0

---

## 📋 Обзор Проблем

| № | Проблема | Приоритет | Статус | Блокирует Phase 1 |
|---|----------|-----------|--------|-------------------|
| 1 | JWT токены пустые | 🔴 Высокий | ⏸️ Не начато | ❌ Нет |
| 2 | Plaintext пароли камер | 🟡 Средний | ⏸️ Не начато | ❌ Нет |
| 3 | Физические камеры недоступны | 🟡 Средний | ⚠️ Проверка | ❌ Нет |

---

## 1. 🔴 Проблема: JWT Токены Пустые

### Описание:

При успешной аутентификации API возвращает пустые токены:

```json
{
    "success": true,
    "data": {
        "accessToken": "",
        "refreshToken": "",
        "user": { ... }
    },
    "message": "Login successful"
}
```

### Симптомы:

- ✅ Логин проходит успешно (200 OK)
- ✅ Пользователь аутентифицирован
- ❌ accessToken пустой
- ❌ refreshToken пустой
- ❌ Защищённые эндпоинты недоступны

### Возможные Причины:

1. **Не настроен JWT_SECRET:**
   ```bash
   JWT_SECRET=ManualCheckJwt#2026-SecureRandomValue-For-Local
   ```
   - Проверить, что переменная передаётся в контейнер
   - Проверить, что ключ достаточно длинный (минимум 32 символа)

2. **Проблема в генерации токенов:**
   - Код генерации токенов может возвращать null
   - Ошибка в библиотеке JWT

3. **Проблема с сериализацией:**
   - Токены генерируются, но не сериализуются в JSON

### План Решения:

#### Шаг 1: Проверка Конфигурации

```powershell
# Проверить переменные окружения в контейнере
docker exec ip-camera-surveillance env | grep JWT

# Ожидается:
# JWT_SECRET=ManualCheckJwt#2026-SecureRandomValue-For-Local
```

**Статус:** ⏸️ Не выполнено

#### Шаг 2: Проверка Локов Генерации Токенов

```powershell
# Проверить логи API сервера
docker logs ip-camera-surveillance --tail 100 | Select-String "JWT\|token\|access"
```

**Статус:** ⏸️ Не выполнено

#### Шаг 3: Исследование Кода

**Локализация проблемы:**
- `server/routing/AuthRoutes.kt` - генерация токенов
- `server/service/AuthService.kt` - логика аутентификации
- `server/util/JwtProvider.kt` - создание JWT

**Действия:**
- [ ] Проверить, что JwtProvider создаёт токены
- [ ] Проверить, что токены возвращаются в ответе
- [ ] Добавить логирование генерации

**Статус:** ⏸️ Не выполнено

#### Шаг 4: Исправление

**Возможные Исправления:**

1. **Увеличить длину JWT_SECRET:**
   ```bash
   JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
   ```

2. **Исправить код генерации:**
   ```kotlin
   // AuthRoutes.kt
   val accessToken = jwtProvider.generateToken(user)
   val refreshToken = jwtProvider.generateRefreshToken(user)
   
   // Проверка
   require(accessToken.isNotBlank()) { "Access token is empty" }
   require(refreshToken.isNotBlank()) { "Refresh token is empty" }
   ```

**Статус:** ⏸️ Не выполнено

#### Шаг 5: Тестирование

```powershell
# Тест логина
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method POST -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}' -UseBasicParsing

$json = $response.Content | ConvertFrom-Json
Write-Output "Access Token Length: $($json.data.accessToken.Length)"
Write-Output "Refresh Token Length: $($json.data.refreshToken.Length)"

# Ожидается: > 0
```

**Статус:** ⏸️ Не выполнено

---

## 2. 🟡 Проблема: Plaintext Пароли Камер

### Описание:

При чтении камер из базы данных возникает ошибка:

```
Plaintext camera password detected for camera '27cf2d4d-0f6b-4de6-a753-78017695e65f'.
Credential must be encrypted at rest.
```

### Симптомы:

- ✅ База данных подключена
- ✅ Камеры существуют в БД
- ❌ Ошибка при чтении камер
- ❌ API не возвращает список камер

### Возможные Причины:

1. **Пароли сохранены в открытом виде:**
   - Раньше пароли не шифровались
   - Теперь требуется шифрование

2. **Ключ шифрования не настроен:**
   ```bash
   DATA_ENCRYPTION_KEY=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
   ```

3. **Миграция данных не выполнена:**
   - Старые записи с plaintext паролями остались

### План Решения:

#### Шаг 1: Проверка Ключа Шифрования

```powershell
# Проверить переменную в контейнере
docker exec ip-camera-surveillance env | grep ENCRYPTION

# Ожидается:
# DATA_ENCRYPTION_KEY=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
```

**Статус:** ⏸️ Не выполнено

#### Шаг 2: Проверка Данных в БД

```powershell
# Подключиться к PostgreSQL
docker exec -it surveillance-postgres psql -U surveillance -d surveillance

# Проверить пароли камер
SELECT id, name, rtsp_url, password FROM camera;

# Ожидается: зашифрованные значения или null
```

**Статус:** ⏸️ Не выполнено

#### Шаг 3: Миграция Данных

**Скрипт миграции:**

```kotlin
// Migration script
val cameras = cameraRepository.getAll()
cameras.forEach { camera ->
    if (camera.password.isPlaintext()) {
        val encrypted = encryptionService.encrypt(camera.password)
        cameraRepository.updatePassword(camera.id, encrypted)
    }
}
```

**Статус:** ⏸️ Не выполнено

#### Шаг 4: Исправление Мэппера

**CameraEntityMapper.kt:**

```kotlin
fun toDomain(entity: CameraEntity): Camera {
    val password = if (entity.password.isEncrypted()) {
        encryptionService.decrypt(entity.password)
    } else {
        // Для новых записей - зашифровать
        val encrypted = encryptionService.encrypt(entity.password)
        cameraRepository.updatePassword(entity.id, encrypted)
        encrypted
    }
    
    return Camera(
        id = entity.id,
        name = entity.name,
        rtspUrl = entity.rtspUrl,
        password = password
    )
}
```

**Статус:** ⏸️ Не выполнено

#### Шаг 5: Тестирование

```powershell
# Тест получения камер
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/cameras" `
  -Method GET -Headers @{ Authorization = "Bearer $token" } -UseBasicParsing

# Ожидается: список камер без ошибок
```

**Статус:** ⏸️ Не выполнено

---

## 3. ⚠️ Проблема: Физические Камеры Недоступны

### Описание:

Камеры в сети 192.168.10.0/24 не обнаружены при сканировании.

### Текущее Состояние:

| Камера | IP | Статус |
|--------|-----|--------|
| camera-1 | 192.168.10.100 | ❌ Не отвечает |
| camera-2 | 192.168.10.101 | ❌ Не отвечает |
| camera-3 | 192.168.10.102 | ❌ Не отвечает |
| camera-4 | 192.168.10.103 | ❌ Не отвечает |
| camera-5 | 192.168.10.104 | ❌ Не отвечает |

### План Проверки:

#### Шаг 1: Проверка Физического Наличия

```powershell
# Пинг шлюза (должен работать)
Test-Connection 192.168.10.1 -Count 2

# Пинг камер
100..104 | ForEach-Object {
    $ip = "192.168.10.$_"
    $result = Test-Connection $ip -Count 1 -Quiet
    Write-Output "$ip: $result"
}
```

**Статус:** ⏸️ Требуется выполнение

#### Шаг 2: Проверка ARP Таблицы

```powershell
# Посмотреть, какие устройства видели в сети
Get-NetNeighbor -InterfaceAlias "Ethernet" | Where-Object {
    $_.IPAddress -like "192.168.10.*"
}
```

**Статус:** ⏸️ Требуется выполнение

#### Шаг 3: Сканирование Портов

```powershell
# Проверить порт 554 на всех IP
100..104 | ForEach-Object {
    $ip = "192.168.10.$_"
    $result = Test-NetConnection $ip -Port 554 -InformationLevel Quiet
    Write-Output "$ip:554 - $result"
}
```

**Статус:** ⏸️ Требуется выполнение

#### Шаг 4: Проверка DHCP Сервера

**Действия:**
- [ ] Зайти в интерфейс роутера (192.168.10.1)
- [ ] Проверить список подключённых устройств
- [ ] Найти камеры по MAC-адресу или имени

**Статус:** ⏸️ Не выполнено

#### Шаг 5: Альтернативные Решения

**Если камеры физически отсутствуют:**

1. **Использовать RTSP Mock-Server:**
   ```bash
   docker run -d --name rtsp-mock \
     -p 8554:8554 \
     bluenviron/mediamtx
   ```

2. **Использовать тестовые RTSP потоки:**
   ```json
   {
     "rtspUrl": "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4"
   }
   ```

3. **Подключить реальные камеры:**
   - Подключить камеры к сети 192.168.10.0/24
   - Настроить статические IP
   - Обновить конфигурацию

**Статус:** ⏸️ Ожидает решения

---

## 📊 Приоритеты И Статус

| Задача | Приоритет | Статус | Ожидаемое Время |
|--------|-----------|--------|-----------------|
| **1. JWT Токены** | 🔴 Высокий | ⏸️ Не начато | 2-4 часа |
| **2. Пароли Камер** | 🟡 Средний | ⏸️ Не начато | 2-4 часа |
| **3. Проверка Камер** | 🟡 Средний | ⏸️ Не начато | 1-2 часа |

---

## 🚀 План Выполнения

### Этап 1: JWT Токены (Приоритет 1)

1. [ ] Проверить конфигурацию JWT_SECRET
2. [ ] Исследовать логи генерации токенов
3. [ ] Найти и исправить код генерации
4. [ ] Протестировать логин
5. [ ] Обновить документацию

**Цель:** API возвращает корректные токены

---

### Этап 2: Пароли Камер (Приоритет 2)

1. [ ] Проверить данные в БД
2. [ ] Создать скрипт миграции паролей
3. [ ] Исправить CameraEntityMapper
4. [ ] Протестировать получение камер
5. [ ] Обновить документацию

**Цель:** API возвращает список камер без ошибок

---

### Этап 3: Проверка Камер (Приоритет 3)

1. [ ] Выполнить ping тесты
2. [ ] Проверить ARP таблицу
3. [ ] Просканировать порты
4. [ ] Проверить DHCP сервер
5. [ ] Принять решение (mock или реальные камеры)

**Цель:** Подтвердить наличие камер или настроить mock

---

## 📝 Примечания

### Для Разработки:

**JWT Secret должен быть:**
- Минимум 32 символа
- Случайная строка
- Безопасный для production

**Шифрование паролей:**
- Использовать AES-256
- Ключ хранить в переменной окружения
- Никогда не хранить plaintext

### Для Тестирования:

**Mock RTSP Server:**
```bash
docker run -d --name rtsp-mock \
  -p 8554:8554 \
  -e RTSP_PORT=8554 \
  bluenviron/mediamtx
```

**Тестовые потоки:**
- `rtsp://localhost:8554/camera1`
- `rtsp://localhost:8554/camera2`

---

## 📈 Прогресс

```
JWT Токены: ░░░░░░░░░░░░░░░░░░░░   0% ⏸️
Пароли Камер: ░░░░░░░░░░░░░░░░░░░░   0% ⏸️
Проверка Камер: ░░░░░░░░░░░░░░░░░░░░   0% ⏸️
```

---

*Документ создан: 2026-06-09*  
*Версия: 1.0*  
*Статус: В РАБОТЕ*
