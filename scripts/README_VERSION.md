# Автоматическое обновление версии продукта

## Описание

Система автоматического обновления версии продукта IP-CSS. Версия хранится в `gradle.properties` и автоматически инкрементируется на 0.0.1 при каждом обновлении.

**Формат версии:** `Alfa-X.Y.Z`
- **X** - Major версия (не изменяется автоматически)
- **Y** - Minor версия (не изменяется автоматически)
- **Z** - Patch версия (инкрементируется автоматически на 1)

**Примеры:**
- `Alfa-0.0.1` → `Alfa-0.0.2`
- `Alfa-0.0.2` → `Alfa-0.0.3`
- `Alfa-0.1.5` → `Alfa-0.1.6`

## Использование

### Через Gradle (рекомендуется)

```bash
# Инкремент версии
./gradlew incrementVersion

# Просмотр текущей версии
./gradlew getVersion
```

### Через скрипты

#### Linux/macOS

```bash
# Инкремент версии
./scripts/increment-version.sh

# Проверка без изменения (dry-run)
./scripts/increment-version.sh --dry-run
```

#### Windows (PowerShell)

```powershell
# Инкремент версии
.\scripts\increment-version.ps1

# Проверка без изменения (dry-run)
.\scripts\increment-version.ps1 -DryRun
```

## Что обновляется

При инкременте версии автоматически обновляются:

1. **gradle.properties** - основная версия проекта
2. **README.md** - версия в основном README
3. **DOCUMENTATION_INDEX.md** - версия в индексе документации

## Интеграция с CI/CD

Для автоматического обновления версии при коммите можно добавить в `.git/hooks/pre-commit`:

```bash
#!/bin/bash
# Автоматический инкремент версии перед коммитом
./gradlew incrementVersion
git add gradle.properties README.md DOCUMENTATION_INDEX.md
```

Или использовать в GitHub Actions:

```yaml
- name: Increment version
  run: ./gradlew incrementVersion
```

## Ручное обновление версии

Если нужно обновить версию вручную, отредактируйте `gradle.properties`:

```properties
version=Alfa-0.0.2
```

Затем обновите версию в документах вручную или используйте скрипты управления документацией.

## Примечания

- Версия всегда должна быть в формате `Alfa-X.Y.Z`
- Инкремент происходит только для последней цифры (patch)
- Для изменения major или minor версии нужно редактировать `gradle.properties` вручную
- Все модули проекта используют одну версию из `gradle.properties`
