package com.company.ipcamera.desktop.media

import java.awt.image.BufferedImage

/**
 * Плоский RGB24 (3 байта на пиксель, порядок R, G, B) для доменных UC аналитики.
 */
fun BufferedImage.toRgb24ByteArray(): ByteArray {
    val w = width
    val h = height
    val pixels = IntArray(w * h)
    getRGB(0, 0, w, h, pixels, 0, w)
    val out = ByteArray(w * h * 3)
    var i = 0
    for (p in pixels) {
        out[i++] = ((p shr 16) and 0xFF).toByte()
        out[i++] = ((p shr 8) and 0xFF).toByte()
        out[i++] = (p and 0xFF).toByte()
    }
    return out
}
