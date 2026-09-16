"""
BaseTool - Базовый класс для инструментов AI-агента

Все инструменты должны наследоваться от этого класса
"""

from abc import ABC, abstractmethod
from typing import Dict, Any, Optional, List
from dataclasses import dataclass, field
from agent.AgentContext import AgentContext


@dataclass
class ToolDefinition:
    """
    Определение инструмента
    
    Attributes:
        name: Название инструмента
        description: Описание функциональности
        parameters: Параметры инструмента
        permissions: Требуемые разрешения
    """
    name: str
    description: str
    parameters: Dict[str, Any] = field(default_factory=dict)
    permissions: List[str] = field(default_factory=list)
    
    def to_dict(self) -> Dict[str, Any]:
        """Преобразование в словарь"""
        return {
            "name": self.name,
            "description": self.description,
            "parameters": self.parameters,
            "permissions": self.permissions
        }


class BaseTool(ABC):
    """
    Базовый класс для инструментов AI-агента
    
    Все инструменты должны реализовать:
    - name: Название инструмента
    - description: Описание
    - execute: Метод выполнения
    """
    
    @property
    @abstractmethod
    def name(self) -> str:
        """Название инструмента"""
        pass
    
    @property
    @abstractmethod
    def description(self) -> str:
        """Описание инструмента"""
        pass
    
    @property
    def permissions(self) -> List[str]:
        """Требуемые разрешения"""
        return []
    
    @property
    def parameters(self) -> Dict[str, Any]:
        """Определение параметров инструмента"""
        return {}
    
    @abstractmethod
    async def execute(
        self,
        parameters: Dict[str, Any],
        context: AgentContext
    ) -> Dict[str, Any]:
        """
        Выполнение инструмента
        
        Args:
            parameters: Параметры инструмента
            context: Контекст запроса
        
        Returns:
            Результат выполнения
        """
        pass
    
    async def check_permissions(
        self,
        user_id: str,
        parameters: Dict[str, Any]
    ) -> bool:
        """
        Проверка прав пользователя
        
        Args:
            user_id: ID пользователя
            parameters: Параметры инструмента
        
        Returns:
            True если пользователь имеет права
        """
        # По умолчанию разрешаем всем
        # Подклассы могут переопределить для RBAC
        return True
    
    def get_definition(self) -> ToolDefinition:
        """
        Получение определения инструмента
        
        Returns:
            ToolDefinition с информацией об инструменте
        """
        return ToolDefinition(
            name=self.name,
            description=self.description,
            parameters=self.parameters,
            permissions=self.permissions
        )
    
    def __str__(self) -> str:
        """Строковое представление"""
        return f"Tool(name={self.name}, description={self.description[:50]}...)"
