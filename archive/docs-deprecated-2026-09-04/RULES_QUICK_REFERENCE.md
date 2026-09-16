# 📋 Быстрая Справочка: Правила Проекта IP-CSS

**Версия:** 1.0.0  
**Дата:** 16 May 2026  
**Полный документ:** [docs/RULES_AND_AUTOMATION.md](RULES_AND_AUTOMATION.md)

---

## 🚀 Быстрый Старт

### 1. Перед Первым Коммитом

```bash
# Установить git hooks
./scripts/setup-git-hooks.sh

# Проверить зависимости
./scripts/check-dependencies.ps1  # Windows
./scripts/check-dependencies.sh   # Linux/macOS
```

---

### 2. Перед Каждым Коммитом

**Linux/macOS/WSL:**
```bash
# Форматирование кода
./gradlew ktlintFormat
clang-format -i native/video-processing/src/*.cpp
cd server/web && npm run format && cd ../..

# Проверки
./gradlew detekt
python scripts/ci/check-commonmain-forbidden-imports.py .
python scripts/ci/verify-kmp-phase1.py --skip-gradle

# Генерация структуры (если изменилась)
python scripts/generate-project-structure.py
```

**Windows PowerShell:**
```powershell
# Форматирование кода
./gradlew ktlintFormat
clang-format -i native/video-processing/src/*.cpp
cd server/web; npm run format; cd ..

# Проверки
./gradlew detekt
.\scripts\ci\check-commonmain-forbidden-imports.ps1 .
.\scripts\ci\verify-kmp-phase1.ps1 -SkipGradle

# Генерация структуры (если изменилась)
python scripts/generate-project-structure.py
```

---

### 3. Перед Pull Request

**Полный чеклист:**
- [ ] ✅ Код отформатирован (ktlint, clang-format, Prettier)
- [ ] ✅ Пройден Detekt (`./gradlew detekt`)
- [ ] ✅ Нет запрещенных импортов в commonMain
- [ ] ✅ Пройдены KMP-гаты (`verify-kmp-phase1.py --ci-profile`)
- [ ] ✅ Все тесты проходят (`./gradlew test`)
- [ ] ✅ Документация обновлена (если нужно)
- [ ] ✅ TIMELINE.md обновлен (если значимые изменения)
- [ ] ✅ PROJECT_STRUCTURE_AUTO.md обновлен (если изменилась структура)
- [ ] ✅ Нет секретов в коде
- [ ] ✅ Code review от 1+ maintainers

**Полная проверка (как в CI):**
```bash
# Linux/macOS/WSL
./scripts/ci/verify-kmp-phase1.sh --ci-profile
./gradlew :core:common:compileKotlinMetadata :core:network:compileKotlinMetadata :shared:compileKotlinMetadata
./gradlew :core:common:desktopTest

# Windows PowerShell
.\scripts\ci\verify-kmp-phase1.ps1 -CiProfile
./gradlew :core:common:compileKotlinMetadata :core:network:compileKotlinMetadata :shared:compileKotlinMetadata
./gradlew :core:common:desktopTest
```

---

## 🎯 Основные Правила

### Код-стили

| Платформа | Инструмент | Команда |
|-----------|-----------|---------|
| Kotlin | ktlint + Detekt | `./gradlew ktlintFormat detekt` |
| C/C++ | clang-format | `clang-format -i *.cpp` |
| JS/TS | ESLint + Prettier | `npm run lint && npm run format` |

**Параметры:**
- indent_size: 4 (Kotlin/C++), 2 (JS/TS)
- max_line_length: 120
- charset: utf-8
- end_of_line: lf

---

### KMP (Kotlin Multiplatform)

**Запрещено в commonMain:**
- ❌ `java.*`
- ❌ `javax.*`
- ❌ `android.*`

**Требуется:**
- ✅ expect/actual для платформенных API
- ✅ Минимум 3 actual реализации (Android, JVM/Desktop, iOS)
- ✅ Нет JVM-зависимостей в native source sets

**Проверки:**
```bash
python scripts/ci/verify-kmp-phase1.py --ci-profile
python scripts/ci/check-commonmain-forbidden-imports.py .
python scripts/ci/check-no-jvm-deps-in-native-source-sets.py .
```

---

### Безопасность

**Обязательно:**
- ✅ Certificate pinning для всех платформ
- ✅ Принудительный HTTPS
- ✅ Нет секретов в коде

**Запрещено коммитить:**
- `*.pem`, `*.key`, `*.crt`, `*.p12`, `*.keystore`
- `secrets/`, `.env`, `*.secret`
- `activation_codes.txt`, `license.xml`

---

### Документация

**Обязательно обновлять:**
- ✅ TIMELINE.md — при значимых изменениях
- ✅ PROJECT_STRUCTURE_AUTO.md — при изменении структуры
- ✅ CHANGELOG.md — при релизе

**Команды:**
```bash
# Создание/обновление документа
.\scripts\manage-documentation.ps1 -Action update -Document "docs/NEW_FEATURE.md"

# Генерация структуры
python scripts/generate-project-structure.py
```

---

## 🧪 Тестирование

**Минимальные требования:**
- ✅ Unit тесты для бизнес-логики
- ✅ Покрытие тестами > 80% (критические модули)

**Команды:**
```bash
# Все тесты
./gradlew test

# Desktop тесты
./gradlew :core:common:desktopTest

# MVP автотесты
.\scripts\ci\mvp-automated-acceptance.ps1
bash scripts/ci/mvp-automated-acceptance.sh --generate-phase1-summary
```

---

## 🤖 CI/CD

### Обязательные Jobs (должны быть green)

| Job | Описание | Когда |
|-----|----------|-------|
| `docs-link-check-active-scope` | Проверка ссылок в Markdown | При изменении `*.md` |
| `code-quality` | Detekt, ESLint, KMP gates | Всегда |
| `build-and-test` | Сборка и тесты | Всегда |
| `kmp-cross-compile` | Кросс-компиляция KMP | Всегда |
| `server-api-compose-integration` | Интеграционные тесты API | Всегда |
| `mvp-automated-acceptance` | Приемочное тестирование | Перед релизом |

**Если job падает — PR заблокирован!**

---

## 📁 Структура Веток

```
main                    # стабильная ветка
dev/android            # разработка Android
dev/ios                # разработка iOS
dev/desktop            # разработка Desktop
test/android           # тестирование Android
test/ios               # тестирование iOS
test/desktop           # тестирование Desktop
feature/*              # feature ветки
release/*              # релизные ветки
```

**Workflow:**
1. `git checkout dev/android` (или `dev/ios`, `dev/desktop`)
2. `git checkout -b feature/my-feature`
3. Внесите изменения
4. `git push` → Create PR в `dev/*`

---

## ⚠️ Нарушения и Наказания

| Уровень | Нарушение | Наказание |
|---------|-----------|-----------|
| 1 | Небольшие отклонения от стилей | Предупреждение, требуется исправление |
| 2 | Провал CI jobs, запрещенные импорты | Блокировка merge до исправления |
| 3 | Не обновлен TIMELINE.md, пропущены тесты | Отклонение PR |
| 4 | Повторные нарушения, умышленное игнорирование | Временный/постоянный ban |

---

## 📚 Полезные Ссылки

| Тема | Документ |
|------|----------|
| Полный список правил | [docs/RULES_AND_AUTOMATION.md](RULES_AND_AUTOMATION.md) |
| Руководство по вкладу | [CONTRIBUTING.md](../CONTRIBUTING.md) |
| Архитектура | [docs/ARCHITECTURE.md](ARCHITECTURE.md) |
| Разработка | [docs/DEVELOPMENT.md](DEVELOPMENT.md) |
| Тестирование | [docs/TESTING.md](TESTING.md) |
| RTSP клиент | [docs/rtsp/ACTIVATION.md](rtsp/ACTIVATION.md) |
| MVP определение | [docs/MVP_DEFINITION.md](MVP_DEFINITION.md) |
| Timeline проекта | [docs/TIMELINE.md](TIMELINE.md) |

---

## 🆘 Помощь

**Вопросы:**
- Создать Issue с меткой `question`
- Обратиться к maintainers в Discord/Slack

**Нарушения:**
- Сообщить через Issue с меткой `violation`
- Либо напрямую maintainers

**Исключения:**
- Обсудить с maintainers
- Зафиксировать в `docs/EXCEPTIONS.md`

---

**Полный документ правил:** [docs/RULES_AND_AUTOMATION.md](RULES_AND_AUTOMATION.md)

**© 2026 IP-CSS Project**
