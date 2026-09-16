# Руководство по внесению вклада

Спасибо за интерес к проекту IP Camera Surveillance System!

## ⚠️ ВАЖНО: Правила Проекта

**Перед началом работы обязательно прочитайте:**

- 📜 [**docs/RULES_AND_AUTOMATION.md**](docs/RULES_AND_AUTOMATION.md) — **ПОЛНЫЙ СПИСОК ПРАВИЛ** проекта
  - Код-стили и форматирование
  - CI/CD автоматизация
  - KMP (Kotlin Multiplatform) правила
  - Правила безопасности
  - Управление документацией
  - Git workflow
  - Тестирование
  - **Управление зависимостями и проверка окружения** ⭐ НОВОЕ
  - Обязательные чеклисты
  - Локальная проверка

**Несоблюдение правил может привести к отклонению Pull Request!**

## Как внести вклад

### Сообщение об ошибках

Если вы нашли ошибку:
1. Проверьте, не была ли она уже сообщена в Issues
2. Создайте новый Issue с подробным описанием
3. Укажите шаги для воспроизведения
4. Приложите логи и скриншоты (если применимо)

### Предложение новых функций

1. Создайте Issue с меткой "enhancement"
2. Опишите проблему, которую решает функция
3. Предложите решение
4. Обсудите с сообществом

### Pull Requests

1. Форкните репозиторий (или работайте в клоне, если есть доступ)
2. Целевая ветка для всех изменений — **`main`**
3. Создайте feature branch от `main`:
   ```bash
   git checkout main
   git pull
   git checkout -b feature/amazing-feature
   ```
4. Внесите изменения
5. Убедитесь, что все тесты проходят
6. Создайте Pull Request в **`main`**

### Обязательные CI-checks для PR

Перед merge все обязательные проверки должны быть `green`.

- `CI Pipeline - Cross-platform / docs-link-check-active-scope` — проверка локальных markdown-ссылок в active scope (исключая `node_modules`, `docs/archive`, `diagnostics`, build-артефакты).
- `CI Pipeline - Cross-platform / code-quality`
- `CI Pipeline - Cross-platform / build-and-test`

Если PR меняет документацию (`*.md`), отсутствие `green` у `docs-link-check-active-scope` блокирует merge.

## Стандарты кода

### Kotlin

- Следуйте [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Используйте `ktlint` для форматирования
- Покрытие тестами должно быть > 80%

#### Kotlin Multiplatform (обязательно)

- В `commonMain` запрещены платформенные API: `java.*`, `javax.*`, `android.*`.
- Любая платформенная логика (crypto, keystore/keychain, security logging, random, file APIs) выносится через `expect/actual`.
- Для новых/изменённых `expect` обязательны `actual` реализации минимум для Android, JVM/Desktop, iOS и Native.
- Не добавляйте JVM/Android-only зависимости в `iosMain`/`nativeMain` source set блоки.
- PR должен проходить KMP-проверки CI на сигнатуры `expect/actual` и границы `commonMain`.

#### Локальная проверка KMP-gates перед PR

Рекомендуется запускать те же проверки, которые стоят в CI:

```bash
python scripts/ci/verify-kmp-phase1.py
python scripts/ci/verify-kmp-phase1.py --ci-profile
python scripts/ci/verify-kmp-phase1.py --strict-runtime-matrix
python scripts/ci/check-commonmain-forbidden-imports.py .
python scripts/ci/check-security-expect-actual-signatures.py .
python scripts/ci/check-no-jvm-deps-in-native-source-sets.py .
./gradlew :core:common:compileKotlinMetadata :core:network:compileKotlinMetadata :shared:compileKotlinMetadata --no-daemon
./gradlew :core:common:desktopTest --no-daemon
```

#### Локальная проверка окружения перед сборкой/тестами

**ОБЯЗАТЕЛЬНО** перед запуском сборок, тестов или компиляции:

```bash
# Проверка всех компонентов окружения
python scripts/check-environment-requirements.py --verbose
python scripts/check-environment-requirements.py --quick
python scripts/check-environment-requirements.py --report diagnostics/environment-report.json

# Windows PowerShell
.\scripts\check-environment-requirements.ps1 -Verbose
.\scripts\check-environment-requirements.ps1 -Quick
.\scripts\check-environment-requirements.ps1 -Report diagnostics\environment-report.json
```

#### Проверка зависимостей перед установкой

**ОБЯЗАТЕЛЬНО** перед установкой новых модулей/библиотек:

```bash
# Проверка конкретного пакета
python scripts/check-dependency-conflicts.py --package <package-name>
python scripts/check-dependency-conflicts.py --list

# Windows PowerShell
.\scripts\check-dependency-conflicts.ps1 -Package <package-name>
.\scripts\check-dependency-conflicts.ps1 -List
```

Для Windows PowerShell можно использовать:

```powershell
.\scripts\ci\verify-kmp-phase1.ps1
.\scripts\ci\verify-kmp-phase1.ps1 -SkipGradle
.\scripts\ci\verify-kmp-phase1.ps1 -StrictRuntimeMatrix
.\scripts\ci\verify-kmp-phase1.ps1 -CiProfile
```

Для Linux/macOS/WSL можно использовать:

```bash
./scripts/ci/verify-kmp-phase1.sh
./scripts/ci/verify-kmp-phase1.sh --skip-gradle
./scripts/ci/verify-kmp-phase1.sh --strict-runtime-matrix
./scripts/ci/verify-kmp-phase1.sh --ci-profile
```

### JavaScript/TypeScript

- Следуйте [TypeScript Style Guide](https://google.github.io/styleguide/tsguide.html)
- Используйте ESLint и Prettier
- Покрытие тестами должно быть > 80%

### C++

- Следуйте [Google C++ Style Guide](https://google.github.io/styleguide/cppguide.html)
- Используйте clang-format
- Комментируйте сложные алгоритмы

## Процесс разработки

### Структура веток

> **Обновлено 15.09.2026:** прежняя платформо-ориентированная модель (`dev/<платформа>`, `test/<платформа>`, `develop/platform-*`) упразднена — эти ветки удалены из репозитория (бэкап: `archive/git-branches-backup-2026-09-15/`). Действует trunk-based модель:

- **`main`** - единственная долгоживущая ветка (точка истины)
- **`feature/<название>`** - короткоживущие feature-ветки от `main`
- **`chore/<название>`, `refactor/<название>`** - служебные ветки от `main`
- Dependabot-ветки (`dependabot/*`) - автоматические обновления зависимостей

### Рабочий процесс

1. Создайте issue для обсуждения
2. Получите одобрение от maintainers
3. Создайте feature branch от актуального `main`:
   ```bash
   git checkout main && git pull
   git checkout -b feature/my-feature
   ```
4. Внесите изменения с тестами
5. Убедитесь, что CI проходит
6. Создайте Pull Request в **`main`**
7. Дождитесь code review
8. После одобрения PR мержится в `main` (squash/rebase) и ветка удаляется

## Тестирование

Все изменения должны включать тесты:
- Unit тесты для бизнес-логики
- Integration тесты для API
- E2E тесты для критических путей

## Документация

### Общие требования

Обновляйте документацию при изменении API или добавлении новых функций.

### Ведение временной шкалы (TIMELINE.md) - ОБЯЗАТЕЛЬНО

**Важно:** Файл `TIMELINE.md` является обязательным для ведения в проекте. Временная шкала должна обновляться при каждом значимом изменении.

#### Когда обновлять TIMELINE.md:

1. **При завершении версии/релиза** - переместить изменения из "В процессе" в "Завершено"
2. **При завершении крупной задачи** - обновить статус и процент прогресса
3. **При изменении планов** - обновить фазы, оценки времени, приоритеты
4. **При добавлении блокеров** - добавить в раздел "Критические блокеры продакшена"

#### Процесс обновления:

1. Определите тип изменения (релиз/задача/план)
2. Найдите соответствующий раздел (Прошлое/Настоящее/Будущее)
3. Обновите информацию согласно формату
4. Обновите метаданные (дата, прогресс)
5. Проверьте согласованность с другими документами

**Подробное руководство:** [docs/TIMELINE_GUIDE.md](docs/TIMELINE_GUIDE.md)

#### Чеклист перед коммитом:

- [ ] Обновлён `REMAINING_TASKS.md` (единый трекер; мастер-план — `PLAN_EXECUTION_MASTER.md`)
- [ ] Использованы правильные статусы (✅/🟡/⚠️/❌)
- [ ] Обновлена дата "Последнее обновление"
- [ ] Обновлен процент прогресса (если применимо)
- [ ] Информация согласована с CHANGELOG.md
- [ ] Информация согласована с PLAN_EXECUTION_MASTER.md / REMAINING_TASKS.md

**Примечание:** Pull Request, который вносит значимые изменения в проект, но не обновляет `REMAINING_TASKS.md` / `CHANGELOG.md`, может быть отклонён до актуализации статуса.

### Управление документацией

Проект использует автоматизированную систему управления документацией с версионированием (реальные скрипты: `scripts/update-documentation.ps1` / `scripts/update-documentation.py`, `scripts/archive-docs.ps1`, `scripts/check-docs-links.ps1`; справка: `scripts/README_DOCUMENTATION_MANAGEMENT.md`).

#### Создание нового документа

```powershell
# Windows PowerShell
.\scripts\update-documentation.ps1 -Document docs/NEW_FEATURE.md

# Linux/macOS / Python
python scripts/update-documentation.py docs/NEW_FEATURE.md
```

#### Обновление существующего документа

Отредактируйте документ, зафиксируйте версию `0.5.1.1-beta` и обновите `DOCUMENTATION_INDEX.md`; устаревшие версии переносите в `archive/`:

```powershell
# Архивация устаревших версий
.\scripts\archive-docs.ps1

# Проверка ссылок
.\scripts\check-docs-links.ps1
```

#### Слияние документов

При объединении нескольких документов исходные переносятся в `archive/`, в корневом `DOCUMENTATION_INDEX.md` остаётся один канонический документ.

**Подробная документация:** [scripts/README_DOCUMENTATION_MANAGEMENT.md](scripts/README_DOCUMENTATION_MANAGEMENT.md)

#### Чеклист при работе с документацией:

- [ ] Использован скрипт управления документацией для создания/обновления
- [ ] Версия проекта указана корректно (из `gradle.properties`)
- [ ] Документ размещен в правильном каталоге (`docs/` или корень проекта)
- [ ] Обновлен `DOCUMENTATION_INDEX.md` (если добавлен новый документ)
- [ ] Обновлен `docs/README.md` (если изменена структура)

## Вопросы?

Создайте Issue с меткой "question" или свяжитесь с maintainers.

