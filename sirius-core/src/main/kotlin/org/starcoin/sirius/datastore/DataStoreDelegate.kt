package org.starcoin.sirius.datastore

import org.starcoin.sirius.serialization.Codec
import org.starcoin.sirius.serialization.LongCodec
import org.starcoin.sirius.serialization.StringCodec
import kotlin.reflect.KProperty


class DataStoreDelegate<V>(
    private val dataStore: DataStore<ByteArray, ByteArray>,
    private val valueCodec: Codec<V>,
    private val defaultValue: V
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): V {
        return dataStore.get(StringCodec.encode(property.name))?.let { valueCodec.decode(it) } ?: defaultValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.dataStore.put(StringCodec.encode(property.name), valueCodec.encode(value))
    }

}

fun <V> DataStore<ByteArray, ByteArray>.delegate(valueCodec: Codec<V>, defaultValue: V): DataStoreDelegate<V> {
    return DataStoreDelegate(this, valueCodec, defaultValue)
}

fun DataStore<ByteArray, ByteArray>.delegate(defaultValue: String) = this.delegate(StringCodec, defaultValue)

fun DataStore<ByteArray, ByteArray>.delegate(defaultValue: Long) = this.delegate(LongCodec, defaultValue)