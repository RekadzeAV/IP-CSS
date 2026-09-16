# Services Module
# Сервисы для интеграции с backend

from .KtorClient import (
    KtorClient,
    KtorClientError,
    KtorAuthenticationError,
    KtorNotFoundError,
    KtorRateLimitError,
    get_ktor_client,
    set_ktor_client
)

__all__ = [
    "KtorClient",
    "KtorClientError",
    "KtorAuthenticationError",
    "KtorNotFoundError",
    "KtorRateLimitError",
    "get_ktor_client",
    "set_ktor_client"
]
