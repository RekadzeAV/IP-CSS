# Исправление CertificatePinner и NativeAnalytics

**Дата:** 28 января 2026

## Проблема

Компилятор не видит `expect` декларации для:
1. `CertificatePinner` - `expect class` в commonMain, `actual class` в jvmMain
2. `NativeAnalytics` - `expect class` и `expect typealias` в commonMain, `actual` реализации в jvmMain

## Исследование

### Версия Kotlin
- Kotlin 2.0.21 (из build.gradle.kts)

### Структура source sets
```
desktopMain (автоматически создается для jvm("desktop"))
  ├── dependsOn(commonMain) - добавлено явно
  └── dependsOn(jvmMain) - через наследование
```

### Проверка соответствия
- ✅ Пакеты совпадают
- ✅ Имена классов совпадают
- ✅ Файлы на месте
- ❌ Компилятор не видит expect декларации

## Попытки решения

1. ✅ Добавлена явная зависимость desktopMain от commonMain
2. ✅ Изменен порядок зависимостей (commonMain первым)
3. ✅ Очищен кэш Gradle
4. ⏳ Проверка конфигурации source sets

## Следующие шаги

1. Проверить, правильно ли настроен jvm("desktop") target
2. Попробовать использовать jvmMain напрямую
3. Временное решение: закомментировать проблемные части

---

**Статус:** В процессе исправления
