# Локальная релизная сборка: Android APK

**Тип выпуска:** Android APK Release  
**Gradle задача:** `:android:app:assembleRelease`

## Назначение

Собрать production APK локально для проверки готовности релизного Android артефакта.

## Предварительные условия

- JDK 17
- Android SDK + Build Tools
- `local.properties` с валидным `sdk.dir`

## Команда проверки

```bash
./gradlew :android:app:assembleRelease --console=plain
```

## Критерии готовности

- Команда завершается с `BUILD SUCCESSFUL`
- Артефакт формируется в `android/app/build/outputs/apk/release/`

## Текущие блокеры

1. `:core:common:compileReleaseKotlinAndroid`  
   - ошибки `expect/actual` в Android security реализациях
   - unresolved `mu.KotlinLogging` в `androidMain`
2. `:android:app:checkReleaseDuplicateClasses`  
   - Jetifier падает на `logback-core-1.5.9` (`Unsupported class file major version 65`)

## Минимальный план исправления

1. Привести `core:common` Android actual-реализации к ожидаемым декларациям
2. Устранить зависимость Android runtime от `logback` (или заменить на Android-совместимый logging stack)
3. Перезапустить `assembleRelease`
