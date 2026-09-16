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

См. документацию по Desktop приложению:
- **[docs/DESKTOP_DETAILED_PLAN.md](../../../archive/docs/guides/DESKTOP_DETAILED_PLAN.md)** - Детальный план с разбивкой на задачи
- **[docs/DESKTOP_PLAN_SUMMARY.md](../../../docs/archive/2026-04-27/plans-obsolete/DESKTOP_PLAN_SUMMARY.md)** - Краткая сводка плана
- **[docs/DESKTOP_REFINEMENT_PLAN.md](../../../archive/docs/guides/DESKTOP_REFINEMENT_PLAN.md)** - План доработки незавершенных задач
- **[docs/DESKTOP_OPTIMIZATION_GUIDE.md](../../../archive/docs/guides/DESKTOP_OPTIMIZATION_GUIDE.md)** - Руководство по оптимизации
- **[IMPLEMENTATION_STATUS.md](../IMPLEMENTATION_STATUS.md)** - Текущий статус реализации (~70%)

