"""
AgentContext - Контекст запроса AI-агента

Хранит всю информацию, необходимую для обработки запроса:
- ID пользователя и диалога
- Время запроса
- Сообщение пользователя
- Контекст из памяти
- Дополнительный контекст
"""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, Any, List, Optional


@dataclass
class AgentContext:
    """
    Контекст запроса AI-агента
    
    Attributes:
        user_id: Уникальный ID пользователя
        conversation_id: Уникальный ID диалога
        timestamp: Время создания запроса
        user_message: Исходное сообщение пользователя
        memory_context: Информация из долговременной памяти
        extra_context: Дополнительный контекст (например, текущие камеры, события)
    """
    
    user_id: str
    conversation_id: str
    timestamp: datetime
    user_message: str
    memory_context: List[Dict[str, Any]] = field(default_factory=list)
    extra_context: Dict[str, Any] = field(default_factory=dict)
    
    @property
    def message_length(self) -> int:
        """Длина сообщения пользователя в символах"""
        return len(self.user_message)
    
    @property
    def memory_size(self) -> int:
        """Количество элементов в памяти"""
        return len(self.memory_context)
    
    def to_dict(self) -> Dict[str, Any]:
        """
        Преобразование контекста в словарь
        
        Returns:
            Словарь с данными контекста
        """
        return {
            "user_id": self.user_id,
            "conversation_id": self.conversation_id,
            "timestamp": self.timestamp.isoformat(),
            "user_message": self.user_message,
            "memory_context": self.memory_context,
            "extra_context": self.extra_context,
            "message_length": self.message_length,
            "memory_size": self.memory_size
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "AgentContext":
        """
        Создание контекста из словаря
        
        Args:
            data: Словарь с данными
        
        Returns:
            AgentContext instance
        """
        return cls(
            user_id=data["user_id"],
            conversation_id=data["conversation_id"],
            timestamp=datetime.fromisoformat(data["timestamp"]),
            user_message=data["user_message"],
            memory_context=data.get("memory_context", []),
            extra_context=data.get("extra_context", {})
        )
    
    def __str__(self) -> str:
        """Строковое представление контекста"""
        return (
            f"AgentContext(user_id={self.user_id}, "
            f"conversation_id={self.conversation_id}, "
            f"message_length={self.message_length}, "
            f"memory_size={self.memory_size})"
        )
