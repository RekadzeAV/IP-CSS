# Руководство по KMP Source Sets для разработчиков

Это руководство поможет новым разработчикам понять архитектуру Kotlin Multiplatform source sets в проекте IP-CSS.

---

## 📋 Содержание

1. [Архитектура Source Sets](#архитектура-source-sets)
2. [Иерархия зависимостей](#иерархия-зависимостей)
3. [Правила размещения кода](#правила-размещения-кода)
4. [Expect/Actual паттерн](#expectactual-паттерн)
5. [Частые ошибки](#частые-ошибки)
6. [Примеры](#примеры)

---

## Архитектура Source Sets

### Общая структура

```
core/network/src/
├── commonMain/              # Общий код для всех платформ
│   ├── kotlin/
│   │   └── com/company/ipcamera/core/network/
│   │       ├── ApiClient.kt
│   │       ├── NetworkScanner.kt
│   │       └── RtspClient.kt
│   └── resources/
│
├── jvmMain/                 # Общий JVM код (Android + Desktop)
│   ├── kotlin/
│   │   └── com/company/ipcamera/core/network/
│   │       ├── Live555RTSPClient.jvm.kt
│   │       ├── RTSPSClient.jvm.kt
│   │       ├── UPnPDiscovery.jvm.kt
│   │       ├── WSDiscovery.jvm.kt
│   │       ├── CertificatePinner.jvm.kt
│   │       ├── ApiClient.jvm.kt
│   │       └── MediaFrame.jvm.kt
│   └── resources/
│
├── androidMain/             # Android-специфичный код
│   ├── kotlin/
│   │   └── com/company/ipcamera/core/network/
│   │       ├── MediaFrame.android.kt
│   │       ├── VideoDecoder.android.kt
│   │       └── CertificatePinner.android.kt
│   └── res/
│
├── desktopMain/             # Desktop (JVM) специфичный код
│   ├── kotlin/
│   │   └── com/company/ipcamera/core/network/
│   │       ├── VideoDecoder.jvm.kt (с JavaCV)
│   │       └── [другие Desktop-specific реализации]
│   └── resources/
│
└── [nativeMain/]/           # Нативные таргеты (отключены временно)
    ├── kotlin/
    └── resources/
```

### Иерархия зависимостей

```
commonMain (все платформы)
    ↓
    jvmMain (общий JVM код)
    ↓
    ├─→ androidMain (Android-specific)
    └─→ desktopMain (Desktop-specific)
```

**Правило:** Источник может зависеть только от своих родителей, не наоборот.

---

## Иерархия зависимостей

### Gradle конфигурация

В `core/network/build.gradle.kts`:

```kotlin
kotlin {
    androidTarget {
        // Настройки Android
    }

    jvm("desktop") {
        // Настройки Desktop
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Общие зависимости
            }
        }

        val jvmMain by creating {
            dependsOn(commonMain)  // ← JVM видит common
            dependencies {
                // JVM-специфичные зависимости
            }
        }

        val androidMain by getting {
            dependsOn(jvmMain)     // ← Android видит jvm + common
            dependencies {
                implementation(libs.ktor.client.android)
            }
        }

        val desktopMain by getting {
            dependsOn(jvmMain)     // ← Desktop видит jvm + common
            dependencies {
                implementation(libs.ktor.client.java)
                implementation("org.bytedeco:javacv:1.5.13")
            }
        }
    }
}
```

### Что может видеть каждый source set

| Source Set | Видит | Не видит |
|------------|-------|----------|
| `commonMain` | Только себя | Всё остальное |
| `jvmMain` | `commonMain` | `androidMain`, `desktopMain` |
| `androidMain` | `jvmMain`, `commonMain` | `desktopMain` |
| `desktopMain` | `jvmMain`, `commonMain` | `androidMain` |

---

## Правила размещения кода

### ✅ Правильно

**1. Общий интерфейс/класс в commonMain:**
```kotlin
// commonMain/kotlin/.../MediaFrame.kt
expect class MediaFrame(
    val data: ByteArray,
    val timestamp: Long
)
```

**2. JVM-общая реализация в jvmMain:**
```kotlin
// jvmMain/kotlin/.../MediaFrame.jvm.kt
actual class MediaFrame actual constructor(
    actual val data: ByteArray,
    actual val timestamp: Long
) {
    // Общая JVM реализация
}
```

**3. Android-specific в androidMain:**
```kotlin
// androidMain/kotlin/.../MediaFrame.android.kt
actual class MediaFrame actual constructor(
    actual val data: ByteArray,
    actual val timestamp: Long
) {
    // Android-specific код
}
```

**4. Desktop-specific в desktopMain:**
```kotlin
// desktopMain/kotlin/.../VideoDecoder.jvm.kt
class VideoDecoder {
    // Используем JavaCV только для Desktop
    private val decoder = org.bytedeco.javacv.FFmpegFrameDecoder()
}
```

### ❌ Неправильно

**1. JVM-код в commonMain:**
```kotlin
// ❌ ОШИБКА: commonMain не может использовать JVM-специфичные API
import java.nio.ByteBuffer  // Не доступно на iOS!

class NetworkScanner {
    fun scan() {
        val buffer = ByteBuffer.allocate(1024)  // ❌ Не компилируется
    }
}
```

**2. Android-код в jvmMain:**
```kotlin
// ❌ ОШИБКА: jvmMain не знает про Android API
import android.content.Context  // Не доступно на Desktop!

fun getContext(): Context {
    return Context()  // ❌ Не компилируется
}
```

**3. Прямая зависимость desktopMain от androidMain:**
```kotlin
// ❌ ОШИБКА: desktopMain не может зависеть от androidMain
val androidMain by getting {
    dependsOn(desktopMain)  // ❌ Невозможно
}
```

---

## Expect/Actual паттерн

### Когда использовать

Используйте `expect/actual` когда:
- Нужно объявить общий интерфейс в `commonMain`
- Но реализация отличается на разных платформах
- Или реализация общая для нескольких платформ

### Пример 1: Простой expect/actual

**commonMain:**
```kotlin
expect object CertificatePinningConfigLoader {
    fun readFile(path: String): String?
}
```

**jvmMain:**
```kotlin
actual object CertificatePinningConfigLoader {
    actual fun readFile(path: String): String? {
        return java.io.File(path).readText()
    }
}
```

**androidMain:**
```kotlin
actual object CertificatePinningConfigLoader {
    actual fun readFile(path: String): String? {
        // Android assets
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }
}
```

### Пример 2: Expect class с конструктором

**commonMain:**
```kotlin
expect class MediaFrame(
    data: ByteArray,
    timestamp: Long
) {
    fun getBytes(): ByteArray
}
```

**jvmMain:**
```kotlin
actual class MediaFrame actual constructor(
    actual val data: ByteArray,
    actual val timestamp: Long
) {
    actual fun getBytes(): ByteArray {
        return data
    }
}
```

### Пример 3: Expect fun с платформо-специфичной реализацией

**commonMain:**
```kotlin
expect fun createDefaultHttpClientEngine(): Any
```

**jvmMain:**
```kotlin
actual fun createDefaultHttpClientEngine(): Any {
    return io.ktor.client.engine.java.Java.create()
}
```

**androidMain:**
```kotlin
actual fun createDefaultHttpClientEngine(): Any {
    return io.ktor.client.engine.android.Android.create()
}
```

---

## Частые ошибки

### Ошибка 1: JVM-код в commonMain

**Симптом:**
```
Unresolved reference: java.nio
```

**Решение:**
Переместите код в `jvmMain` или используйте `expect/actual`.

### Ошибка 2: Неправильный порядок dependsOn

**Симптом:**
```
Source set 'androidMain' depends on 'desktopMain' which is not allowed
```

**Решение:**
Используйте правильную иерархию:
```kotlin
androidMain.dependsOn(jvmMain)
desktopMain.dependsOn(jvmMain)
```

### Ошибка 3: Missing 'actual' modifier

**Симптом:**
```
Declaration must be marked with 'actual'
```

**Решение:**
Добавьте `actual` к реализации:
```kotlin
// Было:
fun readFile(path: String): String? { ... }

// Стало:
actual fun readFile(path: String): String? { ... }
```

### Ошибка 4: Конфликт имён файлов

**Симптом:**
```
Found multiple matches for oldString
```

**Решение:**
Убедитесь, что в разных source sets нет файлов с одинаковыми путями, если они не являются expect/actual парами.

---

## Примеры

### Пример 1: Создание нового HTTP клиента

**Шаг 1: Объявить в commonMain**
```kotlin
// commonMain/kotlin/.../ApiClient.kt
class ApiClient private constructor(
    private val engine: HttpClientEngine
) {
    companion object {
        fun create(config: ApiClientConfig): ApiClient {
            val engine = createDefaultEngine()
            return ApiClient(engine)
        }

        expect fun createDefaultEngine(): HttpClientEngine
    }
}
```

**Шаг 2: Реализовать для JVM в jvmMain**
```kotlin
// jvmMain/kotlin/.../ApiClient.jvm.kt
actual fun ApiClient.Companion.createDefaultEngine(): HttpClientEngine {
    return io.ktor.client.engine.java.Java.create()
}
```

**Шаг 3: Переопределить для Android в androidMain (если нужно)**
```kotlin
// androidMain/kotlin/.../ApiClient.android.kt
actual fun ApiClient.Companion.createDefaultEngine(): HttpClientEngine {
    return io.ktor.client.engine.android.Android.create()
}
```

### Пример 2: Видео декодер

**Шаг 1: Stub в commonMain**
```kotlin
// commonMain/kotlin/.../VideoDecoder.kt
expect class VideoDecoder {
    fun decode(data: ByteArray): Frame?
    fun close()
}
```

**Шаг 2: Stub в jvmMain**
```kotlin
// jvmMain/kotlin/.../VideoDecoder.jvm.kt
actual class VideoDecoder {
    actual fun decode(data: ByteArray): Frame? = null
    actual fun close() {}
}
```

**Шаг 3: Полная реализация в desktopMain**
```kotlin
// desktopMain/kotlin/.../VideoDecoder.jvm.kt
actual class VideoDecoder {
    private val decoder = FFmpegFrameDecoder()
    
    actual fun decode(data: ByteArray): Frame? {
        return decoder.decodeFrame(data)
    }
    
    actual fun close() {
        decoder.close()
    }
}
```

**Шаг 4: Stub в androidMain**
```kotlin
// androidMain/kotlin/.../VideoDecoder.android.kt
actual class VideoDecoder {
    actual fun decode(data: ByteArray): Frame? = null
    actual fun close() {}
}
```

---

## Проверка перед коммитом

Перед коммитом нового кода проверьте:

- [ ] Код размещён в правильном source set
- [ ] Нет прямых зависимостей между siblings (androidMain ↔ desktopMain)
- [ ] Все expect имеют actual реализации
- [ ] Файлы с одинаковым именем имеют `expect`/`actual` модификаторы
- [ ] Компиляция проходит для всех таргетов

### Команды проверки

```bash
# Проверка Android
./gradlew :core:network:compileDebugKotlinAndroid

# Проверка Desktop
./gradlew :core:network:compileKotlinDesktop

# Проверка metadata (KMP contract)
./gradlew :core:network:compileKotlinMetadata

# Полная сборка (без нативных таргетов)
./gradlew :core:network:assembleDebug :core:network:assembleDesktop
```

---

## Ссылки

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Hierarchical Source Sets](https://kotlinlang.org/docs/mpp-share-on-platforms.html#use-hierarchical-source-sets)
- [Expect/Actual](https://kotlinlang.org/docs/mpp-connect-to-apis.html)
- [Project Architecture](ARCHITECTURE.md)
- [Native Targets Setup](NATIVE_TARGETS_SETUP_GUIDE.md)

---

**Версия документа:** 1.0  
**Дата последнего обновления:** 2026-06-14  
**Автор:** Koda AI Assistant