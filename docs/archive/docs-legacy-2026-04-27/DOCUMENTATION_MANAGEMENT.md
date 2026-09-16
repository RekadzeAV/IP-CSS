# Управление документацией проекта IP-CSS

**Версия системы управления:** 1.0
**Дата создания:** 26 January 2026
**Версия проекта:** Alfa-0.0.1

---

## 📋 Обзор

Система управления документацией автоматизирует процессы создания, обновления и слияния документов проекта с автоматическим версионированием и архивацией.

---

## 🎯 Функционал

### 1. Создание новых документов
- Автоматическое присвоение версии проекта "Alfa-0.0.1" (из `gradle.properties`)
- Автоматическое определение целевого каталога по структуре проекта
- Создание шаблона документа с метаданными

### 2. Обновление существующих документов
- Автоматическое извлечение текущей версии из документа
- Инкремент версии на 1 (Alfa-0.0.1 → Alfa-0.0.2)
- Архивирование старой версии в `docs/archive/YYYY-MM-DD/`
- Обновление метаданных (версия, дата)

### 3. Слияние нескольких документов
- Объединение содержимого нескольких документов
- Автоматическая архивация исходных документов
- Создание нового документа с инкрементированной версией
- Размещение результата согласно структуре проекта

---

## 🛠️ Использование

### Windows PowerShell

#### Создание нового документа

```powershell
# Создать документ в автоматически определенном каталоге
.\scripts\manage-documentation.ps1 -Action create -Document "docs/NEW_FEATURE.md"

# Создать документ в корне проекта
.\scripts\manage-documentation.ps1 -Action create -Document "NEW_DOCUMENT.md"
```

#### Обновление существующего документа

```powershell
# Обновить документ (версия увеличится на 1, старая версия будет архивирована)
.\scripts\manage-documentation.ps1 -Action update -Document "docs/ARCHITECTURE.md"

# Обновить с указанием даты архивации
.\scripts\manage-documentation.ps1 -Action update -Document "docs/ARCHITECTURE.md" -Date "2026-01-28"
```

#### Слияние документов

```powershell
# Объединить несколько документов
.\scripts\manage-documentation.ps1 `
    -Action merge `
    -SourceDocuments @("docs/DOC1.md", "docs/DOC2.md", "docs/DOC3.md") `
    -OutputDocument "docs/MERGED_DOCUMENT.md"

# С указанием даты архивации
.\scripts\manage-documentation.ps1 `
    -Action merge `
    -SourceDocuments @("docs/DOC1.md", "docs/DOC2.md") `
    -OutputDocument "docs/MERGED_DOCUMENT.md" `
    -Date "2026-01-28"
```

### Linux/macOS (Bash)

#### Создание нового документа

```bash
# Создать документ в автоматически определенном каталоге
./scripts/manage-documentation.sh --action create --document "docs/NEW_FEATURE.md"

# Создать документ в корне проекта
./scripts/manage-documentation.sh --action create --document "NEW_DOCUMENT.md"
```

#### Обновление существующего документа

```bash
# Обновить документ (версия увеличится на 1, старая версия будет архивирована)
./scripts/manage-documentation.sh --action update --document "docs/ARCHITECTURE.md"

# Обновить с указанием даты архивации
./scripts/manage-documentation.sh --action update --document "docs/ARCHITECTURE.md" --date "2026-01-28"
```

#### Слияние документов

```bash
# Объединить несколько документов
./scripts/manage-documentation.sh \
    --action merge \
    --sources "docs/DOC1.md" "docs/DOC2.md" "docs/DOC3.md" \
    --output "docs/MERGED_DOCUMENT.md"

# С указанием даты архивации
./scripts/manage-documentation.sh \
    --action merge \
    --sources "docs/DOC1.md" "docs/DOC2.md" \
    --output "docs/MERGED_DOCUMENT.md" \
    --date "2026-01-28"
```

---

## 📁 Структура размещения документации

Скрипт автоматически определяет целевой каталог для документации:

### Каталог `docs/`
Документы с префиксами:
- `ARCHITECTURE*`
- `API*`
- `DEPLOYMENT*`
- `DEVELOPMENT*`
- `INTEGRATION*`
- `ONVIF*`
- `RTSP*`
- `WEBSOCKET*`
- `LICENSE*`
- `TESTING*`
- `CONFIGURATION*`
- `SECURITY*`
- `PERFORMANCE*`
- `TROUBLESHOOTING*`
- `ADMINISTRATOR*`
- `OPERATOR*`
- `USER*`
- `IMPLEMENTATION*`
- `MISSING*`
- `REQUIRED*`

### Корень проекта
Все остальные документы (например, `README.md`, `CHANGELOG.md`, `PROJECT_STRUCTURE.md`)

---

## 🔄 Версионирование

### Формат версии
Версия проекта имеет формат: `Alfa-X.Y.Z`

- **X** - мажорная версия (major)
- **Y** - минорная версия (minor)
- **Z** - патч-версия (patch)

### Инкремент версии
При обновлении документа версия увеличивается на 1 в последней цифре:
- `Alfa-0.0.1` → `Alfa-0.0.2`
- `Alfa-0.0.2` → `Alfa-0.0.3`
- `Alfa-0.1.5` → `Alfa-0.1.6`

### Источник версии
Версия проекта берется из файла `gradle.properties`:
```properties
version=Alfa-0.0.1
```

Если версия не найдена, используется значение по умолчанию: `Alfa-0.0.1`

---

## 📦 Архивация

### Структура архива
```
docs/archive/
├── YYYY-MM-DD/          # Папка на дату архивации
│   ├── DOCUMENT1.md     # Архивная версия документа
│   └── DOCUMENT2.md
└── README.md            # Описание архива
```

### Когда происходит архивация
1. **При обновлении документа** - старая версия копируется в архив перед обновлением
2. **При слиянии документов** - все исходные документы перемещаются в архив

### Формат даты
Дата архивации: `YYYY-MM-DD` (например, `2026-01-28`)

---

## 📝 Формат метаданных документов

### Новый документ
```markdown
# Название документа

**Версия проекта:** Alfa-0.0.1
**Дата создания:** 26 January 2026

---

## Описание

[Описание документа]

---

**Версия проекта:** Alfa-0.0.1
**Дата последнего обновления:** 26 January 2026
```

### Обновленный документ
```markdown
# Название документа

**Версия проекта:** Alfa-0.0.2
**Дата создания:** 26 January 2026
**Дата последнего обновления:** 26 January 2026

---
```

### Объединенный документ
```markdown
# Название объединенного документа

**Версия проекта:** Alfa-0.0.2
**Дата создания:** 26 January 2026
**Объединено из:** 3 документов

---

## DOCUMENT1

[Содержимое первого документа]

---

## DOCUMENT2

[Содержимое второго документа]

---

**Версия проекта:** Alfa-0.0.2
**Дата последнего обновления:** 26 January 2026
**Исходные документы архивированы:** 2026-01-28
```

---

## ⚙️ Параметры скрипта

### PowerShell (`manage-documentation.ps1`)

| Параметр | Обязательный | Описание | Пример |
|----------|--------------|----------|--------|
| `-Action` | Нет | Действие: `create`, `update`, `merge` | `-Action create` |
| `-Document` | Да (для create/update) | Путь к документу | `-Document "docs/ARCHITECTURE.md"` |
| `-SourceDocuments` | Да (для merge) | Массив исходных документов | `-SourceDocuments @("doc1.md", "doc2.md")` |
| `-OutputDocument` | Да (для merge) | Путь к результирующему документу | `-OutputDocument "docs/MERGED.md"` |
| `-TargetDirectory` | Нет | Явное указание целевого каталога | `-TargetDirectory "docs"` |
| `-Date` | Нет | Дата архивации (YYYY-MM-DD) | `-Date "2026-01-28"` |

### Bash (`manage-documentation.sh`)

| Параметр | Обязательный | Описание | Пример |
|----------|--------------|----------|--------|
| `--action` / `-a` | Нет | Действие: `create`, `update`, `merge` | `--action create` |
| `--document` / `-d` | Да (для create/update) | Путь к документу | `--document "docs/ARCHITECTURE.md"` |
| `--sources` / `-s` | Да (для merge) | Список исходных документов | `--sources "doc1.md" "doc2.md"` |
| `--output` / `-o` | Да (для merge) | Путь к результирующему документу | `--output "docs/MERGED.md"` |
| `--target` / `-t` | Нет | Явное указание целевого каталога | `--target "docs"` |
| `--date` | Нет | Дата архивации (YYYY-MM-DD) | `--date "2026-01-28"` |

---

## 📊 Примеры использования

### Пример 1: Создание нового документа

```powershell
# Windows
.\scripts\manage-documentation.ps1 -Action create -Document "docs/NEW_API.md"
```

Результат:
- Создан файл `docs/NEW_API.md` с версией `Alfa-0.0.1`
- Добавлены метаданные (версия, дата создания)

### Пример 2: Обновление документа

```powershell
# Windows
.\scripts\manage-documentation.ps1 -Action update -Document "docs/ARCHITECTURE.md"
```

Результат:
- Старая версия архивирована в `docs/archive/2026-01-28/ARCHITECTURE.md`
- Версия в документе обновлена: `Alfa-0.0.1` → `Alfa-0.0.2`
- Обновлена дата последнего обновления

### Пример 3: Слияние документов

```powershell
# Windows
.\scripts\manage-documentation.ps1 `
    -Action merge `
    -SourceDocuments @("docs/RTSP_CLIENT.md", "docs/ONVIF_CLIENT.md") `
    -OutputDocument "docs/NETWORK_CLIENTS.md"
```

Результат:
- Исходные документы архивированы в `docs/archive/2026-01-28/`
- Создан объединенный документ `docs/NETWORK_CLIENTS.md` с версией `Alfa-0.0.2`
- Исходные документы удалены из исходных мест

---

## ⚠️ Важные замечания

1. **Резервное копирование**: Перед слиянием документов убедитесь, что у вас есть резервная копия
2. **Проверка ссылок**: После перемещения документов проверьте и обновите ссылки в других документах
3. **Обновление индексов**: После создания/обновления документов обновите `DOCUMENTATION_INDEX.md` и `docs/README.md`
4. **Версионирование**: Версия проекта инкрементируется автоматически, но не обновляется в `gradle.properties`
5. **Архивация**: Старые версии документов сохраняются в архиве и не удаляются автоматически

---

## 🔗 Связанные документы

- [DOCUMENTATION_VERSIONING.md](DOCUMENTATION_VERSIONING.md) - Процесс версионирования документации
- [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md) - Полный индекс документации
- [docs/archive/README.md](archive/README.md) - Описание архива документации

---

## 🐛 Устранение неполадок

### Проблема: Скрипт не находит версию проекта

**Решение**: Проверьте наличие файла `gradle.properties` в корне проекта и наличие строки `version=Alfa-0.0.1`

### Проблема: Документ не создается в нужном каталоге

**Решение**: Используйте параметр `-TargetDirectory` / `--target` для явного указания каталога

### Проблема: Ошибка при слиянии документов

**Решение**:
1. Убедитесь, что все исходные документы существуют
2. Проверьте права доступа к файлам
3. Убедитесь, что целевой каталог существует или может быть создан

---

**Версия системы управления:** 1.0
**Последнее обновление:** 26 January 2026

