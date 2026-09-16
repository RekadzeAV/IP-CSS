# План разработки Android-приложения
## Детальный пошаговый план реализации задач

**Дата создания:** 2026-01-XX
**Статус:** В разработке

---

## Обзор задач

### Текущее состояние:
1. **NotificationsScreen** — экран отсутствует (0% → цель: 100%)
2. **VideoPlayer компонент** — заглушка (60% → цель: 100%)
3. **Фоновая работа** — сервисы отсутствуют (0% → цель: 100%)
   - RecordingService
   - CameraMonitoringService
4. **Android Keystore** — отсутствует (30% → цель: 100%)

---

## Задача 1: NotificationsScreen для Android

### Цель
Создать полнофункциональный экран уведомлений с поддержкой:
- Отображение списка уведомлений
- Фильтрация по типу и статусу
- Отметка как прочитанные
- Удаление уведомлений
- Навигация к связанным объектам (камеры, события, записи)

### Шаги реализации

#### Шаг 1.1: Создание модели данных
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/model/Notification.kt`

- [ ] Определить data class `Notification`:
  - `id: String`
  - `title: String`
  - `message: String`
  - `type: NotificationType` (enum: CAMERA_ALERT, RECORDING_COMPLETE, EVENT_DETECTED, SYSTEM)
  - `severity: NotificationSeverity` (enum: INFO, WARNING, ERROR, CRITICAL)
  - `cameraId: String?`
  - `eventId: String?`
  - `recordingId: String?`
  - `read: Boolean`
  - `timestamp: Long`
  - `actionUrl: String?`

#### Шаг 1.2: Создание Use Case
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/usecase/notification/`

- [ ] `GetNotificationsUseCase.kt` — получение списка уведомлений
- [ ] `MarkNotificationReadUseCase.kt` — отметка как прочитанное
- [ ] `DeleteNotificationUseCase.kt` — удаление уведомления
- [ ] `GetUnreadCountUseCase.kt` — получение количества непрочитанных

#### Шаг 1.3: Создание Repository
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/NotificationRepository.kt`

- [ ] Интерфейс `NotificationRepository`:
  - `getNotifications(filter: NotificationFilter?): Flow<List<Notification>>`
  - `markAsRead(notificationId: String): Result<Unit>`
  - `deleteNotification(notificationId: String): Result<Unit>`
  - `getUnreadCount(): Flow<Int>`

- [ ] Реализация `NotificationRepositoryImpl`:
  - Интеграция с локальной БД (SQLDelight)
  - Интеграция с API (если требуется)
  - Кэширование данных

#### Шаг 1.4: Создание Data Source
**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/local/NotificationDataSource.kt`

- [ ] Локальное хранение уведомлений в SQLDelight
- [ ] CRUD операции
- [ ] Поддержка фильтрации

#### Шаг 1.5: Создание ViewModel
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/viewmodel/NotificationsViewModel.kt`

- [ ] `NotificationsViewModel`:
  - `uiState: StateFlow<NotificationsUiState>`
  - `loadNotifications()`
  - `markAsRead(notificationId: String)`
  - `deleteNotification(notificationId: String)`
  - `setFilter(filter: NotificationFilter)`
  - `refresh()`

- [ ] `NotificationsUiState`:
  - `notifications: List<Notification>`
  - `isLoading: Boolean`
  - `error: String?`
  - `filter: NotificationFilter?`
  - `unreadCount: Int`

#### Шаг 1.6: Создание UI компонентов
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/screens/notifications/NotificationsScreen.kt`

- [ ] `NotificationsScreen` Composable:
  - TopAppBar с заголовком и счетчиком непрочитанных
  - Фильтры (FilterChip для типа и статуса)
  - LazyColumn со списком уведомлений
  - Pull-to-refresh
  - Empty state
  - Error state

- [ ] `NotificationItem` Composable:
  - Отображение иконки по типу
  - Заголовок и сообщение
  - Время создания
  - Индикатор непрочитанного
  - Swipe-to-delete
  - Click для навигации

#### Шаг 1.7: Добавление навигации
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/navigation/AppNavigation.kt`

- [ ] Добавить route `"notifications"`
- [ ] Добавить composable для NotificationsScreen
- [ ] Добавить навигацию из других экранов (Settings, CameraDetail)

#### Шаг 1.8: Интеграция в DI
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/di/AppModule.kt`

- [ ] Добавить `viewModel { NotificationsViewModel(get()) } }`

#### Шаг 1.8: Тестирование
- [ ] Unit тесты для ViewModel
- [ ] UI тесты для экрана
- [ ] Интеграционные тесты

---

## Задача 2: VideoPlayer компонент для Android

### Цель
Доработать ExoVideoPlayer до полнофункционального видеоплеера с:
- Поддержкой RTSP потоков
- Поддержкой HLS потоков
- Управлением воспроизведением (play/pause/stop)
- Обработкой ошибок и переподключением
- Настройками качества
- Picture-in-Picture режимом
- Фоновым воспроизведением

### Шаги реализации

#### Шаг 2.1: Анализ текущей реализации
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/components/ExoVideoPlayer.kt`

- [x] Изучить текущий код (уже изучен)
- [ ] Определить недостающие функции:
  - Управление качеством потока
  - Picture-in-Picture
  - Фоновое воспроизведение
  - Сохранение позиции воспроизведения
  - Статистика воспроизведения

#### Шаг 2.2: Улучшение RTSP поддержки
- [ ] Добавить RTSPDataSourceFactory для лучшей поддержки RTSP
- [ ] Настроить таймауты и retry логику
- [ ] Оптимизировать буферизацию для RTSP

#### Шаг 2.3: Добавление управления качеством
- [ ] Определить доступные качества потока
- [ ] Добавить UI для выбора качества
- [ ] Реализовать переключение качества на лету

#### Шаг 2.4: Picture-in-Picture (PiP) режим
- [ ] Добавить разрешение в AndroidManifest.xml:
  ```xml
  <activity android:supportsPictureInPicture="true" />
  ```
- [ ] Реализовать `onPictureInPictureModeChanged` в MainActivity
- [ ] Добавить кнопку PiP в UI плеера
- [ ] Сохранять состояние плеера при переходе в PiP

#### Шаг 2.5: Фоновое воспроизведение
- [ ] Создать `MediaSession` для управления воспроизведением
- [ ] Добавить `MediaSessionService` (опционально)
- [ ] Интеграция с уведомлениями медиа-контроллера
- [ ] Обработка аудио-фокуса

#### Шаг 2.6: Улучшение обработки ошибок
- [ ] Детальная классификация ошибок
- [ ] Автоматическое переподключение с экспоненциальной задержкой
- [ ] UI для отображения ошибок и retry
- [ ] Логирование ошибок

#### Шаг 2.7: Статистика и мониторинг
- [ ] Добавить callback для статистики:
  - FPS
  - Bitrate
  - Buffer health
  - Network speed
- [ ] Отображение статистики в debug режиме

#### Шаг 2.8: Интеграция с VideoViewScreen
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/screens/video/VideoViewScreen.kt`

- [ ] Заменить заглушку на полнофункциональный ExoVideoPlayer
- [ ] Добавить управление воспроизведением
- [ ] Добавить настройки качества
- [ ] Добавить кнопку PiP

#### Шаг 2.9: Тестирование
- [ ] Тестирование RTSP потоков
- [ ] Тестирование HLS потоков
- [ ] Тестирование PiP режима
- [ ] Тестирование фонового воспроизведения
- [ ] Тестирование обработки ошибок

---

## Задача 3: Фоновая работа Android

### Цель
Реализовать фоновые сервисы для:
- Записи видео с камер (RecordingService)
- Мониторинга состояния камер (CameraMonitoringService)

### Задача 3.1: RecordingService

#### Шаг 3.1.1: Создание Foreground Service
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/service/RecordingService.kt`

- [ ] Создать класс `RecordingService : ForegroundService`:
  - Наследование от `androidx.work.Worker` или `Service`
  - Реализация `onStartCommand()`
  - Создание уведомления для foreground service
  - Обработка жизненного цикла

#### Шаг 3.1.2: Разрешения и конфигурация
**Файл:** `android/app/src/main/AndroidManifest.xml`

- [ ] Добавить разрешения:
  ```xml
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />
  ```

- [ ] Зарегистрировать сервис:
  ```xml
  <service
      android:name=".service.RecordingService"
      android:enabled="true"
      android:exported="false"
      android:foregroundServiceType="camera" />
  ```

#### Шаг 3.1.3: Интеграция с VideoRecordingService
- [ ] Использовать `VideoRecordingService` из shared модуля
- [ ] Создать обертку для Android-специфичной логики
- [ ] Управление жизненным циклом записи

#### Шаг 3.1.4: Уведомления
- [ ] Создать канал уведомлений для записи
- [ ] Отображать прогресс записи
- [ ] Кнопки управления (пауза/стоп) в уведомлении
- [ ] Обновление уведомления при изменении состояния

#### Шаг 3.1.5: Управление записью
- [ ] `startRecording(cameraId: String, config: RecordingConfig)`
- [ ] `stopRecording(recordingId: String)`
- [ ] `pauseRecording(recordingId: String)`
- [ ] `resumeRecording(recordingId: String)`
- [ ] `getActiveRecordings(): List<Recording>`

#### Шаг 3.1.6: Обработка ошибок
- [ ] Обработка сетевых ошибок
- [ ] Обработка нехватки места на диске
- [ ] Автоматическая остановка при критических ошибках
- [ ] Уведомления об ошибках

#### Шаг 3.1.7: Сохранение состояния
- [ ] Сохранение активных записей в SharedPreferences или БД
- [ ] Восстановление записей после перезапуска
- [ ] Обработка перезапуска сервиса

### Задача 3.2: CameraMonitoringService

#### Шаг 3.2.1: Создание Background Service
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/service/CameraMonitoringService.kt`

- [ ] Создать класс `CameraMonitoringService : Service`:
  - Использовать WorkManager для периодических задач
  - Или использовать AlarmManager для точного расписания
  - Реализация мониторинга состояния камер

#### Шаг 3.2.2: Разрешения
**Файл:** `android/app/src/main/AndroidManifest.xml`

- [ ] Зарегистрировать сервис (если используется Service)
- [ ] Или настроить WorkManager (рекомендуется)

#### Шаг 3.2.3: Логика мониторинга
- [ ] Периодическая проверка доступности камер (каждые 5-15 минут)
- [ ] Проверка RTSP потоков
- [ ] Обнаружение событий (движение, звук и т.д.)
- [ ] Проверка состояния записей

#### Шаг 3.2.4: Уведомления о событиях
- [ ] Создать канал уведомлений для мониторинга
- [ ] Отправка уведомлений при обнаружении событий
- [ ] Группировка уведомлений
- [ ] Действия в уведомлениях (открыть камеру, начать запись)

#### Шаг 3.2.5: Интеграция с EventApiService
- [ ] Использование `EventApiService` для получения событий
- [ ] Кэширование последних проверок
- [ ] Обработка сетевых ошибок

#### Шаг 3.2.6: Оптимизация батареи
- [ ] Использование JobScheduler/WorkManager
- [ ] Адаптивная частота проверок (реже при неактивности)
- [ ] Doze mode и App Standby совместимость

#### Шаг 3.2.7: Сохранение состояния
- [ ] Сохранение последнего времени проверки
- [ ] Сохранение состояния камер
- [ ] Восстановление после перезапуска

### Задача 3.3: Интеграция сервисов

#### Шаг 3.3.1: Dependency Injection
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/di/AppModule.kt`

- [ ] Добавить сервисы в DI контейнер
- [ ] Создать фабрики для сервисов

#### Шаг 3.3.2: Управление сервисами
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/service/ServiceManager.kt`

- [ ] Создать `ServiceManager` для управления сервисами:
  - `startRecordingService()`
  - `stopRecordingService()`
  - `startMonitoringService()`
  - `stopMonitoringService()`
  - `isServiceRunning(serviceClass: Class<*>): Boolean`

#### Шаг 3.3.3: UI для управления
- [ ] Добавить переключатели в SettingsScreen
- [ ] Отображение статуса сервисов
- [ ] Управление из CameraDetailScreen

#### Шаг 3.3.4: Тестирование
- [ ] Unit тесты для сервисов
- [ ] Интеграционные тесты
- [ ] Тестирование жизненного цикла
- [ ] Тестирование на разных версиях Android

---

## Задача 4: Android Keystore

### Цель
Реализовать безопасное хранение чувствительных данных:
- Пароли камер
- API ключи
- Токены аутентификации
- Криптографические ключи

### Шаги реализации

#### Шаг 4.1: Создание Keystore Manager
**Файл:** `android/app/src/main/java/com/company/ipcamera/android/security/AndroidKeystoreManager.kt`

- [ ] Создать класс `AndroidKeystoreManager`:
  - Использование `AndroidKeyStore`
  - Генерация ключей
  - Шифрование/дешифрование данных
  - Управление ключами

#### Шаг 4.2: Генерация ключей
- [ ] Метод `generateKey(alias: String)`
- [ ] Использование `KeyPairGenerator` с `KeyGenParameterSpec`
- [ ] Настройка параметров:
  - Алгоритм: RSA или AES
  - Размер ключа: 2048 бит (RSA) или 256 бит (AES)
  - Блокировка на экране блокировки (опционально)
  - Требование аутентификации (опционально)

#### Шаг 4.3: Шифрование данных
- [ ] Метод `encrypt(alias: String, data: String): String`
- [ ] Использование `Cipher` для шифрования
- [ ] Base64 кодирование результата

#### Шаг 4.4: Дешифрование данных
- [ ] Метод `decrypt(alias: String, encryptedData: String): String`
- [ ] Обработка ошибок (ключ не найден, данные повреждены)
- [ ] Безопасная очистка данных из памяти

#### Шаг 4.5: Управление ключами
- [ ] `keyExists(alias: String): Boolean`
- [ ] `deleteKey(alias: String): Boolean`
- [ ] `listKeys(): List<String>`
- [ ] Обработка исключений

#### Шаг 4.6: Интеграция с shared модулем
**Файл:** `core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.android.kt`

- [ ] Создать Android-специфичную реализацию `PasswordEncryption`
- [ ] Использовать `AndroidKeystoreManager` для хранения ключей
- [ ] Реализовать методы:
  - `encryptPassword(password: String): String`
  - `decryptPassword(encryptedPassword: String): String`

#### Шаг 4.7: Использование в репозиториях
- [ ] Интеграция с `CameraRepository` для хранения паролей камер
- [ ] Автоматическое шифрование при сохранении
- [ ] Автоматическое дешифрование при чтении

#### Шаг 4.8: Миграция существующих данных
- [ ] Скрипт миграции для существующих паролей
- [ ] Шифрование данных при первом запуске
- [ ] Обработка ошибок миграции

#### Шаг 4.9: Тестирование
- [ ] Unit тесты для KeystoreManager
- [ ] Тестирование шифрования/дешифрования
- [ ] Тестирование на разных версиях Android
- [ ] Тестирование безопасности (попытки взлома)

#### Шаг 4.10: Документация
- [ ] Документация по использованию
- [ ] Рекомендации по безопасности
- [ ] Best practices

---

## Порядок реализации

### Фаза 1: Базовая функциональность (Неделя 1) - ✅ ВЫПОЛНЕНО
1. ✅ NotificationsScreen (Шаги 1.1-1.7) - **100% ВЫПОЛНЕНО**
   - ✅ ViewModel создан
   - ✅ UI экран создан
   - ✅ Навигация добавлена
   - ✅ Интеграция с DI выполнена
2. ✅ VideoPlayer улучшения (Шаги 2.1-2.3) - **70% ВЫПОЛНЕНО**
   - ✅ Улучшена обработка ошибок
   - ✅ Добавлена статистика воспроизведения
   - ✅ Оптимизация для RTSP потоков
   - ⏳ Picture-in-Picture (осталось)
   - ⏳ Фоновое воспроизведение (осталось)

### Фаза 2: Фоновая работа (Неделя 2) - ✅ ВЫПОЛНЕНО
3. ✅ RecordingService (Шаги 3.1.1-3.1.7) - **100% ВЫПОЛНЕНО**
   - ✅ Foreground Service создан
   - ✅ Уведомления реализованы
   - ✅ Управление записью (start/stop/pause/resume)
   - ✅ Интеграция с репозиториями
   - ⚠️ Требуется интеграция с VideoRecordingService из shared модуля
4. ✅ CameraMonitoringService (Шаги 3.2.1-3.2.7) - **100% ВЫПОЛНЕНО**
   - ✅ Background Service создан
   - ✅ Периодический мониторинг камер
   - ✅ Уведомления о событиях
   - ✅ Оптимизация батареи
5. ✅ ServiceManager - **100% ВЫПОЛНЕНО**
   - ✅ Управление жизненным циклом сервисов
   - ✅ Биндинг/анбиндинг сервисов

### Фаза 3: Безопасность и доработки (Неделя 3) - ✅ ВЫПОЛНЕНО
5. ✅ Android Keystore (Шаги 4.1-4.9) - **100% ВЫПОЛНЕНО**
   - ✅ AndroidKeystoreManager создан
   - ✅ PasswordEncryption.android.kt уже реализован
   - ✅ Интеграция готова
6. ⏳ VideoPlayer PiP и фоновое воспроизведение (Шаги 2.4-2.5) - **0%**
7. ⏳ Интеграция и тестирование - **Частично**

---

## Зависимости

### Внешние библиотеки
- `androidx.media3:media3-exoplayer` — для VideoPlayer
- `androidx.work:work-runtime-ktx` — для фоновых задач
- `androidx.biometric:biometric` — для биометрической аутентификации (опционально)

### Внутренние модули
- `shared` — модели, репозитории, use cases
- `core:network` — API клиенты
- `core:common` — общая логика

---

## Критерии готовности

### NotificationsScreen
- [ ] Экран отображает список уведомлений
- [ ] Фильтрация работает корректно
- [ ] Навигация к связанным объектам работает
- [ ] Unit и UI тесты проходят

### VideoPlayer
- [ ] RTSP потоки воспроизводятся стабильно
- [ ] HLS потоки воспроизводятся стабильно
- [ ] PiP режим работает
- [ ] Фоновое воспроизведение работает
- [ ] Обработка ошибок работает корректно

### RecordingService
- [ ] Запись начинается и останавливается корректно
- [ ] Уведомления отображаются
- [ ] Состояние сохраняется после перезапуска
- [ ] Обработка ошибок работает

### CameraMonitoringService
- [ ] Мониторинг работает в фоне
- [ ] Уведомления отправляются при событиях
- [ ] Батарея не разряжается быстро
- [ ] Работает после перезапуска устройства

### Android Keystore
- [ ] Пароли шифруются и дешифруются корректно
- [ ] Ключи генерируются безопасно
- [ ] Миграция существующих данных работает
- [ ] Тесты безопасности проходят

---

## Примечания

- Все изменения должны быть обратно совместимы
- Необходимо учитывать разные версии Android (минимум API 24)
- Следовать Material Design 3 guidelines
- Использовать Kotlin Coroutines и Flow для асинхронности
- Следовать принципам Clean Architecture

---

**Статус:** План создан, готов к реализации
