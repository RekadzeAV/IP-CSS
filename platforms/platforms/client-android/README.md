# Платформа: Клиенты Android

## Описание

Клиентское приложение для мобильных устройств Android.

## Поддерживаемые архитектуры

- ARM (armeabi-v7a)
- ARM64 (arm64-v8a)
- x86 (устаревшие устройства)
- x86_64 (устаревшие устройства)

## Архитектура

- **ОС:** Android 8.0+ (API level 26+)
- **UI Framework:** Jetpack Compose
- **Язык:** Kotlin
- **Минимальный SDK:** 26
- **Целевой SDK:** 34

## Структура модулей

```
android/
├── app/             # Android приложение
└── build.gradle.kts
```

Используется существующий модуль `:android:app`

## Используемые общие модули

- `:shared` - общая бизнес-логика (androidMain source set)
- `:core:common` - базовые типы
- `:core:network` - сетевые клиенты
- `:core:license` - система лицензирования
- `:native` - нативные C++ библиотеки (сборка для Android)

## Сборка

```bash
# Debug сборка
./gradlew :android:app:assembleDebug

# Release сборка
./gradlew :android:app:assembleRelease

# APK для всех архитектур
./gradlew :android:app:assembleRelease
```

## Развертывание

Приложение устанавливается через APK или публикуется в Google Play Store.



