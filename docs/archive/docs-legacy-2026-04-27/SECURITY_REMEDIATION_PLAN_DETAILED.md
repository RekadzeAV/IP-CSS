# Детализированный план устранения всех уязвимостей безопасности

**Дата создания:** 27 January 2026
**Версия:** 2.0
**Основан на:** SECURITY_AUDIT_REPORT.md (25 уязвимостей)

---

## Статус реализации

### ✅ Уже реализовано (частично или полностью)

1. ✅ **Аутентификация и авторизация (Риск 1)** - JWT аутентификация реализована, RBAC работает
2. ✅ **Docker конфигурация (Риск 2)** - SYS_ADMIN и seccomp:unconfined удалены, non-root user настроен
3. ⚠️ **Certificate Pinning (Риск 3)** - Структура реализована, требуется интеграция и тестирование
4. ⚠️ **Шифрование паролей (Риск 4)** - Android готов, iOS требует доработки (AES-GCM), Desktop нужно проверить
5. ✅ **CORS (Риск 5)** - Исправлен, anyHost() удален
6. ✅ **HTTPS (Риск 6)** - Redirect и HSTS настроены
7. ✅ **httpOnly Cookies (Риск 7)** - Реализовано
8. ✅ **Security Headers (Риск 8, 22, 25)** - Реализовано
9. ✅ **Rate Limiting (Риск 9)** - Реализован с Redis
10. ✅ **Пароли по умолчанию (Риск 10)** - Исправлено в docker-compose.yml
11. ⚠️ **Валидация входных данных (Риск 11)** - Частично реализована, требуется расширение
12. ⚠️ **Логирование безопасности (Риск 12, 23)** - Базовая реализация есть, требуется расширение
13. ✅ **Защита от перечисления пользователей (Риск 18, 20)** - Реализовано
14. ✅ **Timing attacks (Риск 21)** - Упомянуто в AuthRoutes
15. ✅ **CSRF защита (Риск 24)** - Middleware реализован

### ❌ Требует реализации

16. ❌ **SSRF защита (Риск 19)** - Нет проверки внутренних IP адресов
17. ❌ **Path Traversal (Риск 16)** - Нет защиты в FileSystem
18. ❌ **Валидация размера файлов (Риск 17)** - Нет проверки размера
19. ❌ **Android allowBackup (Риск 13)** - Нужно проверить/настроить
20. ❌ **iOS шифрование доработка (Риск 14)** - Нужна полная AES-GCM реализация
21. ❌ **Desktop шифрование (Риск 14)** - Нужно проверить/реализовать
22. ❌ **Проверка целостности лицензий (Риск 15)** - Не реализовано

---

## План реализации по приоритетам

### Фаза 1: Критические доработки (Неделя 1)

#### 1.1. SSRF защита (Риск 19) - КРИТИЧНО
**Приоритет:** 🔴 КРИТИЧЕСКИЙ
**Оценка:** 1-2 дня

**Задачи:**
- Добавить проверку внутренних IP адресов в `RequestValidator.validateUrl()`
- Блокировать: 127.0.0.0/8, 10.0.0.0/8, 192.168.0.0/16, 172.16.0.0/12, 169.254.0.0/16
- Блокировать localhost, ::1
- Блокировать метаданные сервисы (169.254.169.254)
- Добавить whitelist разрешенных доменов (опционально)

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/validation/RequestValidator.kt`
- `core/common/src/commonMain/kotlin/com/company/ipcamera/core/common/security/InputValidator.kt`

---

#### 1.2. Path Traversal защита (Риск 16) - ВЫСОКИЙ
**Приоритет:** 🟠 ВЫСОКИЙ
**Оценка:** 2-3 дня

**Задачи:**
- Добавить валидацию путей во все FileSystem операции
- Нормализовать пути и проверять базовую директорию
- Блокировать `..`, абсолютные пути вне базовой директории

**Файлы:**
- `shared/src/androidMain/kotlin/com/company/ipcamera/shared/common/FileSystem.android.kt`
- `shared/src/iosMain/kotlin/com/company/ipcamera/shared/common/FileSystem.ios.kt`
- `shared/src/desktopMain/kotlin/com/company/ipcamera/shared/common/FileSystem.desktop.kt`

---

#### 1.3. Валидация размера файлов (Риск 17) - ВЫСОКИЙ
**Приоритет:** 🟠 ВЫСОКИЙ
**Оценка:** 1-2 дня

**Задачи:**
- Добавить проверку размера в `ApiClient.upload()`
- Добавить middleware на сервере для проверки Content-Length
- Установить максимальный размер (100MB)
- Валидировать MIME type

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt`
- Создать `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/FileUploadMiddleware.kt`

---

### Фаза 2: Средние доработки (Неделя 2)

#### 2.1. iOS шифрование доработка (Риск 14) - СРЕДНИЙ
**Приоритет:** 🟡 СРЕДНИЙ
**Оценка:** 2-3 дня

**Задачи:**
- Заменить упрощенное XOR шифрование на полноценный AES-GCM
- Использовать CommonCrypto или CryptoKit для AES-GCM
- Удалить TODO комментарии
- Добавить тесты

**Файлы:**
- `core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.ios.kt`

---

#### 2.2. Desktop шифрование проверка/доработка (Риск 14) - СРЕДНИЙ
**Приоритет:** 🟡 СРЕДНИЙ
**Оценка:** 1-2 дня

**Задачи:**
- Проверить текущую реализацию
- Реализовать Java KeyStore если не реализовано
- Добавить тесты

**Файлы:**
- `core/common/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.jvm.kt`

---

#### 2.3. Android allowBackup (Риск 13) - СРЕДНИЙ
**Приоритет:** 🟡 СРЕДНИЙ
**Оценка:** 1 день

**Задачи:**
- Проверить AndroidManifest.xml
- Настроить backup_rules.xml если нужен backup
- Исключить чувствительные данные

**Файлы:**
- `android/app/src/main/AndroidManifest.xml`
- Создать `android/app/src/main/res/xml/backup_rules.xml` (если нужно)

---

#### 2.4. Проверка целостности лицензий (Риск 15) - СРЕДНИЙ
**Приоритет:** 🟡 СРЕДНИЙ
**Оценка:** 1-2 недели

**Задачи:**
- Реализовать цифровую подпись лицензий (RSA/ECC)
- Проверка подписи на клиенте
- Проверка привязки к устройству
- Проверка хеша и срока действия

**Файлы:**
- `core/license/src/commonMain/kotlin/com/company/ipcamera/core/license/LicenseManager.kt`
- Создать файлы для подписи и проверки

---

### Фаза 3: Доработка и тестирование (Неделя 3)

#### 3.1. Расширение логирования безопасности (Риск 12, 23)
**Задачи:**
- Добавить логирование подозрительной активности
- Логирование изменений конфигурации
- Настроить централизованный сбор логов

#### 3.2. Certificate Pinning интеграция и тестирование (Риск 3)
**Задачи:**
- Интегрировать certificate pinning в ApiClient
- Добавить конфигурацию сертификатов
- Протестировать на всех платформах

#### 3.3. Финальное тестирование
**Задачи:**
- Security тесты для всех исправлений
- Penetration testing
- Обновление документации

---

## Детальные шаги реализации

### SSRF защита - Детальный план

1. **Создать утилиту для проверки IP адресов:**
   ```kotlin
   object SsrfProtection {
       private val privateIpRanges = listOf(
           "127.0.0.0/8",
           "10.0.0.0/8",
           "192.168.0.0/16",
           "172.16.0.0/12",
           "169.254.0.0/16"
       )

       fun isPrivateIp(ip: String): Boolean { ... }
       fun isLocalhost(host: String): Boolean { ... }
       fun validateUrlForSsrf(url: String): ValidationResult { ... }
   }
   ```

2. **Интегрировать в RequestValidator.validateUrl()**

3. **Добавить тесты**

---

### Path Traversal защита - Детальный план

1. **Создать утилиту для валидации путей:**
   ```kotlin
   object PathSecurity {
       fun validatePath(path: String, baseDirectory: String): ValidationResult { ... }
       fun normalizeAndValidate(path: String, base: String): String? { ... }
   }
   ```

2. **Интегрировать во все FileSystem операции**

3. **Добавить тесты**

---

### Валидация размера файлов - Детальный план

1. **Создать FileUploadMiddleware:**
   ```kotlin
   class FileUploadMiddleware {
       private val maxFileSize = 100 * 1024 * 1024 // 100MB
       fun validateFileSize(size: Long): ValidationResult { ... }
       fun validateMimeType(mimeType: String): ValidationResult { ... }
   }
   ```

2. **Интегрировать в ApiClient.upload()**

3. **Добавить middleware на сервере**

4. **Добавить тесты**

---

## Критерии приемки

### SSRF защита
- ✅ URL с внутренними IP адресами отклоняются
- ✅ localhost и метаданные сервисы блокируются
- ✅ Тесты покрывают все сценарии

### Path Traversal
- ✅ Пути с `..` блокируются
- ✅ Абсолютные пути вне базовой директории блокируются
- ✅ Тесты покрывают все сценарии

### Валидация размера файлов
- ✅ Файлы > 100MB отклоняются
- ✅ Неподдерживаемые MIME types отклоняются
- ✅ Тесты покрывают все сценарии

---

## Временная шкала

```
Неделя 1:
- День 1-2: SSRF защита
- День 3-5: Path Traversal защита
- День 6-7: Валидация размера файлов

Неделя 2:
- День 1-3: iOS шифрование доработка
- День 4-5: Desktop шифрование проверка
- День 6: Android allowBackup
- День 7: Начало работы над лицензиями

Неделя 3:
- День 1-3: Лицензии (продолжение)
- День 4-5: Расширение логирования
- День 6-7: Certificate Pinning интеграция и тестирование

Неделя 4:
- Финальное тестирование
- Документация
- Повторный аудит
```

---

## Ответственность

- **Backend Team:** SSRF, валидация файлов, логирование
- **Mobile Team:** iOS шифрование, Android allowBackup
- **Platform Team:** Path Traversal, Desktop шифрование
- **Security Team:** Тестирование, аудит

---

**Последнее обновление:** 27 January 2026
