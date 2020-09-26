package com.gigaspaces.http

import kotlinx.coroutines.delay
import java.io.IOException


@Suppress("unused")
suspend fun <T> retryIO(
        times: Int = 1,
        initialDelay: Long = 1000,
        maxDelay: Long = 30_000,
        factor: Double = 2.0,
        block: suspend () -> T): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: IOException) {
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block()
}

