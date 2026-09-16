# Task: Refactor RtspLongRunStabilityTest for Desktop Compilation

**Дата создания:** 2026-06-14  
**Статус:** 🔴 **BLOCKED**  
**Приоритет:** 🟡 MEDIUM  
**Оценка:** 2-3 дня  
**Связанные задачи:** High Priority Task #2

---

## Проблема

Файл `shared/src/desktopTest/kotlin/com/company/ipcamera/shared/test/RtspLongRunStabilityTest.kt` содержит ошибки компиляции, которые блокируют запуск Desktop тестов:

### Ошибки компиляции

1. **Unresolved reference 'start'** (line 227)
   ```kotlin
   rtspClient.start()  // ❌ Метод не существует
   ```

2. **Unresolved reference 'streamStartTime'** (line 267)
   ```kotlin
   val streamStartTime = System.currentTimeMillis()  // Объявлено ПОСЛЕ использования
   ```

3. **Unresolved reference 'toFixed'** (line 480)
   ```kotlin
   result.averageFps.toFixed(2)  // ❌ Extension function определена в конце файла
   ```

### Анализ кода

Файл содержит ~550 строк кода для long-run тестирования RTSP стабильности:
- Мониторинг FPS, памяти, переподключений
- Генерация отчётов в Markdown
- Обработка ошибок и reconnect логика
- Интеграция с `RtspClient`

**Проблема:** Код написан с предположением, что `RtspClient` имеет метод `start()`, но в текущей реализации такого метода нет.

---

## Анализ RtspClient API

### Текущий API (core/network/src/commonMain/kotlin/.../RtspClient.kt)

```kotlin
class RtspClient(
    private val config: RtspClientConfig
) : RtspClientInterface {
    
    suspend fun connect(): Boolean
    suspend fun disconnect()
    suspend fun play()
    suspend fun pause()
    fun close()
    
    // Нет метода start()!
}
```

### Ожидаемый API (из теста)

```kotlin
class RtspClient {
    fun start()  // ❌ Не существует
    fun stop()   // ❌ Не существует
}
```

---

## Варианты решения

### Вариант 1: Исправить тест под текущий API (РЕКОМЕНДУЕТСЯ)

Заменить `rtspClient.start()` на корректные вызовы:

```kotlin
// Было:
rtspClient.start()

// Стало:
rtspClient.connect()
rtspClient.play()
```

**Плюсы:**
- Минимальные изменения
- Не требует изменения RtspClient API
- Быстро реализуемо

**Минусы:**
- Нужно проверить логику мониторинга потока

### Вариант 2: Добавить метод start() в RtspClient

```kotlin
class RtspClient {
    suspend fun start() {
        connect()
        play()
    }
    
    suspend fun stop() {
        pause()
        disconnect()
    }
}
```

**Плюсы:**
- Более удобный API для тестов
- Упрощает использование

**Минусы:**
- Требует изменения core/network модуля
- Может сломать другие зависимости

### Вариант 3: Пропустить тесты для Desktop (ВРЕМЕННОЕ РЕШЕНИЕ)

Добавить `@Ignore` ко всем тестам в классе:

```kotlin
@Ignore("Requires RTSP server and fixed compilation errors")
class RtspLongRunStabilityTest {
    // ...
}
```

**Плюсы:**
- Быстро
- Позволяет запустить другие тесты

**Минусы:**
- Не решает проблему
- Тесты никогда не выполнятся

---

## План реализации (Вариант 1)

### Шаг 1: Исправить вызовы RTSP клиента

**Файл:** `shared/src/desktopTest/kotlin/.../RtspLongRunStabilityTest.kt`

**Изменения:**

```kotlin
// Line 227: Исправить start() → connect() + play()
- rtspClient.start()
+ rtspClient.connect()
+ rtspClient.play()

// Line 267: Исправить порядок объявления streamStartTime
- while (System.currentTimeMillis() - streamStartTime < (durationMinutes * 60 * 1000L)) {
+ val streamStartTime = System.currentTimeMillis()
+ while (System.currentTimeMillis() - streamStartTime < (durationMinutes * 60 * 1000L)) {

// Line 480: Переместить toFixed() вверх файла или использовать String.format
- result.averageFps.toFixed(2)
+ String.format("%.2f", result.averageFps)
```

### Шаг 2: Проверить зависимости

**Проверить:**
- [ ] `RtspClientInterface` имеет методы `connect()` и `play()`
- [ ] Методы `suspend` требуют `runBlocking` или `coroutineScope`
- [ ] Все импорты корректны

### Шаг 3: Запустить компиляцию

```bash
.\gradlew :shared:compileTestKotlinDesktop --no-daemon
```

**Ожидаемый результат:** BUILD SUCCESSFUL

### Шаг 4: Запустить тесты (опционально)

```bash
.\gradlew :shared:desktopTest --tests RtspLongRunStabilityTest --no-daemon
```

**Примечание:** Тесты требуют реального RTSP сервера для выполнения.

---

## Дополнительные проблемы в файле

### 1. Несоответствие типов

```kotlin
// Line 480: toFixed() определена в конце файла
private fun Double.toFixed(digits: Int): String {
    return String.format("%.${digits}f", this)
}

// Но используется до объявления — Kotlin не поддерживает forward declaration для функций
```

**Решение:** Переместить функцию в начало класса или использовать `String.format()`.

### 2. Отсутствие RTSP сервера для тестов

Тесты требуют реального RTSP сервера:
```kotlin
val rtspUrl = "rtsp://demo:demo@192.168.1.100:554/stream"
```

**Решение:**
- Использовать тестовый RTSP сервер (например, `ffserver` или `VLC`)
- Или добавить `@Ignore` для integration тестов

### 3. Зависимость от внешних библиотек

```kotlin
import com.google.gson.Gson  // ❌ Android-specific
```

**Решение:** Заменить на `kotlinx.serialization` или удалить, если не используется.

---

## Тестовые сценарии

После исправления протестировать:

1. **Компиляция Desktop:**
   ```bash
   .\gradlew :shared:compileTestKotlinDesktop --no-daemon
   ```

2. **Компиляция Android:**
   ```bash
   .\gradlew :shared:compileTestKotlinAndroid --no-daemon
   ```

3. **Запуск Desktop тестов (если есть RTSP сервер):**
   ```bash
   .\gradlew :shared:desktopTest --tests RtspLongRunStabilityTest --no-daemon
   ```

---

## Критерии приемки

- [ ] Компиляция Desktop тестов успешна
- [ ] Компиляция Android тестов успешна
- [ ] Нет предупреждений компилятора
- [ ] Код соответствует стилю проекта
- [ ] Документация обновлена (если требуется)

---

## Связанные файлы

- `shared/src/desktopTest/kotlin/com/company/ipcamera/shared/test/RtspLongRunStabilityTest.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/RtspClient.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/RtspClientInterface.kt`

---

## История изменений

| Дата | Автор | Изменение |
|------|-------|-----------|
| 2026-06-14 | Koda AI | Создание задачи |

---

**Автор:** Koda AI Assistant  
**Дата создания:** 2026-06-14