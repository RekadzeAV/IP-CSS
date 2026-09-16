# Локальная релизная сборка: Desktop x86_64

**Тип выпуска:** Desktop x86_64 Release  
**Gradle задача:** `:platforms:client-desktop-x86_64:app:packageReleaseDistributionForCurrentOS`

## Назначение

Собрать релизный desktop пакет для текущей ОС (x86_64 профиль клиента).

## Предварительные условия

- JDK 17
- Compose Desktop tooling
- Рабочая компиляция desktop UI модуля

## Команда проверки

```bash
./gradlew :platforms:client-desktop-x86_64:app:packageReleaseDistributionForCurrentOS --console=plain
```

## Критерии готовности

- Команда завершается с `BUILD SUCCESSFUL`
- Пакет формируется в `platforms/client-desktop-x86_64/app/build/compose/binaries/`

## Текущие блокеры

Критичных блокеров нет на текущем хосте.

Проверка:

- `:platforms:client-desktop-x86_64:app:packageReleaseDistributionForCurrentOS` — GREEN
- Артефакт: `platforms/client-desktop-x86_64/app/build/compose/binaries/main-release/msi/IP-CSS Desktop-1.0.0.msi`

## Минимальный план исправления

1. Поддерживать green-статус задачи `packageReleaseDistributionForCurrentOS`
2. Периодически прогонять packaging после обновления Compose/JDK
3. Перед релизом проверять запуск собранного MSI
