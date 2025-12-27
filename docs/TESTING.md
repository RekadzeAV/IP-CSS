# Руководство по тестированию

## Обзор

Проект использует Kotlin Multiplatform для кроссплатформенной разработки. Тесты написаны с использованием `kotlin-test` и `kotlinx-coroutines-test`.

## Структура тестов

```
shared/src/commonTest/          # Общие тесты для всех платформ
├── domain/usecase/            # Тесты Use Cases
├── data/repository/            # Тесты репозиториев
├── data/local/                 # Тесты мапперов
└── test/                       # Тестовые утилиты

core/license/src/commonTest/    # Тесты лицензионной системы
└── LicenseManagerTest.kt
```

## Запуск тестов

### Все тесты проекта
```bash
./gradlew testAll
```

### Тесты конкретного модуля
```bash
# Shared модуль
./gradlew :shared:test

# License модуль
./gradlew :core:license:test
```

### Конкретный тест класс
```bash
./gradlew :shared:test --tests "com.company.ipcamera.shared.domain.usecase.AddCameraUseCaseTest"
```

### Тесты с покрытием кода
```bash
./gradlew :shared:test --tests "*" --info
```

## Типы тестов

### Unit тесты

Проверяют отдельные компоненты изолированно:

- **Use Cases** - бизнес-логика
- **Repositories** - работа с данными
- **Mappers** - преобразование данных
- **Models** - валидация и логика моделей

### Integration тесты

Проверяют взаимодействие компонентов:

- **Repository + Database** - работа с SQLDelight
- **Use Case + Repository** - полный цикл операций

## Тестовые утилиты

### TestDataFactory

Создание тестовых данных для моделей:

```kotlin
val camera = TestDataFactory.createTestCamera(
    id = "camera-1",
    name = "Test Camera"
)
```

### TestDatabaseFactory

Создание in-memory базы данных для тестов:

```kotlin
val database = TestDatabaseFactory.createTestDatabase()
```

## Примеры тестов

### Тест Use Case

```kotlin
@Test
fun `test add camera success`() = runTest {
    val repository = mockRepository()
    val useCase = AddCameraUseCase(repository)
    
    val result = useCase(
        name = "Test Camera",
        url = "rtsp://192.168.1.100:554/stream"
    )
    
    assertTrue(result.isSuccess)
}
```

### Тест Repository

```kotlin
@Test
fun `test get camera by id`() = runTest {
    val repository = CameraRepositoryImpl(testDatabaseFactory)
    val camera = TestDataFactory.createTestCamera()
    repository.addCamera(camera)
    
    val result = repository.getCameraById(camera.id)
    
    assertNotNull(result)
    assertEquals(camera.id, result?.id)
}
```

## Зависимости для тестирования

### Common Test
- `kotlin-test-common` - базовые функции тестирования
- `kotlin-test-annotations-common` - аннотации тестов
- `kotlinx-coroutines-test` - тестирование корутин
- `app.cash.sqldelight:sqlite-driver` - in-memory БД для тестов

## Best Practices

1. **Изоляция тестов** - каждый тест должен быть независимым
2. **Использование runTest** - для тестирования suspend функций
3. **Тестовые данные** - используйте TestDataFactory для создания данных
4. **Очистка** - закрывайте ресурсы в @AfterTest
5. **Именование** - используйте описательные имена тестов

## Покрытие тестами

### Текущее покрытие

- ✅ Use Cases (5/5)
- ✅ CameraRepositoryImpl (CRUD операции)
- ✅ CameraEntityMapper (конвертация)
- ✅ LicenseManager (базовая логика)

### Планируется

- ⏳ Integration тесты для API
- ⏳ E2E тесты
- ⏳ UI тесты для платформ

## CI/CD

Тесты автоматически запускаются в CI/CD pipeline:

```yaml
- name: Run tests
  run: ./gradlew testAll
```

## Troubleshooting

### Проблема: Тесты не компилируются

**Решение:** Убедитесь, что все зависимости добавлены в `build.gradle.kts`

### Проблема: Тесты падают с ошибкой БД

**Решение:** Используйте `TestDatabaseFactory` для создания in-memory БД

### Проблема: Тесты зависают

**Решение:** Убедитесь, что используете `runTest` для корутин и правильно закрываете ресурсы



