# 🤖 План Реализации AI-Агента для IP-CSS

**Версия плана:** 1.0  
**Дата создания:** 2026-01-27  
**Статус:** В разработке

---

## 📋 Обзор

AI-агент — это интеллектуальный помощник для системы видеонаблюдения IP-CSS, обеспечивающий взаимодействие через естественный язык, умную автоматизацию и долговременную память.

### Основные возможности:

- 🗣️ Обработка запросов на естественном языке
- 🎯 Умное управление камерами через голос/текст
- 📊 Поиск событий по описанию
- 🧠 Долговременная память и контекст
- 🔗 Интеграция с Ktor backend и Next.js frontend
- ⚙️ Автоматизация через YAML-хуки

---

## 🏗️ Архитектура

### Технологический стек:

| Компонент | Технология | Примечание |
|-----------|-----------|------------|
| **Backend AI** | Python + FastAPI | Основной микросервис |
| **LLM Provider** | LM Studio / Ollama | Local LLM inference |
| **Векторная БД** | ChromaDB | Долговременная память |
| **Очереди** | Redis | AI-specific queues |
| **Клиент** | Kotlin (Ktor) | Интеграция с backend |
| **UI** | Next.js | Чат-интерфейс |

### Компоненты системы:

```
ai-agent/
├── src/
│   ├── agent/           # AgentLoop, AgentContext
│   ├── tools/           # ToolRegistry, BaseTool, IPCameraTools
│   ├── memory/          # MemoryManager, ChromaDBClient
│   ├── hooks/           # EventBus, HookParser, Actions
│   ├── security/        # Auth, Permissions, AuditLogger
│   ├── api/             # REST API, WebSocket
│   ├── config.py        # Настройки
│   └── main.py          # FastAPI сервер
├── tests/               # Модульные тесты
├── logs/                # Логи
├── .env.example         # Шаблон конфигурации
├── requirements.txt     # Зависимости
└── README.md            # Документация
```

---

## 📅 План реализации (7 фаз, ~16 недель)

### Фаза 0: Инфраструктура (2 недели) ✅ ЗАВЕРШЕНА

**Цель:** Подготовить AI инфраструктуру

- [x] 0.1. Проверка LLM провайдеров (Ollama, LM Studio)
- [x] 0.2. Установка ChromaDB Docker образа
- [x] 0.3. Создание docker-compose.ai.yml
- [x] 0.4. Установка Python зависимостей
- [x] 0.5. Настройка конфигурации (.env)
- [x] 0.6. Запуск AI сервисов (ChromaDB, Redis)
- [x] 0.7. Тестирование инфраструктуры

**Результат:** Все сервисы работают и готовы к интеграции

---

### Фаза 1: AI-микросервис (3 недели) 🟡 В ПРОГРЕССЕ

**Цель:** Реализовать базовый FastAPI сервер и AgentLoop

#### Задачи:

- [x] 1.1. Создать структуру проекта ai-agent
- [x] 1.2. Создать FastAPI сервер (main.py, config.py)
- [ ] 1.3. Реализовать AgentLoop (оркестрация запросов)
- [ ] 1.4. Реализовать систему инструментов (ToolRegistry, BaseTool)
- [ ] 1.5. Реализовать систему памяти (ChromaDBClient, MemoryManager)
- [ ] 1.6. Реализовать REST API endpoints
- [ ] 1.7. Реализовать WebSocket API (streaming)
- [ ] 1.8. Реализовать аутентификацию (JWT)
- [ ] 1.9. Создать Dockerfile для production
- [ ] 1.10. Написать модульные тесты

**Результат:** AI-микросервис работает с базовыми функциями

---

### Фаза 2: Интеграция с Backend (2 недели)

**Цель:** Интегрировать AI-агент с Ktor backend

#### Задачи:

- [ ] 2.1. Создать Kotlin client для AI-агента
- [ ] 2.2. Добавить API endpoints в Ktor:
  - POST /api/v1/agent/chat
  - GET /api/v1/agent/tools
  - WS /api/v1/agent/chat/ws
- [ ] 2.3. Реализовать инструменты IP-CSS:
  - list_cameras (READ_CAMERAS)
  - get_camera_snapshot (READ_CAMERAS)
  - search_events (READ_EVENTS)
  - start/stop_recording (CONTROL_CAMERAS, confirm_required)
- [ ] 2.4. Интеграция аутентификации (JWT proxy)
- [ ] 2.5. Rate limiting и middleware

**Результат:** AI-агент интегрирован с backend, инструменты работают

---

### Фаза 3: Система памяти (2 недели)

**Цель:** Реализовать долговременную память агента

#### Задачи:

- [ ] 3.1. Интеграция с ChromaDB
- [ ] 3.2. MemoryManager:
  - remember(user_id, key, value)
  - recall(user_id, query, n_results)
  - forget(user_id, key)
- [ ] 3.3. Сжатие контекста:
  - Микро-сжатие (каждые 10 запросов)
  - Авто-сжатие (>4000 tokens)
  - Ручное сжатие (/compress)
- [ ] 3.4. Интеграция в AgentLoop

**Результат:** Память агента работает с векторным поиском

---

### Фаза 4: Безопасность (1 неделя)

**Цель:** Реализовать модель разрешений и аудит

#### Задачи:

- [ ] 4.1. Модель прав (RBAC):
  - READ_CAMERAS, CONTROL_CAMERAS
  - READ_EVENTS, DELETE_EVENTS
  - USE_AI_AGENT, EXECUTE_HOOKS
- [ ] 4.2. Аннотация инструментов (@Tool(permissions=[...]))
- [ ] 4.3. Подтверждения опасных действий
- [ ] 4.4. Audit Logging (таблица agent_audit_log)
- [ ] 4.5. API для аудита

**Результат:** Безопасность реализована, аудит работает

---

### Фаза 5: Хуки и навыки (2 недели)

**Цель:** Реализовать систему автоматизации

#### Задачи:

- [ ] 5.1. YAML-правила для хуков:
  ```yaml
  hooks:
    - name: motion_alert
      trigger: event_created
      conditions:
        event_type: motion_detected
      actions:
        - take_snapshot
        - send_notification
  ```
- [ ] 5.2. Parser YAML (HookParser)
- [ ] 5.3. Event Bus (subscribe, publish)
- [ ] 5.4. Действия (Actions): TakeSnapshot, SendNotification, StartRecording
- [ ] 5.5. Hot reload правил

**Результат:** Система хуков и навыков работает

---

### Фаза 6: UI агента в Next.js (3 недели)

**Цель:** Создать интерфейс для взаимодействия с агентом

#### Задачи:

- [ ] 6.1. Страница /agent (server/web/src/app/agent/page.tsx)
- [ ] 6.2. Chat компоненты:
  - AgentChat
  - ChatMessage
  - ChatInput
- [ ] 6.3. Redux slice (agentSlice)
- [ ] 6.4. WebSocket hook (useAgentWebSocket)
- [ ] 6.5. Streaming ответов
- [ ] 6.6. Обработка подтверждений

**Результат:** UI агента готов и интегрирован

---

### Фаза 7: Тестирование и документация (1 неделя)

**Цель:** Полное тестирование и документация

#### Задачи:

- [ ] 7.1. End-to-end тесты (Playwright / Cypress)
- [ ] 7.2. Load тесты (100 concurrent users)
- [ ] 7.3. Документация API (OpenAPI / Swagger)
- [ ] 7.4. Пользовательская документация
- [ ] 7.5. README.ai-agent.md

**Результат:** Все протестировано и задокументировано

---

## 🎯 MVP Path (8 недель)

Если нужно быстрее реализовать базовый функционал:

```
MVP (8 недель):
├── Фаза 0: Инфраструктура (2 недели) ✅
├── Фаза 1: AI-микросервис (3 недели) 🟡
├── Фаза 2: Интеграция backend (2 недели)
└── Фаза 4: Безопасность (1 неделя)

Результат: Агент работает с базовыми инструментами и безопасностью
```

---

## 🌐 LLM Провайдеры

### Основной провайдер: LM Studio (порт 1234)

**Преимущества:**
- 28 моделей доступны
- OpenAI-совместимый API
- Мультимодальные модели (vision)
- Русскоязычные модели

**Рекомендуемые модели:**
- qwen/qwen2.5-coder-14b — для кода и задач
- text-embedding-nomic-embed-text-v1.5 — для эмбеддингов
- llama-3.2-11b-vision-instruct — для анализа изображений

### Резервный провайдер: Ollama (порт 11434)

**Преимущества:**
- Docker контейнер (production)
- GPU ускорение через NVIDIA runtime

**Рекомендуемые модели:**
- llama3.1:8b — основная агентная
- nomic-embed-text — эмбеддинги

---

## 🔐 Конфигурация

### Переменные окружения (.env.ai):

**LLM Providers:**
- LLM_PRIMARY_BASE_URL — URL LM Studio (http://localhost:1234/v1)
- LLM_PRIMARY_MODEL — модель по умолчанию
- EMBEDDING_BASE_URL — URL для эмбеддингов

**ChromaDB:**
- CHROMA_HOST — хост ChromaDB
- CHROMA_PORT — порт (8000)
- CHROMA_PASSWORD — пароль
- CHROMA_COLLECTION — имя коллекции

**Redis:**
- REDIS_HOST — хост Redis
- REDIS_PORT — порт (6380)
- REDIS_PASSWORD — пароль

**Security:**
- JWT_SECRET — секрет для JWT
- ENABLE_AUTH — включена ли аутентификация

**См. credentials/.env.ai для полных настроек**

---

## 📊 Текущий статус

**Фаза:** 0 завершена, Фаза 1 в прогрессе

**Готовые компоненты:**
- ✅ AI инфраструктура (ChromaDB, Redis)
- ✅ FastAPI сервер (заглушки)
- ✅ Конфигурация (config.py)
- ✅ LLM провайдеры (Ollama, LM Studio)

**В разработке:**
- 🟡 AgentLoop
- 🟡 Система инструментов
- 🟡 Система памяти

**Планируется:**
- ❌ Интеграция с Ktor backend
- ❌ YAML-хуки
- ❌ UI в Next.js

---

## 🚀 Быстрый старт

### Требования:

- Python 3.11+
- Docker
- Ollama или LM Studio

### Установка:

```bash
# 1. Клонируйте репозиторий
cd IP-CSS

# 2. Установите зависимости
cd ai-agent
pip install -r requirements.txt

# 3. Настройте конфигурацию
cp credentials/.env.ai credentials/.env.ai.local
# Отредактируйте credentials/.env.ai.local

# 4. Запустите AI инфраструктуру
docker-compose -f docker-compose.ai.yml up -d

# 5. Запустите AI Agent
cd ai-agent
uvicorn src.main:app --reload --host 0.0.0.0 --port 8001

# 6. Проверьте API
open http://localhost:8001/docs
```

---

## 📁 Расположение файлов

| Файл/Директория | Путь | Назначие |
|-----------------|------|----------|
| **AI-микросервис** | `ai-agent/` | Python FastAPI сервис |
| **Docker AI** | `docker-compose.ai.yml` | Инфраструктура |
| **Конфигурация** | `credentials/.env.ai` | Пароли и ключи ⚠️ |
| **Шаблон конфигурации** | `ai-agent/.env.example` | Пример .env |
| **Документация** | `docs/ai-agent/` | Документация агента |
| **Этот план** | `docs/planning/AI_AGENT_IMPLEMENTATION_PLAN.md` | План реализации |

---

## 📚 Документация

- [README.ai-agent](../../ai-agent/README.md) — Документация модуля
- [credentials/README.md](../../credentials/README.md) — Настройка credentials
- [LLM Providers](../../docs/ai-agent/LLM_PROVIDERS.md) — Ollama vs LM Studio
- [Progress Tracker](../../docs/ai-agent/PROGRESS.md) — Текущий прогресс

---

**Последнее обновление:** 2026-01-27  
**Следующая задача:** Задача 1.3 — Реализовать AgentLoop
