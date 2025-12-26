# Полный индекс документации проекта IP-CSS

**Версия проекта:** 3.0.0
**Последнее обновление:** Январь 2025

---

## 📚 Оглавление

1. [Начало работы](#начало-работы)
2. [Архитектура и структура](#архитектура-и-структура)
3. [Статус проекта](#статус-проекта)
4. [Техническая документация](#техническая-документация)
5. [Компоненты системы](#компоненты-системы)
6. [Разработка](#разработка)
7. [Развертывание](#развертывание)

---

## Начало работы

### Для новых разработчиков

1. **[README.md](README.md)** - Обзор проекта, быстрый старт
2. **[CURRENT_STATUS.md](CURRENT_STATUS.md)** - Текущее состояние проекта (~20% прогресса)
3. **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Архитектура системы
4. **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** - Структура проекта и модули
5. **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)** - Руководство по разработке

---

## Архитектура и структура

- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Архитектура системы, слои, модули, принципы проектирования
- **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** - Детальная структура проекта, модули, зависимости
- **[docs/PLATFORMS.md](docs/PLATFORMS.md)** - Разделение разработки по платформам (Android, iOS, Desktop, Web, NAS)

---

## Статус проекта

### Статус реализации

- **[docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md)** - Детальный статус реализации всех компонентов
- **[CURRENT_STATUS.md](CURRENT_STATUS.md)** - Краткая сводка текущего состояния проекта
- **[docs/MISSING_FUNCTIONALITY.md](docs/MISSING_FUNCTIONALITY.md)** - Детальный анализ нереализованного функционала

### Планирование

- **[docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md)** - План дальнейшей разработки по фазам
- **[PROJECT_ROADMAP.md](PROJECT_ROADMAP.md)** - Карта выполнения проекта с метриками и приоритетами

---

## Техническая документация

### API

- **[docs/API.md](docs/API.md)** - REST API документация (endpoints, модели, примеры запросов/ответов)
- **[server/web/README.md](server/web/README.md)** - Документация веб-интерфейса (Next.js)
- **[server/web/WEB_UI_IMPLEMENTATION.md](server/web/WEB_UI_IMPLEMENTATION.md)** - Детальная документация реализации веб-интерфейса

### Протоколы и клиенты

- **[docs/ONVIF_CLIENT.md](docs/ONVIF_CLIENT.md)** - Документация ONVIF клиента (использование, API, статус реализации)
- **[docs/RTSP_CLIENT.md](docs/RTSP_CLIENT.md)** - Документация RTSP клиента (использование, API, статус реализации)
- **[docs/WEBSOCKET_CLIENT.md](docs/WEBSOCKET_CLIENT.md)** - Документация WebSocket клиента (использование, API, статус реализации)

### Системные компоненты

- **[docs/LICENSE_SYSTEM.md](docs/LICENSE_SYSTEM.md)** - Система лицензирования (типы лицензий, активация, перенос)
- **[docs/INTEGRATION_GUIDE.md](docs/INTEGRATION_GUIDE.md)** - Руководство по интеграции библиотек (XML парсинг, Live555, FFmpeg, OpenCV, TensorFlow Lite)

---

## Компоненты системы

### Сетевые клиенты

- **[docs/ONVIF_CLIENT.md](docs/ONVIF_CLIENT.md)** - ONVIF клиент для работы с IP-камерами
- **[docs/RTSP_CLIENT.md](docs/RTSP_CLIENT.md)** - RTSP клиент для получения видеопотоков
- **[docs/WEBSOCKET_CLIENT.md](docs/WEBSOCKET_CLIENT.md)** - WebSocket клиент для real-time коммуникации

### Интеграция библиотек

- **[docs/INTEGRATION_GUIDE.md](docs/INTEGRATION_GUIDE.md)** - Руководство по интеграции внешних библиотек
- **[docs/REQUIRED_LIBRARIES.md](docs/REQUIRED_LIBRARIES.md)** - Полный список требуемых библиотек
- **[docs/REQUIRED_LIBRARIES_SUMMARY.md](docs/REQUIRED_LIBRARIES_SUMMARY.md)** - Краткая сводка библиотек

### Нативные библиотеки

- **[docs/NATIVE_LIBRARIES_INTEGRATION.md](docs/NATIVE_LIBRARIES_INTEGRATION.md)** - Интеграция нативных C++ библиотек

---

## Разработка

### Руководства

- **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)** - Руководство по разработке (настройка окружения, сборка, запуск)
- **[docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md)** - План дальнейшей разработки
- **[docs/DEVELOPMENT_TOOLS.md](docs/DEVELOPMENT_TOOLS.md)** - Инструменты разработки
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Руководство по участию в разработке

### Тестирование

- **[docs/TESTING.md](docs/TESTING.md)** - Стратегия тестирования, типы тестов
- **[docs/TESTS_SUMMARY.md](docs/TESTS_SUMMARY.md)** - Сводка созданных тестов
- **[shared/src/commonTest/README.md](shared/src/commonTest/README.md)** - Документация тестов для модуля shared

### Анализ и проектирование

- **[docs/PROMPT_ANALYSIS.md](docs/PROMPT_ANALYSIS.md)** - Анализ исходного промта проекта
- **[docs/ANALYSIS_ERRORS.md](docs/ANALYSIS_ERRORS.md)** - Анализ ошибок и проблем
- **[docs/DOCUMENTATION_GAPS.md](docs/DOCUMENTATION_GAPS.md)** - Анализ недостающей документации
- **[docs/PROJECT_FULL_ANALYSIS.md](docs/PROJECT_FULL_ANALYSIS.md)** - Полный углубленный анализ всего проекта

---

## Развертывание

- **[docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)** - Руководство по развертыванию (Docker, Kubernetes, NAS)
- **[docs/NAS_PLATFORMS_ANALYSIS.md](docs/NAS_PLATFORMS_ANALYSIS.md)** - Детальный анализ NAS платформ (производители, ОС, архитектуры, форматы пакетов, план реализации)
- **[docker-compose.yml](docker-compose.yml)** - Docker Compose конфигурация
- **[Dockerfile](Dockerfile)** - Docker образ для сервера

---

## Дополнительные документы

- **[docs/DOCUMENTATION_GAPS.md](docs/DOCUMENTATION_GAPS.md)** - Анализ недостающей документации

### Отчеты

- **[SECURITY_FIXES_REPORT.md](SECURITY_FIXES_REPORT.md)** - Отчет об исправлениях безопасности
- **[docs/SECURITY_AUDIT_REPORT.md](docs/SECURITY_AUDIT_REPORT.md)** - Отчет аудита безопасности
- **[COMPILATION_REPORT.md](COMPILATION_REPORT.md)** - Отчет о компиляции
- **[KOTLIN_VERSION_ANALYSIS.md](KOTLIN_VERSION_ANALYSIS.md)** - Анализ версий Kotlin
- **[EXTENSIONS_SETUP_REPORT.md](EXTENSIONS_SETUP_REPORT.md)** - Отчет о настройке расширений
- **[PROJECT_REVIEW.md](PROJECT_REVIEW.md)** - Обзор проекта

### Документация модулей

- **[core/network/README.md](core/network/README.md)** - Документация модуля network
- **[core/common/README.md](core/common/README.md)** - Документация модуля common
- **[core/license/src/commonTest/README.md](core/license/src/commonTest/README.md)** - Документация тестов модуля license

### Настройка инструментов

- **[docs/VSCODE_EXTENSIONS.md](docs/VSCODE_EXTENSIONS.md)** - Рекомендуемые расширения VS Code
- **[scripts/install-vscode-extensions.sh](scripts/install-vscode-extensions.sh)** - Скрипт установки расширений VS Code
- **[scripts/install-vscode-extensions.ps1](scripts/install-vscode-extensions.ps1)** - Скрипт установки расширений VS Code (PowerShell)

---

## Быстрые ссылки по задачам

### Я хочу...

- **Понять архитектуру проекта** → [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **Узнать текущий статус** → [CURRENT_STATUS.md](CURRENT_STATUS.md) | [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md)
- **Начать разработку** → [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | [docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md)
- **Работать с ONVIF** → [docs/ONVIF_CLIENT.md](docs/ONVIF_CLIENT.md)
- **Работать с RTSP** → [docs/RTSP_CLIENT.md](docs/RTSP_CLIENT.md)
- **Развернуть систему** → [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)
- **Развернуть на NAS** → [docs/NAS_PLATFORMS_ANALYSIS.md](docs/NAS_PLATFORMS_ANALYSIS.md) | [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)
- **Посмотреть API** → [docs/API.md](docs/API.md)
- **Изучить тесты** → [docs/TESTING.md](docs/TESTING.md) | [docs/TESTS_SUMMARY.md](docs/TESTS_SUMMARY.md)
- **Интегрировать библиотеки** → [docs/INTEGRATION_GUIDE.md](docs/INTEGRATION_GUIDE.md)
- **Работать с лицензированием** → [docs/LICENSE_SYSTEM.md](docs/LICENSE_SYSTEM.md)
- **Узнать, какой документации не хватает** → [docs/DOCUMENTATION_GAPS.md](docs/DOCUMENTATION_GAPS.md)

---

## Карта связей документации

```
README.md (корневой)
  ├── CURRENT_STATUS.md
  ├── PROJECT_STRUCTURE.md
  ├── PROJECT_ROADMAP.md
  └── docs/
      ├── README.md (навигация)
      ├── ARCHITECTURE.md
      ├── IMPLEMENTATION_STATUS.md
      │   └── MISSING_FUNCTIONALITY.md
      ├── DEVELOPMENT_PLAN.md
      ├── DEVELOPMENT.md
      ├── API.md
      ├── ONVIF_CLIENT.md
      │   └── INTEGRATION_GUIDE.md
      ├── RTSP_CLIENT.md
      │   └── INTEGRATION_GUIDE.md
      ├── WEBSOCKET_CLIENT.md
      ├── LICENSE_SYSTEM.md
      ├── DEPLOYMENT_GUIDE.md
      ├── NAS_PLATFORMS_ANALYSIS.md
      └── ...
```

---

## Статистика документации

- **Всего документов:** ~45
- **Основные руководства:** 15+
- **Техническая документация:** 12+
- **Отчеты и анализ:** 10+
- **Документация модулей:** 7+

---

**Версия документации:** 1.0
**Последнее обновление:** Январь 2025

