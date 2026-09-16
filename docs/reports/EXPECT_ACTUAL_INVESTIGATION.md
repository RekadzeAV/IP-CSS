# Исследование проблемы expect/actual в Kotlin Multiplatform

**Дата:** 28 января 2026

## Проблема

Компилятор Kotlin не видит `expect` декларации из `commonMain` при компиляции `desktopMain`, хотя:
- Все файлы на месте
- Пакеты совпадают
- Source sets правильно настроены
- `desktopMain` зависит от `jvmMain`, который зависит от `commonMain`

## Структура проекта

```
core/network/
├── src/
│   ├── commonMain/
│   │   └── kotlin/.../
│   │       ├── security/CertificatePinner.kt (expect class)
│   │       ├── analytics/NativeAnalytics.kt (expect class)
│   │       └── ApiClient.kt (expect fun)
│   ├── jvmMain/
│   │   └── kotlin/.../
│   │       ├── security/CertificatePinner.jvm.kt (actual class)
│   │       ├── analytics/NativeAnalytics.jvm.kt (actual class)
│   │       └── ApiClient.jvm.kt (actual fun)
│   └── desktopMain/ (создается автоматически для jvm("desktop"))
│       └── kotlin/.../ (пусто или использует jvmMain)
```

## Конфигурация build.gradle.kts

```kotlin
kotlin {
    jvm("desktop") {
        // desktopMain создается автоматически
    }

    sourceSets {
        val commonMain by getting { ... }

        val jvmMain by creating {
            dependsOn(commonMain)
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependsOn(commonMain)  // Добавлено явно
        }
    }
}
```

## Ошибки компиляции

1. **CertificatePinner.jvm.kt:22** - `Declaration must be marked with 'actual'`
   - Класс уже помечен как `actual class CertificatePinner`
   - Компилятор не видит `expect class CertificatePinner` из commonMain

2. **NativeAnalytics.jvm.kt** - Множественные ошибки:
   - `actual typealias` не видны (строки 328-332)
   - `actual fun destroyANPREngine` не видна (строка 287)
   - `actual fun createObjectTracker` не видна (строка 295)
   - `actual suspend fun updateTracking` не видна (строка 308)
   - `actual fun destroyObjectTracker` не видна (строка 319)

## Возможные причины

1. **Порядок компиляции source sets**
   - `desktopMain` компилируется, но не видит `commonMain` напрямую
   - `jvmMain` создается вручную, что может влиять на видимость

2. **Версия Kotlin Multiplatform**
   - Возможно, это известная проблема в определенной версии

3. **Особенности компилятора**
   - Компилятор может не видеть expect декларации через промежуточный source set

## Попытки решения

1. ✅ Добавлена явная зависимость `desktopMain` от `commonMain`
2. ✅ Удален `inline` из suspend функций в ApiClient.kt
3. ✅ Очищен кэш Gradle
4. ⏳ Перемещение actual реализаций в desktopMain (в процессе)

## Рекомендации

1. Проверить версию Kotlin и обновить, если необходимо
2. Попробовать использовать `jvmMain` напрямую вместо `desktopMain`
3. Изучить документацию Kotlin Multiplatform по expect/actual
4. Временное решение: закомментировать проблемные части

---

**Статус:** В процессе исследования
