"""
AI Agent Configuration Settings
Загрузка настроек из .env файла
"""

from pydantic_settings import BaseSettings
from pydantic import SecretStr
from typing import Optional


class Settings(BaseSettings):
    """Настройки AI-агента"""
    
    # ============================================
    # LLM Providers
    # ============================================
    LLM_PROVIDER_PRIMARY: str = "lm-studio"
    LLM_PRIMARY_BASE_URL: str = "http://localhost:1234/v1"
    LLM_PRIMARY_MODEL: str = "qwen/qwen2.5-coder-14b"
    LLM_PRIMARY_API_KEY: str = "lm-studio"
    
    LLM_PROVIDER_FALLBACK: str = "ollama"
    LLM_FALLBACK_BASE_URL: str = "http://localhost:11434"
    LLM_FALLBACK_MODEL: str = "llama3.1:8b"
    LLM_FALLBACK_API_KEY: str = "ollama"
    
    # Embeddings
    EMBEDDING_PROVIDER: str = "lm-studio"
    EMBEDDING_BASE_URL: str = "http://localhost:1234/v1"
    EMBEDDING_MODEL: str = "text-embedding-nomic-embed-text-v1.5"
    
    # LLM Parameters
    LLM_TEMPERATURE: float = 0.7
    LLM_MAX_TOKENS: int = 4096
    LLM_TOP_P: float = 0.9
    
    # ============================================
    # ChromaDB
    # ============================================
    CHROMA_HOST: str = "localhost"
    CHROMA_PORT: int = 8000
    CHROMA_PASSWORD: SecretStr
    CHROMA_COLLECTION: str = "agent_memory"
    
    # ============================================
    # Redis (AI-specific)
    # ============================================
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6380
    REDIS_PASSWORD: SecretStr
    REDIS_DB: int = 0
    
    # ============================================
    # Agent
    # ============================================
    AGENT_NAME: str = "IP-CSS Agent"
    AGENT_SYSTEM_PROMPT: str = """You are a helpful AI assistant for IP-CSS surveillance system. 
    You can manage cameras, search events, and help with recordings.
    Always be concise and helpful."""
    
    # ============================================
    # Security — секреты берутся только из .env (см. ai-agent/.env.example).
    # Безопасных дефолтов НЕТ: отсутствие значения = ошибка запуска (request away).
    # ============================================
    JWT_SECRET: SecretStr
    ENABLE_AUTH: bool = True
    # Учётные данные для демо-логина /api/v1/auth/login (берутся из .env, не из кода!)
    ADMIN_USERNAME: str = "admin"
    ADMIN_PASSWORD: SecretStr
    
    # ============================================
    # Logging
    # ============================================
    LOG_LEVEL: str = "INFO"
    LOG_FILE: str = "logs/agent.log"
    
    # ============================================
    # Memory
    # ============================================
    MEMORY_MAX_TOKENS: int = 8000
    MEMORY_COMPRESSION_THRESHOLD: int = 4000
    
    # ============================================
    # Server
    # ============================================
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    DEBUG: bool = True
    
    # ============================================
    # Ktor Backend Integration
    # ============================================
    KTOR_BACKEND_URL: str = "http://localhost:8080"
    KTOR_API_TIMEOUT: float = 30.0
    KTOR_MAX_RETRIES: int = 3
    KTOR_ENABLE_CACHE: bool = True
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


# Глобальный экземпляр настроек
settings = Settings()


def get_settings() -> Settings:
    """Получить настройки"""
    return settings
