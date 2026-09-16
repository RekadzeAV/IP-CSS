"""
Tests for Tools
Unit tests for IPCameraTools and ToolRegistry
"""

import pytest
from unittest.mock import Mock, AsyncMock, patch
from agent.AgentContext import AgentContext
from datetime import datetime
from tools import ToolRegistry, BaseTool, register_tool, get_tool
from tools.IPCameraTools import (
    ListCamerasTool,
    GetCameraSnapshotTool,
    SearchEventsTool,
    StartRecordingTool,
    StopRecordingTool,
    PTZControlTool
)


@pytest.fixture
def mock_context():
    """Create mock AgentContext"""
    return AgentContext(
        user_id="test_user",
        conversation_id="conv_test",
        timestamp=datetime.now(),
        user_message="Test message",
        memory_context=[],
        extra_context={}
    )


class TestToolRegistry:
    """Tests for ToolRegistry"""
    
    def test_singleton_pattern(self):
        """Test ToolRegistry singleton"""
        registry1 = ToolRegistry()
        registry2 = ToolRegistry()
        
        assert registry1 is registry2
    
    def test_register_tool(self):
        """Test tool registration"""
        registry = ToolRegistry()
        tool = ListCamerasTool()
        
        registry.register(tool)
        
        assert registry.has_tool("list_cameras")
        assert len(registry) > 0
    
    def test_get_tool(self):
        """Test getting tool by name"""
        registry = ToolRegistry()
        tool = ListCamerasTool()
        
        registry.register(tool)
        
        retrieved = registry.get_tool("list_cameras")
        assert retrieved is not None
        assert retrieved.name == "list_cameras"
    
    def test_unregister_tool(self):
        """Test tool unregistration"""
        registry = ToolRegistry()
        tool = ListCamerasTool()
        
        registry.register(tool)
        assert registry.has_tool("list_cameras")
        
        registry.unregister("list_cameras")
        assert not registry.has_tool("list_cameras")
    
    def test_get_tool_list(self):
        """Test getting list of all tools"""
        registry = ToolRegistry()
        
        # Clear existing tools
        registry._tools.clear()
        
        # Register test tools
        registry.register(ListCamerasTool())
        registry.register(SearchEventsTool())
        
        tools = registry.get_tool_list()
        
        assert len(tools) == 2
        assert any(t["name"] == "list_cameras" for t in tools)
        assert any(t["name"] == "search_events" for t in tools)


class TestListCamerasTool:
    """Tests for ListCamerasTool"""
    
    @pytest.mark.asyncio
    async def test_tool_properties(self):
        """Test tool properties"""
        tool = ListCamerasTool()
        
        assert tool.name == "list_cameras"
        assert "camera" in tool.description.lower()
        assert "READ_CAMERAS" in tool.permissions
    
    @pytest.mark.asyncio
    async def test_execute(self, mock_context):
        """Test tool execution"""
        tool = ListCamerasTool()
        
        result = await tool.execute({}, mock_context)
        
        assert result["success"] is True
        assert "cameras" in result
        assert "count" in result
        assert isinstance(result["cameras"], list)
    
    @pytest.mark.asyncio
    async def test_execute_with_filter(self, mock_context):
        """Test tool execution with status filter"""
        tool = ListCamerasTool()
        
        result = await tool.execute(
            {"status_filter": "online"},
            mock_context
        )
        
        assert result["success"] is True
        assert result["filter"] == "online"
        # All demo cameras should be filtered correctly
        for camera in result["cameras"]:
            assert camera["status"] == "online" or result["filter"] == "all"


class TestGetCameraSnapshotTool:
    """Tests for GetCameraSnapshotTool"""
    
    @pytest.mark.asyncio
    async def test_tool_properties(self):
        """Test tool properties"""
        tool = GetCameraSnapshotTool()
        
        assert tool.name == "get_camera_snapshot"
        assert "snapshot" in tool.description.lower()
    
    @pytest.mark.asyncio
    async def test_execute_missing_camera_id(self, mock_context):
        """Test execution without camera_id"""
        tool = GetCameraSnapshotTool()
        
        result = await tool.execute({}, mock_context)
        
        assert result["success"] is False
        assert "error" in result
    
    @pytest.mark.asyncio
    async def test_execute_success(self, mock_context):
        """Test successful execution"""
        tool = GetCameraSnapshotTool()
        
        result = await tool.execute(
            {"camera_id": "camera_001"},
            mock_context
        )
        
        assert result["success"] is True
        assert result["camera_id"] == "camera_001"
        assert "snapshot_url" in result


class TestSearchEventsTool:
    """Tests for SearchEventsTool"""
    
    @pytest.mark.asyncio
    async def test_execute(self, mock_context):
        """Test event search"""
        tool = SearchEventsTool()
        
        result = await tool.execute({}, mock_context)
        
        assert result["success"] is True
        assert "events" in result
        assert "count" in result
    
    @pytest.mark.asyncio
    async def test_execute_with_type_filter(self, mock_context):
        """Test event search with type filter"""
        tool = SearchEventsTool()
        
        result = await tool.execute(
            {"event_type": "motion_detected"},
            mock_context
        )
        
        assert result["success"] is True
        for event in result["events"]:
            assert event["type"] == "motion_detected"


class TestStartRecordingTool:
    """Tests for StartRecordingTool"""
    
    @pytest.mark.asyncio
    async def test_permission_check_no_confirm(self, mock_context):
        """Test permission check without confirmation"""
        tool = StartRecordingTool()
        
        result = await tool.check_permissions(
            user_id="test_user",
            parameters={"confirm": False}
        )
        
        assert result is False
    
    @pytest.mark.asyncio
    async def test_permission_check_with_confirm(self, mock_context):
        """Test permission check with confirmation"""
        tool = StartRecordingTool()
        
        result = await tool.check_permissions(
            user_id="test_user",
            parameters={"confirm": True}
        )
        
        assert result is True
    
    @pytest.mark.asyncio
    async def test_execute(self, mock_context):
        """Test recording start"""
        tool = StartRecordingTool()
        
        result = await tool.execute(
            {"camera_id": "camera_001", "confirm": True},
            mock_context
        )
        
        assert result["success"] is True
        assert result["status"] == "recording"


class TestStopRecordingTool:
    """Tests for StopRecordingTool"""
    
    @pytest.mark.asyncio
    async def test_permission_check(self, mock_context):
        """Test permission check"""
        tool = StopRecordingTool()
        
        # Without confirmation
        assert await tool.check_permissions("user", {"confirm": False}) is False
        
        # With confirmation
        assert await tool.check_permissions("user", {"confirm": True}) is True
    
    @pytest.mark.asyncio
    async def test_execute(self, mock_context):
        """Test recording stop"""
        tool = StopRecordingTool()
        
        result = await tool.execute(
            {"camera_id": "camera_001", "confirm": True},
            mock_context
        )
        
        assert result["success"] is True
        assert result["status"] == "stopped"


class TestPTZControlTool:
    """Tests for PTZControlTool"""
    
    @pytest.mark.asyncio
    async def test_execute(self, mock_context):
        """Test PTZ control"""
        tool = PTZControlTool()
        
        result = await tool.execute(
            {
                "camera_id": "camera_001",
                "action": "pan_left",
                "speed": 5
            },
            mock_context
        )
        
        assert result["success"] is True
        assert result["action"] == "pan_left"
        assert result["speed"] == 5


@pytest.mark.asyncio
async def test_register_ipcamera_tools():
    """Test registration of all IPCameraTools"""
    from tools import register_ipcamera_tools, ToolRegistry
    
    registry = ToolRegistry()
    registry._tools.clear()  # Clear existing
    
    register_ipcamera_tools()
    
    expected_tools = [
        "list_cameras",
        "get_camera_snapshot",
        "search_events",
        "start_recording",
        "stop_recording",
        "ptz_control"
    ]
    
    for tool_name in expected_tools:
        assert registry.has_tool(tool_name), f"Tool {tool_name} not registered"
