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
git clone https://github.com/RekadzeAV/IP-CSS.git
cd IP-CSS
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
├── android/         # Android приложение
├── ios/             # iOS приложение
├── desktop/         # Desktop приложения
├── server/          # Серверная часть
├── native/          # C++ библиотеки
├── core/            # Общие модули
├── docs/            # Документация
└── scripts/         # Скрипты
```

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

Действует trunk-based модель веток:

> **Обновлено 15.09.2026:** прежняя платформо-ориентированная структура (`dev/<платформа>`, `test/<платформа>`) упразднена — ветки удалены из репозитория (бэкап: `archive/git-branches-backup-2026-09-15/`).

### Структура веток

- `main` - единственная долгоживущая ветка (точка истины, стабильный код)
- `feature/*` - короткоживущие feature-ветки от `main`
- `chore/*`, `refactor/*` - служебные ветки от `main`
- `dependabot/*` - автоматические обновления зависимостей

### Процесс работы

1. Создайте feature branch от актуального `main`:
   ```bash
   git checkout main && git pull
   git checkout -b feature/my-feature
   ```

2. Внесите изменения и закоммитьте

3. Убедитесь, что CI проходит (detekt, тесты, сборка)

4. Создайте Pull Request в **`main`**

5. После review и approval PR мержится в `main` (squash/rebase), feature-ветка удаляется

Подробнее: [CONTRIBUTING.md](../CONTRIBUTING.md)

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

