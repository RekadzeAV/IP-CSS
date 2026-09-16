# Примеры использования NetworkScanner

## Содержание

1. [Базовые примеры](#базовые-примеры)
2. [Интеграция с UI](#интеграция-с-ui)
3. [Продвинутые сценарии](#продвинутые-сценарии)
4. [Обработка ошибок](#обработка-ошибок)
5. [Оптимизация производительности](#оптимизация-производительности)

---

## Базовые примеры

### Пример 1: Простое сканирование подсети

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    // Сканирование домашней сети
    val cameras = scanner.scanSubnet(
        subnetRange = "192.168.1.0/24",
        ports = listOf(554, 80),
        timeoutPerHost = 1000
    )
    
    println("Найдено камер: ${cameras.size}")
    cameras.forEach { camera ->
        println("  - ${camera.name}: ${camera.url}")
    }
}
```

### Пример 2: Сканирование с прогресс-индикатором

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
        ports = listOf(554, 80, 8080),
        timeoutPerHost = 1000,
        maxConcurrent = 50
    ).collect { progress ->
        // Обновление UI в реальном времени
        println("Прогресс: ${progress.progress * 100}%")
        println("Хостов проверено: ${progress.hostsScanned}/${progress.hostsTotal}")
        println("Камер найдено: ${progress.camerasFound}")
        println("Действие: ${progress.currentActivity}")
    }
}
```

### Пример 3: Быстрое RTSP сканирование

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    // Быстрая проверка только RTSP портов
    val rtspUrls = scanner.quickRtspScan(
        subnetRange = "192.168.1.0/24",
        timeoutPerHost = 500
    )
    
    println("Найдено RTSP endpoints: ${rtspUrls.size}")
    rtspUrls.forEach { url ->
        println("  - $url")
    }
}
```

---

## Интеграция с UI

### Пример 4: Интеграция с Android (ViewModel)

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.core.network.NetworkScanner
import com.company.ipcamera.core.network.NetworkScanProgress
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CameraDiscoveryViewModel : ViewModel() {
    
    private val httpClient = HttpClient()
    private val scanner = NetworkScanner(httpClient)
    
    private val _scanProgress = MutableStateFlow<NetworkScanProgress?>(null)
    val scanProgress: StateFlow<NetworkScanProgress?> = _scanProgress
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    
    fun startDiscovery(subnetRange: String) {
        viewModelScope.launch {
            _isScanning.value = true
            
            scanner.scanSubnetWithProgress(
                subnetRange = subnetRange,
                ports = listOf(554, 80, 8080),
                timeoutPerHost = 1000,
                maxConcurrent = 50
            ).collect { progress ->
                _scanProgress.value = progress
                
                if (progress.progress >= 1f) {
                    _isScanning.value = false
                }
            }
        }
    }
    
    fun cancelDiscovery() {
        // Реализовать отмену сканирования
        _isScanning.value = false
    }
}
```

### Пример 5: Интеграция с Compose Desktop

```kotlin
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.ipcamera.core.network.NetworkScanProgress
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DiscoveryProgressScreen(
    progressState: StateFlow<NetworkScanProgress?>,
    isScanning: StateFlow<Boolean>
) {
    val progress by progressState.collectAsState()
    val scanning by isScanning.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Обнаружение камер",
            style = MaterialTheme.typography.h5
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (scanning) {
            progress?.let { p ->
                LinearProgressIndicator(
                    progress = p.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .progressSemantics()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(text = "${(p.progress * 100).toInt()}%")
                Text(text = p.currentActivity)
                Text(text = "Найдено камер: ${p.camerasFound}")
            }
        } else {
            Text(text = "Сканирование не запущено")
        }
    }
}
```

### Пример 6: Интеграция с Jetpack Compose (Android)

```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.ipcamera.shared.domain.model.DiscoveredCamera

@Composable
fun CameraDiscoveryScreen(
    viewModel: CameraDiscoveryViewModel = viewModel()
) {
    val progress by viewModel.scanProgress.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обнаружение камер") },
                actions = {
                    if (!isScanning) {
                        IconButton(onClick = {
                            viewModel.startDiscovery("192.168.1.0/24")
                        }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Начать сканирование"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Прогресс бар
            if (isScanning) {
                progress?.let { p ->
                    LinearProgressIndicator(
                        progress = p.progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = p.currentActivity,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            // Список камер
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = progress?.discoveredCameras ?: emptyList(),
                    key = { it.ipAddress }
                ) { camera ->
                    CameraListItem(camera = camera)
                }
            }
        }
    }
}

@Composable
fun CameraListItem(camera: DiscoveredCamera) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = camera.name,
                style = MaterialTheme.typography.h6
            )
            Text(text = camera.url)
            Text(text = "IP: ${camera.ipAddress}:${camera.port}")
            camera.model?.let {
                Text(text = "Модель: $it")
            }
        }
    }
}
```

---

## Продвинутые сценарии

### Пример 7: Множественные подсети

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    // Сканирование нескольких подсетей параллельно
    val subnets = listOf(
        "192.168.1.0/24",
        "192.168.2.0/24",
        "10.0.0.0/24"
    )
    
    coroutineScope {
        val results = subnets.map { subnet ->
            async {
                scanner.scanSubnet(
                    subnetRange = subnet,
                    ports = listOf(554),
                    timeoutPerHost = 1000
                )
            }
        }.awaitAll()
        
        val allCameras = results.flatten()
        println("Всего камер: ${allCameras.size}")
    }
}
```

### Пример 8: Фильтрация и дедупликация

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    val cameras = scanner.scanSubnet("192.168.1.0/24")
    
    // Дедупликация по IP
    val uniqueCameras = cameras.distinctBy { it.ipAddress }
    
    // Фильтрация по производителю
    val hikvisionCameras = uniqueCameras.filter {
        it.manufacturer?.contains("Hikvision", ignoreCase = true) == true
    }
    
    // Сортировка по имени
    val sortedCameras = uniqueCameras.sortedBy { it.name }
    
    println("Уникальных камер: ${uniqueCameras.size}")
    println("Hikvision камер: ${hikvisionCameras.size}")
}
```

### Пример 9: Интеграция с ONVIF

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import com.company.ipcamera.core.network.OnvifClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    // Шаг 1: Сканирование подсети
    val discoveredCameras = scanner.scanSubnet(
        subnetRange = "192.168.1.0/24",
        ports = listOf(554, 80)
    )
    
    // Шаг 2: Проверка ONVIF совместимости
    val onvifClient = OnvifClient(httpClient)
    val onvifCameras = discoveredCameras.filter { camera ->
        try {
            val capabilities = onvifClient.getCapabilities(
                url = "http://${camera.ipAddress}:${camera.port}",
                username = "admin",
                password = "password"
            )
            capabilities != null
        } catch (e: Exception) {
            false
        }
    }
    
    println("ONVIF камер: ${onvifCameras.size}")
}
```

---

## Обработка ошибок

### Пример 10: Обработка ошибок сети

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import java.net.UnknownHostException
import java.net.SocketTimeoutException

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    try {
        val cameras = scanner.scanSubnet("invalid.subnet/24")
    } catch (e: UnknownHostException) {
        println("Неверный формат подсети: ${e.message}")
    } catch (e: SocketTimeoutException) {
        println("Таймаут при сканировании")
    } catch (e: Exception) {
        println("Ошибка сканирования: ${e.message}")
    }
}
```

### Пример 11: Повторные попытки

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

suspend fun scanWithRetry(
    scanner: NetworkScanner,
    subnetRange: String,
    maxRetries: Int = 3
): List<com.company.ipcamera.core.network.DiscoveredCamera> {
    var lastException: Exception? = null
    
    for (attempt in 1..maxRetries) {
        try {
            return scanner.scanSubnet(subnetRange)
        } catch (e: Exception) {
            lastException = e
            println("Попытка $attempt/$maxRetries не удалась: ${e.message}")
            
            if (attempt < maxRetries) {
                delay(2000) // Пауза перед повторной попыткой
            }
        }
    }
    
    throw lastException ?: IllegalStateException("Неизвестная ошибка")
}

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    val cameras = scanWithRetry(scanner, "192.168.1.0/24")
    println("Найдено камер: ${cameras.size}")
}
```

### Пример 12: Логирование

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import com.company.ipcamera.core.network.NetworkScanProgress
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    scanner.scanSubnetWithProgress("192.168.1.0/24")
        .collect { progress ->
            when {
                progress.progress == 0f -> {
                    logger.info { "Начало сканирования ${progress.hostsTotal} хостов" }
                }
                progress.progress >= 1f -> {
                    logger.info { "Завершено. Найдено камер: ${progress.camerasFound}" }
                }
                progress.progress % 0.25 < 0.01 -> {
                    logger.debug { "Прогресс: ${(progress.progress * 100).toInt()}%" }
                }
            }
        }
}
```

---

## Оптимизация производительности

### Пример 13: Настройка для большой сети

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    // Для большой сети (/16)
    val cameras = scanner.scanSubnet(
        subnetRange = "10.0.0.0/16",
        ports = listOf(554), // Только RTSP
        timeoutPerHost = 500, // Быстрый таймаут
        maxConcurrent = 100 // Больше параллельных сканов
    )
    
    println("Найдено камер: ${cameras.size}")
}
```

### Пример 14: Кэширование результатов

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.minutes

class CachedNetworkScanner(
    private val scanner: NetworkScanner,
    private val cacheDuration: kotlin.time.Duration = 5.minutes
) {
    private val cache = mutableMapOf<String, Pair<List<com.company.ipcamera.core.network.DiscoveredCamera>, Long>>()
    
    suspend fun scanWithCache(subnetRange: String): List<com.company.ipcamera.core.network.DiscoveredCamera> {
        val cached = cache[subnetRange]
        val now = System.currentTimeMillis()
        
        if (cached != null && (now - cached.second) < cacheDuration.inWholeMilliseconds) {
            println("Используем кэш для $subnetRange")
            return cached.first
        }
        
        println("Сканируем $subnetRange")
        val result = scanner.scanSubnet(subnetRange)
        cache[subnetRange] = Pair(result, now)
        
        return result
    }
}

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    val cachedScanner = CachedNetworkScanner(scanner)
    
    // Первое сканирование
    val cameras1 = cachedScanner.scanWithCache("192.168.1.0/24")
    
    // Второе сканирование (использует кэш)
    val cameras2 = cachedScanner.scanWithCache("192.168.1.0/24")
}
```

### Пример 15: Приоритизация портов

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val httpClient = HttpClient()
    val scanner = NetworkScanner(httpClient)
    
    // Сначала проверяем наиболее вероятные порты
    val cameras = scanner.scanSubnet(
        subnetRange = "192.168.1.0/24",
        ports = listOf(554, 8554, 80, 8080, 5544), // По убыванию вероятности
        timeoutPerHost = 500
    )
    
    println("Найдено камер: ${cameras.size}")
}
```

---

## Тестирование

### Пример 16: Unit тесты

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class NetworkScannerUnitTest {
    
    @Test
    fun testSubnetParsing() = runTest {
        val scanner = NetworkScanner(HttpClient())
        val ips = scanner.parseSubnet("192.168.1.0/24")
        
        assertTrue(ips.isNotEmpty())
        assertTrue(ips.contains("192.168.1.1"))
    }
}
```

### Пример 17: Интеграционные тесты

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class NetworkScannerIntegrationTest {
    
    @Test
    fun testLocalhostScan() = runTest {
        val scanner = NetworkScanner(HttpClient())
        val cameras = scanner.scanSubnet("127.0.0.1/32")
        
        // На localhost скорее всего ничего не найдём, но тест проверяет что код работает
        assertTrue(true)
    }
}
```

---

## См. также

- [API Reference](NETWORK_SCANNER.md#api-reference)
- [Внедрение в CameraRepository](../_to_be_archived/ROOT_FILES_2026-06-21/PHASE1_IMPLEMENTATION_REPORT.md#интеграция-с-camerarepositoryimpl)
- [Проблемы и решения](NETWORK_SCANNER.md#обработка-ошибок)

---

**Версия:** 1.0  
**Дата:** 2025-01-15  
**Автор:** NLP-Core-Team
