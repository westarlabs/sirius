package org.starcoin.sirius.datastore

import org.starcoin.sirius.core.*
import org.starcoin.sirius.serialization.Codec
import org.starcoin.sirius.serialization.StringCodec
import kotlin.reflect.KClass

class SiriusObjectStore<K, V : SiriusObject>(
    keyCodec: Codec<K>,
    valueCodec: SiriusObjectCodec<V>,
    dataStore: DataStore<ByteArray, ByteArray>
) : ObjectStore<K, V>(keyCodec, valueCodec, dataStore) {

    constructor(keyCodec: Codec<K>, clazz: KClass<V>, dataStore: DataStore<ByteArray, ByteArray>) : this(
        keyCodec,
        //default use protobuf
        SiriusObjectProtoBufCodec<V>(clazz),
        dataStore
    )

    companion object {

        fun <V : SiriusObject> hashStore(
            clazz: KClass<V>,
            dataStore: DataStore<ByteArray, ByteArray>
        ): SiriusObjectStore<Hash, V> {
            return SiriusObjectStore(Hash, clazz, dataStore)
        }

        fun <V : SiriusObject> addressStore(
            clazz: KClass<V>,
            dataStore: DataStore<ByteArray, ByteArray>
        ): SiriusObjectStore<Address, V> {
            return SiriusObjectStore(Address, clazz, dataStore)
        }

        fun  <V : SiriusObject> stringStore(
            clazz: KClass<V>,
            dataStore: DataStore<ByteArray, ByteArray>
        ): SiriusObjectStore<String, V> {
            return SiriusObjectStore(StringCodec, clazz, dataStore)
        }
    }
}

