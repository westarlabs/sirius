package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.*
import org.starcoin.sirius.serialization.BinaryElementValueEncoder


class RLPOutput internal constructor(private val out: RLPList, private val begin: Boolean) :
    BinaryElementValueEncoder() {

    override fun beginCollection(
        desc: SerialDescriptor,
        collectionSize: Int,
        vararg typeParams: KSerializer<*>
    ): CompositeEncoder {
        return newEncoder()
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        return newEncoder()
    }

    fun newEncoder(): RLPOutput {
        return if (begin) {
            this
        } else {
            val rlpList = RLPList(mutableListOf())
            out.add(rlpList)
            RLPOutput(rlpList, false)
        }
    }

    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        return true
    }

    override fun encodeNull() = throw SerializationException("null is not supported")

    override fun encodeValue(value: Any) {
        when (value) {
            is Boolean -> out.add(value.toRLP())
            is Byte -> out.add(value.toRLP())
            is Short -> out.add(value.toInt().toRLP())
            is Int -> out.add(value.toRLP())
            is Long -> out.add(value.toRLP())
            is Float -> out.add(value.toRLP())
            is Double -> out.add(value.toRLP())
            is Char -> out.add(value.toRLP())
            is String -> out.add(value.toRLP())
            is ByteArray -> out.add(value.toRLP())
            else -> throw SerializationException("unsupported type ${value.javaClass}")
        }
    }
}