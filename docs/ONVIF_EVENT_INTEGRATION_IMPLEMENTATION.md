# Реализация интеграции ONVIF Event Integration Service

**Версия:** 1.0
**Дата:** 2026-01-26
**Статус:** ✅ Реализовано и интегрировано

---

## Обзор

Интеграция `OnvifEventIntegrationService` в приложение выполнена. Сервис автоматически подписывается на события от ONVIF камер и сохраняет их в `EventRepository`.

---

## Что было реализовано

### 1. Модуль DI для ONVIF сервисов

**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/di/OnvifServicesModule.kt`

Модуль предоставляет:
- `OnvifClient` - для работы с ONVIF камерами
- `OnvifEventService` - для работы с событиями
- `OnvifEventIntegrationService` - для автоматической интеграции событий
- `CameraEventMonitoringService` - для управления мониторингом

### 2. Обертка для CameraRepository

**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/CameraRepositoryWithEventMonitoring.kt`

Автоматически:
- Запускает мониторинг при добавлении камеры
- Перезапускает мониторинг при обновлении учетных данных
- Останавливает мониторинг при удалении камеры

### 3. Сервис управления мониторингом

**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/service/CameraEventMonitoringService.kt`

Предоставляет:
- `initialize()` - запуск мониторинга для всех камер при старте
- `shutdown()` - остановка мониторинга при завершении
- Методы для проверки статуса мониторинга

### 4. Интеграция в Android

**Файл:** `android/app/src/main/java/com/company/ipcamera/android/MainActivity.kt`

- Добавлен модуль `onvifServicesModule` в DI
- Инициализация мониторинга в `onCreate()`
- Остановка мониторинга в `onDestroy()`

### 5. Интеграция в Desktop

**Файлы:**
- `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/di/AppModule.kt`
- `platforms/client-desktop-arm/app/src/main/kotlin/com/company/ipcamera/desktop/di/AppModule.kt`
- `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/App.kt`

- Добавлен модуль `onvifServicesModule` в DI
- Инициализация мониторинга через `LaunchedEffect` в `App()`

---

## Как это работает

### Автоматический запуск мониторинга

1. **При добавлении камеры:**
   ```kotlin
   cameraRepository.addCamera(camera)
   // Автоматически запускается мониторинг событий
   ```

2. **При обновлении камеры:**
   ```kotlin
   cameraRepository.updateCamera(camera)
   // Если изменились учетные данные, мониторинг перезапускается
   ```

3. **При удалении камеры:**
   ```kotlin
   cameraRepository.removeCamera(cameraId)
   // Автоматически останавливается мониторинг
   ```

### При старте приложения

1. **Android:**
   - `MainActivity.onCreate()` вызывает `eventMonitoringService.initialize()`
   - Запускается мониторинг для всех существующих камер

2. **Desktop:**
   - `App()` использует `LaunchedEffect` для инициализации
   - Запускается мониторинг для всех существующих камер

### Обработка событий

1. `OnvifEventIntegrationService` получает события через PullPoint подписки
2. События маппятся в `Event` модель приложения
3. События сохраняются в `EventRepository`
4. События доступны через стандартные методы `EventRepository`

---

## Использование

### Получение событий

```kotlin
// В ViewModel или UseCase
val events = eventRepository.getEvents(
    cameraId = "camera-1",
    limit = 50
)

events.items.forEach { event ->
    println("Event: ${event.type} - ${event.description}")
}
```

### Проверка статуса мониторинга

```kotlin
val monitoringService: CameraEventMonitoringService = get()

// Проверить, активен ли мониторинг для камеры
val isMonitoring = monitoringService.isMonitoring("camera-1")

// Получить список камер с активным мониторингом
val monitoredCameras = monitoringService.getMonitoredCameras()
```

### Ручное управление мониторингом

```kotlin
val integrationService: OnvifEventIntegrationService = get()

// Запустить мониторинг вручную
integrationService.startMonitoring(
    cameraId = "camera-1",
    cameraName = "Camera",
    cameraUrl = "http://192.168.1.100",
    username = "admin",
    password = "password"
)

// Остановить мониторинг
integrationService.stopMonitoring("camera-1")
```

---

## Конфигурация

### Интервал опроса событий

По умолчанию используется интервал 5000 мс (5 секунд). Можно изменить при создании подписки:

```kotlin
integrationService.startMonitoring(
    cameraId = "camera-1",
    cameraName = "Camera",
    cameraUrl = "http://192.168.1.100",
    username = "admin",
    password = "password",
    pullInterval = 3000 // 3 секунды для более частых проверок
)
```

**Рекомендации:**
- **1000-2000 мс** - для камер с частыми событиями
- **5000 мс** - стандартное значение (баланс)
- **10000 мс** - для камер с редкими событиями (экономия ресурсов)

---

## Troubleshooting

### Мониторинг не запускается

**Проверьте:**
1. Камера поддерживает ONVIF Event Service
2. Учетные данные верны
3. Камера доступна по сети

**Решение:**
```kotlin
// Проверить capabilities камеры
val capabilities = onvifClient.getCapabilities(cameraUrl, username, password)
if (capabilities?.eventServiceUrl == null) {
    println("Camera does not support Event Service")
}
```

### События не сохраняются

**Проверьте:**
1. `EventRepository` правильно инициализирован
2. База данных доступна
3. Логи на наличие ошибок

**Решение:**
```kotlin
// Проверить события вручную
val events = eventRepository.getEvents(cameraId = "camera-1")
println("Found ${events.items.size} events")
```

### Высокая нагрузка на камеру

**Решение:**
- Увеличьте `pullInterval` до 10000 мс или больше
- Используйте фильтрацию событий на уровне камеры
- Мониторьте только активные камеры

---

## Производительность

### Оптимизация

1. **Используйте разумный интервал опроса**
   - Слишком частые опросы увеличивают нагрузку
   - Слишком редкие увеличивают задержку

2. **Мониторинг только активных камер**
   - Не запускайте мониторинг для отключенных камер
   - Останавливайте мониторинг при отключении камеры

3. **Фильтрация событий**
   - Используйте фильтры на уровне ONVIF для уменьшения трафика
   - Обрабатывайте только важные события

---

## Мониторинг и логирование

Все операции логируются через `mu.KotlinLogging`:

- `INFO` - успешные операции (запуск/остановка мониторинга)
- `WARN` - предупреждения (неудачные попытки запуска)
- `ERROR` - ошибки (исключения)
- `DEBUG` - детальная информация (получение событий)

---

## Тестирование

### Unit-тесты

Созданы тесты для:
- `OnvifEventIntegrationService` - тесты запуска/остановки мониторинга
- `OnvifExceptions` - тесты обработки ошибок
- `OnvifFaultParser` - тесты парсинга SOAP Fault

### Integration-тесты

Для тестирования с реальными камерами:
1. Настройте тестовую камеру
2. Запустите мониторинг
3. Проверьте, что события сохраняются в `EventRepository`

---

## Следующие шаги

### Возможные улучшения

1. **Уведомления о событиях**
   - Интеграция с системой уведомлений
   - Push-уведомления для критических событий

2. **Веб-интерфейс**
   - Отображение событий в реальном времени
   - Фильтрация и поиск событий

3. **Аналитика**
   - Статистика событий
   - Графики и отчеты

4. **Автоматические действия**
   - Реакция на события (запись, уведомления)
   - Правила обработки событий

---

## Связанные документы

- **[ONVIF_EVENT_INTEGRATION.md](ONVIF_EVENT_INTEGRATION.md)** - Документация по использованию
- **[ONVIF_EVENT_INTEGRATION_EXAMPLES.md](examples/ONVIF_EVENT_INTEGRATION_EXAMPLES.md)** - Примеры использования
- **[ONVIF_CLIENT_STAGE_4.4_DETAILS.md](../archive/docs/onvif/ONVIF_CLIENT_STAGE_4.4_DETAILS.md)** - Детализация этапа 4.4

---

**Последнее обновление:** 2026-01-26
