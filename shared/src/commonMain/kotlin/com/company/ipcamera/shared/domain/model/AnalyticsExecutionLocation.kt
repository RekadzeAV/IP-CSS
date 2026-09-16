package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Заявленное размещение ИИ-конвейера (конфигурация; облачная доставка не реализована в MVP).
 */
@Serializable
enum class AnalyticsExecutionLocation {
    ON_PREM,
    CUSTOMER_SERVER,
    VSAAS,
}
