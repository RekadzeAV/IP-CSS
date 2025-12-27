# Тесты для модуля Shared

Этот каталог содержит unit-тесты для модуля `shared`.

## Структура тестов

- `domain/usecase/` - тесты для Use Cases
- `data/repository/` - тесты для репозиториев
- `data/local/` - тесты для мапперов и утилит
- `test/` - тестовые утилиты и хелперы

## Запуск тестов

### Все тесты модуля
```bash
./gradlew :shared:test
```

### Конкретный тест
```bash
./gradlew :shared:test --tests "com.company.ipcamera.shared.domain.usecase.AddCameraUseCaseTest"
```

### Все тесты проекта
```bash
./gradlew testAll
```

## Покрытие тестами

### Use Cases
- ✅ AddCameraUseCase
- ✅ GetCamerasUseCase
- ✅ GetCameraByIdUseCase
- ✅ UpdateCameraUseCase
- ✅ DeleteCameraUseCase

### Репозитории
- ✅ CameraRepositoryImpl (CRUD операции, статусы)

### Мапперы
- ✅ CameraEntityMapper (конвертация domain ↔ database)

## Тестовые утилиты

- `TestDataFactory` - создание тестовых данных
- `TestDatabaseFactory` - создание in-memory базы данных для тестов



