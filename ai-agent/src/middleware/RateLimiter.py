"""
RateLimiter - Ограничение частоты запросов для AI-агента

Обеспечивает:
- Rate limiting на основе пользователя
- Гибкие лимиты для разных endpoint
- Интеграция с Redis для распределённого rate limiting
"""

import logging
from typing import Callable, List, Optional
from fastapi import Request, HTTPException
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

from config import settings

logger = logging.getLogger(__name__)


class RateLimiterConfig:
    """Конфигурация rate limiting"""
    
    # Лимиты для разных типов запросов
    DEFAULT_LIMIT = "100/minute"  # 100 запросов в минуту
    CHAT_LIMIT = "60/minute"  # 60 чат-запросов в минуту
    MEMORY_LIMIT = "30/minute"  # 30 запросов к памяти в минуту
    AUTH_LIMIT = "10/minute"  # 10 запросов аутентификации в минуту
    TOOLS_LIMIT = "30/minute"  # 30 запросов к инструментам в минуту
    WEBSOCKET_LIMIT = "1000/minute"  # 1000 сообщений WebSocket в минуту
    
    # Лимиты для разных ролей
    ANON_LIMIT = "30/minute"  # Для неавторизованных
    USER_LIMIT = "100/minute"  # Для обычных пользователей
    ADMIN_LIMIT = "500/minute"  # Для администраторов


class CustomLimiter(Limiter):
    """Кастомный Limiter с улучшенной логикой"""
    
    def __init__(self, app, key_func: Callable):
        super().__init__(app=app, key_func=key_func)
        
        # Регистрация обработчика ошибок rate limit
        app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)
        
        logger.info("CustomLimiter initialized")
    
    def get_user_key(self, request: Request) -> str:
        """
        Получить ключ для rate limiting
        
        Приоритет:
        1. user_id из state (если авторизован)
        2. IP адрес
        """
        # Попытка получить user_id из middleware
        user_id = getattr(request.state, "user_id", None)
        
        if user_id:
            return f"user:{user_id}"
        
        # fallback на IP адрес
        return f"ip:{get_remote_address(request)}"


# ============================================
# Global Rate Limiter
# ============================================

_rate_limiter: Optional[CustomLimiter] = None


def get_rate_limiter() -> CustomLimiter:
    """Получить глобальный RateLimiter"""
    if _rate_limiter is None:
        raise RuntimeError("RateLimiter not initialized")
    return _rate_limiter


def set_rate_limiter(limiter: CustomLimiter) -> None:
    """Установить глобальный RateLimiter"""
    global _rate_limiter
    _rate_limiter = limiter


# ============================================
# Middleware для логирования запросов
# ============================================

from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
import time


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    """Middleware для логирования запросов"""
    
    async def dispatch(self, request: Request, call_next):
        """Обработка запроса"""
        start_time = time.time()
        
        # Логирование входящего запроса
        logger.debug(f"Request: {request.method} {request.url.path}")
        
        try:
            response = await call_next(request)
            
            # Логирование ответа
            process_time = time.time() - start_time
            logger.debug(
                f"Response: {response.status_code} "
                f"in {process_time:.3f}s"
            )
            
            # Добавление заголовка процессинга
            response.headers["X-Process-Time"] = str(process_time)
            
            return response
            
        except Exception as e:
            process_time = time.time() - start_time
            logger.error(
                f"Error: {str(e)} "
                f"in {process_time:.3f}s"
            )
            raise


# ============================================
# Audit Logging
# ============================================

class AuditLogger:
    """Логгер аудита для важных действий"""
    
    def __init__(self):
        self.logger = logging.getLogger("audit")
        
        # Настройка отдельного файла для аудита
        handler = logging.FileHandler("logs/audit.log")
        handler.setFormatter(logging.Formatter(
            '%(asctime)s - %(levelname)s - %(message)s'
        ))
        self.logger.addHandler(handler)
        self.logger.setLevel(logging.INFO)
    
    def log_action(
        self,
        user_id: str,
        action: str,
        resource: str,
        details: dict = None
    ):
        """
        Записать действие в аудит
        
        Args:
            user_id: ID пользователя
            action: Действие (create, read, update, delete)
            resource: Ресурс (camera, event, recording, etc.)
            details: Дополнительные детали
        """
        message = f"AUDIT: user={user_id} action={action} resource={resource}"
        
        if details:
            details_str = ", ".join(f"{k}={v}" for k, v in details.items())
            message += f" {details_str}"
        
        self.logger.info(message)
    
    def log_login(self, user_id: str, success: bool, ip: str):
        """Логирование входа"""
        status = "success" if success else "failed"
        self.logger.info(f"AUDIT: login user={user_id} status={status} ip={ip}")
    
    def log_tool_execution(self, user_id: str, tool_name: str, params: dict):
        """Логирование выполнения инструмента"""
        self.logger.info(
            f"AUDIT: tool_execution user={user_id} tool={tool_name} "
            f"params={params}"
        )
    
    def log_memory_access(self, user_id: str, operation: str, key: str):
        """Логирование доступа к памяти"""
        self.logger.info(
            f"AUDIT: memory_access user={user_id} operation={operation} key={key}"
        )


# ============================================
# Utilities
# ============================================

def get_client_ip(request: Request) -> str:
    """Получить IP клиента с учётом прокси"""
    # Проверка X-Forwarded-For заголовка
    forwarded_for = request.headers.get("X-Forwarded-For")
    if forwarded_for:
        return forwarded_for.split(",")[0].strip()
    
    # Fallback на X-Real-IP
    real_ip = request.headers.get("X-Real-IP")
    if real_ip:
        return real_ip
    
    # Fallback на remote address
    return get_remote_address(request)
