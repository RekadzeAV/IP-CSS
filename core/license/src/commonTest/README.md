# Тесты для модуля License

Этот каталог содержит unit-тесты для модуля `core/license`.

## Структура тестов

- `LicenseManagerTest` - тесты для LicenseManager и связанных классов

## Запуск тестов

### Все тесты модуля
```bash
./gradlew :core:license:test
```

### Конкретный тест
```bash
./gradlew :core:license:test --tests "com.company.ipcamera.core.license.LicenseManagerTest"
```

## Покрытие тестами

### LicenseManager
- ✅ ActivatedLicense.isExpired()
- ✅ ActivatedLicense.isOfflineExpired()
- ✅ ActivatedLicense.getDaysRemaining()
- ✅ ActivatedLicense.getOfflineDaysRemaining()
- ✅ License.supportsPlatform()
- ✅ Enum значения (LicenseError, LicenseWarning)

## Примечания

Полное тестирование LicenseManager требует actual реализаций для:
- `PlatformCrypto` (expect class)
- `LicenseRepository` (expect class)

Текущие тесты проверяют доступную логику без платформо-специфичных зависимостей.




