package org.starcoin.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public class WaitUtil {


    public static void waitFor(AtomicBoolean stop, long waitInterval, TimeUnit timeUnit,
                               BooleanSupplier condition) {
        while (!condition.getAsBoolean() && !stop.get()) {
            wait(waitInterval, timeUnit);
        }
    }

    public static void wait(long waitTime, TimeUnit timeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(waitTime));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
