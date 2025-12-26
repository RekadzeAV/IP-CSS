# Структура платформ проекта IP-CSS

**Дата создания:** Декабрь 2025
**Версия:** Alfa-0.0.1

## Обзор платформ

Проект разделен на следующие платформы:

### 1. Микрокомпьютеры ARM (SBC ARM)
- **Директория:** `platforms/sbc-arm/`
- **Архитектура:** ARM (ARMv7, ARMv8/aarch64)
- **Интерфейс:** Веб-интерфейс
- **Назначение:** Raspberry Pi, Orange Pi, Rock64 и другие одноплатные компьютеры

### 2. Серверы x86-x64
- **Директория:** `platforms/server-x86_64/`
- **Архитектура:** x86-64 (Intel/AMD)
- **Интерфейс:** Веб-интерфейс
- **Назначение:** Серверные системы Linux/Windows/macOS

### 3. NAS ARM
- **Директория:** `platforms/nas-arm/`
- **Архитектура:** ARM (ARMv8/aarch64)
- **Интерфейс:** Веб-интерфейс
- **Назначение:** Synology, QNAP, Asustor на базе ARM

### 4. NAS x86-x64
- **Директория:** `platforms/nas-x86_64/`
- **Архитектура:** x86-64
- **Интерфейс:** Веб-интерфейс
- **Назначение:** Synology, QNAP, Asustor, TrueNAS на базе x86-64

### 5. Клиенты Desktop x86-x64
- **Директория:** `platforms/client-desktop-x86_64/`
- **Архитектура:** x86-64
- **ОС:** Windows, Linux, macOS
- **Интерфейс:** Нативное приложение (Compose Desktop)

### 6. Клиенты Desktop ARM
- **Директория:** `platforms/client-desktop-arm/`
- **Архитектура:** ARM64
- **ОС:** Linux ARM64, macOS Apple Silicon
- **Интерфейс:** Нативное приложение (Compose Desktop)

### 7. Клиенты Android
- **Директория:** `platforms/client-android/` (или использовать существующий `android/`)
- **Архитектура:** ARM, ARM64, x86, x86_64
- **Интерфейс:** Нативное приложение (Jetpack Compose)

### 8. Клиенты iOS/macOS
- **Директория:** `platforms/client-ios/`
- **Архитектура:** ARM64, x86_64 (симулятор)
- **ОС:** iOS, macOS
- **Интерфейс:** Нативное приложение (SwiftUI)

## Структура веток Git

### Основные ветки:
- `main` / `master` - основная стабильная ветка

### Ветки для каждой платформы:
- `develop/platform-sbc-arm` → `test/platform-sbc-arm`
- `develop/platform-server-x86_64` → `test/platform-server-x86_64`
- `develop/platform-nas-arm` → `test/platform-nas-arm`
- `develop/platform-nas-x86_64` → `test/platform-nas-x86_64`
- `develop/platform-client-desktop-x86_64` → `test/platform-client-desktop-x86_64`
- `develop/platform-client-desktop-arm` → `test/platform-client-desktop-arm`
- `develop/platform-client-android` → `test/platform-client-android`
- `develop/platform-client-ios` → `test/platform-client-ios`

## Общие модули

Следующие модули используются всеми платформами:
- `shared/` - Kotlin Multiplatform модуль с общей бизнес-логикой
- `core/` - общие модули (common, network, license)
- `native/` - нативные C++ библиотеки

