package com.company.ipcamera.core.network.video

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Декодер Exp-Golomb кодов для парсинга H.264/H.265 параметров
 *
 * Exp-Golomb кодирование используется в H.264/H.265 для компактного представления
 * целых чисел, где меньшие числа кодируются меньшим количеством бит.
 */
internal class ExpGolombDecoder(private val data: ByteArray) {
    private var bitOffset = 0
    private var byteOffset = 0

    /**
     * Чтение одного бита
     */
    internal fun readBit(): Int {
        if (byteOffset >= data.size) {
            logger.warn { "Reading beyond data size" }
            return 0
        }

        val byte = data[byteOffset].toInt() and 0xFF
        val bit = (byte shr (7 - (bitOffset % 8))) and 1

        bitOffset++
        if (bitOffset % 8 == 0) {
            byteOffset++
        }

        return bit
    }

    /**
     * Чтение n бит
     */
    private fun readBits(n: Int): Int {
        var value = 0
        for (i in 0 until n) {
            value = (value shl 1) or readBit()
        }
        return value
    }

    /**
     * Декодирование Exp-Golomb кода (unsigned)
     *
     * Алгоритм:
     * 1. Подсчитываем количество ведущих нулей (leading zeros)
     * 2. Читаем следующую группу бит длиной (leading zeros + 1)
     * 3. Значение = (2^leading_zeros - 1) + прочитанное значение
     */
    fun readUE(): Int {
        var leadingZeros = 0

        // Подсчитываем ведущие нули
        while (readBit() == 0 && leadingZeros < 32) {
            leadingZeros++
        }

        if (leadingZeros == 0) {
            return 0
        }

        // Читаем следующую группу бит
        val value = readBits(leadingZeros)

        // Формула Exp-Golomb: (2^leading_zeros - 1) + value
        return (1 shl leadingZeros) - 1 + value
    }

    /**
     * Декодирование знакового Exp-Golomb кода (signed)
     */
    fun readSE(): Int {
        val ue = readUE()
        // Преобразование unsigned в signed: (-1)^(ue+1) * ceil(ue/2)
        return if (ue % 2 == 0) {
            -(ue / 2)
        } else {
            (ue + 1) / 2
        }
    }

    /**
     * Пропуск битов (для выравнивания)
     */
    fun skipBits(n: Int) {
        for (i in 0 until n) {
            readBit()
        }
    }

    /**
     * Пропуск до выравнивания на байт
     */
    fun alignToByte() {
        val bitsToSkip = (8 - (bitOffset % 8)) % 8
        if (bitsToSkip > 0) {
            skipBits(bitsToSkip)
        }
    }

    /**
     * Проверка, достигнут ли конец данных
     */
    fun isEnd(): Boolean {
        return byteOffset >= data.size
    }

    /**
     * Получить текущую позицию в битах
     */
    fun getBitPosition(): Int {
        return byteOffset * 8 + (bitOffset % 8)
    }
}
