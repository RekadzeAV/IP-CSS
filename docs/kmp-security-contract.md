# KMP Security Contract (Phase 1)

Этот документ фиксирует единый контракт для security/encryption подсистемы в KMP.

## 1. Область действия

Контракт обязателен для:
- `SecureLocalDataEncryption`
- `SecurePasswordEncryption`
- `SecureMobileSecurityLogger`
- `DigestCrypto`

На всех платформах: Android, JVM/Desktop, iOS, Native.

## 2. Контракт шифрования локальных данных

- API:
  - `encrypt(data: ByteArray): ByteArray`
  - `decrypt(encryptedData: ByteArray): ByteArray`
  - `encryptString(data: String): String`
  - `decryptString(encryptedData: String): String`
  - `isEncrypted(data: ByteArray): Boolean`
- Требование round-trip:
  - `decrypt(encrypt(x)) == x` для валидных входных данных.
- Пустой вход:
  - пустые значения обрабатываются без падения.
- Идемпотентность:
  - повторное шифрование уже зашифрованных данных не должно ломать данные.
- Поврежденные данные:
  - не должны приводить к утечке секретов в лог;
  - должны обрабатываться предсказуемо (ошибка безопасности или безопасный fallback, согласно platform actual).

## 3. Контракт шифрования паролей

- API:
  - `encrypt(password: String): String`
  - `decrypt(encryptedPassword: String): String`
  - `isEncrypted(value: String): Boolean`
- Требование round-trip:
  - `decrypt(encrypt(password)) == password` для валидных входных данных.
- Нешифрованный ввод:
  - `decrypt(plain)` возвращает `plain` или обрабатывается по platform policy без потери данных.
- Ошибки расшифровки:
  - обязаны быть классифицированы как security failure (exception или безопасный fallback по контракту реализации).

## 4. Контракт DigestCrypto

- API:
  - `md5Hex(input: String): String`
  - `sha256Hex(input: String): String`
  - `secureRandomHex(byteCount: Int): String`
- Формат:
  - hex строка в lowercase.
- Размер:
  - `secureRandomHex(n)` возвращает строку длиной `2 * n`.
- Детерминизм:
  - `md5Hex` и `sha256Hex` детерминированы для одинакового входа.

## 5. Контракт security-логирования

- `log(event: MobileSecurityEvent)` принимает только безопасные metadata.
- Не допускается логирование секретов:
  - пароль, токен, приватный ключ, session secret, raw credential.
- Helper-методы логгера должны формировать минимально необходимый набор полей и не добавлять секреты автоматически.

## 6. Общие правила ошибок и приватности

- Ошибки security/encryption не должны раскрывать чувствительные данные.
- Сообщения ошибок допустимы только в санитаризованной форме.
- В логах запрещено хранить исходные пароли/ключи/токены.

## 7. Проверка в CI

Контракт поддерживается автоматическими gate-проверками:
- сигнатуры `expect/actual`;
- запрет platform API в `commonMain`;
- защита от JVM/Android зависимостей в `iosMain/nativeMain`;
- KMP compile smoke.
