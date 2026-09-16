# Tools Module
# Инструменты AI-агента

from .ToolRegistry import ToolRegistry, register_tool, get_tool
from .BaseTool import BaseTool, ToolDefinition
from .IPCameraTools import (
    ListCamerasTool,
    GetCameraSnapshotTool,
    SearchEventsTool,
    StartRecordingTool,
    StopRecordingTool,
    PTZControlTool,
    register_ipcamera_tools
)

__all__ = [
    "ToolRegistry",
    "register_tool",
    "get_tool",
    "BaseTool",
    "ToolDefinition",
    "ListCamerasTool",
    "GetCameraSnapshotTool",
    "SearchEventsTool",
    "StartRecordingTool",
    "StopRecordingTool",
    "PTZControlTool",
    "register_ipcamera_tools"
]
