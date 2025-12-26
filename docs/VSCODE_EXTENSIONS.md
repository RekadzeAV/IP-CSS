# Анализ и настройка расширений VS Code / Cursor

## 📋 Анализ проекта

Проект использует следующие технологии:
- **Kotlin Multiplatform** - основной язык разработки
- **C++** - нативные библиотеки для обработки видео
- **TypeScript/React/Next.js** - веб-интерфейс
- **Gradle (Kotlin DSL)** - система сборки
- **CMake** - сборка нативных библиотек
- **SQLDelight** - база данных
- **Docker** - контейнеризация
- **Markdown** - документация

## ✅ Созданные конфигурационные файлы

1. **`.vscode/extensions.json`** - список рекомендуемых расширений
2. **`.vscode/settings.json`** - настройки редактора и форматирования
3. **`.vscode/launch.json`** - конфигурации для отладки
4. **`.vscode/tasks.json`** - задачи сборки и запуска
5. **`.prettierrc.json`** - настройки Prettier
6. **`.editorconfig`** - единые настройки форматирования

## 🔌 Рекомендуемые расширения

### Обязательные (Core)

#### Kotlin Multiplatform
- **fwcd.kotlin** - официальная поддержка Kotlin
- **mathiasfrohlich.kotlin** - альтернативная поддержка Kotlin
- **vscode-gradle** - интеграция с Gradle
- **gradle-language-support** - поддержка Gradle Kotlin DSL

#### C++ / Нативные библиотеки
- **ms-vscode.cpptools** - поддержка C/C++
- **ms-vscode.cpptools-extension-pack** - полный набор инструментов для C++
- **ms-vscode.cmake-tools** - поддержка CMake

#### TypeScript/JavaScript/React
- **dbaeumer.vscode-eslint** - линтер ESLint
- **esbenp.prettier-vscode** - форматтер Prettier
- **ms-vscode.vscode-typescript-next** - улучшенная поддержка TypeScript

#### SQLDelight
- **cashapp.sqldelight** - поддержка SQLDelight

### Рекомендуемые (Highly Recommended)

#### Docker
- **ms-azuretools.vscode-docker** - работа с Docker

#### Git
- **eamodio.gitlens** - расширенные возможности Git
- **mhutchie.git-graph** - визуализация истории Git

#### Markdown
- **yzhang.markdown-all-in-one** - расширенная поддержка Markdown
- **davidanson.vscode-markdownlint** - линтер для Markdown

#### Качество кода
- **detekt.detekt** - интеграция Detekt для Kotlin
- **sonarsource.sonarlint-vscode** - статический анализ кода

#### Тестирование
- **firsttris.vscode-jest-runner** - запуск Jest тестов

### Полезные (Nice to Have)

#### Утилиты
- **ms-vscode.vscode-json** - улучшенная поддержка JSON
- **redhat.vscode-yaml** - поддержка YAML
- **ms-vscode.hexeditor** - редактор hex
- **usernamehw.errorlens** - подсветка ошибок в коде
- **streetsidesoftware.code-spell-checker** - проверка орфографии (английский)
- **streetsidesoftware.code-spell-checker-russian** - проверка орфографии (русский)

#### Управление проектом
- **alefragnani.project-manager** - управление проектами
- **formulahendry.auto-rename-tag** - автоматическое переименование тегов

#### AI помощники (опционально)
- **github.copilot** - GitHub Copilot
- **github.copilot-chat** - GitHub Copilot Chat

## 🚀 Установка расширений

### Автоматическая установка (рекомендуется)

1. Откройте проект в VS Code / Cursor
2. Нажмите `Ctrl+Shift+P` (или `Cmd+Shift+P` на Mac)
3. Введите `Extensions: Show Recommended Extensions`
4. Нажмите на кнопку "Install All" или устанавливайте по одному

### Ручная установка через командную строку

```bash
# Установка всех рекомендуемых расширений через code CLI
code --install-extension fwcd.kotlin
code --install-extension mathiasfrohlich.kotlin
code --install-extension vscjava.vscode-gradle
code --install-extension naco-siren.gradle-language-support
code --install-extension ms-vscode.cpptools
code --install-extension ms-vscode.cpptools-extension-pack
code --install-extension ms-vscode.cmake-tools
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
code --install-extension ms-vscode.vscode-typescript-next
code --install-extension cashapp.sqldelight
code --install-extension ms-azuretools.vscode-docker
code --install-extension eamodio.gitlens
code --install-extension mhutchie.git-graph
code --install-extension yzhang.markdown-all-in-one
code --install-extension davidanson.vscode-markdownlint
code --install-extension detekt.detekt
code --install-extension sonarsource.sonarlint-vscode
code --install-extension firsttris.vscode-jest-runner
code --install-extension usernamehw.errorlens
code --install-extension streetsidesoftware.code-spell-checker
code --install-extension streetsidesoftware.code-spell-checker-russian
```

## ⚙️ Настройки

Все настройки уже сконфигурированы в `.vscode/settings.json`:

- ✅ Автоматическое форматирование при сохранении
- ✅ Настройки отступов для разных языков
- ✅ Исключения файлов из поиска и просмотра
- ✅ Настройки линтеров и форматтеров
- ✅ Проверка орфографии (английский + русский)

## 🎯 Использование задач (Tasks)

Доступные задачи можно запустить через `Ctrl+Shift+P` → `Tasks: Run Task`:

- **Gradle: Build All** - сборка всех модулей
- **Gradle: Test All** - запуск всех тестов
- **Gradle: Clean** - очистка проекта
- **CMake: Configure** - конфигурация CMake
- **CMake: Build** - сборка нативных библиотек
- **Next.js: Install Dependencies** - установка зависимостей веб-проекта
- **Next.js: Build** - сборка веб-проекта
- **Docker: Compose Up** - запуск Docker контейнеров
- **Docker: Compose Down** - остановка Docker контейнеров

## 🐛 Отладка

Конфигурации отладки доступны в `.vscode/launch.json`:

- **Kotlin: Run Tests** - запуск Kotlin тестов
- **Next.js: Debug Server** - отладка Next.js сервера
- **C++: Debug Native Library** - отладка C++ библиотек

## 📝 Проверка установки

После установки расширений проверьте:

1. ✅ Kotlin файлы подсвечиваются синтаксисом
2. ✅ C++ файлы имеют автодополнение
3. ✅ TypeScript файлы проверяются ESLint
4. ✅ SQLDelight файлы (.sq) распознаются
5. ✅ Gradle задачи доступны в палитре команд
6. ✅ Docker контейнеры видны в боковой панели

## 🔄 Обновление расширений

Расширения обновляются автоматически, но можно проверить вручную:

1. Откройте панель расширений (`Ctrl+Shift+X`)
2. Нажмите на иконку обновления вверху
3. Или используйте команду: `Extensions: Check for Extension Updates`

## ❓ Решение проблем

### Расширение не работает
1. Перезапустите VS Code / Cursor
2. Проверьте версию расширения
3. Проверьте логи: `Help` → `Toggle Developer Tools` → `Console`

### Конфликты форматтеров
- Настройки в `.vscode/settings.json` имеют приоритет
- Для конкретных файлов используйте настройки `[language]`

### Gradle не распознается
- Убедитесь, что установлен JDK 17+
- Проверьте переменную окружения `JAVA_HOME`
- Перезапустите редактор после установки JDK

## 📚 Дополнительные ресурсы

- [VS Code Kotlin Extension](https://marketplace.visualstudio.com/items?itemName=fwcd.kotlin)
- [C/C++ Extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode.cpptools)
- [SQLDelight Extension](https://marketplace.visualstudio.com/items?itemName=cashapp.sqldelight)
- [Gradle Extension](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle)

