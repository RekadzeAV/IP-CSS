"""
JWTManager - Управление JWT токенами для AI-агента

Обеспечивает:
- Генерация JWT токенов
- Валидация токенов
- Refresh token механизм
- Интеграция с Ktor backend
"""

import logging
import secrets
from datetime import datetime, timedelta
from typing import Optional, Dict, Any
from jose import JWTError, jwt
from pydantic import BaseModel

from config import settings

logger = logging.getLogger(__name__)


class TokenPayload(BaseModel):
    """Payload JWT токена"""
    user_id: str
    role: str
    exp: datetime
    iat: datetime
    session_id: Optional[str] = None


class JWTManager:
    """
    Менеджер JWT токенов
    
    Использует алгоритм HS256 для подписи
    """
    
    def __init__(
        self,
        secret_key: str,
        algorithm: str = "HS256",
        access_token_expire_minutes: int = 30,
        refresh_token_expire_minutes: int = 1440  # 24 часа
    ):
        """
        Инициализация JWTManager
        
        Args:
            secret_key: Секретный ключ для подписи
            algorithm: Алгоритм подписи (HS256, RS256)
            access_token_expire_minutes: Срок жизни access token
            refresh_token_expire_minutes: Срок жизни refresh token
        """
        self.secret_key = secret_key
        self.algorithm = algorithm
        self.access_token_expire_minutes = access_token_expire_minutes
        self.refresh_token_expire_minutes = refresh_token_expire_minutes
        
        logger.info("JWTManager initialized")
    
    def create_access_token(self, user_id: str, role: str = "user") -> str:
        """
        Создать access token
        
        Args:
            user_id: ID пользователя
            role: Роль пользователя
        
        Returns:
            JWT token
        """
        now = datetime.utcnow()
        expire = now + timedelta(minutes=self.access_token_expire_minutes)
        
        payload = {
            "user_id": user_id,
            "role": role,
            "exp": expire,
            "iat": now,
            "session_id": secrets.token_hex(16)
        }
        
        token = jwt.encode(payload, self.secret_key, algorithm=self.algorithm)
        logger.debug(f"Access token created for user {user_id}")
        
        return token
    
    def create_refresh_token(self, user_id: str, role: str = "user") -> str:
        """
        Создать refresh token
        
        Args:
            user_id: ID пользователя
            role: Роль пользователя
        
        Returns:
            JWT refresh token
        """
        now = datetime.utcnow()
        expire = now + timedelta(minutes=self.refresh_token_expire_minutes)
        
        payload = {
            "user_id": user_id,
            "role": role,
            "exp": expire,
            "iat": now,
            "type": "refresh",
            "session_id": secrets.token_hex(16)
        }
        
        token = jwt.encode(payload, self.secret_key, algorithm=self.algorithm)
        logger.debug(f"Refresh token created for user {user_id}")
        
        return token
    
    def verify_token(self, token: str) -> Optional[Dict[str, Any]]:
        """
        Проверить JWT токен
        
        Args:
            token: JWT token
        
        Returns:
            Payload если токен валиден, None если невалиден
        """
        try:
            payload = jwt.decode(token, self.secret_key, algorithms=[self.algorithm])
            
            # Проверка типа токена
            token_type = payload.get("type", "access")
            if token_type not in ["access", "refresh"]:
                logger.warning(f"Invalid token type: {token_type}")
                return None
            
            return payload
            
        except JWTError as e:
            logger.warning(f"Token verification failed: {e}")
            return None
        except Exception as e:
            logger.error(f"Token verification error: {e}")
            return None
    
    def verify_access_token(self, token: str) -> Optional[Dict[str, Any]]:
        """
        Проверить access token
        
        Args:
            token: Access token
        
        Returns:
            Payload если валиден
        """
        payload = self.verify_token(token)
        
        if payload and payload.get("type", "access") == "access":
            return payload
        
        return None
    
    def verify_refresh_token(self, token: str) -> Optional[Dict[str, Any]]:
        """
        Проверить refresh token
        
        Args:
            token: Refresh token
        
        Returns:
            Payload если валиден
        """
        payload = self.verify_token(token)
        
        if payload and payload.get("type") == "refresh":
            return payload
        
        return None
    
    def get_user_from_token(self, token: str) -> Optional[str]:
        """
        Получить user_id из токена
        
        Args:
            token: JWT token
        
        Returns:
            user_id или None
        """
        payload = self.verify_access_token(token)
        if payload:
            return payload.get("user_id")
        return None
    
    def get_role_from_token(self, token: str) -> Optional[str]:
        """
        Получить роль из токена
        
        Args:
            token: JWT token
        
        Returns:
            role или None
        """
        payload = self.verify_access_token(token)
        if payload:
            return payload.get("role")
        return None


# ============================================
# Authentication Middleware
# ============================================

from fastapi import Request, HTTPException
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware


class JWTAuthMiddleware(BaseHTTPMiddleware):
    """Middleware для проверки JWT токенов"""
    
    def __init__(self, app, jwt_manager: JWTManager, exempt_paths: list = None):
        super().__init__(app)
        self.jwt_manager = jwt_manager
        self.exempt_paths = exempt_paths or [
            "/api/v1/health",
            "/api/v1/version",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/docs",
            "/redoc"
        ]
    
    async def dispatch(self, request: Request, call_next):
        """Обработка запроса"""
        
        # Пропуск exempt путей
        if any(request.url.path.startswith(path) for path in self.exempt_paths):
            return await call_next(request)
        
        # Получение токена из заголовка
        auth_header = request.headers.get("Authorization")
        
        if not auth_header:
            return JSONResponse(
                status_code=401,
                content={"detail": "Authorization header missing"}
            )
        
        # Проверка формата Bearer token
        if not auth_header.startswith("Bearer "):
            return JSONResponse(
                status_code=401,
                content={"detail": "Invalid authorization format"}
            )
        
        token = auth_header.split(" ")[1]
        
        # Проверка токена
        payload = self.jwt_manager.verify_access_token(token)
        
        if not payload:
            return JSONResponse(
                status_code=401,
                content={"detail": "Invalid or expired token"}
            )
        
        # Добавление user_id в запрос
        request.state.user_id = payload.get("user_id")
        request.state.role = payload.get("role")
        
        return await call_next(request)


# ============================================
# Login Credentials Model
# ============================================

class LoginCredentials(BaseModel):
    """Учётные данные для входа"""
    username: str
    password: str


class TokenResponse(BaseModel):
    """Ответ с токенами"""
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int


# ============================================
# Global JWT Manager
# ============================================

_jwt_manager: Optional[JWTManager] = None


def get_jwt_manager() -> JWTManager:
    """Получить глобальный JWTManager"""
    if _jwt_manager is None:
        raise RuntimeError("JWTManager not initialized")
    return _jwt_manager


def set_jwt_manager(manager: JWTManager) -> None:
    """Установить глобальный JWTManager"""
    global _jwt_manager
    _jwt_manager = manager
