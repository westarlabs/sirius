package org.starcoin.sirius.lang

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

suspend fun <T> retry(
    period: Long = 1000,
    condition: (T?) -> Boolean = { it != null },
    block: () -> T?
): T? {
    do {
        val t = block()
        if (condition(t)) {
            return t
        }
        delay(period)
    } while (true)
}

suspend fun <T> retryWithTimeout(
    timeoutMillis: Long = 200000,
    period: Long = 1000,
    condition: (T?) -> Boolean = { it != null },
    block: () -> T?
): T? = withTimeout(timeoutMillis) {
    retry(period, condition, block)
}

suspend fun <T> retryWithTimeoutOrNull(
    timeoutMillis: Long = 200000,
    period: Long = 1000,
    condition: (T?) -> Boolean = { it != null },
    block: () -> T?
): T? = withTimeoutOrNull(timeoutMillis) {
    retry(period, condition, block)
}