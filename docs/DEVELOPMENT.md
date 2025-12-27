# Руководство по разработке

## Настройка окружения разработки

### Требования

- JDK 17+
- Android Studio / Xcode (для мобильных платформ)
- Docker (для серверной версии)
- CMake 3.15+ (для нативных библиотек)
- Node.js 18+ (для веб-интерфейса)

### Установка

1. Клонируйте репозиторий:
```bash
git clone https://github.com/company/ip-camera-surveillance-system.git
cd ip-camera-surveillance-system
```

2. Установите зависимости:
```bash
./gradlew build
cd server/web && npm install
```

3. Настройте IDE:
- Android Studio для Android разработки
- Xcode для iOS разработки
- IntelliJ IDEA / VS Code для остального

## Структура проекта

```
IP-CSS/
├── shared/          # Kotlin Multiplatform модуль
│   ├── commonMain/  # Общий код для всех платформ
│   ├── androidMain/ # Android-специфичные реализации
│   ├── iosMain/     # iOS-специфичные реализации
│   └── desktopMain/ # Desktop-специфичные реализации
├── android/         # Android приложение
├── ios/             # iOS приложение
├── desktop/         # Desktop приложения
├── server/          # Серверная часть
├── native/          # C++ библиотеки
├── core/            # Общие модули
│   ├── common/      # Базовые типы
│   ├── network/     # Сетевые клиенты
│   └── license/     # Система лицензирования
├── docs/            # Документация
└── scripts/         # Скрипты
```

Подробная информация о разделении разработки по платформам: [PLATFORMS.md](PLATFORMS.md)

## Запуск в режиме разработки

### Android
```bash
./gradlew :android:installDebug
```

### iOS
```bash
cd ios
pod install
open IPCameraSurveillance.xcworkspace
```

### Desktop
```bash
./gradlew :desktop:run
```

### Server
```bash
./gradlew :server:run
```

### Web Interface
```bash
cd server/web
npm run dev
```

## Тестирование

### Запуск всех тестов
```bash
./gradlew testAll
```

### Запуск тестов конкретного модуля
```bash
./gradlew :shared:test
./gradlew :android:test
```

### Запуск с покрытием
```bash
./gradlew :shared:test jacocoTestReport
```

## Отладка

### Android
- Используйте Android Studio Debugger
- Логи через Logcat

### iOS
- Используйте Xcode Debugger
- Логи через Console.app

### Desktop
- Используйте IntelliJ IDEA Debugger
- Логи в консоль

### Server
- Используйте IntelliJ IDEA Debugger
- Логи в файлы или консоль

## Code Style

### Kotlin
```bash
./gradlew ktlintCheck
./gradlew ktlintFormat
```

### JavaScript/TypeScript
```bash
cd server/web
npm run lint
npm run format
```

## Git Workflow

1. Создайте feature branch от `develop`
2. Внесите изменения
3. Создайте Pull Request в `develop`
4. После review и approval изменения будут смержены

## CI/CD

Все изменения автоматически проверяются через GitHub Actions:
- Code quality checks
- Unit tests
- Integration tests
- Build verification

## Полезные команды

```bash
# Очистка проекта
./gradlew clean

# Сборка всех платформ
./scripts/build-all-platforms.sh

# Обновление зависимостей
./gradlew --refresh-dependencies

# Генерация документации
./gradlew dokkaHtml
```

