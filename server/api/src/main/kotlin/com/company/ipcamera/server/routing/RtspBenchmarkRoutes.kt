package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.RtspBenchmarkRequest
import com.company.ipcamera.server.dto.RtspBenchmarkResponse
import com.company.ipcamera.server.dto.RtspBenchmarkStatus
import com.company.ipcamera.server.service.RtspBenchmarkService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Route.rtspBenchmarkRoutes(benchmarkService: RtspBenchmarkService) {
    route("/api/v1/benchmark") {
        // Запустить бенчмарк
        post("/start") {
            val request = call.receive<RtspBenchmarkRequest>()
            
            try {
                val testId = if (request.multiStream) {
                    benchmarkService.startMultiStreamBenchmark(request)
                    // Возвращаем первый testId для простоты
                    benchmarkService.startBenchmark(request)
                } else {
                    benchmarkService.startBenchmark(request)
                }
                
                call.respond(HttpStatusCode.Accepted, mapOf(
                    "message" to "Benchmark started",
                    "testId" to testId,
                    "status" to "RUNNING"
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Failed to start benchmark",
                    "message" to e.message
                ))
            }
        }
        
        // Получить статус теста
        get("/status/{testId}") {
            val testId = call.parameters["testId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Missing testId parameter"
                ))
                return@get
            }
            
            val status = benchmarkService.getTestStatus(testId)
            if (status == null) {
                call.respond(HttpStatusCode.NotFound, mapOf(
                    "error" to "Test not found",
                    "testId" to testId
                ))
            } else {
                call.respond(HttpStatusCode.OK, mapOf(
                    "testId" to status.id,
                    "status" to status.status,
                    "progress" to status.progress,
                    "message" to status.errorMessage
                ))
            }
        }
        
        // Получить результат теста
        get("/result/{testId}") {
            val testId = call.parameters["testId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Missing testId parameter"
                ))
                return@get
            }
            
            val result = benchmarkService.getTestResult(testId)
            if (result == null) {
                val status = benchmarkService.getTestStatus(testId)
                if (status == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf(
                        "error" to "Test not found",
                        "testId" to testId
                    ))
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "error" to "Test not completed yet",
                        "status" to status.status
                    ))
                }
            } else {
                call.respond(HttpStatusCode.OK, result)
            }
        }
        
        // Отменить тест
        post("/cancel/{testId}") {
            val testId = call.parameters["testId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Missing testId parameter"
                ))
                return@post
            }
            
            val status = benchmarkService.getTestStatus(testId)
            if (status == null) {
                call.respond(HttpStatusCode.NotFound, mapOf(
                    "error" to "Test not found",
                    "testId" to testId
                ))
            } else if (status.status != "RUNNING") {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Test is not running",
                    "currentStatus" to status.status
                ))
            } else {
                benchmarkService.cancelTest(testId)
                call.respond(HttpStatusCode.OK, mapOf(
                    "message" to "Test cancelled",
                    "testId" to testId
                ))
            }
        }
        
        // Получить все активные тесты
        get("/list") {
            val tests = benchmarkService.getAllTests()
            call.respond(HttpStatusCode.OK, mapOf(
                "tests" to tests.map { mapOf(
                    "testId" to it.id,
                    "status" to it.status,
                    "progress" to it.progress,
                    "rtspUrl" to it.request.rtspUrl
                ) }
            ))
        }
        
        // Очистить завершенные тесты
        post("/cleanup") {
            benchmarkService.cleanupOldTests()
            call.respond(HttpStatusCode.OK, mapOf(
                "message" to "Cleanup completed"
            ))
        }
    }
}
