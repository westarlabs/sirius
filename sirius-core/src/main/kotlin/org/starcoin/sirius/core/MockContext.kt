package org.starcoin.sirius.core

import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

object MockContext {

    private val properites = HashMap<String, Any?>()
    var cryptoKey: CryptoKey by properites

    init {
        cryptoKey = CryptoService.dummyCryptoKey
    }

    fun containsKey(key: String): Boolean {
        return properites.containsKey(key)
    }

    operator fun get(key: String): Any? {
        return properites[key]
    }

    fun getAsBoolean(key: String): Boolean {
        val v = this[key] ?: return false
        return v as Boolean
    }

    fun getAsInt(key: String): Int {
        val v = this[key] ?: return 0
        return v as Int
    }

    fun put(key: String, value: Any): MockContext {
        properites[key] = value
        return this
    }

    fun <V> getOrDefault(key: String, defaultValue: V): V {
        return (properites as java.util.Map<String, Any>).getOrDefault(key, defaultValue) as V
    }

    fun <V> getOrSet(key: String, setValue: V): V {
        var v = this[key]
        if (v == null) {
            this.properites[key] = setValue
            v = setValue
        }
        return v as V
    }


    fun <T : SiriusObject> mock(clazz: KClass<T>): T {
        val companion = clazz.companionObjectInstance as SiriusObjectCompanion<T, *>
        return companion.mock()
    }
}
