# 🔄 Обновлённый анализ LLM провайдеров

**Дата:** 2026-01-27  
**Статус:** Обновлено после проверки LM Studio

---

## LLM Провайдеры в локальном окружении

### ✅ Ollama (порт 11434)

**Статус:** Запущен и работает

**Установленные модели (10):**
| Модель | Размер | Назначение |
|--------|--------|------------|
| llama3.1:8b | 4.9 GB | Основная агентная |
| qwen3:8b | 5.2 GB | Альтернативная |
| qwen3:4b | 2.5 GB | Быстрая |
| gemma3:12b | 8.1 GB | Тяжёлая |
| gpt-oss:20b | 13 GB | Очень тяжёлая |
| deepseek-r1:8b | 5.2 GB | Для кода |
| deepseek-coder:latest | 776 MB | Быстрый для кода |
| qwen2.5-coder:1.5b | 986 MB | Очень быстрый для кода |
| nomic-embed-text:latest | 274 MB | Эмбеддинги |

**Плюсы:**
- ✅ Docker контейнер (легкая изоляция)
- ✅ GPU ускорение через NVIDIA runtime
- ✅ Простое управление через CLI

**Минусы:**
- ❌ Меньше моделей (10 vs 28)
- ❌ Нет GUI
- ❌ Нет мультимодальных моделей

---

### ✅ LM Studio (порт 1234) ⭐ РЕКОМЕНДУЕТСЯ

**Статус:** Запущен и работает

**Установленные модели (28):**

#### Код и разработка
| Модель | Размер | Назначение |
|--------|--------|------------|
| qwen/qwen3-coder-30b | ~18 GB | Мощная для кода |
| qwen/qwen2.5-coder-32b | ~19 GB | Оптимальная для кода |
| qwen/qwen2.5-coder-14b | ~8 GB | Быстрая для кода |
| deepseek/deepseek-r1-0528-qwen3-8b | ~5 GB | Reasoning для кода |

#### Универсальные модели
| Модель | Размер | Назначение |
|--------|--------|------------|
| mistralai/mistral-nemo-instruct-2407 | ~12 GB | Универсальная |
| mistralai/devstral-small-2-2512 | ~4 GB | Быстрая универсальная |
| mistralai/ministral-3-3b | ~2 GB | Очень быстрая |
| mistralai/ministral-3-14b-reasoning | ~9 GB | Reasoning |
| mistralai/magistral-small-2509 | ~4 GB | Эффективная |

#### Мультимодальные
| Модель | Размер | Назначение |
|--------|--------|------------|
| llama-3.2-11b-vision-instruct | ~7 GB | Визуальный анализ |
| bakllava1-mistralllava-7b | ~4 GB | Vision + текст |
| moondream-2b-2025-04-14 | ~1.5 GB | Быстрая vision |
| qwen/qwen2.5-vl-7b | ~4 GB | Vision language |

#### Эмбеддинги
| Модель | Размер | Назначение |
|--------|--------|------------|
| text-embedding-nomic-embed-text-v1.5 | ~500 MB | Векторные эмбеддинги |

#### Русскоязычные
| Модель | Размер | Назначение |
|--------|--------|------------|
| vikhrmodels_vikhr-yandexgpt-5-lite-8b-it | ~5 GB | RU язык |

#### Другие
| Модель | Размер | Назначение |
|--------|--------|------------|
| google/gemma-4-31b | ~18 GB | Мощная Google |
| google/gemma-4-e4b | ~3 GB | Быстрая Google |
| google/gemma-4-e2b | ~2 GB | Очень быстрая Google |
| nvidia/nemotron-3-nano-omni | ~2 GB | Универсальная NVIDIA |
| nvidia/nemotron-3-nano-4b | ~3 GB | NVIDIA 4B |
| nvidia/nemotron-3-nano | ~2 GB | NVIDIA база |
| c4ai-command-r7b-arabic-02-2025 | ~5 GB | Multilingual |
| qwen/qwq-32b | ~19 GB | Reasoning Qwen |
| uigen-t1.1-qwen-14b | ~9 GB | Генерация |
| essentialai/rnj-1 | ? | Специфическая |
| allenai/olmo-3-32b-think | ~19 GB | Reasoning |
| zai-org/glm-4.6v-flash | ~4 GB | Быстрая GLM |
| openai/gpt-oss-20b | ~13 GB | OpenAI OSS |

**Плюсы:**
- ✅ Больше моделей (28 против 10)
- ✅ OpenAI-совместимый API (`/v1/chat/completions`)
- ✅ Мультимодальные модели (vision)
- ✅ Русскоязычные модели (YandexGPT)
- ✅ Удобный GUI для управления
- ✅ Легко переключать модели
- ✅ Поддержка разных квантований

**Минусы:**
- ❌ Работает только на Windows (GUI)
- ❌ Нет Docker контейнера

---

## 🎯 Рекомендация для AI-агента

### Основной провайдер: **LM Studio**

**Причины:**
1. Больше моделей для разных задач
2. OpenAI-совместимый API (легче интеграция с LangChain)
3. Мультимодальные возможности (анализ скриншотов камер)
4. Русскоязычные модели для лучшего понимания контекста
5. Быстрое переключение между моделями

### Резервный провайдер: **Ollama**

**Причины:**
1. Docker контейнер (работает в production)
2. GPU ускорение через NVIDIA runtime
3. Простое управление

---

## 🔧 Конфигурация для AI-агента

### Для разработки (локально):

```python
# ai-agent/src/config.py

LLM_PROVIDERS = {
    "primary": {
        "type": "openai-compatible",
        "base_url": "http://localhost:1234/v1",  # LM Studio
        "model": "qwen/qwen2.5-coder-14b",  # Баланс скорости и качества
        "api_key": "lm-studio",  # Не требуется, но нужно для совместимости
    },
    "fallback": {
        "type": "ollama",
        "base_url": "http://localhost:11434",
        "model": "llama3.1:8b",
    }
}

EMBEDDING_PROVIDER = {
    "type": "openai-compatible",
    "base_url": "http://localhost:1234/v1",  # LM Studio
    "model": "text-embedding-nomic-embed-text-v1.5",
}
```

### Для production (Docker):

```python
# ai-agent/src/config.py

LLM_PROVIDERS = {
    "primary": {
        "type": "ollama",
        "base_url": "http://ollama:11434",  # Docker сервис
        "model": "llama3.1:8b",
    }
}

EMBEDDING_PROVIDER = {
    "type": "ollama",
    "base_url": "http://ollama:11434",
    "model": "nomic-embed-text",
}
```

---

## 📊 Сравнение возможностей

| Функция | Ollama | LM Studio |
|---------|--------|-----------|
| Количество моделей | 10 | 28 |
| OpenAI API совместимость | ❌ | ✅ |
| Мультимодальные модели | ❌ | ✅ |
| Русскоязычные модели | ❌ | ✅ |
| Docker поддержка | ✅ | ❌ |
| GUI управление | ❌ | ✅ |
| GPU ускорение | ✅ | ✅ |
| Эмбеддинги | ✅ | ✅ |
| Streaming | ✅ | ✅ |
| Функция вызова (tools) | ✅ | ✅ |

---

## 🔄 Обновление документации

**Файлы для обновления:**
- [x] `NEW-PLAN.md` - Добавить LM Studio в раздел LLM провайдеров
- [x] `ai-agent/.env.example` - Добавить конфигурацию для LM Studio
- [x] `ai-agent/README.md` - Добавить инструкцию по использованию LM Studio
- [ ] `docs/ai-agent/setup.md` - Создать с инструкциями по настройке

**Действия:**
1. Обновить `.env.example` с двумя провайдерами
2. Обновить `AgentLoop.py` с поддержкой fallback
3. Протестировать оба провайдера

---

*Последнее обновление: 2026-01-27*
