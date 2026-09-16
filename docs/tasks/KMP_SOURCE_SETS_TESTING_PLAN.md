# План тестирования KMP Source Sets

**Дата:** 2026-06-14  
**Статус:** 🟢 **PLANNED**  
**Оценка:** 3-4 дня  
**Приоритет:** 🔥 HIGH

---

## Цель

Создать unit тесты для stub реализаций в KMP source sets модулей `core:network` и `core:common`.

---

## Область охвата

### Модуль: core:network

| Source Set | Файлы | Типы тестов | Статус |
|------------|-------|-------------|--------|
| **jvmMain** | `MediaFrame.jvm.kt`<br>`CertificatePinner.jvm.kt`<br>`ApiClient.jvm.kt`<br>`NetworkScanner.jvm.kt` | Unit тесты<br>Contract тесты | 🔴 TODO |
| **androidMain** | `MediaFrame.android.kt`<br>`VideoDecoder.android.kt`<br>`CertificatePinner.android.kt` | Contract тесты<br>Stub validation | 🔴 TODO |
| **desktopMain** | `VideoDecoder.jvm.kt` (с JavaCV) | Integration тесты<br>JavaCV тесты | 🔴 TODO |

### Модуль: core:common

| Source Set | Файлы | Типы тестов | Статус |
|------------|-------|-------------|--------|
| **jvmMain** | `SecureLocalDataEncryption.jvm.kt` | Unit тесты<br>Encryption тесты | 🔴 TODO |
| **androidMain** | `PasswordEncryption.android.kt` | Integration тесты<br>Keystore тесты | 🔴 TODO |
| **desktopMain** | `PasswordEncryption.jvm.kt` | Unit тесты<br>KeyStore тесты | 🟡 ЧАСТИЧНО |

---

## Задачи

### Задача 1: Тесты для jvmMain (core:network)

**Оценка:** 1-2 дня

#### 1.1 MediaFrame.jvm.kt

**Файл:** `core/network/src/jvmMain/kotlin/.../MediaFrame.jvm.kt`

**Тесты:**
```kotlin
class MediaFrameJvmTest {
    @Test
    fun `frame data should be non-empty`() {
        val frame = MediaFrame(ByteArray(1024), System.currentTimeMillis())
        assertTrue(frame.data.isNotEmpty())
    }
    
    @Test
    fun `frame timestamp should be positive`() {
        val frame = MediaFrame(ByteArray(1024), System.currentTimeMillis())
        assertTrue(frame.timestamp > 0)
    }
    
    @Test
    fun `frame should be immutable`() {
        val originalData = ByteArray(1024)
        val frame = MediaFrame(originalData, System.currentTimeMillis())
        
        // Попытка изменить данные должна не влиять на оригинал
        frame.data[0] = 0xFF.toByte()
        assertEquals(0.toByte(), originalData[0])
    }
}
```

#### 1.2 CertificatePinner.jvm.kt

**Файл:** `core/network/src/jvmMain/kotlin/.../CertificatePinner.jvm.kt`

**Тесты:**
```kotlin
class CertificatePinnerJvmTest {
    @Test
    fun `pinner should load config from file`() {
        val pinner = CertificatePinner()
        val config = pinner.loadConfig("test-config.json")
        assertNotNull(config)
    }
    
    @Test
    fun `pinner should validate certificate chain`() {
        val pinner = CertificatePinner()
        val isValid = pinner.validate("example.com", emptyList())
        // Зависит от конфигурации
    }
}
```

#### 1.3 ApiClient.jvm.kt

**Файл:** `core/network/src/jvmMain/kotlin/.../ApiClient.jvm.kt`

**Тесты:**
```kotlin
class ApiClientJvmTest {
    @Test
    fun `client should use Java engine by default`() {
        val client = ApiClient.create(ApiClientConfig("http://test.com"))
        assertNotNull(client)
    }
    
    @Test
    fun `client should handle timeout configuration`() {
        val config = ApiClientConfig(
            baseUrl = "http://test.com",
            connectTimeout = 5000,
            readTimeout = 10000
        )
        val client = ApiClient.create(config)
        assertNotNull(client)
    }
}
```

### Задача 2: Тесты для androidMain (core:network)

**Оценка:** 1 день

#### 2.1 MediaFrame.android.kt

**Файл:** `core/network/src/androidMain/kotlin/.../MediaFrame.android.kt`

**Тесты:**
```kotlin
class MediaFrameAndroidTest {
    @Test
    fun `android frame should use Bitmap if available`() {
        // Тест для Android-specific реализации
    }
}
```

#### 2.2 VideoDecoder.android.kt (Stub)

**Файл:** `core/network/src/androidMain/kotlin/.../VideoDecoder.android.kt`

**Тесты:**
```kotlin
class VideoDecoderAndroidStubTest {
    @Test
    fun `stub decoder should return null`() {
        val decoder = VideoDecoder()
        val result = decoder.decode(ByteArray(1024))
        assertNull(result, "Stub should return null")
    }
    
    @Test
    fun `stub decoder close should not throw`() {
        val decoder = VideoDecoder()
        decoder.close() // Не должно бросать исключение
    }
}
```

#### 2.3 CertificatePinner.android.kt (Stub)

**Файл:** `core/network/src/androidMain/kotlin/.../CertificatePinner.android.kt`

**Тесты:**
```kotlin
class CertificatePinnerAndroidStubTest {
    @Test
    fun `stub pinner should have empty config`() {
        val pinner = CertificatePinner()
        // Проверка дефолтной конфигурации
    }
}
```

### Задача 3: Тесты для desktopMain (core:network)

**Оценка:** 1-2 дня

#### 3.1 VideoDecoder.jvm.kt (с JavaCV)

**Файл:** `core/network/src/desktopMain/kotlin/.../VideoDecoder.jvm.kt`

**Тесты:**
```kotlin
class VideoDecoderDesktopTest {
    @Test
    fun `decoder should initialize FFmpeg`() {
        val decoder = VideoDecoder()
        assertNotNull(decoder)
    }
    
    @Test
    fun `decoder should decode H264 frame`() {
        val decoder = VideoDecoder()
        val h264Data = loadTestH264Frame()
        val frame = decoder.decode(h264Data)
        assertNotNull(frame)
    }
    
    @Test
    fun `decoder should handle invalid data gracefully`() {
        val decoder = VideoDecoder()
        val invalidData = ByteArray(100) { 0xFF }
        val frame = decoder.decode(invalidData)
        assertNull(frame, "Invalid data should return null")
    }
    
    @Test
    fun `decoder should release resources on close`() {
        val decoder = VideoDecoder()
        decoder.decode(ByteArray(1024))
        decoder.close()
        // Проверка что ресурсы освобождены
    }
    
    @Test
    fun `decoder should handle multiple frames`() {
        val decoder = VideoDecoder()
        repeat(100) {
            val frame = decoder.decode(loadTestH264Frame())
            assertNotNull(frame)
        }
        decoder.close()
    }
    
    private fun loadTestH264Frame(): ByteArray {
        // Загрузка тестового кадра из resources
        return VideoDecoderDesktopTest::class.java
            .getResourceAsStream("/test-h264-frame.bin")
            ?.readBytes() ?: ByteArray(0)
    }
}
```

---

## Структура тестовых файлов

### commonTest (общие тесты)
```
core/network/src/commonTest/kotlin/
├── MediaFrameContractTest.kt
├── CertificatePinnerContractTest.kt
└── ApiClientContractTest.kt
```

### jvmTest (JVM-specific тесты)
```
core/network/src/jvmTest/kotlin/
├── MediaFrameJvmTest.kt
├── CertificatePinnerJvmTest.kt
└── ApiClientJvmTest.kt
```

### androidTest (Android-specific тесты)
```
core/network/src/androidTest/kotlin/
├── MediaFrameAndroidTest.kt
├── VideoDecoderAndroidStubTest.kt
└── CertificatePinnerAndroidStubTest.kt
```

### desktopTest (Desktop-specific тесты)
```
core/network/src/desktopTest/kotlin/
├── VideoDecoderDesktopTest.kt
└── NativeUtilsDesktopTest.kt
```

---

## Зависимости для тестирования

### build.gradle.kts (core:network)

```kotlin
kotlin {
    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.mockk:mockk:1.13.5")
            }
        }
        
        jvmTest {
            dependencies {
                implementation("org.bytedeco:javacv:1.5.13")
                implementation("org.bytedeco:ffmpeg:5.1.2-1.5.9")
            }
        }
        
        androidUnitTest {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("androidx.test:core:1.5.0")
            }
        }
        
        // Android instrumented тесты
        androidAndroidTest {
            dependencies {
                implementation("androidx.test.ext:junit:1.1.5")
            }
        }
        
        desktopTest {
            dependencies {
                implementation("org.bytedeco:javacv-platform:1.5.13")
            }
        }
    }
}
```

---

## План выполнения

### День 1: jvmMain тесты

**Утро:**
- [ ] Создать `MediaFrameJvmTest.kt`
- [ ] Создать `CertificatePinnerJvmTest.kt`

**День:**
- [ ] Создать `ApiClientJvmTest.kt`
- [ ] Запустить тесты: `.\gradlew :core:network:jvmTest`

### День 2: androidMain тесты

**Утро:**
- [ ] Создать `VideoDecoderAndroidStubTest.kt`
- [ ] Создать `CertificatePinnerAndroidStubTest.kt`

**День:**
- [ ] Создать `MediaFrameAndroidTest.kt`
- [ ] Запустить тесты: `.\gradlew :core:network:testDebugUnitTest`

### День 3: desktopMain тесты

**Утро:**
- [ ] Создать `VideoDecoderDesktopTest.kt`
- [ ] Добавить тестовые файлы в resources

**День:**
- [ ] Запустить тесты: `.\gradlew :core:network:desktopTest`
- [ ] Исправить проблемы с JavaCV

### День 4: Документация и ревью

**Утро:**
- [ ] Обновить `core/network/README.md` с информацией о тестировании
- [ ] Создать `docs/TESTING_GUIDE.md`

**День:**
- [ ] Code review всех тестов
- [ ] Финальная проверка сборки

---

## Критерии приемки

- [ ] Все тесты для jvmMain проходят (100% success rate)
- [ ] Stub тесты для androidMain проходят
- [ ] Desktop тесты с JavaCV проходят (при наличии библиотек)
- [ ] Coverage > 70% для stub реализаций
- [ ] Документация обновлена
- [ ] Нет предостережений компилятора в тестах

---

## Риски

1. **JavaCV не установлена** — тесты Desktop могут не запуститься
   - **Mitigation:** Использовать `@Ignore` для интеграционных тестов

2. **Android эмулятор недоступен** — instrumented тесты не запустятся
   - **Mitigation:** Использовать `androidUnitTest` для unit тестов

3. **Отсутствие тестовых RTSP потоков** — integration тесты не пройдут
   - **Mitigation:** Использовать mock или заглушки

---

## Связанные задачи

- Task #1: Исправить PasswordEncryptionContractTest ✅
- Task #2: Исправить shared:desktopTest компиляцию ⚠️
- Task #3: Gradle Build Cache (следующий шаг)

---

**Автор:** Koda AI Assistant  
**Дата создания:** 2026-06-14