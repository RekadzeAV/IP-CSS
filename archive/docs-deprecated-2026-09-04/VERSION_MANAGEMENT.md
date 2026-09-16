# Управление версией продукта IP-CSS

## Автоматическое обновление версии

Система автоматического обновления версии продукта настроена и готова к использованию.

**Текущая версия:** `Alfa-0.1.1`
**Формат версии:** `Alfa-X.Y.Z`
**Инкремент:** При каждом обновлении последняя цифра (Z) увеличивается на 1

## Быстрый старт

### Обновление версии через Gradle (рекомендуется)

```bash
# Инкремент версии (Alfa-0.1.1 → Alfa-0.1.2)
./gradlew incrementVersion

# Просмотр текущей версии
./gradlew getVersion
```

### Обновление версии через скрипты

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

## Что обновляется автоматически

При выполнении инкремента версии автоматически обновляются:

1. ✅ **gradle.properties** - основная версия проекта
2. ✅ **README.md** - версия в основном README (если присутствует)
3. ✅ **DOCUMENTATION_INDEX.md** - версия в индексе документации (если присутствует)

## Примеры использования

### Пример 1: Обновление версии перед релизом

```bash
# 1. Проверить текущую версию
./gradlew getVersion
# Вывод: Текущая версия проекта: Alfa-0.1.1

# 2. Инкрементировать версию
./gradlew incrementVersion
# Вывод: Версия успешно обновлена!
#        Текущая версия: Alfa-0.1.1 → Alfa-0.1.2

# 3. Проверить новую версию
./gradlew getVersion
# Вывод: Текущая версия проекта: Alfa-0.1.2
```

### Пример 2: Проверка перед обновлением (dry-run)

```bash
# Linux/macOS
./scripts/increment-version.sh --dry-run

# Windows
.\scripts\increment-version.ps1 -DryRun
```

## Ручное обновление версии

Если нужно обновить версию вручную (например, изменить major или minor версию):

1. Отредактируйте `gradle.properties`:
   ```properties
   version=Alfa-0.1.1
   ```

2. Обновите версию в документах вручную или используйте скрипты управления документацией.

## Интеграция с CI/CD

### GitHub Actions

```yaml
- name: Increment version
  run: ./gradlew incrementVersion
  continue-on-error: true

- name: Commit version update
  run: |
    git config --local user.email "action@github.com"
    git config --local user.name "GitHub Action"
    git add gradle.properties README.md DOCUMENTATION_INDEX.md
    git commit -m "chore: increment version [skip ci]" || exit 0
    git push
```

### Git Hooks

Добавьте в `.git/hooks/pre-commit`:

```bash
#!/bin/bash
# Автоматический инкремент версии перед коммитом
./gradlew incrementVersion
git add gradle.properties README.md DOCUMENTATION_INDEX.md
```

## Структура версии

```
Alfa-X.Y.Z
│   │ │ └─ Patch версия (инкрементируется автоматически)
│   │ └─── Minor версия (изменяется вручную)
│   └───── Major версия (изменяется вручную)
└───────── Префикс (Alfa, Beta, или отсутствует для release)
```

## Файлы системы версионирования

- `gradle.properties` - основное хранилище версии
- `scripts/increment-version.sh` - скрипт для Linux/macOS
- `scripts/increment-version.ps1` - скрипт для Windows
- `build.gradle.kts` - Gradle tasks для управления версией
- `scripts/README_VERSION.md` - подробная документация

## Примечания

- ✅ Версия всегда должна быть в формате `Alfa-X.Y.Z`
- ✅ Инкремент происходит только для последней цифры (patch)
- ✅ Для изменения major или minor версии нужно редактировать `gradle.properties` вручную
- ✅ Все модули проекта используют одну версию из `gradle.properties`
- ✅ Версия синхронизируется с документацией автоматически

## Поддержка

Для вопросов и предложений см.:
- [scripts/README_VERSION.md](scripts/README_VERSION.md) - подробная документация
- [docs/BUILD_ORGANIZATION.md](docs/BUILD_ORGANIZATION.md) - организация сборки
