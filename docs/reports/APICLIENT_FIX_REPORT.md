# Отчет: Исправление ошибок в ApiClient.kt

**Дата:** 28 января 2026

## ✅ Выполнено

### Проблема
**Строки 587 и 612:** `Argument type mismatch: actual type is 'io.ktor.client.statement.HttpResponse', but 'io.ktor.client.request.HttpRequestBuilder' was expected`

### Причина
Интерцепторы `onResponse` и `onError` ожидают `HttpRequestBuilder` в качестве первого параметра, но в код передавался результат вызова `httpClient.request()`, который возвращает `HttpResponse`.

### Решение

#### 1. Строка 587 (onResponse)
**Было:**
```kotlin
val finalResponse = interceptorChain?.onResponse(
    httpClient.request(path) { this.method = method },
    response
) ?: response
```

**Стало:**
```kotlin
// Сохраняем builder при создании запроса
var savedRequestBuilder: HttpRequestBuilder? = null

val response = httpClient.request(path) {
    savedRequestBuilder = this
    this.method = method
    // ... остальная конфигурация
}

// Используем сохраненный builder для интерцептора
val requestBuilderForInterceptor = savedRequestBuilder ?: run {
    HttpRequestBuilder().apply {
        this.method = method
        url.takeFrom(config.baseUrl + path)
        queryParameters.forEach { (key, value) ->
            parameter(key, value)
        }
    }
}
val finalResponse = interceptorChain?.onResponse(
    requestBuilderForInterceptor,
    response
) ?: response
```

#### 2. Строка 612 (onError)
**Было:**
```kotlin
interceptorChain?.onError(
    httpClient.request(path) { this.method = method },
    e
)
```

**Стало:**
```kotlin
val requestBuilder = HttpRequestBuilder().apply {
    this.method = method
    url.takeFrom(config.baseUrl + path)
    queryParameters.forEach { (key, value) ->
        parameter(key, value)
    }
}
interceptorChain?.onError(
    requestBuilder,
    e
)
```

---

## 📊 Итоговый статус

| Строка | Проблема | Статус |
|--------|----------|--------|
| 587 | `Argument type mismatch` (onResponse) | ✅ Исправлено |
| 612 | `Argument type mismatch` (onError) | ✅ Исправлено |

---

## 🎯 Результат

**✅ Ошибки на строках 587 и 612 исправлены!**

Теперь интерцепторы получают правильный тип `HttpRequestBuilder` вместо `HttpResponse`.

---

## ⚠️ Остающиеся проблемы (не связаны с строками 587 и 612)

1. **Строка 481:** `Unresolved reference` (не связано с исправлением)
2. **Строка 488:** `Public-API inline function cannot access non-public-API` (не связано с исправлением)
3. **Строка 490:** `Public-API inline function cannot access non-public-API` (не связано с исправлением)
4. **ApiClient.jvm.kt:** Проблемы с `expect`/`actual` (не связано с исправлением)

---

**Статус:** ✅ **Ошибки на строках 587 и 612 исправлены**

**Файл обновлен:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt`
