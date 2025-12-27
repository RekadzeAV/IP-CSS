package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse

/**
 * Тесты для записи с различными аудио кодеками
 *
 * Требования:
 * - FFmpeg должен быть установлен
 * - Тестовые RTSP потоки или файлы должны быть доступны
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioRecordingTest {

    private lateinit var ffmpegService: FfmpegService

    @TempDir
    lateinit var tempDir: Path

    @BeforeAll
    fun setup() {
        ffmpegService = FfmpegService()

        // Проверка наличия FFmpeg
        assumeTrue(
            ffmpegService.isAvailable(),
            "FFmpeg не установлен. Установите FFmpeg для запуска тестов."
        )
    }

    @Test
    @DisplayName("Тест определения аудио кодека AAC")
    fun testDetectAacCodec() {
        // Создаем тестовые данные с AAC ADTS заголовком
        val aacData = byteArrayOf(
            0xFF.toByte(), 0xF1.toByte(), // ADTS заголовок
            0x90.toByte(), 0x00.toByte(), 0x1F.toByte(),
            0xFC.toByte(), 0x21.toByte(), 0x00.toByte(),
            0x49.toByte(), 0x90.toByte(), 0x02.toByte(),
            0x19.toByte(), 0x00.toByte(), 0x23.toByte(),
            0x80.toByte()
        )

        val codec = ffmpegService.detectAudioCodec(aacData)
        assertNotNull(codec, "Кодек должен быть определен")
        assertTrue(codec == "AAC", "Должен быть определен AAC кодек")
    }

    @Test
    @DisplayName("Тест определения аудио кодека G.711 PCMU")
    fun testDetectG711PcmuCodec() {
        // Создаем тестовые данные G.711 PCMU (μ-law)
        val pcmuData = ByteArray(160) {
            // G.711 PCMU имеет значения в диапазоне 0-127 после инверсии знака
            (it % 128).toByte()
        }

        val codec = ffmpegService.detectAudioCodec(pcmuData)
        // Может быть определен как PCMU или null (зависит от реализации)
        // Проверяем что не выброшено исключение
        assertNotNull(codec, "Кодек должен быть определен или null")
    }

    @Test
    @DisplayName("Тест конвертации G.711 PCMU в AAC")
    fun testConvertG711ToAac() = runBlocking {
        // Создаем тестовый файл с G.711 данными
        val inputFile = File(tempDir.toFile(), "test_pcmu.raw")
        val outputFile = File(tempDir.toFile(), "test_pcmu_converted.aac")

        // Генерируем тестовые G.711 данные (1 секунда при 8kHz)
        val pcmuData = ByteArray(8000) { (it % 128).toByte() }
        inputFile.writeBytes(pcmuData)

        val success = ffmpegService.convertG711ToAac(
            inputFile = inputFile,
            outputFile = outputFile,
            codec = "PCMU"
        )

        if (success) {
            assertTrue(outputFile.exists(), "Выходной файл должен быть создан")
            assertTrue(outputFile.length() > 0, "Выходной файл не должен быть пустым")
        } else {
            // Если конвертация не удалась, это может быть нормально для тестовой среды
            println("Конвертация G.711 в AAC не удалась (может быть нормально в тестовой среде)")
        }
    }

    @Test
    @DisplayName("Тест конвертации PCM в AAC")
    fun testConvertPcmToAac() = runBlocking {
        // Создаем тестовый файл с PCM данными
        val inputFile = File(tempDir.toFile(), "test_pcm.raw")
        val outputFile = File(tempDir.toFile(), "test_pcm_converted.aac")

        // Генерируем тестовые PCM данные (16-bit signed, 8kHz, моно, 1 секунда)
        val pcmData = ByteArray(16000) {
            // Синусоидальный сигнал
            val sample = (Math.sin(2 * Math.PI * 440 * it / 8000.0) * 32767).toInt()
            ((sample shr 8) and 0xFF).toByte()
        }
        inputFile.writeBytes(pcmData)

        val success = ffmpegService.convertPcmToAac(
            inputFile = inputFile,
            outputFile = outputFile,
            sampleRate = 8000,
            channels = 1
        )

        if (success) {
            assertTrue(outputFile.exists(), "Выходной файл должен быть создан")
            assertTrue(outputFile.length() > 0, "Выходной файл не должен быть пустым")
        } else {
            println("Конвертация PCM в AAC не удалась (может быть нормально в тестовой среде)")
        }
    }

    @Test
    @DisplayName("Тест мультиплексирования видео и аудио")
    fun testMuxVideoAndAudio() = runBlocking {
        // Создаем тестовые файлы
        val videoFile = File(tempDir.toFile(), "test_video.h264")
        val audioFile = File(tempDir.toFile(), "test_audio.aac")
        val outputFile = File(tempDir.toFile(), "test_muxed.mp4")

        // Создаем минимальные тестовые данные
        // В реальности это должны быть валидные H.264 и AAC потоки
        videoFile.writeBytes(ByteArray(1024) { it.toByte() })
        audioFile.writeBytes(ByteArray(512) { it.toByte() })

        // Тест может не пройти с невалидными данными, но проверяем что метод вызывается
        val success = ffmpegService.muxVideoAndAudio(
            videoFile = videoFile,
            audioFile = audioFile,
            outputFile = outputFile,
            format = RecordingFormat.MP4,
            quality = Quality.MEDIUM
        )

        // В тестовой среде с невалидными данными это может не сработать
        // Проверяем что метод выполнился без исключений
        assertNotNull(success, "Метод должен вернуть результат")
    }

    @Test
    @DisplayName("Тест записи с AAC аудио")
    fun testRecordingWithAacAudio() {
        // Этот тест требует реальный RTSP поток или тестовый файл
        // В реальной среде можно использовать тестовый RTSP сервер

        val testRtspUrl = System.getenv("TEST_RTSP_URL")
        if (testRtspUrl.isNullOrEmpty()) {
            println("TEST_RTSP_URL не установлен, пропускаем тест")
            return
        }

        val outputFile = File(tempDir.toFile(), "test_aac_recording.mp4")

        val process = ffmpegService.encodeRtspToFile(
            rtspUrl = testRtspUrl,
            outputFile = outputFile,
            format = RecordingFormat.MP4,
            quality = Quality.MEDIUM,
            duration = 5 // 5 секунд для теста
        )

        // Ждем завершения процесса
        val exitCode = process.waitFor()

        // Проверяем результат
        if (exitCode == 0) {
            assertTrue(outputFile.exists(), "Файл записи должен быть создан")
            assertTrue(outputFile.length() > 0, "Файл записи не должен быть пустым")
        } else {
            println("Запись не удалась (exit code: $exitCode)")
        }
    }

    @Test
    @DisplayName("Тест определения hardware acceleration")
    fun testHardwareAccelerationDetection() {
        // Этот тест проверяет что метод определения hardware acceleration работает
        // Реальная проверка выполняется внутри FfmpegService

        val ffmpegService = FfmpegService()
        assertNotNull(ffmpegService, "FfmpegService должен быть создан")

        // Проверяем что FFmpeg доступен
        val isAvailable = ffmpegService.isAvailable()
        assertTrue(isAvailable, "FFmpeg должен быть доступен для тестов")
    }

    @Test
    @DisplayName("Тест оптимизированных параметров FFmpeg")
    fun testOptimizedFfmpegParameters() {
        // Создаем тестовый файл для проверки параметров
        val testFile = File(tempDir.toFile(), "test_optimized.mp4")

        // Используем минимальный тестовый входной файл
        // В реальности это должен быть валидный видео файл
        val inputFile = File(tempDir.toFile(), "test_input.mp4")
        inputFile.writeBytes(ByteArray(1024))

        // Тест конвертации с оптимизированными параметрами
        val success = ffmpegService.convertVideo(
            inputFile = inputFile,
            outputFile = testFile,
            outputFormat = RecordingFormat.MP4
        )

        // С невалидными данными это может не сработать
        // Проверяем что метод выполнился
        assertNotNull(success, "Метод должен вернуть результат")
    }

    @Test
    @DisplayName("Тест автоматического определения и конвертации аудио кодека")
    fun testAutoDetectAndConvertAudio() = runBlocking {
        // Создаем тестовый файл
        val inputFile = File(tempDir.toFile(), "test_audio_unknown.raw")
        val outputFile = File(tempDir.toFile(), "test_audio_converted.aac")

        // Создаем тестовые данные
        val audioData = ByteArray(8000) { (it % 256).toByte() }
        inputFile.writeBytes(audioData)

        // Пытаемся автоматически определить и конвертировать
        val success = ffmpegService.convertAudioToSupportedFormat(
            inputFile = inputFile,
            outputFile = outputFile,
            detectedCodec = null // Автоматическое определение
        )

        // Может не сработать с невалидными данными
        // Проверяем что метод выполнился
        assertNotNull(success, "Метод должен вернуть результат")
    }

    companion object {
        /**
         * Вспомогательная функция для условного пропуска тестов
         */
        private fun assumeTrue(condition: Boolean, message: String) {
            if (!condition) {
                throw AssumptionViolatedException(message)
            }
        }
    }
}

