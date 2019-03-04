package org.starcoin.sirius.cotoutines

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures.addCallback
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> ListenableFuture<T>.asDeferred(context: CoroutineContext = EmptyCoroutineContext): Deferred<T> {
    val deferred = CompletableDeferred<T>()
    addCallback(this, object : FutureCallback<T> {
        override fun onSuccess(result: T?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onFailure(t: Throwable) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }, context.asExecutor())
    return deferred
}