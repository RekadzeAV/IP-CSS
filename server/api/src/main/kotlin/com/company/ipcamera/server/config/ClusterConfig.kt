package com.company.ipcamera.server.config

/**
 * Конфигурация кластера серверов (4.2.1).
 */
object ClusterConfig {

    val enabled: Boolean
        get() = System.getenv("CLUSTER_ENABLED")?.lowercase() == "true"

    /** Уникальный идентификатор узла (для LB и координации). */
    val nodeId: String
        get() = System.getenv("NODE_ID") ?: System.getenv("HOSTNAME") ?: "node-${java.util.UUID.randomUUID().toString().take(8)}"

    /** Публичный URL узла (для регистрации в discovery). */
    val nodeUrl: String?
        get() = System.getenv("NODE_URL")?.takeIf { it.isNotBlank() }

    /** Интервал heartbeat в секундах (регистрация в Redis). */
    val heartbeatIntervalSec: Long
        get() = System.getenv("CLUSTER_HEARTBEAT_SEC")?.toLongOrNull()?.coerceIn(5, 120) ?: 30L

    /** TTL записи узла в Redis (секунды). */
    val nodeTtlSec: Long
        get() = heartbeatIntervalSec * 3

    /** Включить сжатие данных при репликации (GZIP). */
    val compressionEnabled: Boolean
        get() = System.getenv("CLUSTER_COMPRESSION_ENABLED")?.lowercase() == "true"

    // ========== Load Balancer ==========

    /** Тип балансировщика: haproxy | nginx | none */
    val loadBalancerType: String
        get() = System.getenv("LB_TYPE")?.takeIf { it.isNotBlank() } ?: "none"

    /** URL health check endpoint (проверяется балансировщиком). */
    val healthCheckUrl: String
        get() = System.getenv("LB_HEALTH_CHECK_URL") ?: "/api/v1/health"

    /** Интервал health check (секунды). */
    val healthCheckIntervalSec: Int
        get() = System.getenv("LB_HEALTH_CHECK_INTERVAL")?.toIntOrNull()?.coerceIn(1, 60) ?: 10

    /** Таймаут health check (секунды). */
    val healthCheckTimeoutSec: Int
        get() = System.getenv("LB_HEALTH_CHECK_TIMEOUT")?.toIntOrNull()?.coerceIn(1, 30) ?: 5

    /** Количество неудачных проверок для пометки узла как unhealthy. */
    val healthCheckUnhealthyThreshold: Int
        get() = System.getenv("LB_UNHEALTHY_THRESHOLD")?.toIntOrNull()?.coerceIn(1, 10) ?: 3

    /** Включить session affinity (sticky sessions). */
    val sessionAffinityEnabled: Boolean
        get() = System.getenv("LB_SESSION_AFFINITY")?.lowercase() == "true"

    /** Включить auto-scaling (требует Kubernetes). */
    val autoScalingEnabled: Boolean
        get() = System.getenv("LB_AUTO_SCALING")?.lowercase() == "true"

    /** Минимальное количество реплик при auto-scaling. */
    val autoScalingMinReplicas: Int
        get() = System.getenv("LB_AUTO_SCALING_MIN")?.toIntOrNull()?.coerceIn(1, 100) ?: 2

    /** Максимальное количество реплик при auto-scaling. */
    val autoScalingMaxReplicas: Int
        get() = System.getenv("LB_AUTO_SCALING_MAX")?.toIntOrNull()?.coerceIn(1, 100) ?: 10

    /** Целевое использование CPU для auto-scaling (проценты). */
    val autoScalingTargetCpu: Int
        get() = System.getenv("LB_AUTO_SCALING_CPU")?.toIntOrNull()?.coerceIn(10, 90) ?: 70
}
