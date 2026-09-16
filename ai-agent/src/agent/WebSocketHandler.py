"""
WebSocketHandler - Обработчик WebSocket соединений для AI-агента

Обеспечивает:
- Bi-directional communication
- Streaming ответов от LLM
- Управление сессиями
- Обработка disconnect/reconnect
"""

import logging
import json
from typing import Dict, Any, Optional, AsyncGenerator
from datetime import datetime
from fastapi import WebSocket, WebSocketDisconnect

from agent.AgentLoop import AgentLoop

logger = logging.getLogger(__name__)


class WebSocketSession:
    """Сессия WebSocket подключения"""
    
    def __init__(self, websocket: WebSocket, user_id: str):
        self.websocket = websocket
        self.user_id = user_id
        self.conversation_id: Optional[str] = None
        self.connected_at = datetime.now()
        self.last_activity = datetime.now()
        self.message_count = 0
    
    async def send(self, data: Dict[str, Any]) -> None:
        """Отправить данные клиенту"""
        try:
            await self.websocket.send_json(data)
            self.last_activity = datetime.now()
        except Exception as e:
            logger.error(f"Failed to send data: {e}")
            raise
    
    async def receive(self) -> Dict[str, Any]:
        """Получить данные от клиента"""
        try:
            data = await self.websocket.receive_json()
            self.last_activity = datetime.now()
            self.message_count += 1
            return data
        except Exception as e:
            logger.error(f"Failed to receive data: {e}")
            raise


class WebSocketHandler:
    """
    Обработчик WebSocket подключений
    
    Управляет множеством одновременных сессий
    """
    
    def __init__(self, agent_loop: AgentLoop):
        """
        Инициализация WebSocketHandler
        
        Args:
            agent_loop: Экземпляр AgentLoop для обработки запросов
        """
        self.agent_loop = agent_loop
        self.sessions: Dict[str, WebSocketSession] = {}
        logger.info("WebSocketHandler initialized")
    
    async def connect(
        self,
        websocket: WebSocket,
        user_id: str,
        conversation_id: Optional[str] = None
    ) -> WebSocketSession:
        """
        Принятие WebSocket подключения
        
        Args:
            websocket: WebSocket соединение
            user_id: ID пользователя
            conversation_id: ID диалога (опционально)
        
        Returns:
            WebSocketSession
        """
        await websocket.accept()
        
        session_id = f"{user_id}:{datetime.now().timestamp()}"
        session = WebSocketSession(websocket, user_id)
        session.conversation_id = conversation_id
        
        self.sessions[session_id] = session
        
        logger.info(f"WebSocket connected: user={user_id}, session={session_id}")
        
        # Отправить приветственное сообщение
        await session.send({
            "type": "connected",
            "session_id": session_id,
            "user_id": user_id,
            "conversation_id": conversation_id,
            "timestamp": datetime.now().isoformat()
        })
        
        return session
    
    async def disconnect(self, session_id: str) -> None:
        """
        Закрытие WebSocket подключения
        
        Args:
            session_id: ID сессии
        """
        if session_id in self.sessions:
            session = self.sessions[session_id]
            await session.websocket.close()
            del self.sessions[session_id]
            logger.info(f"WebSocket disconnected: session={session_id}")
    
    async def handle_message(
        self,
        session: WebSocketSession,
        message: Dict[str, Any]
    ) -> None:
        """
        Обработка сообщения от клиента
        
        Args:
            session: WebSocket сессия
            message: Сообщение от клиента
        """
        try:
            message_type = message.get("type", "chat")
            
            if message_type == "chat":
                await self._handle_chat_message(session, message)
            elif message_type == "ping":
                await session.send({
                    "type": "pong",
                    "timestamp": datetime.now().isoformat()
                })
            elif message_type == "get_history":
                await self._handle_get_history(session, message)
            else:
                await session.send({
                    "type": "error",
                    "error": f"Unknown message type: {message_type}"
                })
        
        except Exception as e:
            logger.error(f"Error handling message: {e}", exc_info=True)
            await session.send({
                "type": "error",
                "error": str(e)
            })
    
    async def _handle_chat_message(
        self,
        session: WebSocketSession,
        message: Dict[str, Any]
    ) -> None:
        """
        Обработка чат-сообщения
        
        Args:
            session: WebSocket сессия
            message: Сообщение с текстом
        """
        user_message = message.get("message", "")
        
        if not user_message:
            await session.send({
                "type": "error",
                "error": "Message is required"
            })
            return
        
        # Отправить статус "processing"
        await session.send({
            "type": "status",
            "status": "processing",
            "message": "Обработка запроса..."
        })
        
        # Обработать через AgentLoop
        try:
            result = await self.agent_loop.process_request(
                message=user_message,
                user_id=session.user_id,
                conversation_id=session.conversation_id
            )
            
            # Обновить conversation_id из результата
            if result.get("conversation_id"):
                session.conversation_id = result["conversation_id"]
            
            # Отправить ответ
            await session.send({
                "type": "response",
                "response": result["response"],
                "conversation_id": result["conversation_id"],
                "tools_used": result.get("tools_used", []),
                "memory_used": result.get("memory_used", False),
                "timestamp": datetime.now().isoformat()
            })
        
        except Exception as e:
            logger.error(f"Chat processing error: {e}", exc_info=True)
            await session.send({
                "type": "error",
                "error": f"Failed to process message: {str(e)}"
            })
    
    async def _handle_get_history(
        self,
        session: WebSocketSession,
        message: Dict[str, Any]
    ) -> None:
        """
        Получение истории сообщений
        
        Args:
            session: WebSocket сессия
            message: Запрос истории
        """
        # TODO: Реализовать хранение истории
        await session.send({
            "type": "history",
            "messages": [],
            "message": "История сообщений пока не доступна"
        })
    
    async def stream_response(
        self,
        session: WebSocketSession,
        text: str
    ) -> AsyncGenerator[str, None]:
        """
        Streaming ответа по токенам
        
        Args:
            session: WebSocket сессия
            text: Полный текст ответа
        
        Yields:
            Токены текста
        """
        words = text.split()
        current_text = ""
        
        for word in words:
            current_text += word + " "
            
            await session.send({
                "type": "stream",
                "token": word,
                "accumulated": current_text,
                "timestamp": datetime.now().isoformat()
            })
            
            yield word
    
    def get_active_sessions(self) -> int:
        """Получить количество активных сессий"""
        return len(self.sessions)
    
    def get_session(self, session_id: str) -> Optional[WebSocketSession]:
        """Получить сессию по ID"""
        return self.sessions.get(session_id)
    
    async def cleanup_old_sessions(self, max_age_minutes: int = 60) -> int:
        """
        Очистка старых сессий
        
        Args:
            max_age_minutes: Максимальный возраст сессии в минутах
        
        Returns:
            Количество удалённых сессий
        """
        from datetime import datetime, timedelta
        
        cutoff = datetime.now() - timedelta(minutes=max_age_minutes)
        to_remove = []
        
        for session_id, session in self.sessions.items():
            if session.last_activity < cutoff:
                to_remove.append(session_id)
        
        for session_id in to_remove:
            await self.disconnect(session_id)
        
        if to_remove:
            logger.info(f"Cleaned up {len(to_remove)} old sessions")
        
        return len(to_remove)


# ============================================
# Global WebSocket Handler
# ============================================

_websocket_handler: Optional[WebSocketHandler] = None


def get_websocket_handler() -> WebSocketHandler:
    """Получить глобальный WebSocketHandler"""
    if _websocket_handler is None:
        raise RuntimeError("WebSocketHandler not initialized")
    return _websocket_handler


def set_websocket_handler(handler: WebSocketHandler) -> None:
    """Установить глобальный WebSocketHandler"""
    global _websocket_handler
    _websocket_handler = handler
