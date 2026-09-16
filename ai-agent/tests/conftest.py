"""
Pytest configuration and fixtures for AI Agent tests
"""

import pytest
import asyncio
from unittest.mock import Mock, AsyncMock


@pytest.fixture(scope="session")
def event_loop():
    """Create event loop for async tests"""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture
def mock_chroma_client():
    """Mock ChromaDBClient for tests"""
    client = Mock()
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


@pytest.fixture
def mock_agent_loop(mock_chroma_client):
    """Mock AgentLoop for tests"""
    from agent import AgentLoop, ContextCompressor
    from tools import ToolRegistry
    from memory import MemoryManager
    
    memory_manager = MemoryManager(mock_chroma_client, use_mock_embeddings=True)
    tool_registry = ToolRegistry()
    
    return AgentLoop(
        tool_registry=tool_registry,
        memory_manager=memory_manager,
        context_compressor=ContextCompressor()
    )


@pytest.fixture
def sample_user_id():
    """Sample user ID for tests"""
    return "test_user_123"


@pytest.fixture
def sample_conversation_id():
    """Sample conversation ID for tests"""
    return "conv_test_456"


@pytest.fixture
def sample_message():
    """Sample message for tests"""
    return "Hello, how can you help me today?"


# Markers
def pytest_configure(config):
    """Configure pytest markers"""
    config.addinivalue_line(
        "markers", "unit: Unit tests"
    )
    config.addinivalue_line(
        "markers", "integration: Integration tests"
    )
    config.addinivalue_line(
        "markers", "e2e: End-to-end tests"
    )
    config.addinivalue_line(
        "markers", "slow: Slow tests"
    )
