# Инструменты разработки

Проект использует несколько инструментов для обеспечения качества кода, форматирования и документации.

## Установленные инструменты

### 1. Detekt - Статический анализ кода

**Назначение:** Обнаружение проблем с кодом, сложности, стиля

**Конфигурация:** `detekt.yml` в корне проекта

**Использование:**
```bash
# Запустить анализ
./gradlew detekt

# Сгенерировать отчет
./gradlew detektMain
```

**Основные правила:**
- Максимальная сложность метода: 15
- Максимальная длина метода: 60 строк
- Максимальная длина строки: 120 символов
- Проверка именования классов, функций, переменных

### 2. Ktlint - Форматирование кода

**Назначение:** Автоматическое форматирование Kotlin кода

**Конфигурация:** `.ktlint.yml` в корне проекта

**Использование:**
```bash
# Проверить форматирование
./gradlew ktlintCheck

# Автоматически исправить
./gradlew ktlintFormat
```

**Настройки:**
- Размер отступа: 4 пробела
- Максимальная длина строки: 120 символов
- Автоматическое удаление пробелов в конце строк
- Вставка финального переноса строки

### 3. Dokka - Генерация документации

**Назначение:** Автоматическая генерация API документации из KDoc комментариев

**Использование:**
```bash
# Сгенерировать документацию
./gradlew dokkaHtml

# Документация будет в build/dokka/
```

**Пример KDoc комментария:**
```kotlin
/**
 * Управляет подключением к IP камере через ONVIF протокол.
 *
 * @param cameraId Уникальный идентификатор камеры
 * @return Результат подключения
 * @throws NetworkException если подключение не удалось
 */
suspend fun connectToCamera(cameraId: String): ConnectionResult
```

## Интеграция в IDE

### IntelliJ IDEA / Android Studio

#### Ktlint
1. Установите плагин "Ktlint" из Marketplace
2. Настройте автоформатирование: Settings → Tools → Actions on Save
3. Включите "Run ktlint"

#### Detekt
1. Установите плагин "Detekt" из Marketplace
2. Настройки: Settings → Tools → Detekt
3. Укажите путь к `detekt.yml`

### VS Code

#### Ktlint
1. Установите расширение "ktlint" (fwcd.ktlint)
2. Настройте автоформатирование в settings.json:
```json
{
  "[kotlin]": {
    "editor.formatOnSave": true,
    "editor.defaultFormatter": "fwcd.ktlint"
  }
}
```

## CI/CD интеграция

### GitHub Actions пример

```yaml
name: Code Quality

on: [push, pull_request]

jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/gradle-build-action@v2

      - name: Run Detekt
        run: ./gradlew detekt

      - name: Check Ktlint
        run: ./gradlew ktlintCheck

      - name: Generate Documentation
        run: ./gradlew dokkaHtml
```

## Рекомендации

1. **Перед коммитом:**
   ```bash
   ./gradlew ktlintFormat detekt
   ```

2. **Перед PR:**
   - Убедитесь что все проверки проходят
   - Обновите документацию если нужно

3. **Настройка правил:**
   - Измените `detekt.yml` для Detekt
   - Измените `.ktlint.yml` для Ktlint
   - Не изменяйте правила без обсуждения с командой

## Отключение правил (когда необходимо)

### Detekt
```kotlin
@Suppress("LongMethod", "ComplexMethod")
fun complexFunction() {
    // ...
}
```

### Ktlint
```kotlin
// ktlint-disable
val longLine = "This is a very long line that exceeds the limit"
// ktlint-enable
```

## Полезные ссылки

- [Detekt Documentation](https://detekt.github.io/detekt/)
- [Ktlint Documentation](https://ktlint.github.io/)
- [Dokka Documentation](https://kotlin.github.io/dokka/)

