# Сводка среды разработки IP-CSS

**Дата составления:** 26 January 2026  
**Версия документа:** 1.0  
**Проект:** IP Camera Surveillance System (IP-CSS)  
**Версия проекта:** Alfa-0.0.1  
**Текущий прогресс:** ~60%

---

## 📋 ОГЛАВЛЕНИЕ

1. [Описание проекта](#-описание-проекта)
2. [Локальное окружение](#-локальное-окружение)
3. [Среда разработки](#-среда-разработки)
4. [Модели ИИ и конфигурация Continue](#-модели-и-конфигурация-continue)
5. [Инструменты и расширения](#-инструменты-и-расширения)
6. [MCP и доступ к файловой системе](#-mcp-и-доступ-к-файловой-системе)
7. [Текущий статус разработки](#-текущий-статус-разработки)

---

## 🎯 ОПИСАНИЕ ПРОЕКТА

**IP-CSS (IP Camera Surveillance System)** - кроссплатформенная система видеонаблюдения с IP-камер с продвинутой AI-аналитикой.

### Основные возможности:
- 🎥 Поддержка множества IP-камер (RTSP, ONVIF, HTTP, MJPEG)
- 📹 Непрерывная запись и запись по событиям
- 🤖 AI-аналитика (детекция объектов, трекинг, детекция движения)
- 🔐 Аутентификация и авторизация (JWT, RBAC, 2FA, LDAP/AD, SSO, Kerberos)
- 🌐 Веб-интерфейс и мобильные приложения
- 📱 Кроссплатформенность (Android, iOS, Desktop, NAS, Server)

### Технологический стек:
- **Backend:** Kotlin Multiplatform, Ktor, SQLDelight
- **Android:** Jetpack Compose, Hilt
- **iOS:** SwiftUI
- **Web:** Next.js 15, React, TypeScript, Material-UI
- **Native:** C++/CMake, FFmpeg, OpenCV, TensorFlow Lite
- **Инфраструктура:** Docker, Gradle, CI/CD

---

## 💻 ЛОКАЛЬНОЕ ОКРУЖЕНИЕ

### Операционная система
- **Платформа:** Windows
- **PowerShell:** Доступен для автоматизации

### Установленные компоненты

#### Node.js & NPM
- **MCP Server:** `@modelcontextprotocol/server-filesystem@2026.1.14` (глобально установлен)
- **Использование:** Предоставляет доступ к файловой системе проекта для AI-ассистентов

#### Java/Kotlin Development
- **Gradle:** 8.7.0
- **Kotlin Multiplatform:** 2.0.21
- **JDK:** Настроен через Oracle Java extension

#### C++ Development
- **CMake:** 3.15+
- **FFmpeg:** Интеграция для обработки видео
- **OpenCV:** 4.10.0 для AI-аналитики
- **TensorFlow Lite:** 2.16.0 для AI моделей

#### Docker
- **Docker Desktop:** Установлен
- **Docker Compose:** Множественные конфигурации (dev, integration, manual)

### Сетевая конфигурация

#### Локальный AI-сервер
- **Адрес:** `http://localhost:1234/v1`
- **Тип:** OpenAI-compatible API сервер (LM Studio / llama.cpp)
- **Статус:** Активен и настроен

#### Порты
- **1234:** Local LLM API endpoint
- **9443:** Portainer (Raspberry Pi deployment)

### Каталог проекта
- **Путь:** `D:/GitHub-Ai/IP-CSS`
- **Статус:** Подтверждён через `Test-Path` → `True`
- **Доступность:** Полностью доступна для чтения/записи

---

## 🛠️ СРЕДА РАЗРАБОТКИ

### Основное окружение

#### Visual Studio Code
- **Редактор:** VS Code (основная IDE)
- **Язык пакетов:** PowerShell для автоматизации

#### Расширения VS Code

**AI-ассистенты:**
- `continue.continue` - Continue AI-ассистент
- `saoudrizwan.claude-dev` - Claude Dev
- `koda.koda` - Koda AI-помощник

**Языки и фреймворки:**
- `ms-python.*` - Python (debugpy, pylance, vs-python-envs)
- `redhat.java`, `vscjava.*` - Java/Kotlin разработка
- `ms-vscode.cpp-devtools`, `ms-vscode.cpptools` - C++ разработка
- `ms-vscode.cmake-tools` - CMake интеграция
- `ms-azuretools.vscode-docker` - Docker
- `ms-vscode.powershell` - PowerShell
- `golang.go` - Go
- `devsense.*` - PHP (Intelli, Composer, Profiler)

**Утилиты:**
- `gruntfuggly.todo-tree` - Древовидный просмотр TODO
- `wayou.vscode-todo-highlight` - Подсветка TODO
- `vscode-icons-team.vscode-icons` - Иконки файлов
- `dotjoshjohnson.xml` - XML редактор
- `redhat.vscode-xml`, `redhat.vscode-yaml` - YAML/XML валидация
- `zainchen.json`, `eriklynd.json-tools` - JSON инструменты
- `shd101wyy.markdown-preview-enhanced` - Markdown предпросмотр

**Дополнительно:**
- `beilunyang.cursor-rules` - Cursor правила
- `buianhthang.xml2json` - Конвертер XML→JSON
- `geequlim.godot-tools` - Godot (опционально)
- `oracle.oracle-java` - Oracle Java

### Конфигурация VSCode

**Файл:** `.vscode/settings.json`

```json
{
    "cmake.sourceDirectory": "D:/GitHub-Ai/IP-CSS/native",
    "java.compile.nullAnalysis.mode": "automatic",
    "java.configuration.updateBuildConfiguration": "automatic",
    "git.ignoreLimitWarning": true
}
```

**Настройки:**
- CMake источник: `D:/GitHub-Ai/IP-CSS/native`
- Автоматическая компиляция Java с null-анализом
- Автоматическое обновление build configuration
- Игнорирование предупреждений о лимите Git

---

## 🤖 МОДЕЛИ И КОНФИГУРАЦИЯ CONTINUE

### Continue Extension

**Файлы конфигурации:**
- **Проект:** `.continue/config.json`
- **Агенты:** `.continue/agents/config.json`
- **Глобальный:** `~/.continue/config.json`

### Доступные модели (все через локальный сервер)

Все модели подключены к `http://localhost:1234/v1` (OpenAI-compatible API):

| Название модели | Провайдер | Модель | Назначение |
|----------------|-----------|--------|------------|
| `qwen/qwen2.5-coder-14b` | OpenAI | qwen2.5-coder-14b | Основной код-ассистент |
| `deepseek/deepseek-r1-0528-qwen3-8b` | OpenAI | deepseek-r1-0528-qwen3-8b | Рациональный анализ |
| `mistralai/magistral-small-2509` | OpenAI | magistral-small-2509 | Быстрые задачи |
| `qwen/qwen2.5-coder-32b` | OpenAI | qwen2.5-coder-32b | Сложные задачи, рефакторинг |

### Характеристики моделей

#### qwen2.5-coder-14b / 32b
- **Специализация:** Генерация кода, рефакторинг, отладка
- **Плюсы:** Отличное понимание кода, поддержка множества языков
- **Использование:** Основная разработка, code review

#### deepseek-r1-0528-qwen3-8b
- **Специализация:** Рациональный анализ, планирование
- **Плюсы:** Логическое мышление, анализ архитектуры
- **Использование:** Проектирование, анализ проблем

#### magistral-small-2509
- **Специализация:** Быстрые задачи, ответы на вопросы
- **Плюсы:** Быстрый отклик, низкие ресурсы
- **Использование:** Вопросы, документация, простые правки

---

## 🔌 ИНСТРУМЕНТЫ И РАСШИРЕНИЯ

### AI-Ассистенты

#### Continue
- **Расширение:** `continue.continue`
- **Функции:**
  - Чат с AI в редакторе
  - Автодополнение кода
  - Поиск по кодовой базе
  - Рефакторинг
  - Генерация тестов
  - Работа с MCP серверами

#### Koda
- **Расширение:** `koda.koda`
- **Функции:**
  - AI-помощник по программированию
  - Команды для работы с файлами
  - Автоматизация задач

#### Claude Dev
- **Расширение:** `saoudrizwan.claude-dev`
- **Функции:**
  - Интеграция с Claude
  - Анализ кода
  - Планирование задач

### MCP (Model Context Protocol)

#### Конфигурация MCP

**Файл:** `.continue/config.json`

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "D:/GitHub-Ai/IP-CSS"
      ]
    }
  }
}
```

#### Возможности MCP

**filesystem сервер:**
- ✅ Полный доступ к файловой системе проекта
- ✅ Чтение файлов произвольной глубины
- ✅ Поиск по паттернам
- ✅ Доступ к каталогу `D:/GitHub-Ai/IP-CSS`
- ✅ Работа через `npx @modelcontextprotocol/server-filesystem`

**Что может видеть MCP:**
- Исходный код всех платформ (Kotlin, C++, TypeScript, Python)
- Конфигурационные файлы (Docker, Gradle, CMake)
- Документацию (Markdown, PDF)
- Скрипты автоматизации (PowerShell, Bash)
- Тесты и CI/CD пайплайны

**Что может делать MCP:**
- Анализировать структуру проекта
- Находить определения и ссылки
- Читать конфигурации для понимания окружения
- Предоставлять контекст для AI-ассистентов

---

## 🔧 MCP И ДОСТУП К ФАЙЛОВОЙ СИСТЕМЕ

### Настройка доступа

**Путь проекта:** `D:/GitHub-Ai/IP-CSS`

**Проверка доступности:**
```powershell
Test-Path "D:/GitHub-Ai/IP-CSS"  # → True
```

**MCP сервер:**
- **Пакет:** `@modelcontextprotocol/server-filesystem@2026.1.14`
- **Установка:** Глобально через `npm install -g`
- **Запуск:** Автоматический через Continue

### Доступные каталоги

MCP имеет доступ ко всему проекту:

```
IP-CSS/
├── shared/                    # Kotlin Multiplatform
├── core/                      # Общие модули
├── native/                    # C++ библиотеки
│   ├── video-processing/
│   ├── analytics/
│   └── codecs/
├── android/                   # Android приложение
├── server/                    # Сервер (API + Web)
├── platforms/                 # Платформо-специфичные
├── docs/                      # Документация
├── .continue/                 # Continue конфигурация
└── .vscode/                   # VSCode настройки
```

### Примеры использования MCP

1. **Поиск файлов:**
   - `**/*.kt` - все Kotlin файлы
   - `**/README.md` - все README
   - `**/*.cpp` - все C++ исходники

2. **Анализ структуры:**
   - Просмотр каталогов
   - Подсчёт файлов
   - Анализ зависимостей

3. **Чтение контекста:**
   - Конфигурации (build.gradle, CMakeLists.txt)
   - Документация (docs/*.md)
   - Скрипты (scripts/*.ps1, scripts/*.sh)

---

## 📊 ТЕКУЩИЙ СТАТУС РАЗРАБОТКИ

### Прогресс проекта: ~60%

#### ✅ Реализовано

**Инфраструктура (100%):**
- Модульная структура проекта
- Gradle конфигурация
- SQLDelight настройка
- CI/CD пайплайны
- Docker конфигурация
- Полная документация

**Доменный слой (~45%):**
- Модели данных (Camera, Recording, Event, User, Settings, Notification)
- Интерфейсы репозиториев
- 15 Use Cases реализовано

**Слой данных (~65%):**
- SQLDelight схемы
- Реализации репозиториев (все CRUD операции)
- Entity мапперы
- Unit тесты

**Серверная часть (~85%):**
- REST API сервер (Ktor)
- WebSocket сервер
- JWT аутентификация
- RBAC авторизация
- Серверные сервисы (VideoRecording, HLS, Screenshot)

**Веб-интерфейс (~70%):**
- Next.js 15 конфигурация
- Основные страницы (Login, Dashboard, Cameras, Events, Recordings, Settings)
- Redux store
- WebSocket интеграция
- React компоненты

**Android UI (~30%):**
- Jetpack Compose экраны
- ViewModels
- DI конфигурация (Koin)
- ExoVideoPlayer

#### ⚠️ В разработке

**RTSP клиент (~10%):**
- Kotlin обертка готова
- C++ библиотека с заголовками
- ❌ FFI биндинги
- ❌ Реализация протокола

**AI-аналитика (~5%):**
- Базовая структура
- ❌ Полная реализация

**ONVIF клиент (~40%):**
- Базовые методы
- ❌ WS-Discovery
- ❌ Полноценный XML парсинг

#### ❌ Не реализовано

- iOS UI (SwiftUI)
- Desktop UI (Compose Desktop)
- Расширенная AI-аналитика
- Уведомления (Push, Email, SMS, Telegram)
- Облачная синхронизация
- Распознавание лиц / ANPR

---

## 📝 ДОКУМЕНТАЦИЯ ПРОЕКТА

### Ключевые документы

| Документ | Описание |
|----------|----------|
| `README.md` | Основное описание проекта |
| `PROJECT_PROMPT.md` | Комплексный промпт с деталями реализации |
| `DOCUMENTATION_INDEX.md` | Индекс всей документации |
| `docs/RASPBERRY_PI_CONFIGURATION.md` | Развёртывание на Raspberry Pi |
| `docs/status/MODULE_STATUS_BASELINE_*.md` | Статус модулей по датам |
| `docs/kmp-phase1-progress.md` | Прогресс KMP Phase 1 |
| `docs/PLATFORM_STRUCTURE.md` | Структура платформ |

### Локальная документация

- `docs/` - Основная документация
- `docs/status/` - Статусные документы
- `docs/kmp/` - Kotlin Multiplatform документация
- `CHANGELOG.md` - История изменений
- `CONTRIBUTING.md` - Руководство по вкладу

---

## 🚀 СЛЕДУЮЩИЕ ШАГИ

### Приоритеты разработки

1. **Завершение RTSP клиента** (приоритет: критический)
   - FFI биндинги Kotlin ↔ C++
   - Реализация RTSP протокола
   - Интеграция с видеоплеером

2. **Доработка WS-Discovery** (приоритет: высокий)
   - UDP multicast реализация
   - ONVIF discovery
   - Тестирование с реальными камерами

3. **AI-аналитика** (приоритет: высокий)
   - Детекция движения
   - Детекция объектов
   - Трекинг объектов

4. **iOS / Desktop UI** (приоритет: средний)
   - SwiftUI экраны для iOS
   - Compose Desktop для Windows/Linux/macOS

5. **Система уведомлений** (приоритет: средний)
   - Push (FCM, APNS)
   - Email (SMTP)
   - Telegram бот
   - Webhooks

---

## 📌 ЗАМЕЧАНИЯ И РЕКОМЕНДАЦИИ

### Локальное окружение

**Текущее состояние:**
- ✅ Все необходимые инструменты установлены
- ✅ Continue настроен с локальными моделями
- ✅ MCP предоставляет полный доступ к проекту
- ✅ VSCode с полным набором расширений

**Рекомендации:**
- Поддерживать актуальность локальных моделей
- Регулярно обновлять зависимости
- Документировать изменения в конфигурации

### Модели ИИ

**Использование моделей:**
- `qwen2.5-coder-32b` - для сложных задач и рефакторинга
- `qwen2.5-coder-14b` - для повседневной разработки
- `deepseek-r1` - для анализа архитектуры
- `magistral-small` - для быстрых вопросов

**Оптимизация:**
- Использовать подходящую модель под задачу
- Кешировать контекст проекта
- Использовать MCP для быстрого доступа к файлам

### MCP и файловая система

**Безопасность:**
- MCP имеет доступ только к проекту `D:/GitHub-Ai/IP-CSS`
- Нет доступа к системным файлам
- Нет доступа к другим каталогам

**Производительность:**
- Использование паттернов для поиска
- Кэширование часто используемых файлов
- Минимизация чтения больших файлов

---

## 🔗 ССЫЛКИ И РЕСУРСЫ

### Внутренняя документация
- [PROJECT_PROMPT.md](../../ai-agent/PROJECT_PROMPT.md)
- [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)
- [README.md](README.md)

### Внешние ресурсы
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Ktor Documentation](https://ktor.io/docs/getting-started-ktor-client.html)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Next.js Documentation](https://nextjs.org/docs)

### Инструменты
- [Continue Documentation](https://docs.continue.dev/)
- [MCP Documentation](https://modelcontextprotocol.io/)

---

**Документ составлен автоматически на основе анализа конфигурации проекта и локального окружения.**

**Последнее обновление:** 26 January 2026
