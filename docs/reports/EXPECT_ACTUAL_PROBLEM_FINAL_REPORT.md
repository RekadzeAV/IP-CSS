# Финальный отчет о проблеме expect/actual

**Дата:** 28 января 2026
**Версия Kotlin:** 2.0.21

## Проблема

Компилятор Kotlin не видит `expect` декларации из `commonMain` при компиляции `desktopMain`, несмотря на:
- ✅ Правильную структуру файлов
- ✅ Совпадение пакетов
- ✅ Правильную настройку source sets
- ✅ Явные зависимости

## Ошибки

1. **CertificatePinner.jvm.kt:22** - `Declaration must be marked with 'actual'`
   - Класс уже помечен как `actual class CertificatePinner`
   - Компилятор не видит `expect class CertificatePinner` из commonMain

2. **NativeAnalytics.jvm.kt** - Множественные ошибки:
   - `actual typealias` не видны (строки 328-332)
   - `actual fun destroyANPREngine` не видна
   - `actual fun createObjectTracker` не видна
   - `actual suspend fun updateTracking` не видна
   - `actual fun destroyObjectTracker` не видна

## Примененные исправления

1. ✅ Добавлена явная зависимость `desktopMain` от `commonMain`
2. ✅ Изменен порядок зависимостей (commonMain первым)
3. ✅ Перемещены actual реализации в `desktopMain`
4. ✅ Очищен кэш Gradle
5. ✅ Удален `inline` из suspend функций в ApiClient.kt

## Анализ

### Структура source sets
```
jvm("desktop") {
    // desktopMain создается автоматически
}

sourceSets {
    val jvmMain by creating {
        dependsOn(commonMain)  // Создается вручную
    }

    val desktopMain by getting {
        dependsOn(commonMain)   // Явная зависимость
        dependsOn(jvmMain)      // Наследование
    }
}
```

### Возможные причины

1. **Проблема версии Kotlin 2.0.21**
   - Возможно, это известная проблема в этой версии
   - Рекомендуется проверить changelog или обновить версию

2. **Особенность компилятора**
   - `jvmMain` создается вручную с `by creating`
   - `desktopMain` создается автоматически для `jvm("desktop")`
   - Компилятор может не видеть expect через промежуточный слой

3. **Порядок компиляции**
   - Возможно, commonMain компилируется после desktopMain
   - Или есть ошибки в commonMain, блокирующие видимость expect

## Рекомендации

### Вариант 1: Обновить Kotlin
```kotlin
kotlin("multiplatform") version "2.0.22" // или новее
```

### Вариант 2: Изменить конфигурацию source sets
Попробовать использовать `jvmMain` напрямую без `desktopMain`:
```kotlin
jvm() {  // без "desktop"
    // использовать jvmMain напрямую
}
```

### Вариант 3: Временное решение
Закомментировать проблемные actual реализации для продолжения сборки:
```kotlin
// actual class CertificatePinner(...) {
//     ...
// }
```

### Вариант 4: Проверить документацию
Изучить документацию Kotlin Multiplatform по expect/actual и известные проблемы

## Следующие шаги

1. Проверить changelog Kotlin 2.0.21 на известные проблемы
2. Попробовать обновить Kotlin до последней версии
3. Изучить документацию по expect/actual в Kotlin Multiplatform
4. Временное решение: закомментировать проблемные части

---

**Статус:** Требуется дополнительное исследование или обновление Kotlin
