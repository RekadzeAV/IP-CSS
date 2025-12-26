# Платформа: Клиенты iOS/macOS

## Описание

Клиентские приложения для устройств Apple (iOS и macOS).

## Поддерживаемые устройства

### iOS
- iPhone (ARM64)
- iPad (ARM64)
- iOS Simulator (x86_64, ARM64)

### macOS
- Mac Intel (x86_64)
- Mac Apple Silicon (ARM64)

## Архитектура

- **ОС:** iOS 13.0+, macOS 11.0+
- **UI Framework:** SwiftUI
- **Язык:** Swift (UI), Kotlin/Native (бизнес-логика)
- **Интеграция:** Kotlin Multiplatform framework

## Структура модулей

```
ios/
├── IPCameraSurveillance/  # iOS/macOS приложение (SwiftUI)
├── IPCameraSurveillance.xcodeproj
└── Podfile
```

## Используемые общие модули

- `:shared` - общая бизнес-логика (iosMain source set)
- `:core:common` - базовые типы
- `:core:network` - сетевые клиенты
- `:core:license` - система лицензирования
- `:native` - нативные C++ библиотеки (сборка для iOS/macOS)

## Сборка

```bash
# Сборка Kotlin framework для iOS
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# В Xcode
xcodebuild -workspace IPCameraSurveillance.xcworkspace \
           -scheme IPCameraSurveillance \
           -configuration Release
```

## Развертывание

Приложение публикуется через App Store (iOS) и Mac App Store (macOS).

