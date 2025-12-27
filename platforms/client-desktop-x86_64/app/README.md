# Desktop Client Application (x86_64)

Desktop приложение для системы видеонаблюдения IP-CSS.

## Статус

✅ **Этап 1: Настройка инфраструктуры** - Завершен

- ✅ Создан Gradle модуль
- ✅ Настроен build.gradle.kts с зависимостями
- ✅ Создана базовая структура (Main.kt, App.kt)
- ✅ Настроена система навигации
- ✅ Настроен Koin для DI
- ✅ Создана тема приложения

## Структура

```
app/
├── build.gradle.kts          # Конфигурация сборки
└── src/main/kotlin/
    └── com/company/ipcamera/desktop/
        ├── Main.kt           # Точка входа
        ├── App.kt            # Корневой Compose компонент
        ├── di/
        │   └── AppModule.kt   # Koin модуль для DI
        └── ui/
            ├── theme/         # Тема приложения
            ├── navigation/    # Навигация
            └── screens/       # Экраны приложения
```

## Запуск

```bash
# Запуск в режиме разработки
./gradlew :platforms:client-desktop-x86_64:app:run

# Сборка для Windows (MSI)
./gradlew :platforms:client-desktop-x86_64:app:packageMsi

# Сборка для Linux (DEB)
./gradlew :platforms:client-desktop-x86_64:app:packageDeb

# Сборка для macOS (DMG)
./gradlew :platforms:client-desktop-x86_64:app:packageDmg
```

## Следующие шаги

См. [docs/DESKTOP_IMPLEMENTATION_PLAN.md](../../../docs/DESKTOP_IMPLEMENTATION_PLAN.md) для детального плана реализации.

