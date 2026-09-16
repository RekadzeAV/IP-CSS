"""
MemoryManager - Менеджер долговременной памяти AI-агента

Обеспечивает:
- Запоминание фактов (remember)
- Поиск по памяти (recall)
- Забывание (forget)
- Сжатие контекста
- Управление сессиями
"""

import logging
from typing import Dict, List, Any, Optional
from datetime import datetime, timedelta
import json

from memory.ChromaDBClient import ChromaDBClient
from memory.Embeddings import Embeddings, MockEmbeddings

logger = logging.getLogger(__name__)


class MemoryManager:
    """
    Менеджер долговременной памяти AI-агента
    
    Использует ChromaDB для векторного поиска и хранения
    """
    
    def __init__(
        self,
        chroma_client: ChromaDBClient,
        use_mock_embeddings: bool = False
    ):
        """
        Инициализация MemoryManager
        
        Args:
            chroma_client: Клиент ChromaDB
            use_mock_embeddings: Использовать mock embeddings для тестирования
        """
        self.chroma_client = chroma_client
        self._session_cache: Dict[str, List[Dict[str, Any]]] = {}
        
        # Инициализация Embeddings
        try:
            if use_mock_embeddings:
                self.embeddings = MockEmbeddings()
                logger.info("MemoryManager initialized with MockEmbeddings")
            else:
                self.embeddings = Embeddings()
                logger.info("MemoryManager initialized with Embeddings")
        except Exception as e:
            logger.warning(f"Failed to initialize Embeddings: {e}")
            logger.info("Falling back to MockEmbeddings")
            self.embeddings = MockEmbeddings()
        
        logger.info("MemoryManager initialized")
    
    async def close(self) -> None:
        """Закрытие менеджера"""
        await self.chroma_client.close()
        self._session_cache.clear()
    
    async def remember(
        self,
        user_id: str,
        key: str,
        value: Any,
        metadata: Optional[Dict[str, Any]] = None
    ) -> str:
        """
        Запоминание факта
        
        Args:
            user_id: ID пользователя
            key: Ключ факта
            value: Значение факта
            metadata: Дополнительные метаданные
        
        Returns:
            ID запомненного факта
        """
        try:
            # Форматирование факта для хранения
            fact = {
                "user_id": user_id,
                "key": key,
                "value": value,
                "type": self._detect_value_type(value),
                "timestamp": datetime.now().isoformat()
            }
            
            # Добавление пользовательских метаданных
            if metadata:
                fact.update(metadata)
            
            # Преобразование в строку для ChromaDB
            fact_text = self._format_fact_text(user_id, key, value)
            
            # Генерация ID
            fact_id = f"{user_id}:{key}:{datetime.now().timestamp()}"
            
            # Добавление в ChromaDB
            await self.chroma_client.add_documents(
                documents=[fact_text],
                metadatas=[fact],
                ids=[fact_id]
            )
            
            # Обновление кэша
            if user_id not in self._session_cache:
                self._session_cache[user_id] = []
            self._session_cache[user_id].append(fact)
            
            logger.info(f"Remembered: {key} for user {user_id}")
            return fact_id
            
        except Exception as e:
            logger.error(f"Failed to remember: {e}", exc_info=True)
            raise
    
    async def recall(
        self,
        user_id: str,
        query: str,
        n_results: int = 5,
        filters: Optional[Dict[str, Any]] = None
    ) -> List[Dict[str, Any]]:
        """
        Поиск по памяти
        
        Args:
            user_id: ID пользователя
            query: Запрос для поиска
            n_results: Количество результатов
            filters: Дополнительные фильтры
        
        Returns:
            Список найденных фактов
        """
        try:
            # Проверка кэша
            if user_id in self._session_cache and len(self._session_cache[user_id]) > 0:
                # Простой поиск по кэшу (для разработки)
                cached_results = self._search_cache(
                    self._session_cache[user_id],
                    query,
                    n_results
                )
                if cached_results:
                    logger.debug(f"Found {len(cached_results)} facts in cache")
                    return cached_results
            
            # Векторный поиск в ChromaDB
            results = await self.chroma_client.query(
                query_text=query,
                n_results=n_results
            )
            
            # Фильтрация по user_id и дополнительным фильтрам
            filtered_results = []
            if results.get("metadatas"):
                for i, metadata in enumerate(results["metadatas"]):
                    # Проверка user_id
                    if metadata.get("user_id") != user_id:
                        continue
                    
                    # Применение дополнительных фильтров
                    if filters:
                        match = True
                        for key, value in filters.items():
                            if metadata.get(key) != value:
                                match = False
                                break
                        if not match:
                            continue
                    
                    filtered_results.append({
                        "content": results["documents"][i] if results.get("documents") else "",
                        "metadata": metadata,
                        "distance": results["distances"][i] if results.get("distances") else 0
                    })
            
            logger.debug(f"Recalled {len(filtered_results)} facts for user {user_id}")
            return filtered_results
            
        except Exception as e:
            logger.error(f"Failed to recall: {e}", exc_info=True)
            return []
    
    async def search_semantic(
        self,
        user_id: str,
        query: str,
        n_results: int = 5,
        similarity_threshold: float = 0.5
    ) -> List[Dict[str, Any]]:
        """
        Семантический поиск по памяти
        
        Args:
            user_id: ID пользователя
            query: Запрос для поиска
            n_results: Количество результатов
            similarity_threshold: Порог схожести
        
        Returns:
            Список найденных фактов с оценкой схожести
        """
        try:
            # Генерация эмбеддинга для запроса
            query_embedding = self.embeddings.embed(query)
            
            # Получение всех фактов пользователя
            all_facts = await self.get_user_memory(user_id, limit=1000)
            
            if not all_facts:
                return []
            
            # Вычисление схожести для каждого факта
            scored_results = []
            for fact in all_facts:
                fact_text = self._format_fact_text(
                    user_id,
                    fact.get("key", ""),
                    fact.get("value", "")
                )
                fact_embedding = self.embeddings.embed(fact_text)
                
                similarity = self.embeddings.cosine_similarity(
                    query_embedding,
                    fact_embedding
                )
                
                if similarity >= similarity_threshold:
                    scored_results.append({
                        "fact": fact,
                        "similarity": similarity,
                        "content": fact_text
                    })
            
            # Сортировка по схожести
            scored_results.sort(key=lambda x: x["similarity"], reverse=True)
            
            # Возврат топ результатов
            return scored_results[:n_results]
            
        except Exception as e:
            logger.error(f"Semantic search failed: {e}", exc_info=True)
            return []
    
    async def forget(
        self,
        user_id: str,
        key: Optional[str] = None,
        value: Optional[str] = None,
        older_than: Optional[datetime] = None
    ) -> int:
        """
        Забывание фактов
        
        Args:
            user_id: ID пользователя
            key: Ключ факта (None = все ключи)
            value: Значение для фильтрации
            older_than: Удалить факты старше даты
        
        Returns:
            Количество удалённых фактов
        """
        try:
            # Построение фильтров
            where_clause = {"user_id": user_id}
            if key:
                where_clause["key"] = key
            
            # Получение фактов для удаления
            results = await self.chroma_client.get(
                where=where_clause
            )
            
            if not results.get("ids"):
                logger.info(f"No facts found to forget for user {user_id}")
                return 0
            
            # Фильтрация по значению и дате
            ids_to_delete = []
            for i, metadata in enumerate(results.get("metadatas", [])):
                # Проверка значения
                if value and metadata.get("value") != value:
                    continue
                
                # Проверка даты
                if older_than:
                    timestamp_str = metadata.get("timestamp")
                    if timestamp_str:
                        fact_time = datetime.fromisoformat(timestamp_str)
                        if fact_time > older_than:
                            continue
                
                ids_to_delete.append(results["ids"][i])
            
            if not ids_to_delete:
                return 0
            
            # Удаление фактов
            await self.chroma_client.delete(ids=ids_to_delete)
            
            # Очистка кэша
            if user_id in self._session_cache:
                self._session_cache[user_id] = [
                    f for f in self._session_cache[user_id]
                    if f.get("id") not in ids_to_delete
                ]
            
            logger.info(f"Forget: {key or 'all keys'} for user {user_id} ({len(ids_to_delete)} facts)")
            return len(ids_to_delete)
            
        except Exception as e:
            logger.error(f"Failed to forget: {e}", exc_info=True)
            raise

    async def get_user_memory(
        self,
        user_id: str,
        limit: int = 100
    ) -> List[Dict[str, Any]]:
        """
        Получение всей памяти пользователя
        
        Args:
            user_id: ID пользователя
            limit: Максимальное количество фактов
        
        Returns:
            Список всех фактов пользователя
        """
        try:
            # Использование кэша если доступен
            if user_id in self._session_cache:
                cached = self._session_cache[user_id]
                return cached[:limit]
            
            # Получение из ChromaDB
            results = await self.chroma_client.get(
                where={"user_id": user_id}
            )
            
            if not results.get("metadatas"):
                return []
            
            facts = [
                {
                    "key": metadata.get("key"),
                    "value": metadata.get("value"),
                    "type": metadata.get("type"),
                    "timestamp": metadata.get("timestamp")
                }
                for metadata in results["metadatas"][:limit]
            ]
            
            return facts
            
        except Exception as e:
            logger.error(f"Failed to get user memory: {e}", exc_info=True)
            return []
    
    async def clear_user_memory(
        self,
        user_id: str
    ) -> int:
        """
        Очистка всей памяти пользователя
        
        Args:
            user_id: ID пользователя
        
        Returns:
            Количество удалённых фактов
        """
        try:
            results = await self.chroma_client.get(
                where={"user_id": user_id}
            )
            
            if not results.get("ids"):
                return 0
            
            await self.chroma_client.delete(ids=results["ids"])
            
            # Очистка кэша
            if user_id in self._session_cache:
                self._session_cache[user_id] = []
            
            logger.info(f"Cleared memory for user {user_id} ({len(results['ids'])} facts)")
            return len(results["ids"])
            
        except Exception as e:
            logger.error(f"Failed to clear user memory: {e}", exc_info=True)
            raise
    
    async def update_memory(
        self,
        user_id: str,
        key: str,
        new_value: Any
    ) -> bool:
        """
        Обновление факта в памяти
        
        Args:
            user_id: ID пользователя
            key: Ключ факта
            new_value: Новое значение
        
        Returns:
            True если обновлено, False если не найдено
        """
        try:
            # Поиск факта
            existing = await self.recall(user_id, key, n_results=1)
            
            if not existing:
                logger.warning(f"Cannot update: key '{key}' not found for user {user_id}")
                return False
            
            # Удаление старого факта
            old_key = existing[0]["metadata"].get("key")
            await self.forget(user_id, key=old_key)
            
            # Добавление нового факта
            await self.remember(user_id, key, new_value)
            
            logger.info(f"Updated: {key} for user {user_id}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to update memory: {e}", exc_info=True)
            return False
    
    def _detect_value_type(self, value: Any) -> str:
        """Определение типа значения"""
        if isinstance(value, bool):
            # bool — подкласс int в Python; проверяем ДО (int, float), иначе True/False уходят в "number"
            return "boolean"
        elif isinstance(value, dict):
            return "object"
        elif isinstance(value, list):
            return "array"
        elif isinstance(value, (int, float)):
            return "number"
        elif isinstance(value, str):
            return "string"
        else:
            return "unknown"
    
    def _format_fact_text(self, user_id: str, key: str, value: Any) -> str:
        """Форматирование факта в текст для поиска"""
        if isinstance(value, (dict, list)):
            try:
                value_str = json.dumps(value, ensure_ascii=False)
            except:
                value_str = str(value)
        else:
            value_str = str(value)
        
        return f"User {user_id}: {key} = {value_str}"
    
    def _search_cache(
        self,
        cache: List[Dict[str, Any]],
        query: str,
        n_results: int
    ) -> List[Dict[str, Any]]:
        """Поиск по кэшу (простая эвристика)"""
        # Простой поиск по ключевым словам
        query_lower = query.lower()
        scores = []
        
        for fact in cache:
            score = 0
            key = fact.get("key", "").lower()
            value = str(fact.get("value", "")).lower()
            
            if query_lower in key:
                score += 2
            if query_lower in value:
                score += 1
            
            if score > 0:
                scores.append((score, fact))
        
        # Сортировка по релевантности
        scores.sort(reverse=True, key=lambda x: x[0])
        
        return [
            {
                "content": self._format_fact_text(
                    fact.get("user_id", ""),
                    fact.get("key", ""),
                    fact.get("value", "")
                ),
                "metadata": fact,
                "distance": 0
            }
            for _, fact in scores[:n_results]
        ]
