package com.company.ipcamera.android.media

import android.graphics.Bitmap

/**
 * Плоский RGB24 (3 байта на пиксель, R, G, B) для доменных UC аналитики — в том же порядке, что и на Desktop.
 */
fun Bitmap.toRgb24ByteArray(): ByteArray {
    val w = width
    val h = height
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)
    val out = ByteArray(w * h * 3)
    var i = 0
    for (p in pixels) {
        out[i++] = ((p shr 16) and 0xFF).toByte()
        out[i++] = ((p shr 8) and 0xFF).toByte()
        out[i++] = (p and 0xFF).toByte()
    }
    return out
}
