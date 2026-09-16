# AI Agent Module for IP-CSS

**Статус:** 🟡 В разработке (Фаза 1)  
**Версия:** 0.1.0  
**Последнее обновление:** 2026-01-27

---

## 📋 Описание

AI Agent — это FastAPI-сервис для системы видеонаблюдения IP-CSS с поддержкой естественного языка. Обеспечивает:
- 🗣️ Обработку запросов на естественном языке
- 🎯 Умную работу с инструментами (cameras, events, recordings)
- 🧠 Долговременную память на основе ChromaDB
- 🔒 Безопасность через JWT-аутентификацию
- ⚙️ Автоматизацию через YAML-хуки

---

## 🏗️ Структура проекта

```
ai-agent/
├── src/
│   ├── __init__.py
│   ├── main.py                 # FastAPI точка входа
│   ├── config.py               # Конфигурация
│   │
│   ├── agent/                  # Оркестратор агента
│   │   ├── __init__.py
│   │   ├── AgentLoop.py        # Основной цикл агента
│   │   ├── AgentContext.py     # Контекст запроса
│   │   └── ContextCompressor.py # Сжатие контекста
│   │
│   ├── tools/                  # Инструменты агента
│   │   ├── __init__.py
│   │   ├── ToolRegistry.py     # Реестр инструментов
│   │   ├── BaseTool.py         # Базовый класс
│   │   └── IPCameraTools.py    # Инструменты IP-CSS
│   │
│   ├── memory/                 # Система памяти
│   │   ├── __init__.py
│   │   ├── MemoryManager.py    # Менеджер памяти
│   │   └── ChromaDBClient.py   # Клиент ChromaDB
│   │
│   ├── hooks/                  # Система хуков
│   │   ├── __init__.py
│   │   ├── HookParser.py       # Парсер YAML
│   │   ├── EventBus.py         # Шина событий
│   │   └── Actions.py          # Действия
│   │
│   └── security/               # Безопасность
│       ├── __init__.py
│       ├── Permissions.py      # Модель прав
│       ├── ConfirmationManager.py # Подтверждения
│       └── AuditLogger.py      # Аудит
│
├── tests/                      # Тесты
│   ├── __init__.py
│   ├── test_agent.py
│   ├── test_tools.py
│   └── test_memory.py
│
├── logs/                       # Логи
├── data/                       # Данные (если нужно)
├── requirements.txt            # Зависимости
├── .env.example                # Пример конфигурации
├── .gitignore
├── Dockerfile                  # Docker образ
└── README.md                   # Документация
```

---

## 🚀 Быстрый старт

### 1. Установка зависимостей

```bash
cd ai-agent
pip install -r requirements.txt
```

### 2. Настройка конфигурации

```bash
# Скопируйте пример конфигурации из credentials
cp credentials/.env.ai credentials/.env.ai.local

# Отредактируйте с вашими настройками
nano credentials/.env.ai.local
```

**Важно:** Конфигурация находится в `credentials/.env.ai` для безопасности.  
См. [credentials/README.md](../credentials/README.md) для подробностей.

### 3. Запуск AI инфраструктуры

```bash
# В корне проекта
docker-compose -f docker-compose.ai.yml up -d

# Проверьте статус
docker-compose -f docker-compose.ai.yml ps

# Логи
docker-compose -f docker-compose.ai.yml logs -f chromadb
```

### 4. Запуск сервера разработки

```bash
cd ai-agent
# Переменные окружения из .env.ai.local
export $(cat credentials/.env.ai.local | xargs)

uvicorn src.main:app --reload --host 0.0.0.0 --port 8001
```

### 5. Тестирование

```bash
# Открыть браузер
open http://localhost:8001/docs

# Или через curl
curl http://localhost:8001/api/v1/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Привет!", "user_id": "admin"}'
```

---

## 📚 Документация

### Основные документы:

| Документ | Путь | Назначение |
|----------|------|------------|
| **AI Agent план** | [docs/planning/AI_AGENT_IMPLEMENTATION_PLAN.md](../docs/planning/AI_AGENT_IMPLEMENTATION_PLAN.md) | План реализации |
| **Progress tracker** | [docs/ai-agent/PROGRESS.md](../docs/ai-agent/PROGRESS.md) | Текущий прогресс |
| **LLM провайдеры** | [docs/ai-agent/LLM_PROVIDERS.md](../docs/ai-agent/LLM_PROVIDERS.md) | Ollama vs LM Studio |
| **Credentials** | [credentials/README.md](../credentials/README.md) | Настройка .env |

### API документация:

Сервер автоматически генерирует Swagger UI документацию по адресу:
- **Swagger UI:** http://localhost:8001/docs
- **ReDoc:** http://localhost:8001/redoc

---

## 🛠️ Основные компоненты

### AgentLoop
Основной цикл работы агента:
1. Получить запрос от пользователя
2. Собрать контекст (время, пользователь, память)
3. Вызвать LLM
4. Парсить ответ (инструмент или текст)
5. Выполнить инструмент (если нужно)
6. Вернуть ответ

### ToolRegistry
Реестр доступных инструментов:
- `list_cameras` — список камер
- `get_snapshot` — снимок с камеры
- `search_events` — поиск событий
- `start_recording` / `stop_recording` — управление записью

### MemoryManager
Управление долговременной памятью:
- `remember` — запомнить факт
- `recall` — вспомнить по запросу
- `forget` — забыть

### HookSystem
Система автоматизации на YAML:

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

---

## 🔐 LLM Провайдеры

### Основные провайдеры:

| Провайдер | Порт | Использование |
|-----------|------|---------------|
| **LM Studio** | 1234 | Разработка (рекомендуется) |
| **Ollama** | 11434 | Production |

### Рекомендуемые модели:

- `qwen/qwen2.5-coder-14b` — для кода и задач (LM Studio)
- `llama3.1:8b` — для общих задач (Ollama)
- `text-embedding-nomic-embed-text-v1.5` — для эмбеддингов

См. [docs/ai-agent/LLM_PROVIDERS.md](../docs/ai-agent/LLM_PROVIDERS.md) для подробностей.

---

## 🔐 Безопасность

### Аутентификация:

AI-агент использует JWT токены для аутентификации.

**Endpoints без аутентификации:**
- `GET /api/v1/health` — Health check
- `POST /api/v1/auth/login` — Login

**Endpoints с аутентификацией:**
- `POST /api/v1/agent/chat` — Chat с агентом
- `GET /api/v1/agent/tools` — Список инструментов
- `WS /api/v1/agent/chat/ws` — WebSocket streaming

### Разрешения:

Инструменты имеют модель разрешений (RBAC):
- `READ_CAMERAS` — Просмотр камер
- `CONTROL_CAMERAS` — Управление камерами
- `READ_EVENTS` — Просмотр событий
- `USE_AI_AGENT` — Использование AI-агента

---

## 🧠 Память

AI-агент использует ChromaDB для долговременной памяти.

**Функции памяти:**
- `remember(user_id, key, value)` — Запоминание факта
- `recall(user_id, query, n_results)` — Поиск по памяти
- `forget(user_id, key)` — Забывание факта

**Сжатие контекста:**
- Микро-сжатие (каждые 10 запросов)
- Авто-сжатие (>4000 tokens)
- Ручное сжатие (`/compress`)

---

## ⚙️ Конфигурация

### Переменные окружения:

См. [ai-agent/.env.example](.env.example) для полного списка.

**Основные переменные:**

```bash
# LLM Providers
LLM_PRIMARY_BASE_URL=http://localhost:1234/v1
LLM_PRIMARY_MODEL=qwen/qwen2.5-coder-14b

# ChromaDB
CHROMA_HOST=localhost
CHROMA_PORT=8000
CHROMA_COLLECTION=agent_memory

# Redis
REDIS_HOST=localhost
REDIS_PORT=6380

# Security
JWT_SECRET=your-secret-key
ENABLE_AUTH=true
```

**Полные настройки:** [credentials/.env.ai](../credentials/.env.ai)

---

## 🛠️ Разработка

### Добавление нового инструмента

```python
from tools.BaseTool import BaseTool
from tools.ToolRegistry import permission

class MyNewTool(BaseTool):
    @permission("READ_CAMERAS")
    async def execute(self, params, user_context):
        # Ваша логика
        return {"result": "success"}

# Регистрация
from tools.ToolRegistry import register_tool
register_tool(MyNewTool())
```

### Тестирование

```bash
# Все тесты
pytest

# С покрытием
pytest --cov=src

# Конкретный тест
pytest tests/test_agent.py -v
```

---

## 🐳 Docker

### Построение образа

```bash
docker build -t ip-css-ai-agent .
```

### Запуск в Docker

```bash
docker-compose -f docker-compose.ai.yml up -d ai-agent
```

---

## 🐛 Troubleshooting

### Ошибка подключения к Ollama

```bash
# Проверить, что Ollama запущен
curl http://localhost:11434/api/version

# Если нет - запустить
docker-compose -f docker-compose.ai.yml up -d ollama
```

### Ошибка подключения к ChromaDB

```bash
# Проверить статус
curl http://localhost:8000/api/v1/heartbeat

# Перезапустить
docker-compose -f docker-compose.ai.yml restart chromadb
```

### Ошибки Python зависимостей

```bash
# Переустановить зависимости
pip install -r requirements.txt --force-reinstall
```

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

**Детальный план:** [docs/planning/AI_AGENT_IMPLEMENTATION_PLAN.md](../docs/planning/AI_AGENT_IMPLEMENTATION_PLAN.md)

---

## 📝 Changelog

### 0.1.0 (2026-01-27)

- ✅ Фаза 0: Инфраструктура (ChromaDB, Redis)
- 🟡 Фаза 1: FastAPI сервер (в разработке)
- 📚 Документация и конфигурация

---

## 🤝 Вклад

См. [CONTRIBUTING.md](../CONTRIBUTING.md) для правил внесения вкладов.

---

## 📞 Поддержка

- **Issues:** https://github.com/RekadzeAV/IP-CSS/issues
- **Документация:** [docs/README.md](../docs/README.md)
- **AI Agent план:** [docs/planning/AI_AGENT_IMPLEMENTATION_PLAN.md](../docs/planning/AI_AGENT_IMPLEMENTATION_PLAN.md)

---

## 📄 Лицензия

Часть проекта IP-CSS. См. корневой LICENSE файл.
