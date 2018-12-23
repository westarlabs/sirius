package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.*
import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.context.SerialModule

class RLP : AbstractSerialFormat(), BinaryFormat {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RLP) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray {
        val rlpList = RLPList(mutableListOf())
        val dumper = RLPOutput(rlpList, true)
        dumper.encode(serializer, obj)
        return rlpList.encode()
    }

    override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val rlpList = bytes.decodeRLP() as RLPList
        val loader = RLPInput(rlpList.listIterator(), true)
        return loader.decode(deserializer)
    }

    companion object : BinaryFormat {

        val plain = RLP()

        override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray = plain.dump(serializer, obj)
        override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T =
            plain.load(deserializer, bytes)

        override fun install(module: SerialModule) = plain.install(module)
        override val context: SerialContext get() = plain.context

    }
}
