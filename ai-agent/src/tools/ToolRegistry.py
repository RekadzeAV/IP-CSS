"""
ToolRegistry - Реестр инструментов AI-агента

Управляет регистрацией и поиском инструментов:
- Регистрация новых инструментов
- Поиск инструментов по названию
- Получение списка всех инструментов
"""

import logging
from typing import Dict, List, Optional, Type
from tools.BaseTool import BaseTool, ToolDefinition

logger = logging.getLogger(__name__)


class ToolRegistry:
    """
    Реестр инструментов AI-агента
    
    Singleton паттерн для глобального доступа к инструментам
    """
    
    _instance: Optional["ToolRegistry"] = None
    _tools: Dict[str, BaseTool] = {}
    
    def __new__(cls) -> "ToolRegistry":
        """Singleton паттерн"""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        """Инициализация реестра"""
        if hasattr(self, '_initialized'):
            return
        self._tools = {}
        self._initialized = True
        logger.info("ToolRegistry initialized")
    
    def register(self, tool: BaseTool) -> None:
        """
        Регистрация инструмента
        
        Args:
            tool: Экземпляр инструмента
        """
        self._tools[tool.name] = tool
        logger.info(f"Tool registered: {tool.name}")
    
    def unregister(self, tool_name: str) -> None:
        """
        Удаление инструмента
        
        Args:
            tool_name: Название инструмента
        """
        if tool_name in self._tools:
            del self._tools[tool_name]
            logger.info(f"Tool unregistered: {tool_name}")
    
    def get_tool(self, tool_name: str) -> Optional[BaseTool]:
        """
        Получение инструмента по названию
        
        Args:
            tool_name: Название инструмента
        
        Returns:
            Инструмент или None
        """
        return self._tools.get(tool_name)
    
    def get_tool_list(self) -> List[Dict[str, any]]:
        """
        Получение списка всех инструментов
        
        Returns:
            Список определений инструментов
        """
        return [tool.get_definition().to_dict() for tool in self._tools.values()]
    
    def get_all_tools(self) -> Dict[str, BaseTool]:
        """
        Получение всех зарегистрированных инструментов
        
        Returns:
            Словарь {name: tool}
        """
        return self._tools.copy()
    
    def has_tool(self, tool_name: str) -> bool:
        """
        Проверка наличия инструмента
        
        Args:
            tool_name: Название инструмента
        
        Returns:
            True если инструмент зарегистрирован
        """
        return tool_name in self._tools
    
    def __len__(self) -> int:
        """Количество зарегистрированных инструментов"""
        return len(self._tools)
    
    def __iter__(self):
        """Итерация по инструментам"""
        return iter(self._tools.values())


# Глобальные функции для удобства
def register_tool(tool: BaseTool) -> None:
    """
    Регистрация инструмента (глобальная функция)
    
    Args:
        tool: Экземпляр инструмента
    """
    registry = ToolRegistry()
    registry.register(tool)


def get_tool(tool_name: str) -> Optional[BaseTool]:
    """
    Получение инструмента (глобальная функция)
    
    Args:
        tool_name: Название инструмента
    
    Returns:
        Инструмент или None
    """
    registry = ToolRegistry()
    return registry.get_tool(tool_name)


# Декоратор для автоматической регистрации
def tool(name: str = None, description: str = None, permissions: List[str] = None):
    """
    Декоратор для регистрации инструмента
    
    Args:
        name: Название инструмента (по умолчанию класс.lower())
        description: Описание
        permissions: Список разрешений
    
    Example:
        @tool(permissions=["READ_CAMERAS"])
        class ListCamerasTool(BaseTool):
            async def execute(self, params, context):
                ...
    """
    def decorator(cls: Type[BaseTool]):
        instance = cls()
        if name:
            instance._name_override = name
        if description:
            instance._description_override = description
        if permissions:
            instance._permissions_override = permissions
        
        register_tool(instance)
        return cls
    return decorator
