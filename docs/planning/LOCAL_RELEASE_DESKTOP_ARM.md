# Локальная релизная сборка: Desktop ARM

**Тип выпуска:** Desktop ARM Release  
**Gradle задача:** `:platforms:client-desktop-arm:app:packageReleaseDistributionForCurrentOS`

## Назначение

Собрать релизный desktop пакет ARM-профиля для текущей ОС.

## Предварительные условия

- JDK 17 (toolchain уже выровнен)
- Compose Desktop tooling
- Рабочая компиляция ARM desktop UI

## Команда проверки

```bash
./gradlew :platforms:client-desktop-arm:app:packageReleaseDistributionForCurrentOS --console=plain
```

## Критерии готовности

- Команда завершается с `BUILD SUCCESSFUL`
- Пакет формируется в `platforms/client-desktop-arm/app/build/compose/binaries/`

## Текущие блокеры

Критичных блокеров нет на текущем хосте.

Проверка:

- `:platforms:client-desktop-arm:app:packageReleaseDistributionForCurrentOS` — GREEN
- Выходные файлы: `platforms/client-desktop-arm/app/build/compose/binaries/`

## Минимальный план исправления

1. Поддерживать green-статус задачи `packageReleaseDistributionForCurrentOS`
2. Проверять корректность ARM-профиля после обновлений Compose
3. Перед релизом валидировать установку/запуск пакета на целевой ARM-ОС
