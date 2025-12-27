# Desktop Client Application (ARM)

Desktop приложение для системы видеонаблюдения IP-CSS (ARM версия).

## Статус

✅ **Этап 1: Настройка инфраструктуры** - Завершен

- ✅ Создан Gradle модуль
- ✅ Настроен build.gradle.kts с зависимостями
- ✅ Создана базовая структура (Main.kt, App.kt)
- ✅ Настроена система навигации
- ✅ Настроен Koin для DI
- ✅ Создана тема приложения

## Поддерживаемые платформы

- Linux ARM64
- macOS Apple Silicon (ARM64)

## Запуск

```bash
# Запуск в режиме разработки
./gradlew :platforms:client-desktop-arm:app:run

# Сборка для Linux ARM64 (DEB)
./gradlew :platforms:client-desktop-arm:app:packageDeb

# Сборка для macOS Apple Silicon (DMG)
./gradlew :platforms:client-desktop-arm:app:packageDmg
```

## Следующие шаги

См. [docs/DESKTOP_IMPLEMENTATION_PLAN.md](../../../docs/DESKTOP_IMPLEMENTATION_PLAN.md) для детального плана реализации.

