package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.shared.domain.model.AnalyticsRule

/**
 * Репозиторий для работы с правилами аналитики
 */
interface AnalyticsRuleRepository {
    /**
     * Получить все правила
     */
    suspend fun getAllRules(): List<AnalyticsRule>

    /**
     * Получить правило по ID
     */
    suspend fun getRuleById(id: String): AnalyticsRule?

    /**
     * Получить правила для конкретной камеры
     */
    suspend fun getRulesByCameraId(cameraId: String): List<AnalyticsRule>

    /**
     * Получить активные правила (enabled = true)
     */
    suspend fun getActiveRules(): List<AnalyticsRule>

    /**
     * Получить активные правила для камеры
     */
    suspend fun getActiveRulesByCameraId(cameraId: String): List<AnalyticsRule>

    /**
     * Создать правило
     */
    suspend fun createRule(rule: AnalyticsRule): Result<AnalyticsRule>

    /**
     * Обновить правило
     */
    suspend fun updateRule(rule: AnalyticsRule): Result<AnalyticsRule>

    /**
     * Удалить правило
     */
    suspend fun deleteRule(id: String): Result<Unit>

    /**
     * Включить/выключить правило
     */
    suspend fun setRuleEnabled(
        id: String,
        enabled: Boolean,
    ): Result<Unit>
}
