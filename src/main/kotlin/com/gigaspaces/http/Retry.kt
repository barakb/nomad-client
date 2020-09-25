package com.gigaspaces.http

import kotlinx.coroutines.delay
import java.io.IOException


suspend fun <T> retryIO(
        times: Int,
        initialDelay: Long,
        maxDelay: Long,
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
    return block() // last attempt
}

suspend fun <T> withRetry(retry: Retry, block: suspend () -> T): T {
    return retryIO(retry.times, retry.initialDelay, retry.maxDelay, 2.0, block)
}

