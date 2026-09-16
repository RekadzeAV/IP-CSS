package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportServiceTest {

    private val service = ExportService()

    private fun recording(id: String, cameraName: String? = "Cam A") = Recording(
        id = id, cameraId = "cam-1", cameraName = cameraName,
        startTime = 1700000000000L, endTime = 1700000060000L, duration = 60000L,
        filePath = "/storage/$id.mp4", fileSize = 123456L,
        format = RecordingFormat.MP4, quality = Quality.HIGH,
        status = RecordingStatus.COMPLETED, createdAt = 1700000070000L
    )

    private fun event(id: String, description: String? = null) = Event(
        id = id, cameraId = "cam-1", type = EventType.MOTION_DETECTION,
        severity = EventSeverity.WARNING, timestamp = 1700000000000L,
        description = description, acknowledged = false
    )

    private fun camera(id: String) = Camera(
        id = id, name = "Front $id", url = "rtsp://cam/$id",
        username = "admin", createdAt = 1700000000000L, updatedAt = 1700000001000L
    )

    // ---------- Recordings CSV ----------

    @Test
    fun `recordings csv has header and row values`() {
        val csv = service.exportRecordingsToCsv(listOf(recording("r1")))
        assertTrue(csv.startsWith("ID,Camera ID,Camera Name"))
        assertTrue(csv.contains("\"r1\""))
        assertTrue(csv.contains("\"cam-1\""))
        assertTrue(csv.contains("MP4"))
        assertTrue(csv.contains("COMPLETED"))
    }

    @Test
    fun `recordings csv handles null cameraName and endTime`() {
        val r = recording("r2").copy(cameraName = null, endTime = null, filePath = null, fileSize = null)
        val csv = service.exportRecordingsToCsv(listOf(r))
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        // cameraName=null & endTime=null -> пустые кавычки ""; filePath=null -> ""; fileSize=null -> голое
        assertTrue(lines[1].startsWith("\"r2\",\"cam-1\",\"\","), "row: " + lines[1])
        assertTrue(lines[1].contains("60000,\"\",,"), "fileSize null должен давать пустое поле: " + lines[1])
    }

    // ---------- Recordings JSON ----------

    @Test
    fun `recordings json structure`() {
        val json = service.exportRecordingsToJson(listOf(recording("r1")))
        val parsed = Json.parseToJsonElement(json).jsonObject
        assertEquals(1, parsed["total"]!!.jsonPrimitive.int)
        val arr = parsed["recordings"]!!.jsonArray
        assertEquals(1, arr.size)
        assertEquals("r1", arr[0].jsonObject["id"]!!.jsonPrimitive.content)
        // null-поля сериализуются как null (не строка "null")
        assertTrue(json.contains("\"cameraName\": \"Cam A\""))
    }

    @Test
    fun `recordings json null fields`() {
        val r = recording("r3").copy(cameraName = null, thumbnailUrl = null)
        val json = service.exportRecordingsToJson(listOf(r))
        assertTrue(json.contains("\"cameraName\": null"))
    }

    // ---------- Events JSON ----------

    @Test
    fun `events json escapes quotes in description`() {
        val json = service.exportEventsToJson(listOf(event("e1", description = "said \"hello\"")))
        assertTrue(json.contains("\\\"hello\\\""), "кавычки должны быть экранированы")
        val parsed = Json.parseToJsonElement(json).jsonObject
        assertEquals(1, parsed["total"]!!.jsonPrimitive.int)
    }

    // ---------- Cameras CSV/JSON ----------

    @Test
    fun `cameras csv escapes quotes in name`() {
        val cam = camera("c1").copy(name = """Cam "X", Ltd""")
        val csv = service.exportCamerasToCsv(listOf(cam))
        assertTrue(csv.contains("\"Cam \"\"X\"\", Ltd\""), "кавычки удваиваются по CSV-правилам")
    }

    @Test
    fun `cameras json structure`() {
        val json = service.exportCamerasToJson(listOf(camera("c1")))
        val parsed = Json.parseToJsonElement(json).jsonObject
        assertEquals(1, parsed["total"]!!.jsonPrimitive.int)
        val obj = parsed["cameras"]!!.jsonArray[0].jsonObject
        assertEquals("c1", obj["id"]!!.jsonPrimitive.content)
        assertEquals("admin", obj["username"]!!.jsonPrimitive.content)
    }

    @Test
    fun `empty lists produce headers only`() {
        assertTrue(service.exportRecordingsToCsv(emptyList()).contains("ID,Camera ID"))
        assertTrue(service.exportCamerasToCsv(emptyList()).contains("ID,Name,URL"))
        val emptyJson = Json.parseToJsonElement(service.exportRecordingsToJson(emptyList())).jsonObject
        assertEquals(0, emptyJson["total"]!!.jsonPrimitive.int)
    }

    private val Json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
}