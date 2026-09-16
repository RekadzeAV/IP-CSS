# Локальная релизная сборка: Server JVM

**Тип выпуска:** Server JVM Release Build  
**Gradle задача:** `:server:api:build`

## Назначение

Проверить готовность серверного релизного JVM артефакта и зависимых модулей.

## Предварительные условия

- JDK 17
- Рабочая компиляция `shared`, `core:common`, `core:network`
- Валидные серверные зависимости (Ktor, security, DB)

## Команда проверки

```bash
./gradlew :server:api:build --console=plain
```

## Критерии готовности

- Команда завершается с `BUILD SUCCESSFUL`
- JAR/дистрибутив формируется без compile ошибок

## Текущие блокеры

Зафиксированы множественные compile ошибки в серверном коде:

- unresolved references в роутингах и security
- type mismatch и проблемы smart cast
- ошибки API вызовов в отдельных route/service классах

## Минимальный план исправления

1. Стабилизировать `server/api` по пакетам (`routing`, `security`, `service`)
2. Привести сигнатуры DTO/domain преобразований
3. Повторить `:server:api:build` и зафиксировать green статус
