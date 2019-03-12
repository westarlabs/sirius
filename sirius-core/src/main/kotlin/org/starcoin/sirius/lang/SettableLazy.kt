package org.starcoin.sirius.lang

import kotlin.reflect.KProperty

interface SettableLazy<T> {
    var value: T?

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

fun <T> settableLazy(load: () -> T?): SettableLazy<T> = SettableLazyImpl(load)

internal object UNINITIALIZED_VALUE

internal class SettableLazyImpl<T>(private val load: () -> T?, lock: Any? = null) : SettableLazy<T> {

    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE
    private val lock = lock ?: this

    override var value: T?
        get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return _v1 as T
            }

            return synchronized(lock) {
                val _v2 = _value
                if (_v2 !== UNINITIALIZED_VALUE) {
                    @Suppress("UNCHECKED_CAST") (_v2 as T)
                } else {
                    val typedValue = load()
                    _value = typedValue
                    typedValue
                }
            }
        }
        set(value) {
            this._value = value
        }
}
