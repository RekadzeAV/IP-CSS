"""
Embeddings - Векторные эмбеддинги для семантического поиска

Обеспечивает:
- Генерация эмбеддингов через sentence-transformers
- Кэширование эмбеддингов
- Поддержка различных моделей
"""

import logging
from typing import List, Optional, Dict, Any
import numpy as np

logger = logging.getLogger(__name__)


class EmbeddingsConfig:
    """Конфигурация эмбеддингов"""
    
    # Модели эмбеддингов
    DEFAULT_MODEL = "sentence-transformers/all-MiniLM-L6-v2"  # Быстрая, 384 dims
    ALTERNATIVE_MODEL = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"  # Мультиязычная
    
    # Размеры моделей
    MODEL_DIMENSIONS = {
        "sentence-transformers/all-MiniLM-L6-v2": 384,
        "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2": 384
    }
    
    # Кэширование
    CACHE_SIZE = 1000
    ENABLE_CACHING = True


class Embeddings:
    """
    Генератор векторных эмбеддинг
    
    Использует sentence-transformers для создания эмбеддингов
    """
    
    def __init__(
        self,
        model_name: str = EmbeddingsConfig.DEFAULT_MODEL,
        cache_size: int = EmbeddingsConfig.CACHE_SIZE,
        enable_caching: bool = EmbeddingsConfig.ENABLE_CACHING
    ):
        """
        Инициализация Embeddings
        
        Args:
            model_name: Название модели
            cache_size: Размер кэша
            enable_caching: Включить кэширование
        """
        self.model_name = model_name
        self.cache_size = cache_size
        self.enable_caching = enable_caching
        
        self._model = None
        self._cache: Dict[str, List[float]] = {}
        
        logger.info(f"Embeddings initialized with model: {model_name}")
    
    @property
    def model(self):
        """Ленивая загрузка модели"""
        if self._model is None:
            try:
                from sentence_transformers import SentenceTransformer
                logger.info(f"Loading embedding model: {self.model_name}")
                self._model = SentenceTransformer(self.model_name)
                logger.info("Embedding model loaded successfully")
            except ImportError:
                logger.warning(
                    "sentence-transformers not installed. "
                    "Install with: pip install sentence-transformers"
                )
                raise
            except Exception as e:
                logger.error(f"Failed to load embedding model: {e}")
                raise
        
        return self._model
    
    def embed(self, text: str) -> List[float]:
        """
        Создать эмбеддинг для текста
        
        Args:
            text: Текст для эмбеддинга
        
        Returns:
            Вектор эмбеддинга
        """
        if not text or not text.strip():
            return [0.0] * self.get_dimensions()
        
        # Проверка кэша
        if self.enable_caching:
            cache_key = text.strip().lower()
            if cache_key in self._cache:
                logger.debug("Cache hit for embedding")
                return self._cache[cache_key]
        
        # Генерация эмбеддинга
        try:
            embedding = self.model.encode(text)
            embedding_list = embedding.tolist()
            
            # Добавление в кэш
            if self.enable_caching:
                self._add_to_cache(cache_key, embedding_list)
            
            return embedding_list
            
        except Exception as e:
            logger.error(f"Failed to generate embedding: {e}")
            # Fallback на нулевой вектор
            return [0.0] * self.get_dimensions()
    
    def embed_batch(self, texts: List[str]) -> List[List[float]]:
        """
        Создать эмбеддинги для batch текстов
        
        Args:
            texts: Список текстов
        
        Returns:
            Список векторов эмбеддингов
        """
        if not texts:
            return []
        
        # Генерация batch эмбеддингов
        try:
            embeddings = self.model.encode(texts)
            embeddings_list = embeddings.tolist()
            
            # Добавление в кэш
            if self.enable_caching:
                for text, embedding in zip(texts, embeddings_list):
                    self._add_to_cache(text.strip().lower(), embedding)
            
            return embeddings_list
            
        except Exception as e:
            logger.error(f"Failed to generate batch embeddings: {e}")
            # Fallback на нулевые векторы
            return [[0.0] * self.get_dimensions() for _ in texts]
    
    def similarity(self, text1: str, text2: str) -> float:
        """
        Вычислить косинусную схожесть двух текстов
        
        Args:
            text1: Первый текст
            text2: Второй текст
        
        Returns:
            Схожесть от -1 до 1
        """
        emb1 = self.embed(text1)
        emb2 = self.embed(text2)
        
        return self.cosine_similarity(emb1, emb2)
    
    def cosine_similarity(
        self,
        vec1: List[float],
        vec2: List[float]
    ) -> float:
        """
        Вычислить косинусную схожесть двух векторов
        
        Args:
            vec1: Первый вектор
            vec2: Второй вектор
        
        Returns:
            Схожесть от -1 до 1
        """
        try:
            v1 = np.array(vec1)
            v2 = np.array(vec2)
            
            dot_product = np.dot(v1, v2)
            norm1 = np.linalg.norm(v1)
            norm2 = np.linalg.norm(v2)
            
            if norm1 == 0 or norm2 == 0:
                return 0.0
            
            return float(dot_product / (norm1 * norm2))
            
        except Exception as e:
            logger.error(f"Failed to compute cosine similarity: {e}")
            return 0.0
    
    def get_dimensions(self) -> int:
        """Получить размерность векторов"""
        return EmbeddingsConfig.MODEL_DIMENSIONS.get(
            self.model_name,
            384  # Default
        )
    
    def _add_to_cache(self, key: str, embedding: List[float]):
        """Добавить в кэш с LRU политикой"""
        if len(self._cache) >= self.cache_size:
            # Удалить самый старый элемент
            oldest_key = next(iter(self._cache))
            del self._cache[oldest_key]
        
        self._cache[key] = embedding
    
    def clear_cache(self):
        """Очистить кэш"""
        self._cache.clear()
        logger.info("Embedding cache cleared")
    
    def get_cache_stats(self) -> Dict[str, Any]:
        """Получить статистику кэша"""
        return {
            "size": len(self._cache),
            "max_size": self.cache_size,
            "enabled": self.enable_caching
        }


# ============================================
# Mock Embeddings (для тестирования без модели)
# ============================================

class MockEmbeddings(Embeddings):
    """
    Mock embeddings для тестирования
    
    Генерирует псевдо-случайные векторы на основе хеша текста
    """
    
    def __init__(self, dimensions: int = 384):
        self._dimensions = dimensions
        logger.info("MockEmbeddings initialized")
    
    def embed(self, text: str) -> List[float]:
        """Генерация псевдо-случайного вектора"""
        if not text:
            return [0.0] * self._dimensions
        
        # Хеш текста для детерминированности
        hash_value = hash(text)
        
        # Генерация вектора на основе хеша
        np.random.seed(hash_value)
        return np.random.randn(self._dimensions).tolist()
    
    def embed_batch(self, texts: List[str]) -> List[List[float]]:
        """Batch генерация"""
        return [self.embed(text) for text in texts]
    
    def get_dimensions(self) -> int:
        return self._dimensions
