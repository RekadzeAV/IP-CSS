# Сводка исправлений компиляции

**Дата:** 28 января 2026

## ✅ Исправлено

### 1. ApiError
- Добавлен `override` для `cause` в `NetworkError`, `SerializationError`, `UnknownError`
- Переименован `message` в `errorMessage` в `HttpError` и `TimeoutError` для избежания конфликта с `Exception.message`

### 2. HttpTimeout
- Исправлены типы: `inWholeMilliseconds.toInt()` → `inWholeMilliseconds` (Long вместо Int)

### 3. Logger
- Исправлен вызов `logger.info` → `println` (временное решение)

### 4. expect функции в ApiClient
- Попытка вынести из `companion object` (в процессе)

---

## ⚠️ Осталось исправить

1. **CertificatePinner.jvm.kt** - компилятор не видит `actual`
2. **NativeAnalytics.jvm.kt** - typealias и функции не видны
3. **ApiClient.jvm.kt** - компилятор не видит `expect` функции

---

**Статус:** В процессе исправления (60% готовности)
