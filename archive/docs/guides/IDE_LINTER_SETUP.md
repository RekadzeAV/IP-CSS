# Настройка IDE и линтеров

Этот документ описывает конфигурацию IDE и линтеров для проекта IP-CSS.

## 📋 Содержание

- [C++ (Clangd)](#c-clangd)
- [TypeScript/JavaScript (ESLint)](#typescriptjavascript-eslint)
- [Kotlin (Detekt)](#kotlin-detekt)
- [VS Code настройки](#vs-code-настройки)
- [Генерация compile_commands.json](#генерация-compile_commandsjson)

---

## C++ (Clangd)

### Конфигурация

Проект использует **clangd** для анализа C++ кода. Конфигурация находится в:
- `.clangd` - корневая конфигурация
- `native/.clangd` - специфичная для native кода

### Установка clangd

#### Windows
```powershell
# Через Chocolatey
choco install llvm

# Или скачать с https://github.com/clangd/clangd/releases
```

#### Linux
```bash
sudo apt-get install clangd
# или
sudo yum install clang-tools-extra
```

#### macOS
```bash
brew install llvm
```

### Использование

После установки clangd автоматически подхватит конфигурацию из `.clangd` файла.

### Генерация compile_commands.json

Для правильной работы clangd необходимо сгенерировать `compile_commands.json`:

**Windows (PowerShell):**
```powershell
.\scripts\generate-compile-commands.ps1
```

**Linux/macOS:**
```bash
chmod +x scripts/generate-compile-commands.sh
./scripts/generate-compile-commands.sh
```

Это создаст файл `native/compile_commands.json`, который clangd будет использовать для понимания структуры проекта.

### Форматирование кода

Проект использует `.clang-format` для форматирования C++ кода. Настройки основаны на стиле LLVM с модификациями:
- Отступ: 4 пробела
- Максимальная длина строки: 120 символов
- Стандарт: C++17

---

## TypeScript/JavaScript (ESLint)

### Конфигурация

ESLint конфигурация находится в `server/web/.eslintrc.json`.

### Установка зависимостей

```bash
cd server/web
npm install
```

### Использование

ESLint автоматически работает в VS Code при сохранении файла (если включен `formatOnSave`).

### Правила

- `@typescript-eslint/no-explicit-any`: предупреждение
- `@typescript-eslint/no-unused-vars`: предупреждение (игнорирует переменные с префиксом `_`)
- `react/no-unescaped-entities`: отключено
- `react/react-in-jsx-scope`: отключено (Next.js не требует)

### Prettier

Конфигурация Prettier находится в `server/web/.prettierrc.json`:
- Полужирные точки с запятой
- Двойные кавычки
- Ширина строки: 120 символов
- Отступ: 2 пробела

---

## Kotlin (Detekt)

### Конфигурация

Detekt конфигурация находится в `detekt.yml` в корне проекта.

### Использование

Detekt автоматически работает в VS Code через расширение `detekt.detekt`.

### Запуск вручную

```bash
./gradlew detekt
```

---

## VS Code настройки

### Рекомендуемые расширения

Список рекомендуемых расширений находится в `.vscode/extensions.json`. Основные:

- **Kotlin**: `fwcd.kotlin`
- **C++**: `ms-vscode.cpptools`, `llvm-vs-code-extensions.vscode-clangd`
- **TypeScript**: `ms-vscode.vscode-typescript-next`
- **ESLint**: `dbaeumer.vscode-eslint`
- **Prettier**: `esbenp.prettier-vscode`
- **CMake**: `ms-vscode.cmake-tools`

### Настройки редактора

Основные настройки в `.vscode/settings.json`:
- Форматирование при сохранении включено
- Автоматическое исправление ESLint при сохранении
- Автоматическая организация импортов
- Максимальная длина строки: 120 символов

### Настройки для C++

- **Clangd** используется вместо встроенного IntelliSense
- Встроенный IntelliSense отключен (`C_Cpp.intelliSenseEngine: "disabled"`)
- Clangd использует флаги из `.clangd` конфигурации

---

## Генерация compile_commands.json

### Автоматическая генерация

Используйте скрипты в `scripts/`:
- `generate-compile-commands.ps1` (Windows)
- `generate-compile-commands.sh` (Linux/macOS)

### Ручная генерация

```bash
cd native
cmake -B build -DCMAKE_EXPORT_COMPILE_COMMANDS=ON
cp build/compile_commands.json .
```

### Что это дает?

`compile_commands.json` содержит информацию о том, как компилируется каждый файл:
- Флаги компиляции
- Include директории
- Определения препроцессора

Это позволяет clangd правильно понимать структуру проекта и предоставлять точные подсказки и проверки.

---

## Решение проблем

### Clangd не видит заголовочные файлы

1. Убедитесь, что `compile_commands.json` сгенерирован
2. Проверьте, что `.clangd` файл находится в корне проекта
3. Перезапустите clangd сервер в VS Code: `Ctrl+Shift+P` → "clangd: Restart language server"

### TypeScript ошибки в IDE

1. Убедитесь, что зависимости установлены: `cd server/web && npm install`
2. Перезапустите TypeScript сервер: `Ctrl+Shift+P` → "TypeScript: Restart TS server"
3. Проверьте, что `tsconfig.json` правильно настроен

### ESLint не работает

1. Убедитесь, что расширение ESLint установлено
2. Проверьте, что `server/web/.eslintrc.json` существует
3. Перезагрузите окно VS Code: `Ctrl+Shift+P` → "Developer: Reload Window"

---

## Дополнительные ресурсы

- [Clangd документация](https://clangd.llvm.org/)
- [ESLint документация](https://eslint.org/)
- [Detekt документация](https://detekt.github.io/detekt/)
- [VS Code C++ настройки](https://code.visualstudio.com/docs/languages/cpp)

---

**Последнее обновление:** 2026-01-26
