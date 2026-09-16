"""
IPCameraTools — Инструменты для работы с IP-камерами

Набор инструментов для:
- Список камер
- Получение снимков
- Поиск событий
- Управление записями
- PTZ управление
"""

import logging
from typing import Dict, Any, List
from agent.AgentContext import AgentContext
from tools.BaseTool import BaseTool
from services import KtorClient, get_ktor_client

logger = logging.getLogger(__name__)


# ============================================
# List Cameras Tool
# ============================================

class ListCamerasTool(BaseTool):
    """Инструмент для получения списка камер"""
    
    @property
    def name(self) -> str:
        return "list_cameras"
    
    @property
    def description(self) -> str:
        return "Get list of all cameras with their status and information"
    
    @property
    def permissions(self) -> List[str]:
        return ["READ_CAMERAS"]
    
    @property
    def parameters(self) -> Dict[str, Any]:
        return {
            "status_filter": {
                "type": "string",
                "description": "Filter by status (online, offline, all)",
                "required": False,
                "enum": ["online", "offline", "all"]
            }
        }
    
    async def execute(
        self,
        parameters: Dict[str, Any],
        context: AgentContext
    ) -> Dict[str, Any]:
        """Получить список камер"""
        logger.info(f"List cameras request from user {context.user_id}")
        
        # TODO: Интеграция с реальным API
        # Пока заглушка с демо-данными
        
        demo_cameras = [
            {
                "id": "camera_001",
                "name": "Front Entrance",
                "ip": "192.168.10.100",
                "status": "online",
                "type": "IP Camera",
                "resolution": "1920x1080"
            },
            {
                "id": "camera_002",
                "name": "Backyard",
                "ip": "192.168.10.101",
                "status": "online",
                "type": "IP Camera",
                "resolution": "1920x1080"
            },
            {
                "id": "camera_003",
                "name": "Garage",
                "ip": "192.168.10.102",
                "status": "offline",
                "type": "IP Camera",
                "resolution": "1280x720"
            }
        ]
        
        # Фильтрация по статусу
        status_filter = parameters.get("status_filter", "all")
        if status_filter != "all":
            demo_cameras = [c for c in demo_cameras if c["status"] == status_filter]
        
        logger.info(f"Returning {len(demo_cameras)} cameras")
        
        return {
            "success": True,
            "cameras": demo_cameras,
            "count": len(demo_cameras),
            "filter": status_filter
        }


# ============================================
# Get Camera Snapshot Tool
# ============================================

class GetCameraSnapshotTool(BaseTool):
    """Инструмент для получения снимка с камеры"""
    
    @property
    def name(self) -> str:
        return "get_camera_snapshot"
    
    @property
    def description(self) -> str:
        return "Get a snapshot image from a specific camera"
    
    @property
    def permissions(self) -> List[str]:
        return ["READ_CAMERAS"]
    
    @property
    def parameters(self) -> Dict[str, Any]:
        return {
            "camera_id": {
                "type": "string",
                "description": "ID of the camera",
                "required": True
            },
            "quality": {
                "type": "string",
                "description": "Snapshot quality",
                "required": False,
                "enum": ["low", "medium", "high"],
                "default": "high"
            }
        }
    
    async def execute(
        self,
        parameters: Dict[str, Any],
        context: AgentContext
    ) -> Dict[str, Any]:
        """Получить снимок с камеры"""
        camera_id = parameters.get("camera_id")
        quality = parameters.get("quality", "high")
        
        logger.info(f"Get snapshot request for camera {camera_id} from user {context.user_id}")
        
        if not camera_id:
            return {
                "success": False,
                "error": "camera_id is required"
            }
        
        # TODO: Интеграция с реальным API
        # Пока заглушка
        
        return {
            "success": True,
            "camera_id": camera_id,
            "snapshot_url": f"http://localhost:8080/api/v1/screenshots/{camera_id}",
            "quality": quality,
            "timestamp": "2026-01-28T23:00:00Z"
        }


# ============================================
# Search Events Tool
# ============================================

class SearchEventsTool(BaseTool):
    """Инструмент для поиска событий"""
    
    @property
    def name(self) -> str:
        return "search_events"
    
    @property
    def description(self) -> str:
        return "Search for events (motion detection, alerts) with filters"
    
    @property
    def permissions(self) -> List[str]:
        return ["READ_EVENTS"]
    
    @property
    def parameters(self) -> Dict[str, Any]:
        return {
            "event_type": {
                "type": "string",
                "description": "Type of event",
                "required": False,
                "enum": ["motion_detected", "camera_offline", "recording_started", "all"]
            },
            "camera_id": {
                "type": "string",
                "description": "Filter by camera ID",
                "required": False
            },
            "limit": {
                "type": "integer",
                "description": "Maximum number of events",
                "required": False,
                "default": 20
            },
            "since": {
                "type": "string",
                "description": "Since timestamp (ISO format)",
                "required": False
            }
        }
    
    async def execute(
        self,
        parameters: Dict[str, Any],
        context: AgentContext
    ) -> Dict[str, Any]:
        """Поиск событий"""
        logger.info(f"Search events request from user {context.user_id}")
        
        # Демо-события
        demo_events = [
            {
                "id": "event_001",
                "type": "motion_detected",
                "camera_id": "camera_001",
                "camera_name": "Front Entrance",
                "timestamp": "2026-01-28T22:45:00Z",
                "confidence": 0.95,
                "thumbnail_url": "http://localhost:8080/api/v1/screenshots/event_001"
            },
            {
                "id": "event_002",
                "type": "motion_detected",
                "camera_id": "camera_002",
                "camera_name": "Backyard",
                "timestamp": "2026-01-28T22:30:00Z",
                "confidence": 0.87,
                "thumbnail_url": "http://localhost:8080/api/v1/screenshots/event_002"
            },
            {
                "id": "event_003",
                "type": "camera_offline",
                "camera_id": "camera_003",
                "camera_name": "Garage",
                "timestamp": "2026-01-28T22:00:00Z"
            }
        ]
        
        # Фильтрация
        event_type = parameters.get("event_type", "all")
        if event_type != "all":
            demo_events = [e for e in demo_events if e["type"] == event_type]
        
        camera_id = parameters.get("camera_id")
        if camera_id:
            demo_events = [e for e in demo_events if e["camera_id"] == camera_id]
        
        limit = parameters.get("limit", 20)
        demo_events = demo_events[:limit]
        
        logger.info(f"Returning {len(demo_events)} events")
        
        return {
            "success": True,
            "events": demo_events,
            "count": len(demo_events),
            "filters": {
                "event_type": event_type,
                "camera_id": camera_id,
                "limit": limit
            }
        }


# ============================================
# Start Recording Tool
# ============================================

class StartRecordingTool(BaseTool):
    """Инструмент для запуска записи"""
    
    @property
    def name(self) -> str:
        return "start_recording"
    
    @property
    def description(self) -> str:
        return "Start recording for a specific camera (requires confirmation)"
    
    @property
    def permissions(self) -> List[str]:
        return ["CONTROL_CAMERAS"]
    
    @property
    def parameters(self) -> Dict[str, Any]:
        return {
            "camera_id": {
                "type": "string",
                "description": "ID of the camera",
                "required": True
            },
            "duration": {
                "type": "integer",
                "description": "Recording duration in minutes (0 for continuous)",
                "required": False,
                "default": 0
            },
            "confirm": {
                "type": "boolean",
                "description": "User confirmation for dangerous action",
                "required": True
            }
        }
    
    async def check_permissions(
        self,
        user_id: str,
        parameters: Dict[str, Any]
    ) -> bool:
        """Проверка подтверждения для опасных действий"""
        if not parameters.get("confirm"):
            logger.warning(f"Recording start not confirmed by user {user_id}")
            return False
        return True
    
    async def execute(
        self,
        parameters: Dict[str, Any],
        context: AgentContext
    ) -> Dict[str, Any]:
        """Запустить запись"""
        camera_id = parameters.get("camera_id")
        duration = parameters.get("duration", 0)
        
        logger.info(f"Start recording request for camera {camera_id} from user {context.user_id}")
        
        # TODO: Интеграция с реальным API
        
        return {
            "success": True,
            "message": f"Recording started for camera {camera_id}",
            "camera_id": camera_id,
            "duration_minutes": duration,
            "recording_id": f"rec_{context.timestamp.timestamp()}",
            "status": "recording"
        }


# ============================================
# Stop Recording Tool
# ============================================

class StopRecordingTool(BaseTool):
    """Инструмент для остановки записи"""
    
    @property
    def name(self) -> str:
        return "stop_recording"
    
    @property
    def description(self) -> str:
        return "Stop recording for a specific camera (requires confirmation)"
    
    @property
    def permissions(self) -> List[str]:
        return ["CONTROL_CAMERAS"]
    
    @property
    def parameters(self) -> Dict[str, Any]:
        return {
            "camera_id": {
                "type": "string",
                "description": "ID of the camera",
                "required": True
            },
            "confirm": {
                "type": "boolean",
                "description": "User confirmation for dangerous action",
                "required": True
            }
        }
    
    async def check_permissions(
        self,
        user_id: str,
        parameters: Dict[str, Any]
    ) -> bool:
        """Проверка подтверждения"""
        if not parameters.get("confirm"):
            logger.warning(f"Recording stop not confirmed by user {user_id}")
            return False
        return True
    
    async def execute(
        self,
        parameters: Dict[str, Any],
        context: AgentContext
    ) -> Dict[str, Any]:
        """Остановить запись"""
        camera_id = parameters.get("camera_id")
        
        logger.info(f"Stop recording request for camera {camera_id} from user {context.user_id}")
        
        # TODO: Интеграция с реальным API
        
        return {
            "success": True,
            "message": f"Recording stopped for camera {camera_id}",
            "camera_id": camera_id,
            "status": "stopped"
        }


# ============================================
# PTZ Control Tool
# ============================================

class PTZControlTool(BaseTool):
    """Инструмент для PTZ управления камерой"""
    
    @property
    def name(self) -> str:
        return "ptz_control"
    
    @property
    def description(self) -> str:
        return "Control PTZ camera (pan, tilt, zoom)"
    
    @property
    def permissions(self) -> List[str]:
        return ["CONTROL_CAMERAS"]
    
    @property
    def parameters(self) -> Dict[str, Any]:
        return {
            "camera_id": {
                "type": "string",
                "description": "ID of the PTZ camera",
                "required": True
            },
            "action": {
                "type": "string",
                "description": "PTZ action",
                "required": True,
                "enum": ["pan_left", "pan_right", "tilt_up", "tilt_down", "zoom_in", "zoom_out", "reset"]
            },
            "speed": {
                "type": "integer",
                "description": "Movement speed (1-10)",
                "required": False,
                "default": 5
            }
        }
    
    async def execute(
        self,
        parameters: Dict[str, Any],
        context: AgentContext
    ) -> Dict[str, Any]:
        """PTZ управление"""
        camera_id = parameters.get("camera_id")
        action = parameters.get("action")
        speed = parameters.get("speed", 5)
        
        logger.info(f"PTZ control request: {action} for camera {camera_id}")
        
        # TODO: Интеграция с реальным API
        
        return {
            "success": True,
            "message": f"PTZ action '{action}' executed on camera {camera_id}",
            "camera_id": camera_id,
            "action": action,
            "speed": speed
        }


# ============================================
# Register Tools
# ============================================

def register_ipcamera_tools():
    """Регистрация всех IPCameraTools"""
    from tools import register_tool
    
    register_tool(ListCamerasTool())
    register_tool(GetCameraSnapshotTool())
    register_tool(SearchEventsTool())
    register_tool(StartRecordingTool())
    register_tool(StopRecordingTool())
    register_tool(PTZControlTool())
    
    logger.info("IPCameraTools registered: 6 tools")
