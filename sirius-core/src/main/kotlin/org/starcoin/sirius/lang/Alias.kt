package org.starcoin.sirius.lang

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

class Alias<T>(val delegate: KMutableProperty0<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        delegate.get()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        delegate.set(value)
    }
}