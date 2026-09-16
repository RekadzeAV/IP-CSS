package com.company.ipcamera.server.service

/**
 * Сервис генерации аналитических отчётов (блок 8.4).
 * Экспорт в CSV, PDF и др. по типам отчётов (события, записи, ANPR, сводка).
 */
interface ReportService {

    enum class ReportType {
        EVENTS_SUMMARY,
        RECORDINGS_SUMMARY,
        LICENSE_PLATES_SUMMARY,
        ANALYTICS_DASHBOARD
    }

    enum class ExportFormat {
        CSV,
        PDF
    }

    /**
     * Сгенерировать отчёт и вернуть содержимое файла.
     * @param type тип отчёта
     * @param format формат экспорта
     * @param cameraId опционально — фильтр по камере
     * @param fromTimestamp начало периода (мс)
     * @param toTimestamp конец периода (мс)
     */
    suspend fun generateReport(
        type: ReportType,
        format: ExportFormat,
        cameraId: String? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null
    ): Result<ByteArray>

    /** Рекомендуемое расширение файла для формата */
    fun fileExtension(format: ExportFormat): String =
        when (format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.PDF -> "pdf"
        }
}
