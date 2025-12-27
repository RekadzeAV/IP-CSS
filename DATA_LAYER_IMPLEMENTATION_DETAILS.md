# Детальное описание реализации Слоя данных

**Дата создания:** Январь 2025
**Версия:** 1.0
**Статус:** Актуально

---

## 📋 Оглавление

1. [Архитектура Data Sources](#архитектура-data-sources)
2. [LocalDataSource - Локальные источники данных](#localdatasource---локальные-источники-данных)
3. [RemoteDataSource - Сетевые источники данных](#remotedatasource---сетевые-источники-данных)
4. [Рефакторинг репозиториев](#рефакторинг-репозиториев)
5. [Dependency Injection](#dependency-injection)
6. [Стратегии работы с данными](#стратегии-работы-с-данными)
7. [Примеры использования](#примеры-использования)

---

## Архитектура Data Sources

### Общая концепция

Архитектура Data Sources разделяет ответственность между локальным и удаленным хранением данных, следуя принципам Clean Architecture:

```
┌─────────────────────────────────────────┐
│         Domain Layer                     │
│  (Use Cases, Domain Models)              │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Repository Layer                    │
│  (RepositoryImplV2)                      │
│  - Координирует работу с источниками     │
│  - Реализует стратегии синхронизации    │
└──────┬───────────────────┬──────────────┘
       │                   │
┌──────▼──────┐   ┌────────▼─────────┐
│ Local       │   │ Remote           │
│ DataSource  │   │ DataSource       │
│             │   │                  │
│ - SQLDelight│   │ - API Services   │
│ - Транзакции│   │ - DTO Mapping    │
│ - Batch ops │   │ - Error Handling │
└─────────────┘   └──────────────────┘
```

### Преимущества архитектуры

1. **Разделение ответственности** - четкое разделение между локальным и удаленным хранением
2. **Тестируемость** - каждый компонент можно тестировать изолированно
3. **Гибкость** - легко менять стратегии работы с данными
4. **Офлайн поддержка** - готовность к реализации офлайн режима
5. **Масштабируемость** - легко добавлять новые источники данных

---

## LocalDataSource - Локальные источники данных

### Общая структура

Все LocalDataSource реализуют единый паттерн:

```kotlin
interface CameraLocalDataSource {
    suspend fun getCameras(): List<Camera>
    suspend fun getCameraById(id: String): Camera?
    suspend fun saveCamera(camera: Camera): Result<Camera>
    suspend fun saveCameras(cameras: List<Camera>): Result<List<Camera>>
    suspend fun updateCamera(camera: Camera): Result<Camera>
    suspend fun deleteCamera(id: String): Result<Unit>
    suspend fun cameraExists(id: String): Boolean
}
```

### Реализованные LocalDataSource

#### 1. CameraLocalDataSourceImpl
- **Расположение:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/datasource/local/impl/CameraLocalDataSourceImpl.kt`
- **Особенности:**
  - Использует SQLDelight для работы с БД
  - Поддержка batch операций через транзакции
  - Методы проверки существования записей
  - Оптимизированные запросы с индексами

#### 2. RecordingLocalDataSourceImpl
- **Особенности:**
  - Фильтрация записей по дате, камере, статусу
  - Пагинация для больших списков
  - Batch операции для массового удаления

#### 3. EventLocalDataSourceImpl
- **Особенности:**
  - Массовые операции (acknowledge, delete)
  - Фильтрация по типу события, камере, статусу
  - Поддержка временных диапазонов

#### 4. UserLocalDataSourceImpl
- **Особенности:**
  - Методы по ролям пользователей
  - Фильтрация по активности
  - Batch операции

#### 5. SettingsLocalDataSourceImpl
- **Особенности:**
  - Работа с категориями настроек
  - Batch операции для массового обновления
  - Поддержка системных настроек

#### 6. NotificationLocalDataSourceImpl
- **Особенности:**
  - Фильтрация по типу, приоритету, статусу прочтения
  - Batch операции для массовой отметки как прочитанных
  - Пагинация

### Транзакции и Batch операции

Все LocalDataSource используют транзакции для обеспечения атомарности:

```kotlin
suspend fun saveCameras(cameras: List<Camera>): Result<List<Camera>> {
    return database.transactionWithResult {
        cameras.map { camera ->
            // Вставка в транзакции
            database.cameraQueries.insertCamera(...)
        }
    }
}
```

---

## RemoteDataSource - Сетевые источники данных

### Общая структура

Все RemoteDataSource используют единый паттерн с ApiResult:

```kotlin
interface CameraRemoteDataSource {
    suspend fun getCameras(): ApiResult<List<Camera>>
    suspend fun getCameraById(id: String): ApiResult<Camera>
    suspend fun createCamera(camera: Camera): ApiResult<Camera>
    suspend fun updateCamera(id: String, camera: Camera): ApiResult<Camera>
    suspend fun deleteCamera(id: String): ApiResult<Unit>
}
```

### Реализованные RemoteDataSource

#### 1. CameraRemoteDataSourceImpl
- **Расположение:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/datasource/remote/impl/CameraRemoteDataSourceImpl.kt`
- **Особенности:**
  - Использует CameraApiService для работы с API
  - Маппинг DTO -> Domain модели
  - Обработка ошибок через ApiResult

#### 2. RecordingRemoteDataSourceImpl
- **Особенности:**
  - Работа с RecordingApiService
  - Поддержка пагинации
  - Маппинг RecordingDto -> Recording

#### 3. EventRemoteDataSourceImpl
- **Особенности:**
  - Работа с EventApiService
  - Массовые операции через API
  - Фильтрация на стороне сервера

#### 4. UserRemoteDataSourceImpl
- **Особенности:**
  - Работа с UserApiService
  - Маппинг UserDto -> User
  - Поддержка ролей и прав доступа

#### 5. SettingsRemoteDataSourceImpl
- **Особенности:**
  - Работа с SettingsApiService
  - Поддержка SystemSettings
  - Batch операции через API

#### 6. NotificationRemoteDataSourceImpl
- **Особенности:**
  - Работа напрямую с ApiClient
  - Поддержка фильтрации и пагинации
  - Real-time обновления через WebSocket

### Маппинг DTO -> Domain

Все RemoteDataSource содержат мапперы для преобразования DTO в Domain модели:

```kotlin
private fun CameraDto.toDomain(): Camera {
    return Camera(
        id = this.id,
        name = this.name,
        url = this.url,
        // ... остальные поля
    )
}
```

### Обработка ошибок

Все методы возвращают `ApiResult<T>`, который обрабатывает успешные и ошибочные случаи:

```kotlin
suspend fun getCameras(): ApiResult<List<Camera>> {
    return try {
        val response = apiService.getCameras()
        ApiResult.Success(response.data.map { it.toDomain() })
    } catch (e: Exception) {
        ApiResult.Error(e)
    }
}
```

---

## Рефакторинг репозиториев

### Стратегия рефакторинга

Все V2 репозитории используют паттерн **local-first** с автоматической синхронизацией:

1. **Чтение данных:**
   - Сначала проверяется локальная БД
   - Если данных нет локально, запрашиваются с сервера
   - Полученные данные сохраняются локально для кэширования

2. **Запись данных:**
   - Сначала сохраняется локально
   - Затем синхронизируется с сервером (если доступен)
   - При ошибке синхронизации данные остаются локально

3. **Удаление данных:**
   - Удаляется локально
   - Затем удаляется на сервере (если доступен)
   - При ошибке локальное удаление сохраняется

### Реализованные V2 репозитории

#### 1. CameraRepositoryImplV2
- **Расположение:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/CameraRepositoryImplV2.kt`
- **Особенности:**
  - Local-first стратегия
  - Валидация входных данных через InputValidator
  - Автоматическая синхронизация с удаленным API
  - Fallback механизмы при ошибках сети
  - Интеграция с OnvifClient для discovery и testConnection

#### 2. RecordingRepositoryImplV2
- **Особенности:**
  - Поддержка фильтрации и пагинации
  - Синхронизация записей с сервером
  - Обработка больших объемов данных

#### 3. EventRepositoryImplV2
- **Особенности:**
  - Массовые операции
  - Фильтрация событий
  - Синхронизация статусов событий

#### 4. UserRepositoryImplV2
- **Особенности:**
  - Работа с ролями пользователей
  - Синхронизация профилей
  - Управление правами доступа

#### 5. SettingsRepositoryImplV2
- **Особенности:**
  - Работа с категориями настроек
  - Синхронизация системных настроек
  - Batch операции

### Пример использования

```kotlin
class CameraRepositoryImplV2(
    private val localDataSource: CameraLocalDataSource,
    private val remoteDataSource: CameraRemoteDataSource? = null
) : CameraRepository {

    override suspend fun getCameras(): List<Camera> {
        val localCameras = localDataSource.getCameras()

        if (localCameras.isEmpty() && remoteDataSource != null) {
            // Если локально пусто, получаем с сервера
            remoteDataSource.getCameras().fold(
                onSuccess = { remoteCameras ->
                    // Сохраняем локально для кэширования
                    localDataSource.saveCameras(remoteCameras).getOrNull()
                    remoteCameras
                },
                onError = { error ->
                    logger.warn(error) { "Failed to get cameras from remote" }
                    localCameras
                }
            )
        } else {
            localCameras
        }
    }
}
```

---

## Dependency Injection

### DataSourcesModule

Модуль настроен для работы на всех платформах:

```kotlin
val dataSourcesModule = module {
    // Local Data Sources (всегда доступны)
    single<CameraLocalDataSource> { CameraLocalDataSourceImpl(get<DatabaseFactory>()) }
    // ... остальные LocalDataSource

    // Remote Data Sources (опционально, только если ApiClient доступен)
    single<CameraRemoteDataSource?> {
        try {
            val cameraApiService: CameraApiService = get<CameraApiService>()
            CameraRemoteDataSourceImpl(cameraApiService)
        } catch (e: Exception) {
            null // ApiClient недоступен (например, на сервере)
        }
    }
    // ... остальные RemoteDataSource
}
```

### Особенности DI конфигурации

1. **LocalDataSource** - всегда создаются, так как БД доступна на всех платформах
2. **RemoteDataSource** - опциональны (nullable), создаются только если ApiClient доступен
3. **Гибкость** - модуль работает и на сервере (где RemoteDataSource не нужны), и на клиентах

### Интеграция в AppModule

**Статус:** ⚠️ Требуется интеграция

DataSourcesModule создан, но еще не добавлен в платформенные AppModule:
- Android AppModule
- Desktop AppModule (x86_64, ARM)
- Server AppModule

**План интеграции:**
1. Добавить `dataSourcesModule` в список модулей Koin
2. Заменить старые репозитории на V2 версии
3. Протестировать на всех платформах

---

## Стратегии работы с данными

### Доступные стратегии

V2 репозитории поддерживают несколько стратегий:

```kotlin
enum class DataSourceStrategy {
    LOCAL_ONLY,      // Только локальная БД
    REMOTE_ONLY,     // Только удаленный API
    LOCAL_FIRST,     // Сначала локальная, затем удаленная (fallback)
    REMOTE_FIRST     // Сначала удаленная, затем локальная (fallback)
}
```

### Local-First (используется по умолчанию)

**Преимущества:**
- Быстрый отклик (данные из локальной БД)
- Работа в офлайн режиме
- Меньше нагрузки на сервер
- Кэширование данных

**Недостатки:**
- Возможны устаревшие данные
- Требуется синхронизация

### Remote-First

**Преимущества:**
- Всегда актуальные данные
- Централизованное управление

**Недостатки:**
- Требуется сетевое соединение
- Медленнее отклик
- Больше нагрузка на сервер

---

## Примеры использования

### Использование LocalDataSource напрямую

```kotlin
class SomeService(
    private val cameraLocalDataSource: CameraLocalDataSource
) {
    suspend fun getLocalCameras(): List<Camera> {
        return cameraLocalDataSource.getCameras()
    }

    suspend fun saveCamera(camera: Camera): Result<Camera> {
        return cameraLocalDataSource.saveCamera(camera)
    }
}
```

### Использование RemoteDataSource напрямую

```kotlin
class SyncService(
    private val cameraRemoteDataSource: CameraRemoteDataSource
) {
    suspend fun syncCameras(): Result<List<Camera>> {
        return cameraRemoteDataSource.getCameras().fold(
            onSuccess = { Result.success(it) },
            onError = { Result.failure(it) }
        )
    }
}
```

### Использование V2 репозитория

```kotlin
class CameraUseCase(
    private val cameraRepository: CameraRepository // V2 версия
) {
    suspend fun getAllCameras(): List<Camera> {
        // Репозиторий автоматически использует local-first стратегию
        return cameraRepository.getCameras()
    }

    suspend fun addCamera(camera: Camera): Result<Camera> {
        // Автоматически сохраняется локально и синхронизируется с сервером
        return cameraRepository.addCamera(camera)
    }
}
```

---

## Тестирование

### Unit тесты для LocalDataSource

```kotlin
class CameraLocalDataSourceTest {
    @Test
    fun `test save and get camera`() = runTest {
        val dataSource = CameraLocalDataSourceImpl(databaseFactory)
        val camera = createTestCamera()

        val result = dataSource.saveCamera(camera)
        assertTrue(result.isSuccess)

        val retrieved = dataSource.getCameraById(camera.id)
        assertEquals(camera, retrieved)
    }
}
```

### Unit тесты для RemoteDataSource

```kotlin
class CameraRemoteDataSourceTest {
    @Test
    fun `test get cameras from API`() = runTest {
        val mockApiService = mockk<CameraApiService>()
        val dataSource = CameraRemoteDataSourceImpl(mockApiService)

        coEvery { mockApiService.getCameras() } returns ApiResponse(
            data = listOf(createTestCameraDto()),
            success = true
        )

        val result = dataSource.getCameras()
        assertTrue(result is ApiResult.Success)
    }
}
```

### Интеграционные тесты для V2 репозиториев

```kotlin
class CameraRepositoryImplV2Test {
    @Test
    fun `test local-first strategy`() = runTest {
        val localDataSource = CameraLocalDataSourceImpl(databaseFactory)
        val remoteDataSource = mockk<CameraRemoteDataSource>()
        val repository = CameraRepositoryImplV2(localDataSource, remoteDataSource)

        // Сохраняем локально
        localDataSource.saveCamera(createTestCamera())

        // Получаем через репозиторий (должен вернуть локальные данные)
        val cameras = repository.getCameras()
        assertEquals(1, cameras.size)

        // RemoteDataSource не должен вызываться, так как данные есть локально
        coVerify(exactly = 0) { remoteDataSource.getCameras() }
    }
}
```

---

## Миграция с старых репозиториев

### Поэтапная миграция

1. **Этап 1:** Создать V2 репозитории (✅ Завершено)
2. **Этап 2:** Добавить DataSourcesModule в DI (⏳ В процессе)
3. **Этап 3:** Заменить старые репозитории на V2 в DI (⏳ Запланировано)
4. **Этап 4:** Протестировать на всех платформах (⏳ Запланировано)
5. **Этап 5:** Удалить старые репозитории (⏳ Запланировано)

### Обратная совместимость

Старые репозитории остаются в коде для обратной совместимости до полной миграции.

---

## Следующие шаги

1. ✅ Создать все Data Sources - **ЗАВЕРШЕНО**
2. ✅ Создать V2 репозитории - **ЗАВЕРШЕНО (5/6)**
3. ⏳ Создать NotificationRepositoryImplV2
4. ⏳ Интегрировать DataSourcesModule в платформенные AppModule
5. ⏳ Мигрировать на V2 репозитории в DI
6. ⏳ Написать unit тесты для всех компонентов
7. ⏳ Настроить систему миграций БД

---

**Последнее обновление:** Январь 2025
**Версия документа:** 1.0

