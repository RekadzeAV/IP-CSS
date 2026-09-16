"""
AgentLoop - Основной цикл оркестрации AI-агента

Этот модуль реализует основной цикл работы AI-агента:
1. Получение запроса от пользователя
2. Сборка контекста (время, пользователь, память)
3. Вызов LLM
4. Парсинг ответа (инструмент или текст)
5. Выполнение инструмента (если нужно)
6. Возврат ответа пользователю
"""

import logging
from typing import Dict, Any, List, Optional
from datetime import datetime
import json

from agent.AgentContext import AgentContext
from agent.ContextCompressor import ContextCompressor
from tools.ToolRegistry import ToolRegistry
from memory.MemoryManager import MemoryManager
from config import settings

logger = logging.getLogger(__name__)


class AgentLoop:
    """
    Основной цикл работы AI-агента
    
    Оркестрирует взаимодействие между:
    - LLM (большие языковые модели)
    - Инструментами (tools)
    - Памятью (memory)
    - Контекстом (context)
    """
    
    def __init__(
        self,
        tool_registry: ToolRegistry,
        memory_manager: MemoryManager,
        context_compressor: Optional[ContextCompressor] = None
    ):
        """
        Инициализация AgentLoop
        
        Args:
            tool_registry: Реестр доступных инструментов
            memory_manager: Менеджер памяти
            context_compressor: Сжиматель контекста (опционально)
        """
        self.tool_registry = tool_registry
        self.memory_manager = memory_manager
        self.context_compressor = context_compressor or ContextCompressor()
        
        self._system_prompt = self._build_system_prompt()
        logger.info("AgentLoop initialized successfully")
    
    def _build_system_prompt(self) -> str:
        """
        Построение системного промпта для LLM
        
        Returns:
            Системный промпт с инструкциями для агента
        """
        prompt = f"""You are {settings.AGENT_NAME}, an AI assistant for IP-CSS video surveillance system.

Your capabilities:
- Manage IP cameras (list, add, update, delete, test connection)
- View camera snapshots and status
- Search and manage events (motion detection, alerts)
- Control video recordings (start, stop, pause, resume)
- Access long-term memory for context and preferences

Available tools:
{json.dumps(self.tool_registry.get_tool_list(), indent=2)}

Rules:
1. Always confirm before executing dangerous actions (delete, stop recording, etc.)
2. Use memory to remember user preferences and context
3. Provide clear, helpful responses in the user's language
4. If you don't know something, admit it and suggest alternatives
5. For video-related requests, explain technical details simply

Current time: {datetime.now().isoformat()}

Respond naturally to user queries. If a tool needs to be called, use the tool response format."""
        
        return prompt
    
    async def process_request(
        self,
        message: str,
        user_id: str,
        conversation_id: Optional[str] = None,
        context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Обработка запроса пользователя
        
        Args:
            message: Текст запроса пользователя
            user_id: ID пользователя
            conversation_id: ID диалога (опционально)
            context: Дополнительный контекст (опционально)
        
        Returns:
            Dict с ответом агента:
            - response: Текст ответа
            - conversation_id: ID диалога
            - tools_used: Список использованных инструментов
            - memory_used: Использовалась ли память
            - tokens_used: Количество токенов (если доступно)
        """
        logger.info(f"Processing request from user {user_id}: {message[:100]}...")
        
        # Шаг 1: Создание контекста
        agent_context = await self._create_context(
            user_id=user_id,
            conversation_id=conversation_id,
            message=message,
            extra_context=context
        )
        
        # Шаг 2: Сборка полного промпта
        full_prompt = self._build_full_prompt(agent_context, message)
        
        # Шаг 3: Сжатие контекста (если нужно)
        if self.context_compressor and len(full_prompt) > settings.MEMORY_COMPRESSION_THRESHOLD:
            logger.info(f"Compressing context: {len(full_prompt)} tokens > {settings.MEMORY_COMPRESSION_THRESHOLD}")
            full_prompt = await self.context_compressor.compress(
                full_prompt,
                max_tokens=settings.MEMORY_MAX_TOKENS
            )
        
        # Шаг 4: Вызов LLM
        try:
            llm_response = await self._call_llm(full_prompt, user_id)
        except Exception as e:
            logger.error(f"LLM call failed: {e}", exc_info=True)
            return {
                "response": "I'm sorry, I encountered an error while processing your request. "
                           "Please try again later or contact support.",
                "conversation_id": agent_context.conversation_id,
                "tools_used": [],
                "memory_used": False,
                "error": str(e)
            }
        
        # Шаг 5: Парсинг ответа LLM
        parsed_response = await self._parse_llm_response(llm_response)
        
        # Шаг 6: Выполнение инструмента (если нужно)
        tools_used = []
        final_response = parsed_response.get("text")
        
        if parsed_response.get("tool_call"):
            tool_name = parsed_response["tool_call"]["name"]
            tool_params = parsed_response["tool_call"]["parameters"]
            
            logger.info(f"Executing tool: {tool_name} with params: {tool_params}")
            
            try:
                tool_result = await self._execute_tool(
                    tool_name=tool_name,
                    parameters=tool_params,
                    user_id=user_id,
                    context=agent_context
                )
                tools_used.append(tool_name)
                
                # Шаг 7: Отправка результата инструмента обратно в LLM
                final_response = await self._finalize_with_tool_result(
                    tool_name=tool_name,
                    tool_result=tool_result,
                    original_prompt=full_prompt,
                    user_id=user_id
                )
                
            except Exception as e:
                logger.error(f"Tool execution failed: {e}", exc_info=True)
                final_response = f"I encountered an error while executing {tool_name}: {str(e)}"
        
        # Шаг 8: Сохранение в память (если нужно)
        memory_used = await self._save_to_memory(
            user_id=user_id,
            conversation_id=agent_context.conversation_id,
            message=message,
            response=final_response
        )
        
        logger.info(f"Request processed successfully. Tools used: {tools_used}, Memory used: {memory_used}")
        
        return {
            "response": final_response,
            "conversation_id": agent_context.conversation_id,
            "tools_used": tools_used,
            "memory_used": memory_used
        }
    
    async def _create_context(
        self,
        user_id: str,
        conversation_id: Optional[str],
        message: str,
        extra_context: Optional[Dict[str, Any]]
    ) -> AgentContext:
        """
        Создание контекста для запроса
        
        Args:
            user_id: ID пользователя
            conversation_id: ID диалога
            message: Сообщение пользователя
            extra_context: Дополнительный контекст
        
        Returns:
            AgentContext с полной информацией
        """
        # Получение памяти пользователя
        memory = await self.memory_manager.recall(
            user_id=user_id,
            query=message,
            n_results=5
        )
        
        # Создание контекста
        context = AgentContext(
            user_id=user_id,
            conversation_id=conversation_id or f"conv_{datetime.now().timestamp()}",
            timestamp=datetime.now(),
            user_message=message,
            memory_context=memory,
            extra_context=extra_context or {}
        )
        
        return context
    
    def _build_full_prompt(self, context: AgentContext, message: str) -> str:
        """
        Построение полного промпта для LLM
        
        Args:
            context: Контекст запроса
            message: Сообщение пользователя
        
        Returns:
            Полный промпт для LLM
        """
        prompt_parts = [
            self._system_prompt,
            "\n\n--- CONTEXT ---\n",
            f"Conversation ID: {context.conversation_id}\n",
            f"Timestamp: {context.timestamp.isoformat()}\n",
        ]
        
        # Добавление памяти
        if context.memory_context:
            prompt_parts.append("\n--- MEMORY ---\n")
            for mem in context.memory_context[:5]:
                prompt_parts.append(f"- {mem.get('content', '')}\n")
        
        # Добавление дополнительного контекста
        if context.extra_context:
            prompt_parts.append("\n--- EXTRA CONTEXT ---\n")
            for key, value in context.extra_context.items():
                prompt_parts.append(f"{key}: {value}\n")
        
        prompt_parts.append("\n--- USER MESSAGE ---\n")
        prompt_parts.append(message)
        
        return "".join(prompt_parts)
    
    async def _call_llm(self, prompt: str, user_id: str) -> Dict[str, Any]:
        """
        Вызов LLM для получения ответа
        
        Args:
            prompt: Полный промпт для LLM
            user_id: ID пользователя
        
        Returns:
            Ответ от LLM
        """
        # TODO: Интеграция с реальным LLM клиентом
        # Пока заглушка для разработки
        
        logger.debug(f"Calling LLM for user {user_id}")
        
        # Заглушка - в production заменить на реальный вызов
        return {
            "content": prompt,  # Эхо для тестирования
            "usage": {"prompt_tokens": 100, "completion_tokens": 50}
        }
    
    async def _parse_llm_response(self, llm_response: Dict[str, Any]) -> Dict[str, Any]:
        """
        Парсинг ответа от LLM
        
        Args:
            llm_response: Ответ от LLM
        
        Returns:
            Парсированный ответ с информацией об инструментах
        """
        # TODO: Реализовать парсинг JSON responses от LLM
        # LLM должен возвращать JSON в формате:
        # {"tool_call": {"name": "...", "parameters": {...}}} или
        # {"text": "..."}
        
        content = llm_response.get("content", "")
        
        # Простой эвристики для тестирования
        if "tool_call" in content:
            try:
                parsed = json.loads(content)
                return {
                    "text": None,
                    "tool_call": parsed.get("tool_call")
                }
            except json.JSONDecodeError:
                pass
        
        return {
            "text": content,
            "tool_call": None
        }
    
    async def _execute_tool(
        self,
        tool_name: str,
        parameters: Dict[str, Any],
        user_id: str,
        context: AgentContext
    ) -> Dict[str, Any]:
        """
        Выполнение инструмента
        
        Args:
            tool_name: Название инструмента
            parameters: Параметры инструмента
            user_id: ID пользователя
            context: Контекст запроса
        
        Returns:
            Результат выполнения инструмента
        """
        tool = self.tool_registry.get_tool(tool_name)
        
        if not tool:
            raise ValueError(f"Tool not found: {tool_name}")
        
        # Проверка прав доступа
        if not await tool.check_permissions(user_id, parameters):
            raise PermissionError(f"User {user_id} does not have permission to use {tool_name}")
        
        # Выполнение инструмента
        result = await tool.execute(parameters, context)
        
        return result
    
    async def _finalize_with_tool_result(
        self,
        tool_name: str,
        tool_result: Dict[str, Any],
        original_prompt: str,
        user_id: str
    ) -> str:
        """
        Финализация ответа с результатом инструмента
        
        Args:
            tool_name: Название инструмента
            tool_result: Результат выполнения
            original_prompt: Оригинальный промпт
            user_id: ID пользователя
        
        Returns:
            Финальный текстовый ответ
        """
        # TODO: Отправить результат инструмента обратно в LLM для формирования ответа
        
        # Пока заглушка - форматируем результат инструмента
        result_text = json.dumps(tool_result, indent=2, ensure_ascii=False)
        
        return f"I've executed the {tool_name} tool. Here are the results:\n\n{result_text}"
    
    async def _save_to_memory(
        self,
        user_id: str,
        conversation_id: str,
        message: str,
        response: str
    ) -> bool:
        """
        Сохранение диалога в память
        
        Args:
            user_id: ID пользователя
            conversation_id: ID диалога
            message: Сообщение пользователя
            response: Ответ агента
        
        Returns:
            True если сохранено, False иначе
        """
        # Критически важные факты сохраняем в память
        # TODO: Использовать LLM для определения, что сохранять
        
        try:
            await self.memory_manager.remember(
                user_id=user_id,
                key=f"conversation:{conversation_id}",
                value={
                    "user_message": message,
                    "agent_response": response,
                    "timestamp": datetime.now().isoformat()
                }
            )
            return True
        except Exception as e:
            logger.warning(f"Failed to save to memory: {e}")
            return False
