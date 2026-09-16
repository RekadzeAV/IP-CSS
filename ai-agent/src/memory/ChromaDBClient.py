"""
ChromaDBClient - Клиент для векторной базы данных ChromaDB

Обеспечивает:
- Подключение к ChromaDB
- Создание/управление коллекциями
- Векторный поиск (similarity search)
- CRUD операции с документами
"""

import logging
from typing import Dict, List, Any, Optional
import aiohttp

logger = logging.getLogger(__name__)


class ChromaDBClient:
    """
    Клиент для работы с ChromaDB
    
    Использует HTTP API для взаимодействия с ChromaDB
    """
    
    def __init__(
        self,
        host: str = "localhost",
        port: int = 8000,
        password: Optional[str] = None,
        collection: str = "agent_memory"
    ):
        """
        Инициализация клиента
        
        Args:
            host: Хост ChromaDB
            port: Порт ChromaDB
            password: Пароль для аутентификации
            collection: Имя коллекции по умолчанию
        """
        self.host = host
        self.port = port
        self.password = password
        self.collection_name = collection
        self.base_url = f"http://{host}:{port}/api/v1"
        self._session: Optional[aiohttp.ClientSession] = None
        self._collection = None
        
        logger.info(f"ChromaDBClient initialized: {host}:{port}, collection={collection}")
    
    async def initialize(self) -> None:
        """Инициализация клиента и создание сессии"""
        self._session = aiohttp.ClientSession()
        logger.info("ChromaDB session created")
        
        # Проверка подключения
        await self.heartbeat()
        
        # Создание коллекции
        await self._ensure_collection()
    
    async def close(self) -> None:
        """Закрытие клиента и сессии"""
        if self._session:
            await self._session.close()
            logger.info("ChromaDB session closed")
    
    async def heartbeat(self) -> Dict[str, Any]:
        """
        Проверка подключения к ChromaDB
        
        Returns:
            Ответ от ChromaDB
        """
        try:
            async with self._session.get(f"{self.base_url}/heartbeat") as response:
                data = await response.json()
                logger.debug(f"ChromaDB heartbeat: {data}")
                return data
        except Exception as e:
            logger.error(f"ChromaDB heartbeat failed: {e}")
            raise
    
    async def _ensure_collection(self) -> None:
        """Создание коллекции если не существует"""
        try:
            # Получение списка коллекций
            async with self._session.get(f"{self.base_url}/collections") as response:
                collections = await response.json()
                
                # Проверка наличия коллекции
                collection_exists = any(
                    c.get("name") == self.collection_name 
                    for c in collections.get("collections", [])
                )
                
                if not collection_exists:
                    # Создание коллекции
                    async with self._session.post(
                        f"{self.base_url}/collections",
                        json={"name": self.collection_name}
                    ) as create_response:
                        result = await create_response.json()
                        self._collection = result
                        logger.info(f"Collection created: {self.collection_name}")
                else:
                    logger.info(f"Collection already exists: {self.collection_name}")
                    
        except Exception as e:
            logger.warning(f"Failed to ensure collection: {e}")
            # Продолжаем работу без коллекции (базовый режим)
    
    async def add_documents(
        self,
        documents: List[str],
        metadatas: List[Dict[str, Any]],
        ids: List[str]
    ) -> None:
        """
        Добавление документов в коллекцию
        
        Args:
            documents: Список документов
            metadatas: Метаданные для каждого документа
            ids: Уникальные идентификаторы
        """
        if not self._session:
            raise RuntimeError("ChromaDBClient not initialized")
        
        try:
            async with self._session.post(
                f"{self.base_url}/collections/{self.collection_name}/add",
                json={
                    "documents": documents,
                    "metadatas": metadatas,
                    "ids": ids
                }
            ) as response:
                result = await response.json()
                logger.debug(f"Documents added: {len(documents)}")
        except Exception as e:
            logger.error(f"Failed to add documents: {e}")
            raise
    
    async def query(
        self,
        query_text: str,
        n_results: int = 5
    ) -> Dict[str, Any]:
        """
        Векторный поиск по коллекции
        
        Args:
            query_text: Текст запроса
            n_results: Количество результатов
        
        Returns:
            Результаты поиска
        """
        if not self._session:
            raise RuntimeError("ChromaDBClient not initialized")
        
        try:
            # TODO: Использовать embeddings для векторного поиска
            # Пока заглушка - возвращаем пустой результат
            
            return {
                "documents": [],
                "metadatas": [],
                "distances": [],
                "ids": []
            }
        except Exception as e:
            logger.error(f"Query failed: {e}")
            raise
    
    async def get(
        self,
        ids: Optional[List[str]] = None,
        where: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Получение документов по ID или фильтрам
        
        Args:
            ids: Список ID
            where: Фильтры
        
        Returns:
            Документы
        """
        if not self._session:
            raise RuntimeError("ChromaDBClient not initialized")
        
        try:
            async with self._session.post(
                f"{self.base_url}/collections/{self.collection_name}/get",
                json={"ids": ids, "where": where}
            ) as response:
                result = await response.json()
                return result
        except Exception as e:
            logger.error(f"Get failed: {e}")
            raise
    
    async def delete(
        self,
        ids: List[str]
    ) -> None:
        """
        Удаление документов
        
        Args:
            ids: Список ID для удаления
        """
        if not self._session:
            raise RuntimeError("ChromaDBClient not initialized")
        
        try:
            async with self._session.post(
                f"{self.base_url}/collections/{self.collection_name}/delete",
                json={"ids": ids}
            ) as response:
                result = await response.json()
                logger.debug(f"Documents deleted: {len(ids)}")
        except Exception as e:
            logger.error(f"Delete failed: {e}")
            raise
