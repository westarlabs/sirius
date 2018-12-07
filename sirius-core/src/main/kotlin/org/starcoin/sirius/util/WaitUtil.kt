package org.starcoin.sirius.util

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BooleanSupplier

object WaitUtil {


    fun waitFor(
        stop: AtomicBoolean, waitInterval: Long, timeUnit: TimeUnit,
        condition: BooleanSupplier
    ) {
        while (!condition.asBoolean && !stop.get()) {
            wait(waitInterval, timeUnit)
        }
    }

    fun wait(waitTime: Long, timeUnit: TimeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(waitTime))
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException(e)
        }

    }
}
