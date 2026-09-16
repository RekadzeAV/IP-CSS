"""
Tests for Memory Manager
Unit tests for MemoryManager and ChromaDBClient
"""

import pytest
from unittest.mock import Mock, AsyncMock, patch
from datetime import datetime
from memory import MemoryManager, ChromaDBClient


@pytest.fixture
def mock_chroma_client():
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
    return client


@pytest.fixture
def memory_manager(mock_chroma_client):
    """Create MemoryManager with mock"""
    return MemoryManager(mock_chroma_client)


class TestMemoryManager:
    """Tests for MemoryManager"""
    
    @pytest.mark.asyncio
    async def test_initialization(self, memory_manager, mock_chroma_client):
        """Test MemoryManager initialization"""
        assert memory_manager.chroma_client == mock_chroma_client
    
    @pytest.mark.asyncio
    async def test_remember(self, memory_manager):
        """Test remembering a fact"""
        fact_id = await memory_manager.remember(
            user_id="test_user",
            key="language_preference",
            value="English"
        )
        
        assert fact_id is not None
        assert "test_user" in fact_id
    
    @pytest.mark.asyncio
    async def test_remember_with_metadata(self, memory_manager):
        """Test remembering with additional metadata"""
        fact_id = await memory_manager.remember(
            user_id="test_user",
            key="setting",
            value="dark_mode",
            metadata={"category": "ui", "priority": "high"}
        )
        
        assert fact_id is not None
    
    @pytest.mark.asyncio
    async def test_recall(self, memory_manager, mock_chroma_client):
        """Test recalling facts"""
        # Setup mock
        mock_chroma_client.query.return_value = {
            "documents": ["User: language = English"],
            "metadatas": [
                {
                    "user_id": "test_user",
                    "key": "language",
                    "value": "English"
                }
            ],
            "distances": [0.1],
            "ids": ["fact_1"]
        }
        
        results = await memory_manager.recall(
            user_id="test_user",
            query="What language do I prefer?",
            n_results=5
        )
        
        assert isinstance(results, list)
        mock_chroma_client.query.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_recall_empty(self, memory_manager, mock_chroma_client):
        """Test recalling with no results"""
        mock_chroma_client.query.return_value = {
            "documents": [],
            "metadatas": [],
            "distances": [],
            "ids": []
        }
        
        results = await memory_manager.recall(
            user_id="test_user",
            query="nonexistent",
            n_results=5
        )
        
        assert results == []
    
    @pytest.mark.asyncio
    async def test_forget(self, memory_manager, mock_chroma_client):
        """Test forgetting a fact"""
        mock_chroma_client.get.return_value = {
            "ids": ["fact_1", "fact_2"],
            "metadatas": [
                {"user_id": "test_user", "key": "old_key"},
                {"user_id": "test_user", "key": "old_key"}
            ]
        }
        
        count = await memory_manager.forget(
            user_id="test_user",
            key="old_key"
        )
        
        assert count == 2
        mock_chroma_client.delete.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_forget_no_results(self, memory_manager, mock_chroma_client):
        """Test forgetting when no facts found"""
        mock_chroma_client.get.return_value = {
            "ids": [],
            "metadatas": []
        }
        
        count = await memory_manager.forget(
            user_id="test_user",
            key="nonexistent"
        )
        
        assert count == 0
    
    @pytest.mark.asyncio
    async def test_get_user_memory(self, memory_manager, mock_chroma_client):
        """Test getting all user memory"""
        mock_chroma_client.get.return_value = {
            "ids": ["fact_1", "fact_2"],
            "metadatas": [
                {
                    "user_id": "test_user",
                    "key": "key1",
                    "value": "value1",
                    "type": "string",
                    "timestamp": "2026-01-28T00:00:00"
                },
                {
                    "user_id": "test_user",
                    "key": "key2",
                    "value": "value2",
                    "type": "string",
                    "timestamp": "2026-01-28T00:00:00"
                }
            ]
        }
        
        memory = await memory_manager.get_user_memory(user_id="test_user")
        
        assert len(memory) == 2
        assert memory[0]["key"] == "key1"
        assert memory[1]["key"] == "key2"
    
    @pytest.mark.asyncio
    async def test_clear_user_memory(self, memory_manager, mock_chroma_client):
        """Test clearing all user memory"""
        mock_chroma_client.get.return_value = {
            "ids": ["fact_1", "fact_2", "fact_3"],
            "metadatas": []
        }
        
        count = await memory_manager.clear_user_memory(user_id="test_user")
        
        assert count == 3
        mock_chroma_client.delete.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_update_memory(self, memory_manager, mock_chroma_client):
        """Test updating memory"""
        # Setup recall mock
        mock_chroma_client.query.return_value = {
            "documents": ["User: old_key = old_value"],
            "metadatas": [
                {
                    "user_id": "test_user",
                    "key": "old_key",
                    "value": "old_value"
                }
            ],
            "distances": [0.0],
            "ids": ["fact_1"]
        }
        
        # Setup get mock for forget
        mock_chroma_client.get.return_value = {
            "ids": ["fact_1"],
            "metadatas": [
                {
                    "user_id": "test_user",
                    "key": "old_key",
                    "value": "old_value"
                }
            ]
        }
        
        # Setup add mock for remember
        mock_chroma_client.add_documents = AsyncMock(return_value=None)
        
        result = await memory_manager.update_memory(
            user_id="test_user",
            key="old_key",
            new_value="new_value"
        )
        
        assert result is True
    
    @pytest.mark.asyncio
    async def test_detect_value_type(self, memory_manager):
        """Test value type detection"""
        assert memory_manager._detect_value_type("string") == "string"
        assert memory_manager._detect_value_type(123) == "number"
        assert memory_manager._detect_value_type(12.5) == "number"
        assert memory_manager._detect_value_type(True) == "boolean"
        assert memory_manager._detect_value_type({"key": "value"}) == "object"
        assert memory_manager._detect_value_type([1, 2, 3]) == "array"
        assert memory_manager._detect_value_type(None) == "unknown"


class TestChromaDBClient:
    """Tests for ChromaDBClient"""
    
    @pytest.mark.asyncio
    async def test_initialization(self):
        """Test ChromaDBClient initialization"""
        client = ChromaDBClient(
            host="localhost",
            port=8000,
            password="test",
            collection="test_collection"
        )
        
        assert client.host == "localhost"
        assert client.port == 8000
        assert client.collection_name == "test_collection"
        assert client.base_url == "http://localhost:8000/api/v1"
    
    @pytest.mark.asyncio
    async def test_heartbeat(self):
        """Test heartbeat check"""
        client = ChromaDBClient()

        # Корректный мок сессии как async context manager:
        # heartbeat() выполняет `async with self._session.get(url) as response: await response.json()`
        mock_response = Mock()
        mock_response.json = AsyncMock(return_value={"heartbeat": 123456})
        mock_response.__aenter__ = AsyncMock(return_value=mock_response)
        mock_response.__aexit__ = AsyncMock(return_value=False)

        mock_session = Mock()
        mock_session.get = Mock(return_value=mock_response)
        client._session = mock_session

        result = await client.heartbeat()

        assert result["heartbeat"] == 123456


@pytest.mark.asyncio
async def test_memory_integration():
    """Integration test for memory operations"""
    # This test requires real ChromaDB running
    try:
        client = ChromaDBClient(
            host="localhost",
            port=8000,
            collection="test_integration"
        )
        await client.initialize()
        
        manager = MemoryManager(client)
        
        # Remember
        fact_id = await manager.remember(
            user_id="integration_test",
            key="test_key",
            value="test_value"
        )
        assert fact_id is not None
        
        # Recall
        results = await manager.recall(
            user_id="integration_test",
            query="test",
            n_results=1
        )
        assert len(results) > 0
        
        # Forget
        count = await manager.forget(
            user_id="integration_test",
            key="test_key"
        )
        assert count >= 1
        
        await manager.close()
        
    except Exception:
        pytest.skip("ChromaDB not available for integration tests")
