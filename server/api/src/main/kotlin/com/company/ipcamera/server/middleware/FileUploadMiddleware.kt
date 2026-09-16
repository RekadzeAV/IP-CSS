package com.company.ipcamera.server.middleware

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.security.SecurityLogger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация для валидации загружаемых файлов
 */
data class FileUploadConfig(
    val maxFileSize: Long = 100 * 1024 * 1024, // 100 MB по умолчанию
    val allowedMimeTypes: Set<String> = setOf(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
        "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo",
        "application/pdf", "application/zip", "application/json",
        "text/plain", "text/csv"
    ),
    val allowedExtensions: Set<String> = setOf(
        "jpg", "jpeg", "png", "gif", "webp",
        "mp4", "mpeg", "mov", "avi",
        "pdf", "zip", "json", "txt", "csv"
    )
)

/**
 * Middleware для валидации загружаемых файлов
 * Проверяет размер файла, MIME тип и расширение
 */
fun Application.configureFileUploadValidation(config: FileUploadConfig = FileUploadConfig()) {
    intercept(ApplicationCallPipeline.Call) {
        val call: io.ktor.server.application.ApplicationCall = context
        val uploadInfo = extractUploadInfo(call, config)
        
        if (uploadInfo != null) {
            val rejection = validateUpload(uploadInfo)
            if (rejection != null) {
                respondUploadRejection(call, rejection)
                return@intercept finish()
            }
        }
    }
}

private data class UploadInfo(
    val call: io.ktor.server.application.ApplicationCall,
    val config: FileUploadConfig,
    val contentLength: Long?,
    val ipAddress: String,
    val mimeType: String,
    val fileName: String
)

private fun extractUploadInfo(call: io.ktor.server.application.ApplicationCall, config: FileUploadConfig): UploadInfo? {
    if (!isUploadRequest(call)) return null
    
    val contentLength = call.request.contentLength()
    val ipAddress = call.request.local.remoteHost
    val mimeType = call.request.contentType().toString()
    val fileName = extractFileName(call.request)
    
    return UploadInfo(call, config, contentLength, ipAddress, mimeType, fileName)
}

private fun isUploadRequest(call: io.ktor.server.application.ApplicationCall): Boolean {
    val contentType = call.request.contentType()
    val isFileUpload = contentType.match(ContentType.MultiPart.FormData) ||
                       contentType.match(ContentType.Application.OctetStream) ||
                       call.request.headers["Content-Disposition"] != null
    
    return isFileUpload && (call.request.httpMethod == HttpMethod.Post || call.request.httpMethod == HttpMethod.Put)
}

private fun extractFileName(request: io.ktor.server.request.ApplicationRequest): String {
    val contentDisposition = request.headers["Content-Disposition"] ?: return ""
    val fileNameMatch = Regex("filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)").find(contentDisposition)
    return fileNameMatch?.groupValues?.get(1)?.trim('"', '\'') ?: ""
}

private fun validateUpload(info: UploadInfo): UploadRejection? {
    validateFileSize(info)?.let { return it }
    validateMimeType(info)?.let { return it }
    validateFileExtension(info)?.let { return it }
    return null
}

private fun validateFileSize(info: UploadInfo): UploadRejection? {
    val contentLength = info.contentLength ?: return null
    if (contentLength <= info.config.maxFileSize) return null
    
    return UploadRejection(
        status = HttpStatusCode.PayloadTooLarge,
        message = "File size exceeds maximum allowed size of ${info.config.maxFileSize / (1024 * 1024)}MB",
        reason = "file_size_exceeded",
        fileName = null,
        fileSize = contentLength,
        maxSize = info.config.maxFileSize
    )
}

private fun validateMimeType(info: UploadInfo): UploadRejection? {
    val mimeType = info.mimeType
    if (mimeType.isEmpty() || mimeType.startsWith("multipart/") || info.config.allowedMimeTypes.contains(mimeType)) {
        return null
    }
    
    return UploadRejection(
        status = HttpStatusCode.UnsupportedMediaType,
        message = "File type not allowed: $mimeType",
        reason = "mime_type_not_allowed",
        fileName = null,
        fileSize = info.contentLength,
        maxSize = null
    )
}

private fun validateFileExtension(info: UploadInfo): UploadRejection? {
    val fileName = info.fileName
    if (fileName.isEmpty()) return null
    
    val fileExtension = fileName.substringAfterLast('.', "").lowercase()
    if (fileExtension.isEmpty() || info.config.allowedExtensions.contains(fileExtension)) {
        return null
    }
    
    return UploadRejection(
        status = HttpStatusCode.UnsupportedMediaType,
        message = "File extension not allowed: .$fileExtension",
        reason = "file_extension_not_allowed",
        fileName = fileName,
        fileSize = info.contentLength,
        maxSize = null
    )
}

private data class UploadRejection(
    val status: HttpStatusCode,
    val message: String,
    val reason: String,
    val fileName: String?,
    val fileSize: Long?,
    val maxSize: Long?
)

private suspend fun respondUploadRejection(
    call: io.ktor.server.application.ApplicationCall,
    rejection: UploadRejection
) {
    logger.warn {
        "File upload rejected: ${rejection.reason} from ${call.request.local.remoteHost}" +
        rejection.fileName?.let { " file=$it" } ?: ""
    }
    
    SecurityLogger.logFileUploadRejected(
        ipAddress = call.request.local.remoteHost,
        reason = rejection.reason,
        fileName = rejection.fileName,
        fileSize = rejection.fileSize,
        maxSize = rejection.maxSize
    )
    
    call.respond(
        rejection.status,
        ApiResponse<Unit>(success = false, data = null, message = rejection.message)
    )
}
