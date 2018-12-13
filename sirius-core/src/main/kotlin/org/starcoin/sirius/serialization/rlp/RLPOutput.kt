package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.*


class RLPOutput(val out: RLPList) : ElementValueEncoder() {
    override fun beginCollection(
        desc: SerialDescriptor,
        collectionSize: Int,
        vararg typeParams: KSerializer<*>
    ): CompositeEncoder {
        return super.beginCollection(desc, collectionSize, *typeParams).also {
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
            else -> throw SerializationException("unsupported type ${value.javaClass}")
        }
    }
}