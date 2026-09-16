package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.LicensePlateRepository
import com.company.ipcamera.shared.domain.repository.RecordingRepository

/**
 * Реализация ReportService (блок 8.4).
 * Текущая версия поддерживает генерацию CSV и базовый текстовый fallback для PDF.
 */
class ReportServiceImpl(
    private val eventRepository: EventRepository,
    private val recordingRepository: RecordingRepository,
    private val licensePlateRepository: LicensePlateRepository
) : ReportService {

    override suspend fun generateReport(
        type: ReportService.ReportType,
        format: ReportService.ExportFormat,
        cameraId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): Result<ByteArray> {
        return runCatching {
            val csv = when (type) {
                ReportService.ReportType.EVENTS_SUMMARY ->
                    generateEventsSummaryCsv(cameraId, fromTimestamp, toTimestamp)

                ReportService.ReportType.RECORDINGS_SUMMARY ->
                    generateRecordingsSummaryCsv(cameraId, fromTimestamp, toTimestamp)

                ReportService.ReportType.LICENSE_PLATES_SUMMARY ->
                    generateLicensePlatesSummaryCsv(cameraId, fromTimestamp, toTimestamp)

                ReportService.ReportType.ANALYTICS_DASHBOARD ->
                    generateAnalyticsDashboardCsv(cameraId, fromTimestamp, toTimestamp)
            }

            when (format) {
                ReportService.ExportFormat.CSV -> csv.encodeToByteArray()
                ReportService.ExportFormat.PDF -> generateSimplePdf(csv)
            }
        }
    }

    private suspend fun generateEventsSummaryCsv(
        cameraId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): String {
        val events = fetchAllEvents(cameraId, fromTimestamp, toTimestamp)
        val meta = buildMetadataLines("EVENTS_SUMMARY", cameraId, fromTimestamp, toTimestamp)
        val summary = listOf(
            "# summary,total_events,${events.size}",
            "# summary,acknowledged_events,${events.count { it.acknowledged }}",
            "# summary,critical_events,${events.count { it.severity.name == "CRITICAL" }}"
        )
        val byType = events.groupingBy { it.type.name }.eachCount().entries
            .sortedByDescending { it.value }
            .map { "# by_type,${it.key},${it.value}" }
        val header = "id,camera_id,type,severity,timestamp,acknowledged,description"
        val rows = events.joinToString("\n") { event ->
            listOf(
                escapeCsv(event.id),
                escapeCsv(event.cameraId),
                escapeCsv(event.type.name),
                escapeCsv(event.severity.name),
                escapeCsv(event.timestamp.toString()),
                escapeCsv(event.acknowledged.toString()),
                escapeCsv(event.description ?: "")
            ).joinToString(",")
        }
        return (meta + summary + byType + listOf(header) + listOf(rows))
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private suspend fun generateRecordingsSummaryCsv(
        cameraId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): String {
        val recordings = fetchAllRecordings(cameraId, fromTimestamp, toTimestamp)
        val meta = buildMetadataLines("RECORDINGS_SUMMARY", cameraId, fromTimestamp, toTimestamp)
        val summary = listOf(
            "# summary,total_recordings,${recordings.size}",
            "# summary,active_recordings,${recordings.count { it.isActive() }}",
            "# summary,completed_recordings,${recordings.count { it.isCompleted() }}",
            "# summary,total_size_bytes,${recordings.sumOf { it.fileSize ?: 0L }}"
        )
        val header = "id,camera_id,start_time,end_time,duration,status,format,quality,file_size"
        val rows = recordings.joinToString("\n") { recording ->
            listOf(
                escapeCsv(recording.id),
                escapeCsv(recording.cameraId),
                escapeCsv(recording.startTime.toString()),
                escapeCsv(recording.endTime?.toString() ?: ""),
                escapeCsv(recording.duration.toString()),
                escapeCsv(recording.status.name),
                escapeCsv(recording.format.name),
                escapeCsv(recording.quality.name),
                escapeCsv(recording.fileSize?.toString() ?: "")
            ).joinToString(",")
        }
        return (meta + summary + listOf(header) + listOf(rows))
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private suspend fun generateLicensePlatesSummaryCsv(
        cameraId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): String {
        require(!cameraId.isNullOrBlank()) {
            "cameraId is required for LICENSE_PLATES_SUMMARY report"
        }

        val plates = if (fromTimestamp != null && toTimestamp != null) {
            licensePlateRepository.getByCameraIdAndDateRange(cameraId, fromTimestamp, toTimestamp)
        } else {
            licensePlateRepository.getByCameraId(cameraId, limit = 5000)
        }
        val meta = buildMetadataLines("LICENSE_PLATES_SUMMARY", cameraId, fromTimestamp, toTimestamp)
        val summary = listOf(
            "# summary,total_plates,${plates.size}",
            "# summary,unique_plates,${plates.map { it.plateNumber }.distinct().size}",
            "# summary,avg_confidence,${if (plates.isEmpty()) 0.0f else plates.map { it.confidence }.average()}"
        )

        val header = "id,camera_id,timestamp,plate_number,confidence,country,bbox_x,bbox_y,bbox_width,bbox_height"
        val rows = plates.joinToString("\n") { plate ->
            listOf(
                escapeCsv(plate.id),
                escapeCsv(plate.cameraId),
                escapeCsv(plate.timestamp.toString()),
                escapeCsv(plate.plateNumber),
                escapeCsv(plate.confidence.toString()),
                escapeCsv(plate.country ?: ""),
                escapeCsv(plate.bboxX.toString()),
                escapeCsv(plate.bboxY.toString()),
                escapeCsv(plate.bboxWidth.toString()),
                escapeCsv(plate.bboxHeight.toString())
            ).joinToString(",")
        }
        return (meta + summary + listOf(header) + listOf(rows))
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private suspend fun generateAnalyticsDashboardCsv(
        cameraId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): String {
        val events = fetchAllEvents(cameraId, fromTimestamp, toTimestamp)
        val recordings = fetchAllRecordings(cameraId, fromTimestamp, toTimestamp)
        val platesCount = if (!cameraId.isNullOrBlank()) {
            val plates = if (fromTimestamp != null && toTimestamp != null) {
                licensePlateRepository.getByCameraIdAndDateRange(cameraId, fromTimestamp, toTimestamp)
            } else {
                licensePlateRepository.getByCameraId(cameraId, limit = 5000)
            }
            plates.size
        } else {
            0
        }

        val criticalEvents = events.count { it.type == EventType.SYSTEM_ERROR || it.severity.name == "CRITICAL" }
        val activeRecordings = recordings.count { it.isActive() }

        return buildString {
            appendLine("# report,ANALYTICS_DASHBOARD")
            appendLine("# generated_at,${System.currentTimeMillis()}")
            appendLine("# camera_id,${cameraId ?: "all"}")
            appendLine("# from,${fromTimestamp ?: ""}")
            appendLine("# to,${toTimestamp ?: ""}")
            appendLine("metric,value")
            appendLine("events_total,${events.size}")
            appendLine("events_critical,${criticalEvents}")
            appendLine("recordings_total,${recordings.size}")
            appendLine("recordings_active,${activeRecordings}")
            appendLine("license_plates_total,${platesCount}")
        }.trim()
    }

    private fun buildMetadataLines(
        reportType: String,
        cameraId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): List<String> {
        return listOf(
            "# report,$reportType",
            "# generated_at,${System.currentTimeMillis()}",
            "# camera_id,${cameraId ?: "all"}",
            "# from,${fromTimestamp ?: ""}",
            "# to,${toTimestamp ?: ""}"
        )
    }

    private suspend fun fetchAllEvents(
        cameraId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ) = buildList {
        var page = 1
        val limit = 200
        while (true) {
            val result = eventRepository.getEvents(
                cameraId = cameraId,
                startTime = fromTimestamp,
                endTime = toTimestamp,
                page = page,
                limit = limit
            )
            addAll(result.items)
            if (!result.hasMore || result.items.isEmpty()) break
            page++
        }
    }

    private suspend fun fetchAllRecordings(
        cameraId: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ) = buildList {
        var page = 1
        val limit = 200
        while (true) {
            val result = recordingRepository.getRecordings(
                cameraId = cameraId,
                startTime = fromTimestamp,
                endTime = toTimestamp,
                page = page,
                limit = limit
            )
            addAll(result.items)
            if (!result.hasMore || result.items.isEmpty()) break
            page++
        }
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(',') || escaped.contains('"') || escaped.contains('\n')) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    /**
     * Минимальный одностраничный PDF-генератор для текстового содержимого отчёта.
     * Используется без внешних зависимостей до внедрения полноценного PDF-рендера.
     */
    private fun generateSimplePdf(text: String): ByteArray {
        val safeLines = text
            .lineSequence()
            .take(120)
            .map { escapePdfText(it).take(140) }
            .toList()
            .ifEmpty { listOf("Report is empty") }

        val content = buildString {
            append("BT\n/F1 10 Tf\n50 800 Td\n")
            safeLines.forEachIndexed { index, line ->
                if (index == 0) {
                    append("(${line}) Tj\n")
                } else {
                    append("0 -12 Td\n(${line}) Tj\n")
                }
            }
            append("ET")
        }

        val objects = mutableListOf<String>()
        objects += "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
        objects += "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
        objects += "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n"
        objects += "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"
        objects += "5 0 obj\n<< /Length ${content.toByteArray(Charsets.UTF_8).size} >>\nstream\n$content\nendstream\nendobj\n"

        val pdf = StringBuilder()
        pdf.append("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        objects.forEach { obj ->
            offsets += pdf.toString().toByteArray(Charsets.UTF_8).size
            pdf.append(obj)
        }

        val xrefOffset = pdf.toString().toByteArray(Charsets.UTF_8).size
        pdf.append("xref\n0 ${objects.size + 1}\n")
        pdf.append("0000000000 65535 f \n")
        offsets.forEach { offset ->
            pdf.append(offset.toString().padStart(10, '0')).append(" 00000 n \n")
        }
        pdf.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        pdf.append("startxref\n$xrefOffset\n%%EOF")

        return pdf.toString().toByteArray(Charsets.UTF_8)
    }

    private fun escapePdfText(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
    }
}
