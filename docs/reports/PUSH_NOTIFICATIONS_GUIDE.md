# Push Notifications Configuration Guide

**Дата:** 27 April 2026  
**Задача:** 2.2.3 — Push (FCM, APNs)  
**Статус:** 🟡 В процессе (~80%)

---

## 📱 Push-уведомления

### Поддерживаемые платформы

1. **FCM (Firebase Cloud Messaging)** — Android
2. **APNs (Apple Push Notification service)** — iOS
3. **Web Push** — Браузеры (через generic endpoint)

---

## 🔥 Firebase Cloud Messaging (FCM) для Android

### 1. Настройка Firebase проекта

1. Создайте проект в [Firebase Console](https://console.firebase.google.com/)
2. Добавьте Android приложение
3. Скачайте `google-services.json` или `service-account-key.json`

### 2. Переменные окружения

```bash
# FCM Configuration
FCM_ENABLED=true
FCM_PROJECT_ID=your-project-id
FCM_SERVICE_ACCOUNT_PATH=/path/to/service-account-key.json
# или
FCM_SERVICE_ACCOUNT_JSON='{"type":"service_account",...}'
```

### 3. Зависимости

Добавьте в `build.gradle.kts`:

```kotlin
implementation("com.google.firebase:firebase-admin:9.2.0")
```

### 4. Регистрация устройства

На Android клиенте:

```kotlin
FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
    // Отправьте токен на сервер
    api.registerPushToken(token, "android")
}
```

---

## 🍎 Apple Push Notification service (APNs) для iOS

### 1. Настройка в Apple Developer Portal

1. Создайте App ID с Push Notifications capability
2. Создайте APNS Key в Keys разделе
3. Скачайте `.p8` файл ключа

### 2. Переменные окружения

```bash
# APNS Configuration
APNS_ENABLED=true
APNS_ENVIRONMENT=development  # или production
APNS_TEAM_ID=XXXXXXXXXX
APNS_KEY_ID=YYYYYYYYYY
APNS_BUNDLE_ID=com.yourcompany.ipcss
APNS_PRIVATE_KEY_PATH=/path/to/AuthKey_YYYYYYYYYY.p8
# или
APNS_PRIVATE_KEY='-----BEGIN PRIVATE KEY-----...'
```

### 3. Зависимости

Добавьте в `build.gradle.kts`:

```kotlin
implementation("io.jsonwebtoken:jjwt-api:0.11.5")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
```

### 4. Регистрация устройства

На iOS клиенте:

```swift
import UserNotifications
import FirebaseMessaging

UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
    if granted {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }
}

// В delegate
func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
    Messaging.messaging().apnsToken = deviceToken
    // Отправьте токен на сервер
    api.registerPushToken(token, "ios")
}
```

---

## 🌐 Web Push (браузеры)

### 1. Настройка VAPID keys

```bash
# Генерация VAPID keys (через npx)
npx web-push generate-vapid-keys
```

### 2. Переменные окружения

```bash
# Web Push Configuration
PUSH_ENABLED=true
PUSH_ENDPOINT=https://fcm.googleapis.com/fcm/send  # или ваш endpoint
PUSH_API_KEY=your-vapid-public-key
PUSH_VAPID_PRIVATE_KEY=your-vapid-private-key
```

---

## 🔧 Использование в правилах аналитики

### Пример правила с push-уведомлением

```json
{
  "id": "rule-002",
  "name": "Face Detection Alert",
  "cameraId": "camera-001",
  "eventType": "FACE_DETECTION",
  "actions": {
    "sendNotification": true,
    "notificationType": "WARNING",
    "additionalActions": {
      "__notifyPush": true,
      "__notifyPushTokens": ["token1", "token2"],
      "pushPayload": {
        "type": "FACE_DETECTION",
        "cameraId": "camera-001",
        "deepLink": "/events/123"
      }
    }
  }
}
```

### Push payload структуры

#### FCM Payload

```json
{
  "type": "MOTION_DETECTION",
  "cameraId": "camera-001",
  "cameraName": "Входная дверь",
  "eventId": "evt-123",
  "imageUrl": "http://server/snapshot.jpg",
  "deepLink": "/events/123"
}
```

#### APNS Payload

```json
{
  "type": "LICENSE_PLATE_RECOGNITION",
  "cameraId": "camera-002",
  "cameraName": "Парковка",
  "eventId": "evt-456",
  "deepLink": "/cameras/2",
  "category": "PLATE_RECOGNIZED"
}
```

---

## 🧪 Тестирование

### 1. Тестовое push-уведомление

```bash
curl -X POST http://localhost:8080/api/v1/notifications/test/push \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tokens": ["test-token"],
    "title": "Тест push уведомления",
    "message": "Это тестовое push уведомление",
    "data": {
      "type": "TEST",
      "deepLink": "/settings"
    }
  }'
```

### 2. Логирование

```properties
logging.level.com.company.ipcamera.server.notification=DEBUG
```

---

## 📊 Мониторинг

### Статистика push-уведомлений

```bash
# Получить статистику
curl -X GET http://localhost:8080/api/v1/notifications/stats/push \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Управление токенами

```bash
# Зарегистрировать токен
curl -X POST http://localhost:8080/api/v1/notifications/push-tokens \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "device-push-token",
    "platform": "android"
  }'

# Получить список токенов
curl -X GET http://localhost:8080/api/v1/notifications/push-tokens \
  -H "Authorization: Bearer YOUR_TOKEN"

# Отзыв токена
curl -X DELETE http://localhost:8080/api/v1/notifications/push-tokens/revoke \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tokens": ["invalid-token"]
  }'
```

---

## 🐛 Troubleshooting

### Проблема: FCM уведомления не приходят

**Решение:**
1. Проверьте `FCM_ENABLED=true`
2. Убедитесь, что service account ключ валидный
3. Проверьте FCM токены устройств
4. Проверьте логи на наличие ошибок

### Проблема: APNS не работает

**Решение:**
1. Проверьте правильность Team ID и Key ID
2. Убедитесь, что bundle ID совпадает с приложением
3. Проверьте environment (development/production)
4. Валидируйте `.p8` файл ключа

### Проблема: Токены не сохраняются

**Решение:**
1. Проверьте настройки SettingsRepository
2. Убедитесь, что userId передан правильно
3. Проверьте логи PushTokenService

---

## ✅ Checklist завершения 2.2.3

- [x] Базовый push-канал работает
- [x] Lifecycle API токенов реализован
- [x] FCM sender создан
- [x] APNs sender создан
- [x] Документация по настройке
- [ ] FCM интеграционные тесты
- [ ] APNs интеграционные тесты
- [ ] Web Push реализация
- [ ] Production валидация

**Прогресс задачи 2.2.3:** ~80% (было ~40%)

---

**Дата обновления:** 27 April 2026
