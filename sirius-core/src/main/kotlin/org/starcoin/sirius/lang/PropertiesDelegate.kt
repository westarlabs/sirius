package org.starcoin.sirius.lang

import java.util.*
import kotlin.reflect.KProperty

class PropertiesDelegate<T>(val properties: Properties, val transform: String.() -> T) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return properties.getProperty(property.name)?.transform()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.properties.setProperty(property.name, value.toString())
    }

    fun default(defaultValue: T): PropertiesDelegateWitDefault<T> {
        return PropertiesDelegateWitDefault(this, defaultValue)
    }
}

class PropertiesDelegateWitDefault<T>(private val delegate: PropertiesDelegate<T>, private val defaultValue: T) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return delegate.getValue(thisRef, property) ?: defaultValue
    }

}

fun <T> Properties.delegate(transform: String.() -> T): PropertiesDelegate<T> {
    return PropertiesDelegate(this, transform)
}

fun Properties.delegate() = this.delegate { this }
