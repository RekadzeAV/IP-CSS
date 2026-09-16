# Network Scanner

Сетевой сканер для обнаружения IP камер в подсети.

## Обзор

`NetworkScanner` предоставляет возможности для:
- Сканирования подсети на наличие IP камер
- Обнаружения RTSP сервисов
- Прогресс-отслеживания процесса сканирования
- Интеграции с ONVIF WS-Discovery

## Быстрый старт

### Базовое сканирование подсети

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    // Сканирование подсети 192.168.1.0/24
    val cameras = scanner.scanSubnet(
        subnetRange = "192.168.1.0/24",
        ports = listOf(554, 80, 8080),
        timeoutPerHost = 1000,
        maxConcurrent = 50
    )
    
    println("Found ${cameras.size} cameras:")
    cameras.forEach { camera ->
        println("  - ${camera.name} (${camera.url})")
    }
}
```

### Сканирование с прогресс-индикатором

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    scanner.scanSubnetWithProgress(
        subnetRange = "192.168.1.0/24",
        ports = listOf(554),
        timeoutPerHost = 1000,
        maxConcurrent = 50
    ).collect { progress ->
        println("Progress: ${progress.progress * 100}%")
        println("Activity: ${progress.currentActivity}")
        println("Cameras found: ${progress.camerasFound}")
        
        // Обновление UI
        updateProgressUI(progress)
    }
}
```

### Быстрое RTSP сканирование

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    // Быстрое сканирование только RTSP портов
    val rtspUrls = scanner.quickRtspScan(
        subnetRange = "192.168.1.0/24",
        timeoutPerHost = 500
    )
    
    println("Found ${rtspUrls.size} RTSP endpoints:")
    rtspUrls.forEach { url ->
        println("  - $url")
    }
}
```

## API Reference

### NetworkScanner

#### Конструктор

```kotlin
class NetworkScanner(
    private val httpClient: HttpClient,
    private val discoveryTimeout: Long = 5000
)
```

**Параметры:**
- `httpClient` - Ktor HTTP клиент для ONVIF запросов
- `discoveryTimeout` - Таймаут обнаружения в миллисекундах

#### Методы

##### scanSubnet

```kotlin
suspend fun scanSubnet(
    subnetRange: String,
    ports: List<Int> = listOf(554, 80, 8080),
    timeoutPerHost: Long = 1000,
    maxConcurrent: Int = 50
): List<DiscoveredCamera>
```

Сканирует подсеть на наличие камер.

**Параметры:**
- `subnetRange` - Диапазон подсети (например, "192.168.1.0/24")
- `ports` - Порты для проверки
- `timeoutPerHost` - Таймаут на каждый хост (мс)
- `maxConcurrent` - Максимальное количество одновременных сканов

**Возвращает:** Список обнаруженных камер

##### scanSubnetWithProgress

```kotlin
fun scanSubnetWithProgress(
    subnetRange: String,
    ports: List<Int> = listOf(554, 80, 8080),
    timeoutPerHost: Long = 1000,
    maxConcurrent: Int = 50
): Flow<NetworkScanProgress>
```

Сканирует подсеть с прогресс-индикатором.

**Возвращает:** Flow с прогрессом сканирования

##### quickRtspScan

```kotlin
suspend fun quickRtspScan(
    subnetRange: String,
    timeoutPerHost: Long = 500
): List<String>
```

Быстрое сканирование только RTSP портов.

**Возвращает:** Список RTSP URL

### DiscoveredCamera

```kotlin
data class DiscoveredCamera(
    val name: String,
    val url: String,
    val model: String? = null,
    val manufacturer: String? = null,
    val ipAddress: String,
    val port: Int = 554,
    val username: String? = null,
    val password: String? = null
)
```

### NetworkScanProgress

```kotlin
data class NetworkScanProgress(
    val currentActivity: String,
    val progress: Float,           // 0.0-1.0
    val hostsScanned: Int,
    val hostsTotal: Int,
    val camerasFound: Int,
    val discoveredCameras: List<DiscoveredCamera>
)
```

## Интеграция с CameraRepository

```kotlin
import com.company.ipcamera.shared.data.repository.CameraRepositoryImpl
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Создание репозитория
    val databaseFactory = DatabaseFactory()
    val repository = CameraRepositoryImpl(databaseFactory)
    
    // Создание сканера
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    // Сканирование подсети
    val discoveredCameras = scanner.scanSubnet("192.168.1.0/24")
    
    // Добавление обнаруженных камер в репозиторий
    for (camera in discoveredCameras) {
        repository.addCamera(
            com.company.ipcamera.shared.domain.model.Camera(
                id = generateId(),
                name = camera.name,
                url = camera.url,
                username = camera.username ?: "admin",
                password = camera.password ?: "",
                // ... остальные поля
            )
        )
    }
}
```

## Интеграция с WS-Discovery

```kotlin
import com.company.ipcamera.core.network.WSDiscovery
import com.company.ipcamera.core.network.NetworkScanner
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Шаг 1: WS-Discovery для быстрого обнаружения
    val wsDiscovery = WSDiscovery()
    val wsCameras = wsDiscovery.discover(timeoutMillis = 5000)
    wsDiscovery.close()
    
    // Шаг 2: Сканирование подсети для полного обнаружения
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    val subnetCameras = scanner.scanSubnet("192.168.1.0/24")
    
    // Шаг 3: Объединение результатов
    val allCameras = (wsCameras + subnetCameras).distinctBy { it.ipAddress }
    
    println("Total discovered: ${allCameras.size} cameras")
}
```

## Конфигурация

### Рекомендуемые настройки для разных сетей

#### Малая сеть (дом/офис до 50 устройств)

```kotlin
scanner.scanSubnet(
    subnetRange = "192.168.1.0/24",
    ports = listOf(554, 80),
    timeoutPerHost = 500,
    maxConcurrent = 25
)
```

#### Средняя сеть (до 200 устройств)

```kotlin
scanner.scanSubnet(
    subnetRange = "192.168.0.0/24",
    ports = listOf(554, 80, 8080),
    timeoutPerHost = 1000,
    maxConcurrent = 50
)
```

#### Большая сеть (сотни устройств)

```kotlin
scanner.scanSubnet(
    subnetRange = "10.0.0.0/16",
    ports = listOf(554),
    timeoutPerHost = 2000,
    maxConcurrent = 100
)
```

## Обработка ошибок

```kotlin
try {
    val cameras = scanner.scanSubnet("192.168.1.0/24")
    // Обработка результатов
} catch (e: Exception) {
    when (e) {
        is java.net.UnknownHostException -> {
            println("Invalid subnet range")
        }
        is java.net.SocketTimeoutException -> {
            println("Scan timeout - try increasing timeout")
        }
        else -> {
            println("Scan failed: ${e.message}")
        }
    }
}
```

## Производительность

### Факторы, влияющие на скорость сканирования

1. **Размер подсети**: /24 (254 хоста) vs /16 (65534 хостов)
2. **Таймаут на хост**: Меньше = быстрее, но больше пропущенных устройств
3. **Количество портов**: Каждый порт добавляет время сканирования
4. **Параллелизм**: Больше concurrent = быстрее, но больше нагрузка на сеть

### Рекомендации по оптимизации

```kotlin
// Для быстрого сканирования используйте:
scanner.quickRtspScan("192.168.1.0/24", timeoutPerHost = 300)

// Для полного сканирования с приемлемой скоростью:
scanner.scanSubnet(
    subnetRange = "192.168.1.0/24",
    ports = listOf(554),
    timeoutPerHost = 500,
    maxConcurrent = 50
)
```

## Тестирование

### Unit тесты

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkScannerTest {
    
    @Test
    fun testSubnetParsing() = runTest {
        val scanner = NetworkScanner(HttpClient())
        val ips = scanner.parseSubnet("192.168.1.0/24")
        
        assertEquals(254, ips.size)
    }
}
```

### Интеграционные тесты

```kotlin
class NetworkScannerIntegrationTest {
    
    @Test
    fun testRealNetworkScan() = runTest {
        val scanner = NetworkScanner(HttpClient())
        
        scanner.scanSubnetWithProgress("127.0.0.1/32")
            .collect { progress ->
                println("Progress: ${progress.progress}")
            }
    }
}
```

## Поддерживаемые форматы подсети

- `/32` - Одиночный IP (127.0.0.1/32)
- `/24` - Малая подсеть (192.168.1.0/24)
- `/16` - Средняя подсеть (10.0.0.0/16)
- `/8` - Большая подсеть (172.16.0.0/8)

## Ограничения

- Максимальный размер подсети: /8 (16+ миллионов хостов)
- Требуется доступ к локальной сети
- Multicast может быть заблокирован фаерволом
- Некоторые камеры могут не отвечать на RTSP запросы

## См. также

- ONVIF WS-Discovery *(утерян/в архиве)*
- [RTSP Client](../archive/docs-duplicates-2026-08-08/RTSP_CLIENT.md)
- Camera Repository *(утерян/в архиве)*
