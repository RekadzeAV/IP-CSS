# Платформа: Клиенты iOS/macOS

## Описание

Текущий статус платформы: **planned**.  
В репозитории пока нет рабочего iOS/macOS app-проекта (Swift/Xcode), директория используется как placeholder и документация.

## Целевые устройства (план)

### iOS
- iPhone (ARM64)
- iPad (ARM64)
- iOS Simulator (x86_64, ARM64)

### macOS
- Mac Intel (x86_64)
- Mac Apple Silicon (ARM64)

## Целевая архитектура (план)

- **ОС:** iOS 13.0+, macOS 11.0+
- **UI Framework:** SwiftUI
- **Язык:** Swift (UI), Kotlin/Native (бизнес-логика)
- **Интеграция:** Kotlin Multiplatform framework

## Структура модулей (план)

```
platforms/client-ios/
└── README.md
```

## Используемые общие модули (когда платформа будет реализована)

- `:shared` - общая бизнес-логика (iosMain source set)
- `:core:common` - базовые типы
- `:core:network` - сетевые клиенты
- `native/` - нативные C++ библиотеки (при необходимости)

## Сборка (план)

```bash
# Пример целевой команды для Kotlin framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# В Xcode
xcodebuild -workspace IPCameraSurveillance.xcworkspace \
           -scheme IPCameraSurveillance \
           -configuration Release
```

## Готовность

Платформа будет считаться `implemented` после появления:
- app-модуля и Xcode-проекта в репозитории;
- CI-задачи сборки iOS/macOS артефактов;
- smoke-тестов или эквивалентного quality gate.

## Развертывание (план)

Приложение публикуется через App Store (iOS) и Mac App Store (macOS).



