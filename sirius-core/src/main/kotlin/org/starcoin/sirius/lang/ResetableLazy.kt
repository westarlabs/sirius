package org.starcoin.sirius.lang

import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

fun <T> resetableLazy(initializer: () -> T) = ResetableDelegate(initializer)

class ResetableDelegate<T>(private val initializer: () -> T) {
    private val lazyRef: AtomicReference<Lazy<T>> = AtomicReference(
        lazy(
            initializer
        )
    )

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return lazyRef.get().getValue(thisRef, property)
    }

    fun reset() {
        lazyRef.set(lazy(initializer))
    }
}
