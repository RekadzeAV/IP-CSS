# Руководство по оптимизации Desktop приложения

**Дата создания:** 2026-01-26
**Версия:** 1.0
**Статус:** Реализовано (Этап 8)

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)
> **📋 Связанные документы:**
> - **[DESKTOP_DETAILED_PLAN.md](DESKTOP_DETAILED_PLAN.md)** - Детальный план реализации Desktop приложения
> - **[DESKTOP_PLAN_SUMMARY.md](DESKTOP_PLAN_SUMMARY.md)** - Краткая сводка плана
> - **[DESKTOP_REFINEMENT_PLAN.md](DESKTOP_REFINEMENT_PLAN.md)** - План доработки незавершенных задач
> - **[DESKTOP_IMPLEMENTATION_PLAN.md](DESKTOP_IMPLEMENTATION_PLAN.md)** - Базовый план реализации
> - **[platforms/client-desktop-x86_64/IMPLEMENTATION_STATUS.md](../platforms/client-desktop-x86_64/IMPLEMENTATION_STATUS.md)** - Текущий статус реализации

---

## 📋 Содержание

1. [Обзор оптимизаций](#обзор-оптимизаций)
2. [Система кэширования](#система-кэширования)
3. [Улучшение состояний UI](#улучшение-состояний-ui)
4. [Анимации и переходы](#анимации-и-переходы)
5. [Компоненты обратной связи](#компоненты-обратной-связи)
6. [Оптимизация ViewModels](#оптимизация-viewmodels)
7. [Тестирование](#тестирование)
8. [Best Practices](#best-practices)

---

## Обзор оптимизаций

Этап 8 включает следующие оптимизации:

### ✅ Реализовано

1. **Система кэширования данных**
   - `CacheManager` для управления кэшем
   - TTL (Time To Live) для кэшированных данных
   - Автоматическая инвалидация кэша

2. **Улучшенные состояния UI**
   - Унифицированный `UiState<T>` с поддержкой Loading, Success, Error, Empty
   - Компонент `StateContent` для автоматической обработки состояний
   - Компонент `EmptyView` для пустых состояний

3. **Анимации и переходы**
   - Плавные переходы между экранами
   - Анимации появления/исчезновения элементов
   - Анимированные компоненты обратной связи

4. **Компоненты обратной связи**
   - `ToastMessage` для уведомлений
   - Улучшенный `ErrorView` с анимациями
   - `EmptyView` для пустых состояний

5. **Оптимизация ViewModels**
   - Использование кэша для быстрой загрузки данных
   - Фоновая синхронизация данных
   - Правильная инвалидация кэша при изменениях

6. **Тестирование**
   - Unit тесты для ViewModels
   - Тесты кэширования
   - Тесты обработки состояний

---

## Система кэширования

### CacheManager

`CacheManager` - централизованная система кэширования данных.

**Расположение:** `com.company.ipcamera.desktop.data.CacheManager`

**Основные возможности:**

```kotlin
// Сохранение данных в кэш
cacheManager.put(CacheKeys.CAMERAS, cameras, ttl = 5.minutes)

// Получение данных из кэша
val cameras: List<Camera>? = cacheManager.get(CacheKeys.CAMERAS)

// Проверка наличия данных
if (cacheManager.contains(CacheKeys.CAMERAS)) {
    // Данные есть в кэше
}

// Инвалидация кэша
cacheManager.invalidate(CacheKeys.CAMERAS)
cacheManager.invalidateAll() // Очистить весь кэш
```

### Ключи кэша

Используйте константы из `CacheKeys`:

```kotlin
CacheKeys.CAMERAS           // Список камер
CacheKeys.camera(id)        // Конкретная камера
CacheKeys.SETTINGS          // Настройки
CacheKeys.EVENTS            // События
CacheKeys.RECORDINGS        // Записи
```

### Использование в ViewModels

```kotlin
class CamerasViewModel(
    private val getCamerasUseCase: GetCamerasUseCase,
    private val cacheManager: CacheManager
) {
    fun loadCameras(useCache: Boolean = true) {
        // Проверяем кэш
        if (useCache) {
            val cached: List<Camera>? = cacheManager.get(CacheKeys.CAMERAS)
            if (cached != null) {
                _uiState.value = UiState.Success(cached)
                // Загружаем свежие данные в фоне
                loadFromNetwork()
                return
            }
        }

        // Загружаем данные
        val cameras = getCamerasUseCase.invoke()
        cacheManager.put(CacheKeys.CAMERAS, cameras, ttl = 5.minutes)
        _uiState.value = UiState.Success(cameras)
    }
}
```

---

## Улучшение состояний UI

### UiState<T>

Унифицированный sealed class для состояний UI:

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
    data class Empty(val message: String = "Нет данных") : UiState<Nothing>()
}
```

### StateContent

Компонент для автоматической обработки состояний:

```kotlin
StateContent(
    state = uiState,
    onRetry = { viewModel.loadData() },
    emptyMessage = "Нет камер",
    loadingMessage = "Загрузка камер...",
    content = { cameras ->
        // Отображение данных
        LazyColumn {
            items(cameras) { camera ->
                CameraCard(camera)
            }
        }
    }
)
```

### EmptyView

Компонент для отображения пустых состояний:

```kotlin
EmptyView(
    message = "Нет камер",
    icon = Icons.Default.Camera
)
```

---

## Анимации и переходы

### Переходы между экранами

Навигация использует `AnimatedContent` для плавных переходов:

```kotlin
AnimatedContent(
    targetState = selectedScreen,
    transitionSpec = {
        fadeIn(animationSpec = tween(300)) + slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300)
        ) togetherWith fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300)
        )
    }
) { screen ->
    // Отображение экрана
}
```

### Анимации компонентов

Компоненты используют анимации для улучшения UX:

- `ErrorView` - анимация появления с fadeIn/slideIn
- `EmptyView` - анимация появления
- `ToastMessage` - анимация slideIn/slideOut

---

## Компоненты обратной связи

### ToastMessage

Система уведомлений для пользователя:

```kotlin
val toastState = rememberToastState()

// Показать уведомление
toastState.showSuccess("Камера добавлена")
toastState.showError("Ошибка подключения")
toastState.showInfo("Информация")
toastState.showWarning("Предупреждение")

// В UI
ToastMessage(
    state = toastState.currentState,
    onDismiss = { toastState.dismiss() }
)
```

### Типы уведомлений

- `SUCCESS` - успешная операция (зеленый)
- `ERROR` - ошибка (красный)
- `INFO` - информация (синий)
- `WARNING` - предупреждение (желтый)

---

## Оптимизация ViewModels

### Использование кэша

1. **Проверка кэша перед загрузкой:**
   ```kotlin
   if (useCache) {
       val cached = cacheManager.get(key)
       if (cached != null) {
           _uiState.value = UiState.Success(cached)
           loadFromNetwork() // Фоновая синхронизация
           return
       }
   }
   ```

2. **Сохранение в кэш после загрузки:**
   ```kotlin
   val data = useCase.invoke()
   cacheManager.put(key, data, ttl = 5.minutes)
   _uiState.value = UiState.Success(data)
   ```

3. **Инвалидация кэша при изменениях:**
   ```kotlin
   fun deleteItem(id: String) {
       deleteUseCase.invoke(id)
       cacheManager.invalidate(key)
       loadData(useCache = false)
   }
   ```

### Фоновая синхронизация

Для улучшения UX данные из кэша показываются сразу, а свежие данные загружаются в фоне:

```kotlin
private suspend fun loadFromNetwork() {
    try {
        val data = useCase.invoke()
        cacheManager.put(key, data, ttl = 5.minutes)
        _uiState.value = UiState.Success(data)
    } catch (e: Exception) {
        // Ошибка в фоне - не обновляем состояние, оставляем кэш
    }
}
```

---

## Тестирование

### Unit тесты для ViewModels

Пример теста для `CamerasViewModel`:

```kotlin
@Test
fun `loadCameras should emit Loading then Success when cameras are loaded`() = runTest {
    // Arrange
    val cameras = listOf(/* ... */)
    whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

    val viewModel = CamerasViewModel(/* ... */)

    // Act
    viewModel.loadCameras(useCache = false)
    advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    assertTrue(state is CamerasUiState.Success)
    assertEquals(cameras, (state as CamerasUiState.Success).cameras)
}
```

### Тестирование кэша

```kotlin
@Test
fun `loadCameras should use cache when available`() = runTest {
    // Arrange
    val cached = listOf(/* ... */)
    cacheManager.put(CacheKeys.CAMERAS, cached)

    // Act
    viewModel.loadCameras(useCache = true)
    advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    assertTrue(state is CamerasUiState.Success)
    assertEquals(cached, (state as CamerasUiState.Success).cameras)
}
```

---

## Best Practices

### 1. Использование кэша

✅ **Хорошо:**
- Использовать кэш для данных, которые редко меняются
- Устанавливать разумный TTL (5-10 минут)
- Инвалидировать кэш при изменениях

❌ **Плохо:**
- Кэшировать данные, которые часто меняются
- Использовать слишком долгий TTL
- Забывать инвалидировать кэш

### 2. Обработка состояний

✅ **Хорошо:**
- Использовать `UiState<T>` для всех состояний
- Использовать `StateContent` для автоматической обработки
- Показывать понятные сообщения об ошибках

❌ **Плохо:**
- Смешивать разные типы состояний
- Показывать технические ошибки пользователю
- Игнорировать пустые состояния

### 3. Анимации

✅ **Хорошо:**
- Использовать плавные переходы (300ms)
- Анимировать важные изменения состояния
- Использовать стандартные анимации Material Design

❌ **Плохо:**
- Слишком быстрые или медленные анимации
- Анимировать все подряд
- Использовать нестандартные анимации

### 4. Обратная связь

✅ **Хорошо:**
- Показывать Toast для успешных операций
- Показывать ошибки с возможностью повтора
- Использовать правильные типы уведомлений

❌ **Плохо:**
- Показывать слишком много уведомлений
- Использовать неправильные типы уведомлений
- Игнорировать ошибки пользователя

---

## Примеры использования

### Полный пример ViewModel с кэшированием

```kotlin
class CamerasViewModel(
    private val getCamerasUseCase: GetCamerasUseCase,
    private val cacheManager: CacheManager
) {
    private val _uiState = MutableStateFlow<UiState<List<Camera>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Camera>>> = _uiState.asStateFlow()

    init {
        loadCameras(useCache = true)
    }

    fun loadCameras(useCache: Boolean = true) {
        coroutineScope.launch {
            // Проверяем кэш
            if (useCache) {
                val cached: List<Camera>? = cacheManager.get(CacheKeys.CAMERAS)
                if (cached != null && cached.isNotEmpty()) {
                    _uiState.value = UiState.Success(cached)
                    loadFromNetwork()
                    return@launch
                }
            }

            _uiState.value = UiState.Loading
            try {
                val cameras = getCamerasUseCase.invoke()
                cacheManager.put(CacheKeys.CAMERAS, cameras, ttl = 5.minutes)

                if (cameras.isEmpty()) {
                    _uiState.value = UiState.Empty("Нет камер")
                } else {
                    _uiState.value = UiState.Success(cameras)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    private suspend fun loadFromNetwork() {
        try {
            val cameras = getCamerasUseCase.invoke()
            cacheManager.put(CacheKeys.CAMERAS, cameras, ttl = 5.minutes)
            if (cameras.isNotEmpty()) {
                _uiState.value = UiState.Success(cameras)
            }
        } catch (e: Exception) {
            // Ошибка в фоне - не обновляем состояние
        }
    }
}
```

### Пример использования в UI

```kotlin
@Composable
fun CamerasScreen(viewModel: CamerasViewModel = koinInject()) {
    val uiState by viewModel.uiState.collectAsState()
    val toastState = rememberToastState()

    StateContent(
        state = uiState,
        onRetry = { viewModel.loadCameras() },
        emptyMessage = "Нет камер",
        loadingMessage = "Загрузка камер...",
        content = { cameras ->
            LazyColumn {
                items(cameras) { camera ->
                    CameraCard(camera)
                }
            }
        }
    )

    ToastMessage(
        state = toastState.currentState,
        onDismiss = { toastState.dismiss() }
    )
}
```

---

## Следующие шаги

1. **Оптимизация рендеринга видео** (Этап 4)
   - Hardware acceleration
   - Оптимизация буферизации
   - Многокамерный просмотр

2. **Ленивая загрузка компонентов**
   - Ленивая загрузка экранов
   - Виртуализация списков
   - Отложенная инициализация

3. **Мониторинг производительности**
   - Профилирование приложения
   - Измерение времени загрузки
   - Оптимизация памяти

---

**Последнее обновление:** 2026-01-26
**Автор:** Development Team
