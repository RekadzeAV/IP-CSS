"""
IP-CSS AI Agent - FastAPI Server
Основной сервер AI-агента
"""

import logging
from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import Optional, List

from config import settings
from agent import AgentLoop, AgentContext, ContextCompressor, WebSocketHandler, set_websocket_handler
from tools import ToolRegistry
from memory import MemoryManager, ChromaDBClient
from security import JWTManager, JWTAuthMiddleware, LoginCredentials, TokenResponse, set_jwt_manager
from middleware import CustomLimiter, RateLimiterConfig, RequestLoggingMiddleware, AuditLogger, set_rate_limiter
from services import KtorClient, set_ktor_client

# Настройка логирования
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(settings.LOG_FILE),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Создание FastAPI приложения
app = FastAPI(
    title=settings.AGENT_NAME,
    description="AI Agent for IP-CSS Video Surveillance System",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # В production заменить на конкретные домены
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Глобальные компоненты агента
agent_loop: Optional[AgentLoop] = None
tool_registry: Optional[ToolRegistry] = None
memory_manager: Optional[MemoryManager] = None
websocket_handler: Optional[WebSocketHandler] = None


# ============================================
# Models
# ============================================

class HealthResponse(BaseModel):
    status: str
    version: str
    services: dict


class ChatRequest(BaseModel):
    message: str
    conversation_id: Optional[str] = None
    user_id: Optional[str] = None


class ChatResponse(BaseModel):
    response: str
    conversation_id: str
    tools_used: List[str] = []
    memory_used: bool = False


# ============================================
# Startup/Shutdown
# ============================================

@app.on_event("startup")
async def startup_event():
    """Запуск при старте сервера"""
    logger.info(f"Starting {settings.AGENT_NAME} v1.0.0")
    logger.info(f"LLM Provider: {settings.LLM_PROVIDER_PRIMARY}")
    logger.info(f"LLM Model: {settings.LLM_PRIMARY_MODEL}")
    
    # Инициализация компонентов
    try:
        # 1. Tool Registry
        global tool_registry
        tool_registry = ToolRegistry()
        logger.info("ToolRegistry initialized")
        
        # 2. Register IPCameraTools
        from tools import register_ipcamera_tools
        register_ipcamera_tools()
        logger.info(f"Tools registered: {len(tool_registry)}")
        
        # 3. ChromaDB Client
        chroma_client = ChromaDBClient(
            host=settings.CHROMA_HOST,
            port=settings.CHROMA_PORT,
            password=settings.CHROMA_PASSWORD.get_secret_value(),
            collection=settings.CHROMA_COLLECTION
        )
        await chroma_client.initialize()
        logger.info("ChromaDB client initialized")
        
        # 4. Memory Manager
        global memory_manager
        memory_manager = MemoryManager(chroma_client)
        logger.info("MemoryManager initialized")
        
        # 5. Context Compressor
        context_compressor = ContextCompressor()
        logger.info("ContextCompressor initialized")
        
        # 6. Agent Loop
        global agent_loop
        agent_loop = AgentLoop(
            tool_registry=tool_registry,
            memory_manager=memory_manager,
            context_compressor=context_compressor
        )
        logger.info("AgentLoop initialized")
        
        # 7. WebSocket Handler
        global websocket_handler
        websocket_handler = WebSocketHandler(agent_loop)
        set_websocket_handler(websocket_handler)
        logger.info("WebSocketHandler initialized")
        
        # 8. JWT Manager
        jwt_manager = JWTManager(
            secret_key=settings.JWT_SECRET.get_secret_value(),
            algorithm="HS256",
            access_token_expire_minutes=30,
            refresh_token_expire_minutes=1440
        )
        set_jwt_manager(jwt_manager)
        
        # 9. JWT Auth Middleware
        app.add_middleware(
            JWTAuthMiddleware,
            jwt_manager=jwt_manager,
            exempt_paths=[
                "/api/v1/health",
                "/api/v1/version",
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/docs",
                "/redoc"
            ]
        )
        logger.info("JWT Manager and Middleware initialized")
        
        # 10. Rate Limiter
        def get_user_key(request: Request):
            return get_rate_limiter().get_user_key(request)
        
        rate_limiter = CustomLimiter(app=app, key_func=get_user_key)
        set_rate_limiter(rate_limiter)
        logger.info("RateLimiter initialized")
        
        # 11. Request Logging Middleware
        app.add_middleware(RequestLoggingMiddleware)
        logger.info("RequestLoggingMiddleware initialized")
        
        # 12. Audit Logger
        audit_logger = AuditLogger()
        logger.info("AuditLogger initialized")
        
        # 13. Ktor Client
        ktor_client = KtorClient(
            base_url=settings.KTOR_BACKEND_URL,
            timeout=settings.KTOR_API_TIMEOUT,
            max_retries=settings.KTOR_MAX_RETRIES,
            enable_cache=settings.KTOR_ENABLE_CACHE
        )
        set_ktor_client(ktor_client)
        logger.info(f"KtorClient initialized: {settings.KTOR_BACKEND_URL}")
        
        logger.info("All components initialized successfully")
        
    except Exception as e:
        logger.error(f"Failed to initialize components: {e}", exc_info=True)
        raise
    
    logger.info("AI Agent server started successfully")


@app.on_event("shutdown")
async def shutdown_event():
    """Закрытие при остановке сервера"""
    logger.info("Shutting down AI Agent server")
    
    # Очистка ресурсов
    if memory_manager:
        await memory_manager.close()
    
    # Закрытие KtorClient
    try:
        from services import get_ktor_client
        ktor = get_ktor_client()
        await ktor.close()
        logger.info("KtorClient closed")
    except:
        pass
    
    logger.info("AI Agent server shut down")


# ============================================
# Health Checks
# ============================================

@app.get("/")
async def root():
    """Корневой endpoint"""
    return {
        "name": settings.AGENT_NAME,
        "version": "1.0.0",
        "docs": "/docs"
    }


@app.get("/api/v1/health")
async def health_check():
    """Детальная проверка здоровья"""
    # TODO: Добавить проверки сервисов
    services = {
        "ollama": "unknown",
        "lm_studio": "unknown",
        "chromadb": "unknown",
        "redis": "unknown"
    }
    
    return HealthResponse(
        status="healthy",
        version="1.0.0",
        services=services
    )


@app.get("/api/v1/version")
async def version():
    """Версия API"""
    return {
        "version": "1.0.0",
        "api_version": "v1",
        "build_date": "2026-01-27"
    }


# ============================================
# Chat and Tools Endpoints
# ============================================

@app.post("/api/v1/agent/chat", response_model=ChatResponse)
@get_rate_limiter().limit(RateLimiterConfig.CHAT_LIMIT)
async def chat(request: ChatRequest):
    """
    Обработка чат-запроса к AI-агенту
    
    Использует AgentLoop для оркестрации запроса:
    1. Сборка контекста
    2. Вызов LLM
    3. Парсинг ответа
    4. Выполнение инструментов (если нужно)
    5. Сохранение в память
    6. Возврат ответа
    """
    if not agent_loop:
        raise HTTPException(status_code=503, detail="Agent not initialized")
    
    logger.info(f"Received chat request from user {request.user_id or 'anonymous'}")
    
    try:
        # Обработка запроса через AgentLoop
        result = await agent_loop.process_request(
            message=request.message,
            user_id=request.user_id or "anonymous",
            conversation_id=request.conversation_id
        )
        
        return ChatResponse(
            response=result["response"],
            conversation_id=result["conversation_id"],
            tools_used=result.get("tools_used", []),
            memory_used=result.get("memory_used", False)
        )
        
    except Exception as e:
        logger.error(f"Chat request failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
    

@app.get("/api/v1/agent/tools")
async def list_tools():
    """
    Список доступных инструментов
    
    Возвращает информацию о зарегистрированных инструментах
    """
    if not tool_registry:
        return {
            "tools": [],
            "message": "Tool registry not initialized"
        }
    
    tools = tool_registry.get_tool_list()
    
    return {
        "tools": tools,
        "count": len(tools)
    }


@app.get("/api/v1/agent/memory")
async def get_memory(user_id: str):
    """
    Получение памяти пользователя
    
    Args:
        user_id: ID пользователя
    
    Returns:
        Список фактов памяти
    """
    if not memory_manager:
        raise HTTPException(status_code=503, detail="Memory manager not initialized")
    
    try:
        memory = await memory_manager.get_user_memory(user_id)
        return {
            "user_id": user_id,
            "memory": memory,
            "count": len(memory)
        }
    except Exception as e:
        logger.error(f"Get memory failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/agent/memory/remember")
async def remember_fact(request: dict):
    """
    Запоминание факта
    
    Args:
        user_id: ID пользователя
        key: Ключ факта
        value: Значение факта
    
    Returns:
        ID запомненного факта
    """
    if not memory_manager:
        raise HTTPException(status_code=503, detail="Memory manager not initialized")
    
    user_id = request.get("user_id")
    key = request.get("key")
    value = request.get("value")
    
    if not user_id or not key or value is None:
        raise HTTPException(status_code=400, detail="user_id, key, and value are required")
    
    try:
        fact_id = await memory_manager.remember(user_id, key, value)
        return {
            "success": True,
            "fact_id": fact_id
        }
    except Exception as e:
        logger.error(f"Remember failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/agent/memory/forget")
async def forget_fact(request: dict):
    """
    Забывание факта
    
    Args:
        user_id: ID пользователя
        key: Ключ факта (опционально)
    
    Returns:
        Количество удалённых фактов
    """
    if not memory_manager:
        raise HTTPException(status_code=503, detail="Memory manager not initialized")
    
    user_id = request.get("user_id")
    key = request.get("key")
    
    if not user_id:
        raise HTTPException(status_code=400, detail="user_id is required")
    
    try:
        count = await memory_manager.forget(user_id, key)
        return {
            "success": True,
            "deleted_count": count
        }
    except Exception as e:
        logger.error(f"Forget failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/agent/memory/clear")
async def clear_memory(user_id: str):
    """
    Очистка всей памяти пользователя
    
    Args:
        user_id: ID пользователя
    
    Returns:
        Количество удалённых фактов
    """
    if not memory_manager:
        raise HTTPException(status_code=503, detail="Memory manager not initialized")
    
    if not user_id:
        raise HTTPException(status_code=400, detail="user_id is required")
    
    try:
        count = await memory_manager.clear_user_memory(user_id)
        return {
            "success": True,
            "deleted_count": count
        }
    except Exception as e:
        logger.error(f"Clear memory failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================
# WebSocket Endpoints
# ============================================

@app.websocket("/api/v1/agent/chat/ws")
async def websocket_chat(
    websocket: WebSocket,
    user_id: str = Query(..., description="ID пользователя"),
    conversation_id: Optional[str] = Query(None, description="ID диалога")
):
    """
    WebSocket endpoint для чата с AI-агентом
    
    Обеспечивает bi-directional communication и streaming ответов.
    
    Сообщения от клиента:
    {
        "type": "chat",
        "message": "Текст сообщения"
    }
    
    Сообщения от сервера:
    {
        "type": "response" | "stream" | "status" | "error",
        "response": "Текст ответа",
        "tools_used": [...],
        "memory_used": true/false
    }
    """
    if not websocket_handler:
        await websocket.close(code=503, reason="WebSocket handler not initialized")
        return
    
    try:
        # Принять подключение
        session = await websocket_handler.connect(websocket, user_id, conversation_id)
        logger.info(f"WebSocket chat started: user={user_id}")
        
        # Обработка сообщений в цикле
        while True:
            try:
                # Получить сообщение от клиента
                message = await session.receive()
                
                # Обработать сообщение
                await websocket_handler.handle_message(session, message)
            
            except WebSocketDisconnect:
                logger.info(f"WebSocket disconnected: user={user_id}")
                break
            
            except Exception as e:
                logger.error(f"WebSocket message error: {e}", exc_info=True)
                await session.send({
                    "type": "error",
                    "error": f"Message processing error: {str(e)}"
                })
    
    except Exception as e:
        logger.error(f"WebSocket connection error: {e}", exc_info=True)
        try:
            await websocket.close(code=1011, reason=str(e))
        except:
            pass
    
    finally:
        # Закрытие сессии
        if 'session' in locals():
            await websocket_handler.disconnect(session.session_id if hasattr(session, 'session_id') else f"{user_id}:unknown")


@app.get("/api/v1/agent/ws/stats")
async def websocket_stats():
    """
    Статистика WebSocket подключений
    
    Returns:
        Статистика активных сессий
    """
    if not websocket_handler:
        return {
            "active_sessions": 0,
            "message": "WebSocket handler not initialized"
        }
    
    return {
        "active_sessions": websocket_handler.get_active_sessions(),
        "timestamp": "2026-01-28T00:00:00Z"
    }


# ============================================
# Authentication Endpoints
# ============================================

@app.post("/api/v1/auth/login", response_model=TokenResponse)
async def login(credentials: LoginCredentials):
    """
    Аутентификация пользователя
    
    Args:
        username: Имя пользователя
        password: Пароль
    
    Returns:
        Access и refresh токены
    """
    # TODO: Интеграция с Ktor backend для проверки учётных данных
    # Пока заглушка для демонстрации
    
    # Проверка учётных данных (демо)
    # Логин/пароль берутся из .env (ADMIN_USERNAME/ADMIN_PASSWORD), см. ai-agent/.env.example.
    if (
        credentials.username == settings.ADMIN_USERNAME
        and credentials.password == settings.ADMIN_PASSWORD.get_secret_value()
    ):
        jwt_manager = get_jwt_manager()
        
        access_token = jwt_manager.create_access_token(
            user_id=credentials.username,
            role="admin"
        )
        
        refresh_token = jwt_manager.create_refresh_token(
            user_id=credentials.username,
            role="admin"
        )
        
        logger.info(f"Login successful for user {credentials.username}")
        
        return TokenResponse(
            access_token=access_token,
            refresh_token=refresh_token,
            token_type="bearer",
            expires_in=30 * 60  # 30 минут
        )
    
    logger.warning(f"Login failed for user {credentials.username}")
    raise HTTPException(
        status_code=401,
        detail="Invalid username or password"
    )


@app.post("/api/v1/auth/refresh", response_model=TokenResponse)
async def refresh_token(request: dict):
    """
    Обновление access token
    
    Args:
        refresh_token: Refresh token
    
    Returns:
        Новые access и refresh токены
    """
    refresh_token = request.get("refresh_token")
    
    if not refresh_token:
        raise HTTPException(status_code=400, detail="refresh_token is required")
    
    jwt_manager = get_jwt_manager()
    
    # Проверка refresh token
    payload = jwt_manager.verify_refresh_token(refresh_token)
    
    if not payload:
        raise HTTPException(
            status_code=401,
            detail="Invalid or expired refresh token"
        )
    
    user_id = payload.get("user_id")
    role = payload.get("role")
    
    # Создание новых токенов
    new_access_token = jwt_manager.create_access_token(user_id, role)
    new_refresh_token = jwt_manager.create_refresh_token(user_id, role)
    
    logger.info(f"Token refreshed for user {user_id}")
    
    return TokenResponse(
        access_token=new_access_token,
        refresh_token=new_refresh_token,
        token_type="bearer",
        expires_in=30 * 60
    )


@app.get("/api/v1/auth/me")
async def get_current_user():
    """
    Получение информации о текущем пользователе
    
    Требуется валидный access token
    
    Returns:
        Информация о пользователе
    """
    # user_id добавляется middleware
    user_id = getattr(request.state, "user_id", None)
    role = getattr(request.state, "role", None)
    
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")
    
    return {
        "user_id": user_id,
        "role": role,
        "permissions": ["READ_CAMERAS", "USE_AI_AGENT"]
    }


# ============================================
# Error Handlers
# ============================================

@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    """Глобальный обработчик исключений"""
    logger.error(f"Global exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error", "message": str(exc)}
    )


# ============================================
# Main
# ============================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG
    )
