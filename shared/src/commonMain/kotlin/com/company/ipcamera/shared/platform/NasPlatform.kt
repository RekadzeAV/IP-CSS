package com.company.ipcamera.shared.platform

/**
 * NAS платформы
 */
enum class NasPlatform {
    SYNOLOGY_DSM,
    QNAP_QTS,
    ASUSTOR_ADM,
    TRUENAS_CORE,
    TRUENAS_SCALE,
    UNKNOWN,
}

/**
 * Системные пути для NAS платформы
 */
data class SystemPaths(
    val dataPath: String,
    val recordingsPath: String,
    val logsPath: String,
    val configPath: String,
) {
    companion object {
        fun default(): SystemPaths {
            val base = ".ip-css"
            return SystemPaths(
                dataPath = "$base/data",
                recordingsPath = "$base/recordings",
                logsPath = "$base/logs",
                configPath = "$base/config",
            )
        }
    }
}
