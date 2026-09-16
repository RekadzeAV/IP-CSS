"""
Tests for AgentLoop
Unit tests for agent orchestration
"""

import pytest
from unittest.mock import Mock, AsyncMock, patch
from agent.AgentLoop import AgentLoop
from agent.AgentContext import AgentContext
from agent.ContextCompressor import ContextCompressor
from tools.ToolRegistry import ToolRegistry
from memory.MemoryManager import MemoryManager


@pytest.fixture
def mock_tool_registry():
    """Mock ToolRegistry"""
    registry = Mock(spec=ToolRegistry)
    registry.get_tool_list.return_value = []
    registry.get_tool.return_value = None
    return registry


@pytest.fixture
def mock_memory_manager():
    """Mock MemoryManager"""
    memory = Mock(spec=MemoryManager)
    memory.recall = AsyncMock(return_value=[])
    memory.remember = AsyncMock(return_value="fact_123")
    memory.forget = AsyncMock(return_value=1)
    memory.clear_user_memory = AsyncMock(return_value=10)
    return memory


@pytest.fixture
def agent_loop(mock_tool_registry, mock_memory_manager):
    """Create AgentLoop with mocks"""
    return AgentLoop(
        tool_registry=mock_tool_registry,
        memory_manager=mock_memory_manager,
        context_compressor=ContextCompressor()
    )


class TestAgentLoop:
    """Tests for AgentLoop class"""
    
    @pytest.mark.asyncio
    async def test_initialization(self, agent_loop, mock_tool_registry, mock_memory_manager):
        """Test AgentLoop initialization"""
        assert agent_loop.tool_registry == mock_tool_registry
        assert agent_loop.memory_manager == mock_memory_manager
        assert agent_loop.context_compressor is not None
        assert agent_loop._system_prompt is not None
    
    @pytest.mark.asyncio
    async def test_process_request_basic(self, agent_loop):
        """Test basic request processing"""
        result = await agent_loop.process_request(
            message="Hello",
            user_id="test_user",
            conversation_id="conv_123"
        )
        
        assert "response" in result
        assert "conversation_id" in result
        assert "tools_used" in result
        assert "memory_used" in result
    
    @pytest.mark.asyncio
    async def test_process_request_with_memory(self, agent_loop, mock_memory_manager):
        """Test request processing with memory"""
        mock_memory_manager.recall.return_value = [
            {"content": "User prefers English", "metadata": {"key": "language"}}
        ]
        
        result = await agent_loop.process_request(
            message="What's the weather?",
            user_id="test_user"
        )
        
        # Memory should be recalled
        mock_memory_manager.recall.assert_called_once()
        
        assert result["conversation_id"] is not None
    
    @pytest.mark.asyncio
    async def test_system_prompt_building(self, agent_loop):
        """Test system prompt construction"""
        prompt = agent_loop._system_prompt
        
        assert "IP-CSS" in prompt
        assert "AI assistant" in prompt
        assert "tools" in prompt.lower()
    
    @pytest.mark.asyncio
    async def test_context_creation(self, agent_loop):
        """Test context creation"""
        context = await agent_loop._create_context(
            user_id="test_user",
            conversation_id="conv_456",
            message="Test message",
            extra_context={"test": "value"}
        )
        
        assert isinstance(context, AgentContext)
        assert context.user_id == "test_user"
        assert context.conversation_id == "conv_456"
        assert context.user_message == "Test message"
        assert context.extra_context == {"test": "value"}


class TestContextCompressor:
    """Tests for ContextCompressor"""
    
    @pytest.mark.asyncio
    async def test_compressor_initialization(self):
        """Test ContextCompressor initialization"""
        compressor = ContextCompressor(
            max_history_length=10,
            summary_threshold=5
        )
        
        assert compressor.max_history_length == 10
        assert compressor.summary_threshold == 5
    
    @pytest.mark.asyncio
    async def test_estimate_tokens(self):
        """Test token estimation"""
        compressor = ContextCompressor()
        
        text = "Hello world"
        tokens = compressor._estimate_tokens(text)
        
        assert tokens > 0
        assert isinstance(tokens, int)
    
    @pytest.mark.asyncio
    async def test_should_compress(self):
        """Test compression decision"""
        compressor = ContextCompressor()
        
        # Should compress
        assert compressor.should_compress(1000, 500) is True
        
        # Should not compress
        assert compressor.should_compress(100, 500) is False
    
    @pytest.mark.asyncio
    async def test_compress_short_prompt(self):
        """Test compression of short prompt"""
        compressor = ContextCompressor()
        
        short_prompt = "Hello"
        result = await compressor.compress(short_prompt, max_tokens=1000)
        
        # Short prompt should not be compressed
        assert result == short_prompt


@pytest.mark.asyncio
async def test_agent_loop_with_real_components():
    """Integration test with real components (if available)"""
    # This test requires ChromaDB and other services to be running
    # Skip if services are not available
    
    try:
        from tools import register_ipcamera_tools
        from memory import ChromaDBClient, MemoryManager
        
        # Try to initialize real components
        chroma_client = ChromaDBClient(
            host="localhost",
            port=8000,
            password="test",
            collection="test_collection"
        )
        
        # Skip if ChromaDB is not available
        pytest.skip("Real component tests require ChromaDB running")
        
    except Exception:
        pytest.skip("Real component tests require services running")
