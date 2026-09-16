# План рефакторинга IP-CSS — варианты решений оставшихся проблем

**Дата:** 22 June 2026  
**Версия:** 1.0  
**Статус:** План действий

---

## 1. `java.time.Clock` в shared/commonMain

**Файл:** `shared/src/commonMain/.../data/repository/CameraRepositoryImpl.kt`  
**Проблема:** Используется `java.time.Clock` (JVM-only) в commonMain, что ломает iOS сборку.

### Вариант A (рекомендуемый): Замена на kotlinx-datetime

```kotlin
// commonMain — замена java.time.Clock на kotlinx-datetime
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class CameraRepositoryImpl(
    private val localDataSource: CameraLocalDataSource,
    private val remoteDataSource: CameraRemoteDataSource
) : CameraRepository {
    
    override suspend fun getCameras(): List<Camera> {
        return localDataSource.getCameras().filter { 
            it.updatedAt > Clock.System.now().minus(30, DateTimeUnit.MINUTE)
        }
    }
}
```

**Затрагиваемые файлы:**
- `shared/build.gradle.kts` → добавить `kotlinx-datetime` в commonMain dependencies
- `CameraRepositoryImpl.kt` → заменить `java.time.Clock` на `kotlinx.datetime.Clock`
- Другие файлы с `java.time.*` в commonMain (поиск через grep)

**Риски:** Низкие. kotlinx-datetime — официальная библиотека JetBrains.

### Вариант B: expect/actual для Clock

```kotlin
// commonMain
expect class PlatformClock {
    fun now(): Long // millis
}

// androidMain
actual class PlatformClock {
    actual fun now(): Long = System.currentTimeMillis()
}

// iosMain  
actual class PlatformClock {
    actual fun now(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
}
```

**Риски:** Средние. Больше кода, нужно поддерживать 4 платформы.

---

## 2. `java.security` в commonMain core/common

**Файл:** `core/common/src/commonMain/.../security/`  
**Проблема:** `java.security.*` используется в commonMain, несовместимо с iOS/JS.

### Вариант A (рекомендуемый): Clean Architecture — разделение интерфейса и реализации

```kotlin
// commonMain — только интерфейсы
interface SecureStorage {
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
    fun generateKey(): ByteArray
}

// commonMain — модель данных
data class EncryptedData(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val salt: ByteArray
)

// jvmMain — реализация с java.security
actual class JvmSecureStorage : SecureStorage {
    override fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // ... JVM-specific implementation
    }
}

// androidMain — реализация с AndroidKeyStore  
actual class AndroidSecureStorage : SecureStorage {
    // Android-specific: KeyStore, AndroidKeyStore
}

// iosMain — реализация с CommonCrypto
actual class IosSecureStorage : SecureStorage {
    // iOS-specific: Security framework, CommonCrypto
}
```

**Затрагиваемые файлы:**
- `core/common/build.gradle.kts` — настройка source sets
- Создать `commonMain/security/SecureStorage.kt` (интерфейс)
- Создать `jvmMain/security/JvmSecureStorage.kt`
- Создать `androidMain/security/AndroidSecureStorage.kt`
- Создать `iosMain/security/IosSecureStorage.kt`

### Вариант B: Использование multiplatform-security библиотеки

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation("com.benasher44:uuid:0.8.4")
}

// Использовать существующую библиотеку для шифрования
// Например: https://github.com/aaassseee/skad-encryption
```

**Риски:** Зависимость от сторонней библиотеки, возможна несовместимость версий.

---

## 3. Android-код на Java (34 файла)

**Путь:** `android/app/src/main/java/`  
**Проблема:** KMP-проект содержит 34 Java-файла, что усложняет поддержку.

### Вариант (рекомендуемый): Постепенная миграция Java → Kotlin

```bash
# Стратегия: мигрировать по одному пакету в день
# 1. Мигрировать Presentation слой (Activity, Fragment)
# 2. Мигрировать ViewModel
# 3. Мигрировать Repository
# 4. Мигрировать модель/Entity
```

**Конкретный план для каждого файла:**

```kotlin
// Activity.java → Activity.kt
// До (Java):
public class CameraActivity extends AppCompatActivity {
    private CameraViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        viewModel = new ViewModelProvider(this).get(CameraViewModel.class);
    }
}

// После (Kotlin):
class CameraActivity : AppCompatActivity() {
    private val viewModel: CameraViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
    }
}
```

**Приоритет миграции:**
1. `**/viewmodel/*.java` (7 файлов) — наименьшая связанность
2. `**/ui/**/*.java` (12 файлов) — простой перевод
3. `**/data/**/*.java` (10 файлов) — средняя сложность
4. `**/di/**/*.java` (5 файлов) — высокая сложность

**Затрагиваемые файлы:** 34 Java-файла + build.gradle.kts (добавить kotlin плагин)

---

## 4. desktopApp без build.gradle.kts

**Путь:** `desktopApp/`  
**Проблема:** Модуль не собирается, нет build.gradle.kts, всего 4 Kotlin-файла.

### Вариант (рекомендуемый): Создать корректный build.gradle.kts

```kotlin
// desktopApp/build.gradle.kts
plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(project(":shared"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
}

compose.desktop {
    application {
        mainClass = "com.ipcamera.desktop.MainKt"
        
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "IP-CSS Desktop"
            packageVersion = "1.0.0"
            
            windows {
                menuGroup = "IP-CSS"
                upgradeUuid = "ip-css-desktop"
            }
            
            linux {
                packageName = "ipcss-desktop"
            }
            
            macOS {
                bundleID = "com.ipcamera.desktop"
            }
        }
    }
}
```

**Затрагиваемые файлы:**
- Создать `desktopApp/build.gradle.kts`
- Переместить `desktopApp/src/main/kotlin/` → переиспользовать

**Дополнительно:** Реализовать минимальный Main.kt:

```kotlin
// desktopApp/src/main/kotlin/com/ipcamera/desktop/Main.kt
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 800.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "IP-CSS Desktop",
        state = windowState
    ) {
        App()
    }
}

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "IP-CSS Desktop Client",
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}
```

---

## 5. InMemory репозитории в server

**Путь:** `server/api/src/main/kotlin/.../di/AppModule.kt`  
**Проблема:** Используются InMemory-репозитории вместо SQLDelight.

### Вариант (рекомендуемый): Замена на SQLDelight репозитории (существующие в shared)

```kotlin
// server/api/.../di/AppModule.kt — текущая проблема
val appModule = module {
    // ❌ InMemory — теряет данные при перезапуске
    single<CameraRepository> { InMemoryCameraRepository() }
}

// ✅ Должно быть:
val appModule = module {
    single<CameraRepository> { CameraRepositoryImpl(get(), get()) }
    single<CameraLocalDataSource> { SqlDelightCameraDataSource(get()) }
    single<CameraRemoteDataSource> { KtorCameraDataSource(get()) }
}
```

**Проблема:** `InMemoryCameraRepository` определён в server, а `CameraRepositoryImpl` — в shared/commonMain. 
Нужно проверить:
1. Есть ли SQLDelight схема в shared?
2. Есть ли миграции Flyway?
3. Есть ли PostgreSQL драйвер?

**Если SQLDelight схемы нет (промежуточное решение):**

```kotlin
// server/.../repositories/DatabaseCameraRepository.kt
class DatabaseCameraRepository(private val db: Database) : CameraRepository {
    private val queries = db.cameraQueries
    
    override fun getAll(): List<Camera> {
        return queries.selectAll().executeAsList().map { it.toCamera() }
    }
    
    override fun getById(id: String): Camera? {
        return queries.selectById(id).executeAsOneOrNull()?.toCamera()
    }
    
    override fun save(camera: Camera) {
        queries.upsert(camera.toEntity())
    }
    
    override fun delete(id: String) {
        queries.deleteById(id)
    }
}
```

**Затрагиваемые файлы:**
- `server/api/.../di/AppModule.kt`
- Создать `server/api/.../repositories/DatabaseCameraRepository.kt`
- `server/api/build.gradle.kts` — зависимости SQLDelight

---

## 6. Отсутствие CI/CD workflows

**Путь:** `.github/workflows/` — папка существует, но пустая  
**Проблема:** Нет автоматических сборок и тестов.

### Вариант (рекомендуемый): Создать CI workflows

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    name: Build & Test
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      
      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: ~/.gradle/caches
          key: gradle-${{ hashFiles('*.gradle.kts', 'gradle.properties') }}
      
      - name: Build core modules
        run: ./gradlew ciBuildCoreSharedModules
      
      - name: Run unit tests
        run: ./gradlew ciUnitSmokeSuite
      
      - name: Coverage gate
        run: ./gradlew ciCoverageGate

  security-scan:
    name: Security Scan
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run Trivy scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          format: 'sarif'
          output: 'trivy-results.sarif'
      
      - name: Upload Trivy results
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: 'trivy-results.sarif'

  docker:
    name: Build Docker
    needs: [build-and-test]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      
      - name: Build Docker image
        run: docker build -t ipcss-server .
```

```yaml
# .github/workflows/release.yml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    name: Create Release
    runs-on: ubuntu-latest
    permissions:
      contents: write
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build all
        run: ./gradlew assemble
      
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: |
            server/build/libs/*.jar
            androidApp/build/outputs/apk/release/*.apk
          generate_release_notes: true
```

---

## 7. cinterop без graceful degradation

**Файл:** `core/network/src/nativeInterop/`  
**Проблема:** Если Live555/FFmpeg cinterop не настроен — вся сборка падает.

### Вариант (рекомендуемый): Опциональный cinterop

```kotlin
// core/network/build.gradle.kts
kotlin {
    sourceSets {
        val nativeMain by getting {
            dependencies {
                // Делаем cinterop опциональным
                if (project.findProperty("ipcss.enableNativeInterop") == "true") {
                    // Только если явно включено
                }
            }
        }
    }
}

// Использование: ./gradlew build -Pipcss.enableNativeInterop=true
```

**В commonMain добавить интерфейс:**

```kotlin
// commonMain — интерфейс для RTSP
interface RtspClient {
    fun connect(url: String): Boolean
    fun disconnect()
    fun onFrame(callback: (ByteArray) -> Unit)
}

// Если native не доступен — fallback на FFmpeg процесс
expect fun createRtspClient(): RtspClient

// androidMain
actual fun createRtspClient(): RtspClient = AndroidRtspClient()

// jvmMain — используем FFmpeg через ProcessBuilder, если native библиотек нет
actual fun createRtspClient(): RtspClient = 
    if (hasNativeLibrary()) NativeRtspClient() else ProcessRtspClient()
```

---

## 8. JNA в KMP core/network

**Проблема:** Использование JNA (Java Native Access) непереносимо на iOS/JS.

### Вариант (рекомендуемый): Изоляция JNA в jvmMain

```kotlin
// jvmMain — только здесь используем JNA
package com.ipcamera.core.network.jna

import com.sun.jna.Native
import com.sun.jna.Pointer

interface RtspLibrary : com.sun.jna.Library {
    fun rtsp_connect(url: String): Pointer
    fun rtsp_read_frame(handle: Pointer): ByteArray?
}

object JnaRtspBridge {
    private val lib = Native.load("rtsp-native", RtspLibrary::class.java)
    
    fun