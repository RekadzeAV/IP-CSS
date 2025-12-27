package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import mu.KotlinLogging
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Сервис для работы с FFmpeg
 *
 * Предоставляет функциональность для:
 * - Кодирования видео потоков в различные форматы
 * - Генерации thumbnail'ов
 * - Конвертации видео
 */
class FfmpegService {

    /**
     * Проверка доступности FFmpeg
     */
    fun isAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("ffmpeg", "-version")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor(5, TimeUnit.SECONDS)
            process.destroy()
            exitCode == 0
        } catch (e: Exception) {
            logger.debug(e) { "FFmpeg not available" }
            false
        }
    }

    /**
     * Кодирование RTSP потока в файл
     *
     * @param rtspUrl URL RTSP потока
     * @param outputFile Выходной файл
     * @param format Формат записи
     * @param quality Качество записи
     * @param duration Длительность записи в секундах (null = бесконечная)
     * @param username Имя пользователя для RTSP (опционально)
     * @param password Пароль для RTSP (опционально)
     * @return Process процесс FFmpeg
     */
    fun encodeRtspToFile(
        rtspUrl: String,
        outputFile: File,
        format: RecordingFormat,
        quality: Quality,
        duration: Long? = null,
        username: String? = null,
        password: String? = null
    ): Process {
        val args = mutableListOf<String>()

        // FFmpeg команда
        args.add("ffmpeg")

        // Оптимизация производительности
        // Использование всех доступных CPU ядер
        args.add("-threads")
        args.add("0") // 0 = автоматическое определение количества потоков

        // Отключение логирования для снижения нагрузки
        args.add("-loglevel")
        args.add("error") // Только ошибки

        // RTSP опции
        args.add("-rtsp_transport")
        args.add("tcp") // Используем TCP для стабильности

        // Оптимизация буферизации
        args.add("-fflags")
        args.add("nobuffer") // Минимальная буферизация для низкой задержки

        args.add("-i")
        // Добавляем credentials в URL если они есть
        val inputUrl = if (username != null && password != null) {
            val url = rtspUrl.replace("rtsp://", "")
            val hostAndPath = if (url.contains("/")) {
                val parts = url.split("/", limit = 2)
                "${parts[0]}/${parts[1]}"
            } else {
                url
            }
            "rtsp://$username:$password@$hostAndPath"
        } else {
            rtspUrl
        }
        args.add(inputUrl)

        // Длительность записи
        if (duration != null) {
            args.add("-t")
            args.add(duration.toString())
        }

        // Кодеки и качество
        when (format) {
            RecordingFormat.MP4 -> {
                args.add("-c:v")
                // Попытка использовать hardware acceleration
                val videoCodec = detectHardwareAcceleration()
                args.add(videoCodec)

                // Оптимизация для H.264
                if (videoCodec == "libx264") {
                    args.add("-preset")
                    args.add("fast") // Быстрее чем medium, но немного больше размер
                    args.add("-tune")
                    args.add("zerolatency") // Минимальная задержка
                    args.add("-profile:v")
                    args.add("baseline") // Совместимость и скорость
                }

                args.add("-c:a")
                args.add("aac") // AAC аудио кодек
                args.add("-movflags")
                args.add("+faststart") // Для веб-воспроизведения
            }
            RecordingFormat.MKV -> {
                args.add("-c:v")
                val videoCodec = detectHardwareAcceleration()
                args.add(videoCodec)
                if (videoCodec == "libx264") {
                    args.add("-preset")
                    args.add("fast")
                    args.add("-tune")
                    args.add("zerolatency")
                }
                args.add("-c:a")
                args.add("aac")
            }
            RecordingFormat.AVI -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("libmp3lame")
            }
            RecordingFormat.MOV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("aac")
                args.add("-movflags")
                args.add("+faststart")
            }
            RecordingFormat.FLV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("libmp3lame")
            }
        }

        // Настройки качества
        when (quality) {
            Quality.LOW -> {
                args.add("-crf")
                args.add("28") // Низкое качество, меньший размер
                args.add("-b:v")
                args.add("500k")
            }
            Quality.MEDIUM -> {
                args.add("-crf")
                args.add("23") // Среднее качество
                args.add("-b:v")
                args.add("2000k")
            }
            Quality.HIGH -> {
                args.add("-crf")
                args.add("20") // Высокое качество
                args.add("-b:v")
                args.add("4000k")
            }
            Quality.ULTRA -> {
                args.add("-crf")
                args.add("18") // Максимальное качество
                args.add("-b:v")
                args.add("8000k")
            }
        }

        // Аудио опции (оптимизированные)
        args.add("-ar")
        args.add("44100") // Частота дискретизации
        args.add("-ac")
        args.add("2") // Стерео
        args.add("-b:a")
        args.add("128k") // Битрейт аудио (баланс качества и размера)
        args.add("-acodec")
        args.add("aac") // Явное указание AAC кодек

        // Выходной файл
        args.add("-y") // Перезаписать файл если существует
        args.add(outputFile.absolutePath)

        logger.info { "FFmpeg command: ${args.joinToString(" ")}" }

        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .directory(outputFile.parentFile)
            .start()

        return process
    }

    /**
     * Генерация thumbnail из видео файла
     *
     * @param videoFile Входной видео файл
     * @param thumbnailFile Выходной файл thumbnail
     * @param timeOffset Смещение времени в секундах (по умолчанию 1 секунда)
     * @param width Ширина thumbnail (по умолчанию 320)
     * @return true если успешно
     */
    fun generateThumbnail(
        videoFile: File,
        thumbnailFile: File,
        timeOffset: Double = 1.0,
        width: Int = 320
    ): Boolean {
        if (!videoFile.exists()) {
            logger.error { "Video file not found: ${videoFile.absolutePath}" }
            return false
        }

        // Создаем директорию для thumbnail если не существует
        thumbnailFile.parentFile?.mkdirs()

        val args = listOf(
            "ffmpeg",
            "-i", videoFile.absolutePath,
            "-ss", timeOffset.toString(),
            "-vframes", "1",
            "-vf", "scale=$width:-1",
            "-y",
            thumbnailFile.absolutePath
        )

        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor(30, TimeUnit.SECONDS)

            if (exitCode == 0 && thumbnailFile.exists()) {
                logger.info { "Thumbnail generated: ${thumbnailFile.absolutePath}" }
                true
            } else {
                logger.warn { "FFmpeg failed to generate thumbnail (exit code: $exitCode)" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error generating thumbnail" }
            false
        }
    }

    /**
     * Получить информацию о видео файле
     *
     * @param videoFile Видео файл
     * @return Map с информацией о видео или null при ошибке
     */
    fun getVideoInfo(videoFile: File): Map<String, String>? {
        if (!videoFile.exists()) {
            return null
        }

        val args = listOf(
            "ffprobe",
            "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            videoFile.absolutePath
        )

        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor(10, TimeUnit.SECONDS)

            if (exitCode == 0) {
                // Парсим JSON (упрощенная версия)
                // В продакшене лучше использовать JSON библиотеку
                val info = mutableMapOf<String, String>()

                // Извлекаем базовую информацию
                val durationMatch = Regex("\"duration\":\\s*\"([^\"]+)\"").find(output)
                durationMatch?.let {
                    info["duration"] = it.groupValues[1]
                }

                val sizeMatch = Regex("\"size\":\\s*\"([^\"]+)\"").find(output)
                sizeMatch?.let {
                    info["size"] = it.groupValues[1]
                }

                val widthMatch = Regex("\"width\":\\s*(\\d+)").find(output)
                widthMatch?.let {
                    info["width"] = it.groupValues[1]
                }

                val heightMatch = Regex("\"height\":\\s*(\\d+)").find(output)
                heightMatch?.let {
                    info["height"] = it.groupValues[1]
                }

                info
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting video info" }
            null
        }
    }

    /**
     * Конвертация видео в другой формат
     *
     * @param inputFile Входной файл
     * @param outputFile Выходной файл
     * @param outputFormat Целевой формат
     * @return true если успешно
     */
    fun convertVideo(
        inputFile: File,
        outputFile: File,
        outputFormat: RecordingFormat
    ): Boolean {
        if (!inputFile.exists()) {
            return false
        }

        outputFile.parentFile?.mkdirs()

        val args = mutableListOf<String>(
            "ffmpeg",
            "-i", inputFile.absolutePath
        )

        // Настройки кодеков в зависимости от формата
        when (outputFormat) {
            RecordingFormat.MP4 -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("aac")
                args.add("-movflags")
                args.add("+faststart")
            }
            RecordingFormat.MKV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("aac")
            }
            RecordingFormat.AVI -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("libmp3lame")
            }
            RecordingFormat.MOV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("aac")
            }
            RecordingFormat.FLV -> {
                args.add("-c:v")
                args.add("libx264")
                args.add("-c:a")
                args.add("libmp3lame")
            }
        }

        args.add("-y")
        args.add(outputFile.absolutePath)

        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor(300, TimeUnit.SECONDS) // 5 минут максимум

            exitCode == 0 && outputFile.exists()
        } catch (e: Exception) {
            logger.error(e) { "Error converting video" }
            false
        }
    }

    /**
     * Мультиплексирование видео и аудио потоков в один файл
     *
     * @param videoFile Временный файл с видеопотоком
     * @param audioFile Временный файл с аудиопотоком
     * @param outputFile Выходной файл
     * @param format Формат записи
     * @param quality Качество записи
     * @return true если успешно
     */
    fun muxVideoAndAudio(
        videoFile: File,
        audioFile: File,
        outputFile: File,
        format: RecordingFormat,
        quality: Quality
    ): Boolean {
        if (!videoFile.exists()) {
            logger.error { "Video file not found: ${videoFile.absolutePath}" }
            return false
        }

        outputFile.parentFile?.mkdirs()

        val args = mutableListOf<String>(
            "ffmpeg",
            "-i", videoFile.absolutePath
        )

        // Добавляем аудио файл, если он существует
        if (audioFile.exists() && audioFile.length() > 0) {
            args.add("-i")
            args.add(audioFile.absolutePath)
        }

        // Настройки кодеков в зависимости от формата
        when (format) {
            RecordingFormat.MP4 -> {
                args.add("-c:v")
                args.add("copy") // Копируем видео без перекодирования
                if (audioFile.exists()) {
                    args.add("-c:a")
                    args.add("aac") // Кодируем аудио в AAC
                }
                args.add("-movflags")
                args.add("+faststart")
            }
            RecordingFormat.MKV -> {
                args.add("-c:v")
                args.add("copy")
                if (audioFile.exists()) {
                    args.add("-c:a")
                    args.add("aac")
                }
            }
            RecordingFormat.AVI -> {
                args.add("-c:v")
                args.add("copy")
                if (audioFile.exists()) {
                    args.add("-c:a")
                    args.add("libmp3lame")
                }
            }
            RecordingFormat.MOV -> {
                args.add("-c:v")
                args.add("copy")
                if (audioFile.exists()) {
                    args.add("-c:a")
                    args.add("aac")
                }
                args.add("-movflags")
                args.add("+faststart")
            }
            RecordingFormat.FLV -> {
                args.add("-c:v")
                args.add("copy")
                if (audioFile.exists()) {
                    args.add("-c:a")
                    args.add("libmp3lame")
                }
            }
        }

        // Настройки качества (применяются только если требуется перекодирование)
        when (quality) {
            Quality.LOW -> {
                args.add("-b:v")
                args.add("500k")
            }
            Quality.MEDIUM -> {
                args.add("-b:v")
                args.add("2000k")
            }
            Quality.HIGH -> {
                args.add("-b:v")
                args.add("4000k")
            }
            Quality.ULTRA -> {
                args.add("-b:v")
                args.add("8000k")
            }
        }

        // Аудио опции
        if (audioFile.exists()) {
            args.add("-ar")
            args.add("44100")
            args.add("-ac")
            args.add("2")
        }

        args.add("-y")
        args.add(outputFile.absolutePath)

        logger.info { "FFmpeg mux command: ${args.joinToString(" ")}" }

        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor(300, TimeUnit.SECONDS)

            if (exitCode == 0 && outputFile.exists()) {
                logger.info { "Successfully muxed video and audio: ${outputFile.absolutePath}" }
                true
            } else {
                logger.warn { "FFmpeg mux failed (exit code: $exitCode)" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Error muxing video and audio" }
            false
        }
    }

    /**
     * Определение поддержки hardware acceleration
     *
     * @return Название кодека с hardware acceleration или libx264 как fallback
     */
    private fun detectHardwareAcceleration(): String {
        return try {
            // Проверка поддержки hardware acceleration
            val process = ProcessBuilder("ffmpeg", "-hide_banner", "-encoders")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)

            // Проверка доступности hardware кодеков (приоритет)
            when {
                output.contains("h264_nvenc") -> {
                    logger.info { "Используется NVIDIA NVENC для hardware acceleration" }
                    "h264_nvenc"
                }
                output.contains("h264_qsv") -> {
                    logger.info { "Используется Intel Quick Sync для hardware acceleration" }
                    "h264_qsv"
                }
                output.contains("h264_videotoolbox") -> {
                    logger.info { "Используется VideoToolbox для hardware acceleration" }
                    "h264_videotoolbox"
                }
                output.contains("h264_vaapi") -> {
                    logger.info { "Используется VAAPI для hardware acceleration" }
                    "h264_vaapi"
                }
                else -> {
                    logger.debug { "Hardware acceleration не доступен, используется libx264" }
                    "libx264"
                }
            }
        } catch (e: Exception) {
            logger.debug(e) { "Не удалось определить hardware acceleration, используется libx264" }
            "libx264"
        }
    }

    /**
     * Определение аудио кодека из потока
     *
     * @param audioData Данные аудио потока
     * @return Название кодека (AAC, PCM, G.711 и т.д.) или null
     */
    fun detectAudioCodec(audioData: ByteArray): String? {
        if (audioData.isEmpty()) return null

        // Простая эвристика для определения кодека
        // В реальности лучше использовать FFprobe или нативный декодер

        // G.711 PCMU (μ-law) - обычно начинается с определенных паттернов
        if (audioData.size >= 2) {
            val firstBytes = audioData.sliceArray(0..minOf(1, audioData.size - 1))
            // G.711 имеет характерные паттерны
            if (firstBytes.all { it.toInt() and 0x7F in 0..127 }) {
                return "PCMU"
            }
        }

        // G.711 PCMA (A-law)
        if (audioData.size >= 2) {
            val firstBytes = audioData.sliceArray(0..minOf(1, audioData.size - 1))
            if (firstBytes.all { it.toInt() and 0x7F in 0..127 }) {
                return "PCMA"
            }
        }

        // AAC обычно имеет ADTS заголовок (0xFF 0xF1-0xF9)
        if (audioData.size >= 2 && audioData[0] == 0xFF.toByte() &&
            (audioData[1].toInt() and 0xF0) == 0xF0) {
            return "AAC"
        }

        // PCM (raw audio) - сложно определить без метаданных
        // Возвращаем null, чтобы использовать определение из потока

        return null
    }

    /**
     * Конвертация аудио из G.711 (PCMU/PCMA) в AAC
     *
     * @param inputFile Входной файл с G.711 аудио
     * @param outputFile Выходной файл с AAC аудио
     * @return true если успешно
     */
    fun convertG711ToAac(
        inputFile: File,
        outputFile: File,
        codec: String = "PCMU" // PCMU или PCMA
    ): Boolean {
        if (!inputFile.exists()) {
            return false
        }

        outputFile.parentFile?.mkdirs()

        val args = listOf(
            "ffmpeg",
            "-f", codec.lowercase(), // pcmu или pcma
            "-ar", "8000", // G.711 обычно 8kHz
            "-ac", "1", // Моно
            "-i", inputFile.absolutePath,
            "-c:a", "aac",
            "-ar", "44100", // Конвертируем в 44.1kHz
            "-ac", "2", // Стерео
            "-b:a", "128k", // Битрейт аудио
            "-y",
            outputFile.absolutePath
        )

        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor(60, TimeUnit.SECONDS)

            exitCode == 0 && outputFile.exists()
        } catch (e: Exception) {
            logger.error(e) { "Error converting G.711 to AAC" }
            false
        }
    }

    /**
     * Конвертация аудио из PCM в AAC
     *
     * @param inputFile Входной файл с PCM аудио
     * @param outputFile Выходной файл с AAC аудио
     * @param sampleRate Частота дискретизации PCM (по умолчанию 8000)
     * @param channels Количество каналов (по умолчанию 1)
     * @return true если успешно
     */
    fun convertPcmToAac(
        inputFile: File,
        outputFile: File,
        sampleRate: Int = 8000,
        channels: Int = 1
    ): Boolean {
        if (!inputFile.exists()) {
            return false
        }

        outputFile.parentFile?.mkdirs()

        val args = listOf(
            "ffmpeg",
            "-f", "s16le", // 16-bit signed little-endian PCM
            "-ar", sampleRate.toString(),
            "-ac", channels.toString(),
            "-i", inputFile.absolutePath,
            "-c:a", "aac",
            "-ar", "44100", // Конвертируем в 44.1kHz
            "-ac", "2", // Стерео
            "-b:a", "128k",
            "-y",
            outputFile.absolutePath
        )

        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor(60, TimeUnit.SECONDS)

            exitCode == 0 && outputFile.exists()
        } catch (e: Exception) {
            logger.error(e) { "Error converting PCM to AAC" }
            false
        }
    }

    /**
     * Определение и конвертация аудио кодека в поддерживаемый формат
     *
     * @param inputFile Входной аудио файл
     * @param outputFile Выходной файл (AAC)
     * @param detectedCodec Определенный кодек (опционально)
     * @return true если успешно
     */
    fun convertAudioToSupportedFormat(
        inputFile: File,
        outputFile: File,
        detectedCodec: String? = null
    ): Boolean {
        if (!inputFile.exists()) {
            return false
        }

        // Пытаемся определить кодек, если не указан
        val codec = detectedCodec ?: run {
            val audioData = inputFile.readBytes().take(1024).toByteArray()
            detectAudioCodec(audioData)
        }

        return when (codec?.uppercase()) {
            "PCMU" -> convertG711ToAac(inputFile, outputFile, "PCMU")
            "PCMA" -> convertG711ToAac(inputFile, outputFile, "PCMA")
            "PCM" -> convertPcmToAac(inputFile, outputFile)
            else -> {
                // Пытаемся автоматически определить через FFmpeg
                logger.info { "Auto-detecting audio codec for: ${inputFile.absolutePath}" }
                convertPcmToAac(inputFile, outputFile) // Fallback на PCM
            }
        }
    }
}

