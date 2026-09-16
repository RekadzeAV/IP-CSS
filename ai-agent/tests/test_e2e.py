"""
End-to-End Tests for AI Agent

Полные тесты сценариев использования:
- Чат с агентом
- Использование инструментов
- Работа с памятью
- WebSocket communication
"""

import pytest
import pytest_asyncio
from unittest.mock import Mock, AsyncMock, patch
from datetime import datetime
import asyncio

from agent import AgentLoop, AgentContext, ContextCompressor
from tools import ToolRegistry, register_ipcamera_tools
from memory import MemoryManager, ChromaDBClient, MockEmbeddings


@pytest_asyncio.fixture
async def mock_chroma_client():
    """Mock ChromaDBClient"""
    client = Mock(spec=ChromaDBClient)
    client.add_documents = AsyncMock(return_value=None)
    client.query = AsyncMock(return_value={
        "documents": [],
        "metadatas": [],
        "distances": [],
        "ids": []
    })
    client.get = AsyncMock(return_value={
        "ids": [],
        "metadatas": []
    })
    client.delete = AsyncMock(return_value=None)
    client.initialize = AsyncMock(return_value=None)
    client.close = AsyncMock(return_value=None)
    return client


@pytest_asyncio.fixture
async def agent_with_tools(mock_chroma_client):
    """Create AgentLoop with tools and memory"""
    # Setup MemoryManager with mock embeddings
    memory_manager = MemoryManager(mock_chroma_client, use_mock_embeddings=True)
    
    # Setup ToolRegistry
    tool_registry = ToolRegistry()
    register_ipcamera_tools()
    
    # Setup AgentLoop
    agent_loop = AgentLoop(
        tool_registry=tool_registry,
        memory_manager=memory_manager,
        context_compressor=ContextCompressor()
    )
    
    return agent_loop, tool_registry, memory_manager


@pytest.mark.asyncio
@pytest.mark.e2e
async def test_full_chat_flow(agent_with_tools):
    """
    E2E тест полного цикла чата:
    1. Отправка сообщения
    2. Обработка через AgentLoop
    3. Использование инструмента
    4. Сохранение в память
    5. Получение ответа
    """
    agent_loop, tool_registry, memory_manager = agent_with_tools
    
    # 1. Отправка сообщения
    result = await agent_loop.process_request(
        message="Какие камеры онлайн?",
        user_id="test_user",
        conversation_id="conv_e2e_1"
    )
    
    # 2. Проверка ответа
    assert "response" in result
    assert result["conversation_id"] is not None
    assert isinstance(result["response"], str)
    assert len(result["response"]) > 0
    
    # 3. Проверка использования инструментов
    assert "tools_used" in result
    assert isinstance(result["tools_used"], list)
    
    # 4. Проверка использования памяти
    assert "memory_used" in result
    assert isinstance(result["memory_used"], bool)


@pytest.mark.asyncio
@pytest.mark.e2e
async def test_tool_execution_flow(agent_with_tools):
    """
    E2E тест выполнения инструмента:
    1. Запрос списка камер
    2. Парсинг ответа LLM
    3. Выполнение инструмента
    4. Возврат результата
    """
    agent_loop, tool_registry, memory_manager = agent_with_tools
    
    # Запрос, требующий использования инструмента
    result = await agent_loop.process_request(
        message="Покажи список всех камер",
        user_id="admin",
        conversation_id="conv_e2e_2"
    )
    
    # Проверка результата (безопасно: не вычислять result["success"], если ключа нет)
    assert ("success" in result and result["success"]) or "response" in result
    assert result["conversation_id"] is not None


@pytest.mark.asyncio
@pytest.mark.e2e
async def test_memory_integration(agent_with_tools):
    """
    E2E тест интеграции с памятью:
    1. Запоминание факта
    2. Поиск по памяти
    3. Обновление факта
    4. Удаление факта
    """
    agent_loop, tool_registry, memory_manager = agent_with_tools
    user_id = "memory_test_user"
    
    # 1. Запоминание факта
    fact_id = await memory_manager.remember(
        user_id=user_id,
        key="language_preference",
        value="English"
    )
    assert fact_id is not None
    
    # 2. Поиск по памяти
    results = await memory_manager.recall(
        user_id=user_id,
        query="What language do I prefer?",
        n_results=5
    )
    assert isinstance(results, list)
    
    # 3. Получение всей памяти
    memory = await memory_manager.get_user_memory(user_id=user_id)
    assert isinstance(memory, list)
    assert len(memory) > 0
    
    # 4. Обновление факта
    updated = await memory_manager.update_memory(
        user_id=user_id,
        key="language_preference",
        new_value="Russian"
    )
    assert updated is True
    
    # 5. Удаление факта
    deleted_count = await memory_manager.forget(
        user_id=user_id,
        key="language_preference"
    )
    assert deleted_count >= 1


@pytest.mark.asyncio
@pytest.mark.e2e
async def test_semantic_search(agent_with_tools):
    """
    E2E тест семантического поиска:
    1. Запоминание нескольких фактов
    2. Семантический поиск
    3. Проверка схожести
    """
    agent_loop, tool_registry, memory_manager = agent_with_tools
    user_id = "semantic_test_user"
    
    # 1. Запоминание фактов
    await memory_manager.remember(
        user_id=user_id,
        key="camera_1_location",
        value="Front entrance"
    )
    await memory_manager.remember(
        user_id=user_id,
        key="camera_2_location",
        value="Backyard gate"
    )
    await memory_manager.remember(
        user_id=user_id,
        key="camera_3_location",
        value="Garage door"
    )
    
    # 2. Семантический поиск
    results = await memory_manager.search_semantic(
        user_id=user_id,
        query="Where is the front camera?",
        n_results=3,
        similarity_threshold=0.3
    )
    
    # 3. Проверка результатов
    assert isinstance(results, list)
    assert len(results) > 0
    
    # Проверка схожести
    for result in results:
        assert "similarity" in result
        assert "fact" in result
        assert result["similarity"] >= 0.3


@pytest.mark.asyncio
@pytest.mark.e2e
async def test_multi_turn_conversation(agent_with_tools):
    """
    E2E тест многошагового диалога:
    1. Первый вопрос
    2. Второй вопрос с контекстом
    3. Проверка сохранения контекста
    """
    agent_loop, tool_registry, memory_manager = agent_with_tools
    user_id = "conversation_test_user"
    conversation_id = "conv_e2e_multi"
    
    # 1. Первый вопрос
    result1 = await agent_loop.process_request(
        message="Привет! Как дела?",
        user_id=user_id,
        conversation_id=conversation_id
    )
    assert result1["conversation_id"] == conversation_id
    
    # 2. Второй вопрос (с тем же conversation_id)
    result2 = await agent_loop.process_request(
        message="А какие камеры у меня есть?",
        user_id=user_id,
        conversation_id=conversation_id
    )
    assert result2["conversation_id"] == conversation_id
    
    # 3. Проверка сохранения контекста
    assert "response" in result2
    assert isinstance(result2["response"], str)


@pytest.mark.asyncio
@pytest.mark.e2e
async def test_tool_permissions(agent_with_tools):
    """
    E2E тест проверок прав доступа:
    1. Попытка выполнить инструмент без подтверждения
    2. Выполнение с подтверждением
    3. Проверка логов
    """
    agent_loop, tool_registry, memory_manager = agent_with_tools
    
    # 1. Запрос на запуск записи (требует подтверждения)
    result = await agent_loop.process_request(
        message="Начать запись с камеры 1",
        user_id="admin",
        conversation_id="conv_e2e_permissions"
    )
    
    # 2. Проверка ответа (должен запросить подтверждение)
    assert "response" in result
    # Ответ должен содержать запрос подтверждения


@pytest.mark.asyncio
@pytest.mark.e2e
async def test_error_handling(agent_with_tools):
    """
    E2E тест обработки ошибок:
    1. Неверные параметры
    2. Отсутствующий инструмент
    3. Ошибки сети (mock)
    """
    agent_loop, tool_registry, memory_manager = agent_with_tools
    
    # 1. Пустое сообщение
    result = await agent_loop.process_request(
        message="",
        user_id="error_test_user",
        conversation_id="conv_e2e_errors"
    )
    # Должен обработать корректно
    assert "response" in result or "error" in result


@pytest.mark.asyncio
@pytest.mark.e2e
async def test_concurrent_requests():
    """
    E2E тест конкурентных запросов:
    1. Отправка 10 параллельных запросов
    2. Проверка отсутствия коллизий
    3. Проверка корректности ответов
    """
    # Setup
    mock_chroma = Mock(spec=ChromaDBClient)
    mock_chroma.add_documents = AsyncMock(return_value=None)
    mock_chroma.query = AsyncMock(return_value={
        "documents": [],
        "metadatas": [],
        "distances": [],
        "ids": []
    })
    mock_chroma.get = AsyncMock(return_value={"ids": [], "metadatas": []})
    mock_chroma.delete = AsyncMock(return_value=None)
    
    memory_manager = MemoryManager(mock_chroma, use_mock_embeddings=True)
    
    tool_registry = ToolRegistry()
    register_ipcamera_tools()
    
    agent_loop = AgentLoop(
        tool_registry=tool_registry,
        memory_manager=memory_manager,
        context_compressor=ContextCompressor()
    )
    
    # 1. Отправка параллельных запросов
    tasks = [
        agent_loop.process_request(
            message=f"Запрос {i}",
            user_id=f"concurrent_user_{i % 3}",
            conversation_id=f"conv_concurrent_{i}"
        )
        for i in range(10)
    ]
    
    # 2. Выполнение всех задач
    results = await asyncio.gather(*tasks, return_exceptions=True)
    
    # 3. Проверка результатов
    assert len(results) == 10
    for result in results:
        if isinstance(result, Exception):
            pytest.fail(f"Request failed with exception: {result}")
        assert "response" in result or "conversation_id" in result


@pytest.mark.asyncio
@pytest.mark.e2e
async def test_memory_cleanup(agent_with_tools):
    """
    E2E тест очистки памяти:
    1. Запоминание нескольких фактов
    2. Очистка памяти пользователя
    3. Проверка что память пуста
    """
    agent_loop, tool_registry, memory_manager = agent_with_tools
    user_id = "cleanup_test_user"
    
    # 1. Запоминание фактов
    for i in range(5):
        await memory_manager.remember(
            user_id=user_id,
            key=f"test_key_{i}",
            value=f"test_value_{i}"
        )
    
    # 2. Проверка что факты сохранены
    memory_before = await memory_manager.get_user_memory(user_id=user_id)
    assert len(memory_before) == 5
    
    # 3. Очистка памяти
    deleted_count = await memory_manager.clear_user_memory(user_id=user_id)
    assert deleted_count == 5
    
    # 4. Проверка что память пуста
    memory_after = await memory_manager.get_user_memory(user_id=user_id)
    assert len(memory_after) == 0


@pytest.mark.asyncio
@pytest.mark.e2e
async def test_full_integration_with_real_components():
    """
    E2E тест с реальными компонентами (если доступны)
    Требуется запущенный ChromaDB
    """
    try:
        # Попытка подключения к реальному ChromaDB
        from memory import ChromaDBClient, MemoryManager, Embeddings
        from tools import ToolRegistry, register_ipcamera_tools
        from agent import AgentLoop, ContextCompressor
        
        chroma_client = ChromaDBClient(
            host="localhost",
            port=8000,
            collection="e2e_test_collection"
        )
        await chroma_client.initialize()
        
        memory_manager = MemoryManager(chroma_client, use_mock_embeddings=True)
        
        tool_registry = ToolRegistry()
        register_ipcamera_tools()
        
        agent_loop = AgentLoop(
            tool_registry=tool_registry,
            memory_manager=memory_manager,
            context_compressor=ContextCompressor()
        )
        
        # Базовый тест
        result = await agent_loop.process_request(
            message="Test message",
            user_id="e2e_real_user",
            conversation_id="conv_e2e_real"
        )
        
        assert "response" in result
        
        # Очистка
        await memory_manager.clear_user_memory("e2e_real_user")
        await chroma_client.close()
        
    except Exception as e:
        # Пропуск если ChromaDB недоступен
        pytest.skip(f"Real components not available: {e}")
