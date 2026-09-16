# Безопасность мобильных платформ

**Дата создания:** 26 января 2026
**Версия:** 1.0
**Статус:** ✅ Реализовано

---

## Обзор

Этот документ описывает меры безопасности, реализованные для мобильных платформ (Android и iOS) проекта IP Camera Surveillance System.

---

## 1. Android безопасность

### 1.1. Конфигурация приложения

#### AndroidManifest.xml

**allowBackup:**
- ✅ Установлено `android:allowBackup="false"` для предотвращения автоматического резервного копирования
- Создан файл `backup_rules.xml` для контролируемого backup (если потребуется в будущем)

**usesCleartextTraffic:**
- ✅ Установлено `android:usesCleartextTraffic="false"` - запрет незащищенного HTTP трафика
- Настроен `network_security_config.xml` для управления сетевыми политиками

**Файлы:**
- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/res/xml/backup_rules.xml`
- `android/app/src/main/res/xml/network_security_config.xml`

### 1.2. Шифрование паролей

**Реализация:**
- ✅ Использует Android Keystore для генерации и хранения ключей шифрования
- ✅ Алгоритм: AES-256-GCM
- ✅ Ключ уникален для каждого устройства
- ✅ Ключ не может быть экспортирован из Keystore
- ✅ Обработка ошибок: выбрасывает исключения вместо возврата незашифрованных данных

**Файл:** `core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.android.kt`

**Особенности:**
- Ключ генерируется автоматически при первом использовании
- Используется `KeyGenParameterSpec` с параметрами:
  - `PURPOSE_ENCRYPT | PURPOSE_DECRYPT`
  - `BLOCK_MODE_GCM`
  - `ENCRYPTION_PADDING_NONE`
  - Размер ключа: 256 бит

### 1.3. Сетевая безопасность

**Network Security Config:**
- ✅ Запрещен cleartext трафик в production
- ✅ Разрешен cleartext только для localhost в debug режиме
- ✅ Поддержка certificate pinning (настраивается через XML)

**Certificate Pinning:**
- ✅ Реализовано через OkHttp `CertificatePinner` в Ktor Android engine
- ✅ Поддержка нескольких pins для ротации сертификатов
- ✅ Инструкции по настройке в `network_security_config.xml`

**Файлы:**
- `android/app/src/main/res/xml/network_security_config.xml`
- `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.android.kt`

### 1.4. Защита от скриншотов

**Реализация:**
- ✅ Создан `ScreenSecurityHelper` для защиты чувствительных экранов
- ✅ Использует `FLAG_SECURE` для предотвращения скриншотов и записи экрана
- ✅ Реализован Compose composable `PreventScreenshots()` для простого использования
- ✅ Пример использования в `SettingsScreen`

**Файлы:**
- `android/app/src/main/java/com/company/ipcamera/android/security/ScreenSecurityHelper.kt`

**Использование:**
```kotlin
@Composable
fun SensitiveScreen() {
    PreventScreenshots() // Защищает экран от скриншотов
    // Ваш контент
}
```

### 1.5. Логирование безопасности

**Реализация:**
- ✅ Создан `MobileSecurityLogger` для логирования событий безопасности
- ✅ Логируются:
  - Успешные/неуспешные операции шифрования/дешифрования
  - Ошибки Keystore
  - Ошибки certificate pinning
  - Ошибки TLS
  - Подозрительная активность

**ВАЖНО:** Логи НЕ содержат пароли, токены, ключи или другую чувствительную информацию.

**Файлы:**
- `core/common/src/commonMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.kt`
- `core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.android.kt`

---

## 2. iOS безопасность

### 2.1. Шифрование паролей

**Реализация:**
- ✅ Использует Keychain Services для хранения ключей шифрования
- ✅ Ключ уникален для каждого устройства
- ✅ Ключ хранится с атрибутом `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`
- ✅ Обработка ошибок: выбрасывает исключения вместо возврата незашифрованных данных

**Файл:** `core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.ios.kt`

**Особенности:**
- Ключ генерируется с использованием `SecRandomCopyBytes`
- Размер ключа: 32 байта (256 бит)
- Использует AES-CBC шифрование с HMAC-SHA256 для аутентификации
- Реализована проверка целостности данных через HMAC
- PKCS7 padding для корректной работы с блоками

### 2.2. Сетевая безопасность

**App Transport Security (ATS):**
- ✅ Настроен для запрета незащищенного HTTP трафика
- ✅ Certificate pinning реализован через `CertificatePinningDelegate`

**Certificate Pinning:**
- ✅ Реализовано через `NSURLSessionDelegate`
- ✅ Проверка SHA-256 fingerprints сертификатов
- ✅ Поддержка enforcePinning (отклонение соединений при несовпадении)

**Файлы:**
- `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.ios.kt`
- `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinningDelegate.ios.kt`

### 2.3. Логирование безопасности

**Реализация:**
- ✅ Использует общий `MobileSecurityLogger` интерфейс
- ✅ iOS-специфичная реализация через mu.KotlinLogging

**Файл:** `core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.ios.kt`

---

## 3. Общие меры безопасности

### 3.1. Обработка ошибок

**Принципы:**
- ✅ При ошибках шифрования/дешифрования выбрасываются исключения
- ✅ НЕ возвращаются незашифрованные данные при ошибках
- ✅ Все ошибки логируются через `MobileSecurityLogger`

### 3.2. Хранение данных

**Запрещено:**
- ❌ Хранение паролей в открытом виде
- ❌ Хранение токенов в SharedPreferences/UserDefaults без шифрования
- ❌ Логирование чувствительных данных

**Разрешено:**
- ✅ Шифрование всех паролей перед сохранением
- ✅ Использование Android Keystore / iOS Keychain для ключей
- ✅ Использование EncryptedSharedPreferences (Android) при необходимости

### 3.3. Сетевое взаимодействие

**Требования:**
- ✅ Только TLS 1.2+ соединения
- ✅ Certificate pinning для критичных доменов
- ✅ Запрет cleartext трафика в production
- ✅ Валидация сертификатов сервера

---

## 4. Настройка Certificate Pinning

### 4.1. Android

**Шаги:**

1. Получите SHA-256 fingerprint сертификата:
   ```bash
   openssl s_client -connect api.yourdomain.com:443 -showcerts | \
     openssl x509 -fingerprint -sha256 -noout
   ```

2. Отредактируйте `android/app/src/main/res/xml/network_security_config.xml`:
   ```xml
   <domain includeSubdomains="true">api.yourdomain.com</domain>
   <pin-set expiration="2026-12-31">
       <pin digest="SHA-256">YOUR_PRIMARY_CERTIFICATE_SHA256_FINGERPRINT</pin>
       <pin digest="SHA-256">YOUR_BACKUP_CERTIFICATE_SHA256_FINGERPRINT</pin>
   </pin-set>
   ```

3. **ВАЖНО:** Всегда используйте минимум 2 pin (основной + backup) для возможности ротации сертификатов

### 4.2. iOS

Certificate pinning настраивается программно через `CertificatePinningConfig`:

```kotlin
val config = CertificatePinningConfig(
    enablePinning = true,
    enforcePinning = true,
    pinnedCertificates = mapOf(
        "api.yourdomain.com" to listOf(
            "YOUR_PRIMARY_CERTIFICATE_SHA256_FINGERPRINT",
            "YOUR_BACKUP_CERTIFICATE_SHA256_FINGERPRINT"
        )
    )
)
```

---

## 5. Мониторинг и аудит

### 5.1. События безопасности

Все критические события безопасности логируются через `MobileSecurityLogger`:

- `ENCRYPTION_FAILURE` - ошибка шифрования
- `DECRYPTION_FAILURE` - ошибка расшифровки
- `CERTIFICATE_PINNING_FAILURE` - ошибка certificate pinning (возможная MITM атака)
- `KEYSTORE_ERROR` - ошибка доступа к Keystore/Keychain
- `SUSPICIOUS_ACTIVITY` - подозрительная активность

### 5.2. Интеграция с сервером

**TODO:** Реализовать отправку критических событий на сервер для централизованного мониторинга.

---

## 6. Чеклист безопасности

### Android

- [x] `allowBackup="false"` в AndroidManifest
- [x] `usesCleartextTraffic="false"` в AndroidManifest
- [x] Network Security Config настроен
- [x] Android Keystore используется для ключей
- [x] Certificate pinning поддерживается
- [x] Логирование безопасности реализовано
- [x] Защита от скриншотов для чувствительных экранов

### iOS

- [x] Keychain используется для ключей
- [x] Certificate pinning реализован
- [x] Логирование безопасности реализовано
- [x] Улучшена реализация шифрования (AES-CBC + HMAC-SHA256)

---

## 7. Рекомендации для production

### 7.1. Перед релизом

1. **Настроить certificate pinning** для всех production доменов
2. **Проверить**, что cleartext traffic полностью отключен
3. **Протестировать** MITM атаки (должны быть заблокированы)
4. **Настроить** централизованный сбор логов безопасности
5. **Провести** security audit с использованием инструментов:
   - Android: MobSF, QARK
   - iOS: iLEAPP, iLEAPP

### 7.2. Мониторинг

- Настроить алерты на критические события безопасности
- Регулярно проверять логи на подозрительную активность
- Мониторить ошибки certificate pinning (возможные MITM атаки)

### 7.3. Обновления

- Регулярно обновлять зависимости для исправления уязвимостей
- Следить за обновлениями Android Security Bulletins
- Следить за обновлениями iOS Security Updates

---

## 8. Связанные документы

- [SECURITY_AUDIT_REPORT.md](SECURITY_AUDIT_REPORT.md) - Полный отчет об аудите безопасности
- [SECURITY_REMEDIATION_PLAN.md](SECURITY_REMEDIATION_PLAN.md) - План устранения уязвимостей
- [CERTIFICATE_PINNING.md](../core/network/CERTIFICATE_PINNING.md) - Детали реализации certificate pinning

---

**Последнее обновление:** 26 января 2026
