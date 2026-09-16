# Правки компиляции core:network — по шагам

Документ описывает внесённые исправления и оставшиеся проблемы для сборки `:server:api` (и пакета Synology).

**Последнее обновление:** продолжение исправлений — NativeAnalytics (Long), FloatRange, CertificatePinner в jvmMain, OnvifImagingParser.

---

## ✅ Выполненные правки

### 1. ApiClient — конфликт перегрузок (Conflicting overloads)

**Проблема:** Для target `desktop` компилируются оба source set — `jvmMain` и `desktopMain` (desktopMain dependsOn jvmMain). В обоих были объявления `actual fun ApiClient.Companion.createDefaultEngine()` и `createEngineWithPinning()`, из‑за чего возникал конфликт перегрузок.

**Решение:** Удалён дубликат — файл `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/ApiClient.jvm.kt`. Оставлена единственная реализация в `core/network/src/desktopMain/.../ApiClient.jvm.kt`.

---

### 2. WSDiscovery.jvm.kt — NodeList не имеет конструктора

**Проблема:** `org.w3c.dom.NodeList` — интерфейс, вызов `NodeList()` недопустим.

**Решение:** Добавлена функция-хелпер и её использование:

```kotlin
private fun emptyNodeList(): NodeList = object : NodeList {
    override fun item(index: Int): Node? = null
    override fun getLength(): Int = 0
}
// ...
) ?: emptyNodeList()
```

Файл: `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/WSDiscovery.jvm.kt`.

---

### 3. VideoDecoderDemoMain — Unresolved reference 'RtspStreamSimulator'

**Проблема:** `VideoDecoderDemoMain.kt` в `jvmMain` использовал `RtspStreamSimulator`, который объявлен только в `jvmTest`, из‑за чего при сборке main возникала ошибка. Дополнительно: тип параметра `frame` в лямбде и вызов suspend-функции.

**Решение:** Файл перенесён из `jvmMain` в `jvmTest`:
- Удалён: `core/network/src/jvmMain/.../video/VideoDecoderDemoMain.kt`
- Создан: `core/network/src/jvmTest/.../video/VideoDecoderDemoMain.kt`

В лямбде явно указан тип: `simulator.generateFrames(50).collect { frame: RtspFrame -> ... }`. Демо остаётся запускаемым как тест/демо из `jvmTest`.

---

### 4. NativeAnalytics — expect/actual и typealias

**Проблема:** Компилятор сообщал о несовпадении типов (expect использует `NativeMotionDetectorHandle` и др., actual — `Long`) или «no corresponding expected declaration» после переноса expect typealias в начало файла.

**Что сделано:** В `desktopMain` в начале файла `NativeAnalytics.jvm.kt` добавлены объявления actual typealias (перед `actual class`), дубликаты в конце файла удалены. Ожидалось, что так компилятор будет корректно связывать expect/actual. Для target `desktop` с двумя source set (jvmMain + desktopMain) связывание expect/actual по-прежнему может давать сбои.

**Рекомендация:** Имеет смысл перенести весь `NativeAnalytics.jvm.kt` (вместе с actual typealias) в `jvmMain`, чтобы для одного target (desktop) была одна actual-реализация в одном source set. Либо временно исключить использование `NativeAnalytics` из кода, собираемого для server:api (если аналитика там не нужна).

---

### 5. CertificatePinner.jvm.kt — "Declaration must be marked with 'actual'"

**Проблема:** Ошибка в строке 22, столбец 31: `Declaration must be marked with 'actual'`.

**Что проверено:** Класс и методы уже помечены как `actual`. Конструктор приведён к виду `actual class CertificatePinner(private val config: CertificatePinningConfig)`.

**Рекомендация:** Уточнить по сообщению компилятора, какой именно символ/объявление имеется в виду (например, параметр конструктора или вложенный тип). При необходимости добавить явную реализацию expect-членов или проверить, что в target `desktop` не подмешивается вторая реализация CertificatePinner из другого source set.

---

## ❌ Оставшиеся ошибки (по логу сборки)

После перечисленных правок сборка по-прежнему падает. В логе остаются ошибки в:

1. **commonMain/kotlin/.../OnvifImagingTypes.kt**  
   - `FloatRange?`, `Unresolved reference 'FloatRange'`, сериализаторы.

2. **commonMain/.../ApiClient.kt**  
   - Unresolved reference `info`, ограничения Public-API для inline, параметры `message`/`errorMessage`, `MultiPartFormDataContent`, `formData` и др.

3. **commonMain/.../ChunkingManager.kt**  
   - Доступ к `private val receivedChunks` в `ChunkedMessage`.

4. **commonMain/.../OnvifClient.kt**  
   - Unresolved reference `xml`, `requestTimeoutMillis`, `connectTimeoutMillis`, expect-классы без конструктора по умолчанию, `discover`, `xAddrs`, `Xml`, `XmlSerialName`, `XmlElement`, конфликтующие перегрузки и т.д.

5. **desktopMain/.../NativeAnalytics.jvm.kt**  
   - Связывание expect/actual (и при необходимости typealias), как в п. 4 выше.

6. **desktopMain/.../CertificatePinner.jvm.kt**  
   - Требование «Declaration must be marked with 'actual'» (п. 5 выше).

Эти места нужно разбирать по одному (типы, видимость, expect/actual, зависимости и версии библиотек).

---

## Краткая сводка

| Компонент              | Статус   | Действие |
|------------------------|----------|----------|
| ApiClient duplicate    | Исправлено | Удалён jvmMain ApiClient.jvm.kt |
| WSDiscovery NodeList   | Исправлено | Добавлен emptyNodeList() |
| VideoDecoderDemoMain   | Исправлено | Перенесён в jvmTest, указан тип RtspFrame |
| NativeAnalytics        | Частично | Typealias в начало; при необходимости перенос в jvmMain |
| CertificatePinner      | Не снято | Уточнить объявление на позиции 22:31 |
| OnvifImagingTypes      | Не правлено | FloatRange, сериализаторы |
| ApiClient commonMain   | Не правлено | Много ошибок видимости и API |
| OnvifClient            | Не правлено | XML, таймауты, expect-классы |
| ChunkingManager        | Не правлено | Доступ к private |

Для успешной сборки `:server:api` и пакета Synology нужно последовательно устранить оставшиеся ошибки в commonMain и в actual-реализациях (NativeAnalytics, CertificatePinner).
