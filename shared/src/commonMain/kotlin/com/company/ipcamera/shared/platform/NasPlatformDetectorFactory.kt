package com.company.ipcamera.shared.platform

/**
 * Фабрика для создания NasPlatformDetector
 *
 * Создает комбинированный детектор, который использует оба варианта:
 * 1. Определение через системные файлы (быстрое, надежное)
 * 2. Определение через API (дополнительная проверка)
 */
object NasPlatformDetectorFactory {
    /**
     * Создать детектор платформы
     *
     * @param useApiDetection Использовать ли API для определения (по умолчанию true)
     * @return NasPlatformDetector
     */
    fun create(useApiDetection: Boolean = true): NasPlatformDetector {
        val fileBasedDetector = FileBasedNasDetector()

        return if (useApiDetection) {
            // Комбинированный детектор: сначала файлы, потом API
            ApiBasedNasDetector(fileBasedDetector)
        } else {
            // Только файловый детектор
            fileBasedDetector
        }
    }

    /**
     * Создать только файловый детектор (быстрый вариант)
     */
    fun createFileBased(): NasPlatformDetector {
        return FileBasedNasDetector()
    }

    /**
     * Создать только API детектор (для тестирования)
     */
    fun createApiBased(): NasPlatformDetector {
        return ApiBasedNasDetector(FileBasedNasDetector())
    }
}
