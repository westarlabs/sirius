package org.starcoin.sirius.cotoutines

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

fun CoroutineContext.asExecutor(): Executor {
    return if (this is ExecutorCoroutineDispatcher) {
        this.executor
    } else {
        DirectExecutor
    }
}