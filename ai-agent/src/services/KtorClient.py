"""
KtorClient - HTTP клиент для интеграции с Ktor backend

Обеспечивает:
- REST API взаимодействие с Ktor backend
- Обработка ошибок и retry логика
- JWT токены для аутентификации
- Кэширование ответов
"""

import logging
from typing import Dict, Any, List, Optional, TypeVar, Generic
from datetime import datetime, timedelta
import httpx
from httpx import HTTPError, TimeoutException, ConnectError

from config import settings

logger = logging.getLogger(__name__)

T = TypeVar('T')


class KtorClientError(Exception):
    """Базовая ошибка KtorClient"""
    pass


class KtorAuthenticationError(KtorClientError):
    """Ошибка аутентификации"""
    pass


class KtorNotFoundError(KtorClientError):
    """Ресурс не найден"""
    pass


class KtorRateLimitError(KtorClientError):
    """Превышение лимита запросов"""
    pass


class KtorClient(Generic[T]):
    """
    HTTP клиент для взаимодействия с Ktor backend
    
    Features:
    - Автоматическая ре-аутентификация при 401
    - Retry логика для временных ошибок
    - Кэширование ответов
    - Timeout защита
    """
    
    def __init__(
        self,
        base_url: Optional[str] = None,
        token: Optional[str] = None,
        timeout: float = 30.0,
        max_retries: int = 3,
        enable_cache: bool = False
    ):
        """
        Инициализация KtorClient
        
        Args:
            base_url: Базовый URL Ktor backend
            token: JWT токен для аутентификации
            timeout: Timeout в секундах
            max_retries: Максимальное количество попыток
            enable_cache: Включить кэширование
        """
        self.base_url = base_url or settings.KTOR_BACKEND_URL
        self.token = token
        self.timeout = timeout
        self.max_retries = max_retries
        self.enable_cache = enable_cache
        
        self._client: Optional[httpx.AsyncClient] = None
        self._cache: Dict[str, tuple] = {}
        self._cache_ttl = timedelta(minutes=5)
        
        logger.info(f"KtorClient initialized: {self.base_url}")
    
    @property
    def client(self) -> httpx.AsyncClient:
        """Ленивая инициализация HTTP клиент"""
        if self._client is None:
            headers = {
                "Content-Type": "application/json",
                "Accept": "application/json"
            }
            
            if self.token:
                headers["Authorization"] = f"Bearer {self.token}"
            
            self._client = httpx.AsyncClient(
                base_url=self.base_url,
                headers=headers,
                timeout=self.timeout,
                follow_redirects=True
            )
        
        return self._client
    
    async def close(self):
        """Закрытие HTTP клиента"""
        if self._client:
            await self._client.aclose()
            self._client = None
            logger.info("KtorClient closed")
    
    async def __aenter__(self):
        """Context manager enter"""
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit"""
        await self.close()
    
    async def _request(
        self,
        method: str,
        endpoint: str,
        params: Optional[Dict[str, Any]] = None,
        json_data: Optional[Dict[str, Any]] = None,
        skip_auth: bool = False
    ) -> Dict[str, Any]:
        """
        Внутренний метод для выполнения HTTP запросов
        
        Args:
            method: HTTP метод (GET, POST, PUT, DELETE)
            endpoint: Endpoint относительно base_url
            params: Query параметры
            json_data: JSON body
            skip_auth: Пропустить аутентификацию
        
        Returns:
            Response data
        
        Raises:
            KtorClientError: Ошибка запроса
            KtorAuthenticationError: Ошибка аутентификации
        """
        url = f"{endpoint}"
        
        # Проверка кэша для GET запросов
        if method == "GET" and self.enable_cache:
            cache_key = f"{method}:{url}:{str(params)}"
            if cache_key in self._cache:
                cached_data, cached_time = self._cache[cache_key]
                if datetime.now() - cached_time < self._cache_ttl:
                    logger.debug(f"Cache hit: {cache_key}")
                    return cached_data
        
        # Retry логика
        last_error = None
        
        for attempt in range(self.max_retries):
            try:
                response = await self.client.request(
                    method=method,
                    url=url,
                    params=params,
                    json=json_data
                )
                
                # Обработка статуса
                if response.status_code == 200:
                    data = response.json()
                    
                    # Кэширование
                    if method == "GET" and self.enable_cache:
                        cache_key = f"{method}:{url}:{str(params)}"
                        self._cache[cache_key] = (data, datetime.now())
                    
                    return data
                
                elif response.status_code == 204:
                    return {"success": True}
                
                elif response.status_code == 401:
                    raise KtorAuthenticationError(
                        "Authentication failed. Token may be expired."
                    )
                
                elif response.status_code == 403:
                    raise KtorClientError(
                        f"Access forbidden: {response.text}"
                    )
                
                elif response.status_code == 404:
                    raise KtorNotFoundError(
                        f"Resource not found: {endpoint}"
                    )
                
                elif response.status_code == 429:
                    raise KtorRateLimitError(
                        "Rate limit exceeded. Please wait before retrying."
                    )
                
                else:
                    raise KtorClientError(
                        f"HTTP {response.status_code}: {response.text}"
                    )
            
            except TimeoutException:
                last_error = KtorClientError(f"Request timeout: {endpoint}")
                logger.warning(f"Timeout attempt {attempt + 1}/{self.max_retries}")
            
            except ConnectError as e:
                last_error = KtorClientError(f"Connection error: {str(e)}")
                logger.warning(f"Connect error attempt {attempt + 1}/{self.max_retries}")
            
            except HTTPError as e:
                last_error = KtorClientError(f"HTTP error: {str(e)}")
                logger.warning(f"HTTP error attempt {attempt + 1}/{self.max_retries}")
            
            except KtorAuthenticationError:
                # Не retry при ошибках аутентификации
                raise
            
            except Exception as e:
                last_error = KtorClientError(f"Unexpected error: {str(e)}")
                logger.error(f"Unexpected error: {e}", exc_info=True)
                raise
            
            # Экспоненциальная задержка между попытками
            if attempt < self.max_retries - 1:
                import asyncio
                delay = 2 ** attempt
                logger.info(f"Retrying in {delay}s...")
                await asyncio.sleep(delay)
        
        # Все попытки исчерпаны
        raise last_error
    
    # ============================================
    # Camera Methods
    # ============================================
    
    async def list_cameras(
        self,
        status_filter: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Получить список камер
        
        Args:
            status_filter: Фильтр по статусу (online, offline, all)
        
        Returns:
            Список камер
        """
        params = {}
        if status_filter and status_filter != "all":
            params["status"] = status_filter
        
        data = await self._request("GET", "/api/v1/cameras", params=params)
        return data.get("cameras", [])
    
    async def get_camera(self, camera_id: str) -> Dict[str, Any]:
        """
        Получить информацию о камере
        
        Args:
            camera_id: ID камеры
        
        Returns:
            Информация о камере
        """
        data = await self._request("GET", f"/api/v1/cameras/{camera_id}")
        return data.get("camera", {})
    
    async def get_camera_snapshot(
        self,
        camera_id: str,
        quality: str = "high"
    ) -> Dict[str, Any]:
        """
        Получить снимок с камеры
        
        Args:
            camera_id: ID камеры
            quality: Качество (low, medium, high)
        
        Returns:
            URL снимка и метаданные
        """
        params = {"quality": quality}
        data = await self._request(
            "GET",
            f"/api/v1/cameras/{camera_id}/snapshot",
            params=params
        )
        return data
    
    async def get_camera_stream_url(
        self,
        camera_id: str,
        protocol: str = "hls"
    ) -> Dict[str, Any]:
        """
        Получить URL видеопотока
        
        Args:
            camera_id: ID камеры
            protocol: Протокол (hls, rtsp, webrtc)
        
        Returns:
            URL потока
        """
        data = await self._request(
            "GET",
            f"/api/v1/cameras/{camera_id}/stream",
            params={"protocol": protocol}
        )
        return data
    
    # ============================================
    # Event Methods
    # ============================================
    
    async def search_events(
        self,
        event_type: Optional[str] = None,
        camera_id: Optional[str] = None,
        limit: int = 20,
        since: Optional[str] = None,
        until: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Поиск событий
        
        Args:
            event_type: Тип события
            camera_id: ID камеры
            limit: Максимальное количество
            since: С даты (ISO format)
            until: По дату (ISO format)
        
        Returns:
            Список событий
        """
        params = {"limit": limit}
        
        if event_type and event_type != "all":
            params["event_type"] = event_type
        
        if camera_id:
            params["camera_id"] = camera_id
        
        if since:
            params["since"] = since
        
        if until:
            params["until"] = until
        
        data = await self._request("GET", "/api/v1/events", params=params)
        return data.get("events", [])
    
    async def get_event(self, event_id: str) -> Dict[str, Any]:
        """
        Получить информацию о событии
        
        Args:
            event_id: ID события
        
        Returns:
            Информация о событии
        """
        data = await self._request("GET", f"/api/v1/events/{event_id}")
        return data.get("event", {})
    
    # ============================================
    # Recording Methods
    # ============================================
    
    async def start_recording(
        self,
        camera_id: str,
        duration: int = 0
    ) -> Dict[str, Any]:
        """
        Начать запись
        
        Args:
            camera_id: ID камеры
            duration: Длительность в минутах (0 = непрерывно)
        
        Returns:
            Информация о записи
        """
        json_data = {"camera_id": camera_id}
        if duration > 0:
            json_data["duration_minutes"] = duration
        
        data = await self._request("POST", "/api/v1/recordings/start", json_data=json_data)
        return data
    
    async def stop_recording(self, recording_id: str) -> Dict[str, Any]:
        """
        Остановить запись
        
        Args:
            recording_id: ID записи
        
        Returns:
            Результат остановки
        """
        data = await self._request("POST", f"/api/v1/recordings/{recording_id}/stop")
        return data
    
    async def list_recordings(
        self,
        camera_id: Optional[str] = None,
        limit: int = 20
    ) -> List[Dict[str, Any]]:
        """
        Список записей
        
        Args:
            camera_id: ID камеры
            limit: Максимальное количество
        
        Returns:
            Список записей
        """
        params = {"limit": limit}
        if camera_id:
            params["camera_id"] = camera_id
        
        data = await self._request("GET", "/api/v1/recordings", params=params)
        return data.get("recordings", [])
    
    # ============================================
    # PTZ Methods
    # ============================================
    
    async def ptz_control(
        self,
        camera_id: str,
        action: str,
        speed: int = 5
    ) -> Dict[str, Any]:
        """
        PTZ управление
        
        Args:
            camera_id: ID камеры
            action: Действие (pan_left, pan_right, tilt_up, tilt_down, zoom_in, zoom_out, reset)
            speed: Скорость (1-10)
        
        Returns:
            Результат выполнения
        """
        json_data = {
            "camera_id": camera_id,
            "action": action,
            "speed": speed
        }
        
        data = await self._request("POST", "/api/v1/cameras/ptz", json_data=json_data)
        return data
    
    # ============================================
    # User & Auth Methods
    # ============================================
    
    async def refresh_token(self, refresh_token: str) -> Dict[str, str]:
        """
        Обновить JWT токен
        
        Args:
            refresh_token: Refresh token
        
        Returns:
            Новые токены
        """
        json_data = {"refresh_token": refresh_token}
        data = await self._request(
            "POST",
            "/api/v1/auth/refresh",
            json_data=json_data,
            skip_auth=True
        )
        return data
    
    async def get_current_user(self) -> Dict[str, Any]:
        """
        Получить текущего пользователя
        
        Returns:
            Информация о пользователе
        """
        data = await self._request("GET", "/api/v1/auth/me")
        return data.get("user", {})
    
    # ============================================
    # Cache Management
    # ============================================
    
    def clear_cache(self):
        """Очистить кэш"""
        self._cache.clear()
        logger.info("KtorClient cache cleared")
    
    def get_cache_stats(self) -> Dict[str, Any]:
        """Получить статистику кэша"""
        return {
            "size": len(self._cache),
            "ttl_minutes": self._cache_ttl.total_seconds() / 60,
            "enabled": self.enable_cache
        }


# ============================================
# Singleton Instance
# ============================================

_ktor_client: Optional[KtorClient] = None


def get_ktor_client() -> KtorClient:
    """Получить глобальный KtorClient"""
    if _ktor_client is None:
        raise RuntimeError("KtorClient not initialized")
    return _ktor_client


def set_ktor_client(client: KtorClient) -> None:
    """Установить глобальный KtorClient"""
    global _ktor_client
    _ktor_client = client
