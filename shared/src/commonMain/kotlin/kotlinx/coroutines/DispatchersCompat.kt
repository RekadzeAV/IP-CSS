package kotlinx.coroutines

/**
 * KMP-safe fallback for source sets where Dispatchers.IO is unavailable.
 */
val Dispatchers.IO: CoroutineDispatcher
    get() = Dispatchers.Default
