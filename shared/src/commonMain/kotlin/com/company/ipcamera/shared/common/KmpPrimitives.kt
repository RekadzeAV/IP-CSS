package com.company.ipcamera.shared.common

import kotlinx.datetime.Clock
import kotlin.random.Random

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

fun newRandomId(): String {
    fun hex(count: Int): String =
        buildString {
            repeat(count) { append(Random.nextInt(16).toString(16)) }
        }
    return "${hex(8)}-${hex(4)}-${hex(4)}-${hex(4)}-${hex(12)}"
}
