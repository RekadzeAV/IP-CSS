"""
ContextCompressor - Сжатие контекста для LLM

Оптимизирует длину контекста, чтобы уложиться в лимиты токенов LLM:
- Удаление старых сообщений
- Сжатие истории диалога
- Приоритизация важной информации
"""

import logging
from typing import Dict, Any, List, Optional
from datetime import datetime

logger = logging.getLogger(__name__)


class ContextCompressor:
    """
    Сжиматель контекста для оптимизации длины промпта
    
    Методы сжатия:
    1. Удаление старых сообщений из истории
    2. Суммаризация (краткое изложение) истории
    3. Приоритизация по релевантности
    """
    
    def __init__(
        self,
        max_history_length: int = 10,
        summary_threshold: int = 5
    ):
        """
        Инициализация ContextCompressor
        
        Args:
            max_history_length: Максимальное количество сообщений в истории
            summary_threshold: Порог для суммаризации (кол-во сообщений)
        """
        self.max_history_length = max_history_length
        self.summary_threshold = summary_threshold
        logger.info(f"ContextCompressor initialized (max_history={max_history_length}, summary_threshold={summary_threshold})")
    
    async def compress(
        self,
        prompt: str,
        max_tokens: int,
        strategy: str = "auto"
    ) -> str:
        """
        Сжатие промпта до указанного количества токенов
        
        Args:
            prompt: Оригинальный промпт
            max_tokens: Максимальное количество токенов
            strategy: Стратегия сжатия ("auto", "truncate", "summarize")
        
        Returns:
            Сжатый промпт
        """
        current_tokens = self._estimate_tokens(prompt)
        
        logger.debug(f"Compressing prompt: {current_tokens} tokens -> max {max_tokens} tokens")
        
        if current_tokens <= max_tokens:
            logger.debug("Prompt already within token limit, no compression needed")
            return prompt
        
        if strategy == "auto":
            # Автоматический выбор стратегии
            if current_tokens > max_tokens * 2:
                strategy = "summarize"
            else:
                strategy = "truncate"
        
        if strategy == "truncate":
            return await self._truncate_prompt(prompt, max_tokens)
        elif strategy == "summarize":
            return await self._summarize_prompt(prompt, max_tokens)
        else:
            return await self._truncate_prompt(prompt, max_tokens)
    
    async def _truncate_prompt(self, prompt: str, max_tokens: int) -> str:
        """
        Обрезка промпта (удаление старых частей)
        
        Args:
            prompt: Оригинальный промпт
            max_tokens: Максимальное количество токенов
        
        Returns:
            Обрезанный промпт
        """
        logger.info("Truncating prompt")
        
        # Простая стратегия: оставляем последние N токенов
        # В production использовать более умные стратегии
        
        estimated_tokens = self._estimate_tokens(prompt)
        if estimated_tokens <= max_tokens:
            return prompt
        
        # Удаление 20% за раз до достижения лимита
        while estimated_tokens > max_tokens:
            # Удаляем начало промпта (старые сообщения)
            lines = prompt.split('\n')
            if len(lines) > 10:
                # Оставляем последние 80% строк
                keep_lines = int(len(lines) * 0.8)
                prompt = '\n'.join(lines[-keep_lines:])
                estimated_tokens = self._estimate_tokens(prompt)
            else:
                # Не можем больше обрезать
                break
        
        return prompt
    
    async def _summarize_prompt(self, prompt: str, max_tokens: int) -> str:
        """
        Суммаризация промпта (краткое изложение)
        
        Args:
            prompt: Оригинальный промпт
            max_tokens: Максимальное количество токенов
        
        Returns:
            Сжатый промпт
        """
        logger.info("Summarizing prompt")
        
        # TODO: Использовать LLM для суммаризации
        # Пока заглушка - просто обрезка
        
        return await self._truncate_prompt(prompt, max_tokens)
    
    def _estimate_tokens(self, text: str) -> int:
        """
        Оценка количества токенов в тексте
        
        Args:
            text: Текст для оценки
        
        Returns:
            Примерное количество токенов
        """
        # Грубая оценка: 1 токен ≈ 4 символа для английского
        # Для русского может быть больше
        return len(text) // 4
    
    async def compress_history(
        self,
        messages: List[Dict[str, str]],
        max_messages: Optional[int] = None
    ) -> List[Dict[str, str]]:
        """
        Сжатие истории сообщений
        
        Args:
            messages: История сообщений [{role: "user/assistant", content: "..."}]
            max_messages: Максимальное количество сообщений (по умолчанию self.max_history_length)
        
        Returns:
            Сжатая история сообщений
        """
        if max_messages is None:
            max_messages = self.max_history_length
        
        if len(messages) <= max_messages:
            return messages
        
        logger.info(f"Compressing history: {len(messages)} -> {max_messages} messages")
        
        # Сохраняем последние max_messages
        # В production: суммаризация старых сообщений
        
        return messages[-max_messages:]
    
    def should_compress(self, prompt_length: int, threshold: int) -> bool:
        """
        Проверка, нужно ли сжимать контекст
        
        Args:
            prompt_length: Длина промпта в токенах
            threshold: Порог для сжатия
        
        Returns:
            True если нужно сжимать
        """
        return prompt_length > threshold
