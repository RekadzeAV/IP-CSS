package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.AnalyticsRule
import com.company.ipcamera.shared.domain.repository.AnalyticsRuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * In-memory реализация репозитория правил аналитики
 *
 * В будущем можно заменить на реализацию с использованием БД
 */
class AnalyticsRuleRepositoryImpl : AnalyticsRuleRepository {
    private val rules = mutableMapOf<String, AnalyticsRule>()
    private val mutex = Mutex()

    override suspend fun getAllRules(): List<AnalyticsRule> =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                rules.values.toList()
            }
        }

    override suspend fun getRuleById(id: String): AnalyticsRule? =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                rules[id]
            }
        }

    override suspend fun getRulesByCameraId(cameraId: String): List<AnalyticsRule> =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                rules.values.filter { rule ->
                    rule.cameraId == null || rule.cameraId == cameraId
                }
            }
        }

    override suspend fun getActiveRules(): List<AnalyticsRule> =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                rules.values.filter { it.enabled }
            }
        }

    override suspend fun getActiveRulesByCameraId(cameraId: String): List<AnalyticsRule> =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                rules.values.filter { rule ->
                    rule.enabled && (rule.cameraId == null || rule.cameraId == cameraId)
                }
            }
        }

    override suspend fun createRule(rule: AnalyticsRule): Result<AnalyticsRule> =
        withContext(Dispatchers.Default) {
            try {
                mutex.withLock {
                    if (rules.containsKey(rule.id)) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Rule with id ${rule.id} already exists"),
                        )
                    }
                    rules[rule.id] = rule
                    logger.info { "Created analytics rule: ${rule.id} (${rule.name})" }
                    Result.success(rule)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error creating analytics rule: ${rule.id}" }
                Result.failure(e)
            }
        }

    override suspend fun updateRule(rule: AnalyticsRule): Result<AnalyticsRule> =
        withContext(Dispatchers.Default) {
            try {
                mutex.withLock {
                    if (!rules.containsKey(rule.id)) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Rule with id ${rule.id} not found"),
                        )
                    }
                    val updatedRule = rule.copy(updatedAt = nowMillis())
                    rules[rule.id] = updatedRule
                    logger.info { "Updated analytics rule: ${rule.id} (${rule.name})" }
                    Result.success(updatedRule)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error updating analytics rule: ${rule.id}" }
                Result.failure(e)
            }
        }

    override suspend fun deleteRule(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                mutex.withLock {
                    val removed = rules.remove(id)
                    if (removed != null) {
                        logger.info { "Deleted analytics rule: $id" }
                        Result.success(Unit)
                    } else {
                        Result.failure(IllegalArgumentException("Rule with id $id not found"))
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error deleting analytics rule: $id" }
                Result.failure(e)
            }
        }

    override suspend fun setRuleEnabled(
        id: String,
        enabled: Boolean,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                mutex.withLock {
                    val rule = rules[id]
                    if (rule == null) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Rule with id $id not found"),
                        )
                    }
                    val updatedRule =
                        rule.copy(
                            enabled = enabled,
                            updatedAt = nowMillis(),
                        )
                    rules[id] = updatedRule
                    logger.info { "Set rule $id enabled: $enabled" }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error setting rule enabled: $id" }
                Result.failure(e)
            }
        }
}
