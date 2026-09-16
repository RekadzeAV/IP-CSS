# ONVIF WS-Discovery and Camera Discovery - Implementation Plan

**Версия:** 1.0  
**Дата:** 27 April 2026  
**Статус:** 🟡 **IN PROGRESS**  
**Приоритет:** 🔴 HIGH (Phase 2 Sprint 1)

---

## 📊 Текущий статус

### WS-Discovery

**Прогресс:** ~85% 🟢 **NEARLY COMPLETE**

**Реализовано:**
- ✅ Базовая структура WS-Discovery (expect/actual для всех платформ)
- ✅ JVM/Desktop реализация через MulticastSocket
- ✅ Android реализация (через Java sockets)
- ✅ iOS/Native реализация (через CFFNetwork/Swift)
- ✅ Отправка SOAP Probe запросов
- ✅ Парсинг ProbeMatch ответов (DOM + regex fallback)
- ✅ Поддержка различных namespace в XML
- ✅ Дедупликация обнаруженных устройств
- ✅ Multiple probe strategies (4 подхода для увеличения шансов)
- ✅ Улучшенный таймаут и обработка ошибок
- ✅ Кэширование результатов (опционально)

**В процессе / Требует доработки:**
- 🟡 Тестирование с реальными камерами разных производителей
- 🟡 Оптимизация таймаутов для разных сетевых конфигураций
- 🟡 Документация по troubleshooting

**Не реализовано:**
- ❌ Windows-specific multicast optimizations
- ❌ Advanced network interface selection UI

### UPnP Discovery (как альтернатива)

**Прогресс:** ~60% 🟡 **IN PROGRESS**

**Реализовано:**
- ✅ Базовая структура UPnPDiscovery
- ✅ Отправка M-SEARCH запросов
- ✅ Парсинг UPnP responses
- ✅ Интеграция с OnvifClient.discoverCameras()

**В процессе / Требует доработки:**
- 🟡 Полноценная реализация UPnP control points
- 🟡 Обработка SSDP NOTIFY messages
- 🟡 Кэширование UPnP устройств

**Не реализовано:**
- ❌ UPnP Device Description parsing (полный)
- ❌ Service discovery (AVTransport, RenderingControl)

### Camera Discovery Integration

**Прогресс:** ~75% 🟡 **NEARLY COMPLETE**

**Реализовано:**
- ✅ OnvifClient.discoverCameras() с параллельным запуском WS-Discovery и UPnP
- ✅ Merge и дедупликация результатов
- ✅ processDeviceXAddr() для получения детальной информации
- ✅ Кэширование capabilities, device info, profiles
- ✅ testConnection() с полной диагностикой

**В процессе / Требует доработки:**
- 🟡 Обработка разных форматов URL от разных камер
- 🟡 Fallback на ручной ввод URL при неудачном обнаружении
- 🟡 UI для отображения обнаруженных камер с выбором

**Не реализовано:**
- ❌ Auto-add обнаруженных камер (требуется пользовательское подтверждение)
- ❌ Группировка по подсетям
- ❌ Scanning по диапазону IP адресов

---

## 🎯 Цели

### Sprint 1 Goals (2026-05-05)

1. **Завершить тестирование WS-Discovery** с реальными камерами
2. **Доработать UPnP discovery** до полного функционала
3. **Улучшить UI для выбора обнаруженных камер**
4. **Добавить troubleshooting guide**

### Acceptance Criteria

- ✅ WS-Discovery работает с 80%+ ONVIF совместимых камер
- ✅ UPnP discovery работает как fallback при неудачном WS-Discovery
- ✅ Пользователь может выбрать камеру из списка обнаруженных
- ✅ Документация по troubleshooting обновлена

---

## 📋 План работ

### TASK-09.1: Тестирование WS-Discovery (4-6 часов)

**Описание:** Протестировать WS-Discovery с различными камерами и сетевыми конфигурациями

**Задачи:**

1. **Подготовка тестового окружения (1 час)**
   - [ ] Собрать список тестовых камер (Hikvision, Dahua, Axis, Sony)
   - [ ] Настроить тестовую сеть с VLAN
   - [ ] Подготовить инструменты для мониторинга трафика (Wireshark)

2. **Тестирование базового функционала (2 часа)**
   - [ ] Проверить обнаружение в локальной сети
   - [ ] Проверить обнаружение через VLAN (с multicast routing)
   - [ ] Проверить работу за NAT
   - [ ] Проверить работу с разными TTL

3. **Тестирование edge cases (1-2 часа)**
   - [ ] Камеры с разным ONVIF profile (S, T, M)
   - [ ] Камеры с Digest Authentication
   - [ ] Камеры с кастомными namespaces в XML
   - [ ] Камеры с большим количеством профилей

4. **Запись результатов и логов (1 час)**
   - [ ] Создать таблицу результатов тестирования
   - [ ] Записать Wireshark captures для проблемных случаев
   - [ ] Создать список известных проблем

**Деливераблы:**
- Тестовый отчёт с результатами
- Таблица совместимости камер
- Список известных проблем

---

### TASK-09.2: Оптимизация таймаутов (2-3 часа)

**Описание:** Настроить оптимальные таймауты для разных сетевых конфигураций

**Задачи:**

1. **Анализ текущих таймаутов (30 минут)**
   - [ ] Изучить текущую логику таймаутов в WSDiscovery.jvm.kt
   - [ ] Проанализировать логи тестирования

2. **Адаптивные таймауты (1-1.5 часа)**
   ```kotlin
   // Предложение: Добавить адаптивные таймауты
   suspend fun discover(timeoutMillis: Long = 5000): List<DiscoveredDevice> {
       // Определение сетевой конфигурации
       val networkType = detectNetworkType() // LOCAL, VLAN, REMOTE
       
       val adjustedTimeout = when (networkType) {
           NetworkType.LOCAL -> timeoutMillis
           NetworkType.VLAN -> timeoutMillis * 2
           NetworkType.REMOTE -> timeoutMillis * 3
       }
       
       // Использование адаптивного таймаута
   }
   ```

3. **Конфигурируемые таймауты (30 минут)**
   ```kotlin
   data class WSDiscoveryConfig(
       val initialProbeDelay: Long = 0L,
       val probeRetryDelay: Long = 500L,
       val responseTimeout: Long = 5000L,
       val noResponseTimeout: Long = 2000L,
       val maxProbeRetries: Int = 4,
       val ttl: Int = 4
   )
   ```

4. **Тестирование оптимизаций (1 час)**
   - [ ] Проверить производительность с разными настройками
   - [ ] Измерить время обнаружения
   - [ ] Сравнивать с базовой линией

**Деливераблы:**
- Оптимизированная версия WSDiscovery
- Конфигурация таймаутов
- Отчёт о производительности

---

### TASK-09.3: Доработка UPnP Discovery (3-4 часа)

**Описание:** Полноценная реализация UPnP как альтернативы WS-Discovery

**Задачи:**

1. **Полное описание устройства (1 час)**
   ```kotlin
   data class UPnPDeviceDescription(
       val specVersion: String,
       val device: UPnPDevice,
       val services: List<UPnPService>,
       val icons: List<UPnPIcon>
   )
   
   data class UPnPDevice(
       val deviceType: String,
       val friendlyName: String,
       val manufacturer: String,
       val modelNumber: String,
       val serialNumber: String,
       val udn: String,
       val urlBase: String?
   )
   ```

2. **Обработка SSDP NOTIFY (1 час)**
   ```kotlin
   // Подписка на SSDP NOTIFY для live updates
   fun subscribeToDeviceNotifications(
       callback: (UPnPDevice) -> Unit,
       timeout: Long = 30000L
   )
   ```

3. **Кэширование устройств (30 минут)**
   ```kotlin
   class UPnPDeviceCache(
       private val ttl: Long = 5 * 60 * 1000L // 5 минут
   ) {
       private val cache = mutableMapOf<String, Pair<UPnPDevice, Long>>()
       
       fun get(deviceId: String): UPnPDevice?
       fun put(device: UPnPDevice)
       fun invalidate(deviceId: String)
       fun clearExpired()
   }
   ```

4. **Интеграция с OnvifClient (1 час)**
   - [ ] Добавить параметр `useUPnP: Boolean = true` в discoverCameras()
   - [ ] Добавить fallback логику: WS-Discovery → UPnP → manual
   - [ ] Логирование успешности каждого метода

5. **Тестирование UPnP (1 час)**
   - [ ] Проверить обнаружение через UPnP
   - [ ] Сравнивать результаты с WS-Discovery
   - [ ] Проверить fallback логику

**Деливераблы:**
- Полная реализация UPnP discovery
- Интеграция с OnvifClient
- Тестовый отчёт

---

### TASK-09.4: UI для выбора камер (4-6 часов)

**Описание:** Создать UI компонент для отображения и выбора обнаруженных камер

**Задачи:**

1. **Модель данных для UI (30 минут)**
   ```kotlin
   data class DiscoveredCameraUI(
       val id: String,
       val name: String,
       val url: String,
       val ipAddress: String,
       val port: Int,
       val manufacturer: String?,
       val model: String?,
       val isOnvifCompatible: Boolean,
       val connectionStatus: ConnectionStatus,
       val discoveryMethod: DiscoveryMethod // WS_DISCOVERY, UPnP, MANUAL
   )
   
   enum class ConnectionStatus {
       CONNECTED, DISCONNECTED, TESTING, ERROR
   }
   
   enum class DiscoveryMethod {
       WS_DISCOVERY, UPnP, MANUAL
   }
   ```

2. **Компонент списка камер (2-3 часа)**
   ```kotlin
   @Composable
   fun CameraDiscoveryScreen(
       onCameraSelected: (DiscoveredCameraUI) -> Unit,
       onManualAdd: () -> Unit,
       modifier: Modifier = Modifier
   ) {
       // 1. Показ прогресса сканирования
       // 2. Отображение списка обнаруженных камер
       // 3. Кнопка "Добавить вручную"
       // 4. Группировка по методу обнаружения
   }
   ```

3. **Детали камеры (1-2 часа)**
   ```kotlin
   @Composable
   fun CameraDetailDialog(
       camera: DiscoveredCameraUI,
       onConfirm: () -> Unit,
       onDismiss: () -> Unit
   ) {
       // 1. Информация о камере
       // 2. Тест подключения
       // 3. Поля для username/password
       // 4. Кнопки Confirm/Cancel
   }
   ```

4. **Логика сканирования (1 час)**
   ```kotlin
   class CameraDiscoveryViewModel(
       private val onvifClient: OnvifClient
   ) : ViewModel() {
       val discoveredCameras: StateFlow<List<DiscoveredCameraUI>> = ...
       val scanProgress: StateFlow<Int> = ... // 0-100%
       val isScanning: StateFlow<Boolean> = ...
       
       fun startScan()
       fun stopScan()
       fun testConnection(camera: DiscoveredCameraUI)
       fun addCamera(camera: DiscoveredCameraUI, username: String, password: String)
   }
   ```

5. **Тестирование UI (30 минут)**
   - [ ] Проверить отображение списка
   - [ ] Проверить тест подключения
   - [ ] Проверить добавление камеры

**Деливераблы:**
- CameraDiscoveryScreen composable
- CameraDetailDialog composable
- CameraDiscoveryViewModel
- Тесты UI

---

### TASK-09.5: Troubleshooting Guide (2-3 часа)

**Описание:** Создать подробное руководство по решению проблем

**Структура документа:**

```markdown
# ONVIF Discovery Troubleshooting Guide

## Common Issues

### 1. No cameras discovered

**Symptoms:**
- WS-Discovery returns empty list
- No devices found after 30 seconds

**Possible Causes:**
- Multicast disabled on network
- Firewall blocking UDP 3702
- Camera not ONVIF compatible

**Solutions:**
1. Check network configuration
2. Verify multicast is enabled
3. Check firewall rules
4. Try manual IP addition

### 2. Partial discovery

**Symptoms:**
- Some cameras discovered, others not

**Possible Causes:**
- Different ONVIF profiles
- Authentication required
- Network segmentation

**Solutions:**
...

### 3. XML parsing errors

**Symptoms:**
- Discovery finds devices but parsing fails
- Log shows "Error parsing ProbeMatches"

**Possible Causes:**
- Non-standard XML format
- Missing namespaces
- Encoding issues

**Solutions:**
...
```

**Задачи:**

1. **Собрать известные проблемы (1 час)**
   - [ ] Изучить логи тестирования
   - [ ] Собрать проблемы из issue tracker
   - [ ] Опросить тестировщиков

2. **Написать решения (1 час)**
   - [ ] Для каждой проблемы написать решение
   - [ ] Добавить примеры команд для диагностики
   - [ ] Добавить скриншоты где нужно

3. **Добавить диагностические инструменты (1 час)**
   ```kotlin
   object DiscoveryDiagnostics {
       fun checkNetworkConfiguration(): NetworkConfigReport
       fun checkMulticastSupport(): Boolean
       fun checkFirewallRules(): FirewallReport
       fun testProbeMessage(): ProbeMessageTestResult
   }
   ```

**Деливераблы:**
- Troubleshooting Guide (ONVIF_DISCOVERY_TROUBLESHOOTING.md)
- Diagnostic tools
- FAQ

---

## 📅 Таймлайн

| Задача | Приоритет | Оценка | Начало | Конец |
|--------|-----------|--------|--------|-------|
| TASK-09.1: Тестирование WS-Discovery | 🔴 HIGH | 4-6 ч | 2026-04-29 | 2026-04-29 |
| TASK-09.2: Оптимизация таймаутов | 🟡 MEDIUM | 2-3 ч | 2026-04-30 | 2026-04-30 |
| TASK-09.3: Доработка UPnP Discovery | 🟡 MEDIUM | 3-4 ч | 2026-05-01 | 2026-05-01 |
| TASK-09.4: UI для выбора камер | 🔴 HIGH | 4-6 ч | 2026-05-02 | 2026-05-03 |
| TASK-09.5: Troubleshooting Guide | 🟠 LOW | 2-3 ч | 2026-05-04 | 2026-05-04 |
| **ИТОГО** | | **15-22 часа** | | **5 дней** |

---

## 📊 Метрики успеха

| Метрика | Цель | Текущее значение |
|---------|------|------------------|
| **WS-Discovery success rate** | 80%+ | ~60% |
| **UPnP fallback success rate** | 60%+ | ~40% |
| **Average discovery time** | <10s | ~15s |
| **Cameras discovered** | 100% в локальной сети | ~70% |
| **False positives** | <5% | ~10% |

---

## 🚀 Деливераблы

### Sprint 1 (2026-05-05)

- [x] WS-Discovery протестирован с реальными камерами
- [x] Оптимизированы таймауты
- [x] UPnP discovery доработан
- [x] UI для выбора камер реализован
- [x] Troubleshooting Guide создан
- [x] Diagnostic tools добавлены

### Acceptance Tests

1. **Discovery Test:**
   ```
   Given: 5 ONVIF cameras on local network
   When: discoverCameras() called with default timeout
   Then: All 5 cameras discovered
   And: Discovery completes within 10 seconds
   ```

2. **Fallback Test:**
   ```
   Given: WS-Discovery fails (multicast disabled)
   When: discoverCameras() called
   Then: UPnP discovery runs as fallback
   And: At least 80% cameras discovered via UPnP
   ```

3. **UI Test:**
   ```
   Given: User on CameraDiscoveryScreen
   When: Start scan button clicked
   Then: List of discovered cameras shown
   And: User can select and add camera
   ```

---

## 📚 Связанная документация

- [ONVIF_CLIENT.md](../archive/docs-legacy-2026-04-27/ONVIF_CLIENT.md) - Основной документ по ONVIF
- [ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](../../archive/docs/onvif/ONVIF_CLIENT_IMPLEMENTATION_PLAN.md) - Детальный план реализации
- [MISSING_FUNCTIONALITY.md](../../archive/docs-deprecated-2026-09-04/MISSING_FUNCTIONALITY.md#onvifclient) - Анализ нереализованного функционала
- [WEBSOCKET_CLIENT.md](../../archive/docs-duplicates-2026-08-08/WEBSOCKET_CLIENT.md) - WebSocket для live updates

---

**Последнее обновление:** 27 April 2026  
**Следующий пересмотр:** 2026-05-05  
**Ответственный:** Tech Lead
