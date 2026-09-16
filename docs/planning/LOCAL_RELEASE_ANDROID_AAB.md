# Локальная релизная сборка: Android AAB

**Тип выпуска:** Android App Bundle Release  
**Gradle задача:** `:android:app:bundleRelease`

## Назначение

Собрать AAB для публикации в маркетплейсах и проверки финальной конфигурации Android release pipeline.

## Предварительные условия

- JDK 17
- Android SDK + Build Tools
- Валидный Android release configuration

## Команда проверки

```bash
./gradlew :android:app:bundleRelease --console=plain
```

## Критерии готовности

- Команда завершается с `BUILD SUCCESSFUL`
- Артефакт формируется в `android/app/build/outputs/bundle/release/`

## Текущие блокеры

Критичных блокеров нет на текущем хосте.

Проверка:

- `:android:app:bundleRelease` — GREEN
- Артефакт формируется в `android/app/build/outputs/bundle/release/`

## Минимальный план исправления

1. Поддерживать green-статус `bundleRelease` после обновлений AGP/Kotlin/Compose
2. Перед публикацией дополнительно валидировать подпись и содержимое AAB через `bundletool`
