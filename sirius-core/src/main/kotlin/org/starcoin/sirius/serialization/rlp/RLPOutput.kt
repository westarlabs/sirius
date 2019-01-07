package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerializationException
import org.starcoin.sirius.serialization.BinaryElementValueEncoder
import java.math.BigInteger


class RLPOutput internal constructor(private val out: RLPList, private var begin: Boolean) :
    BinaryElementValueEncoder() {

    override fun beginCollection(
        desc: SerialDescriptor,
        collectionSize: Int,
        vararg typeParams: KSerializer<*>
    ): CompositeEncoder {
        return newEncoder(collectionSize)
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        return newEncoder()
    }

    private fun newEncoder(size: Int = -1): RLPOutput {
        return if (begin) {
            begin = false
            this
        } else {
            val rlpList = RLPList(if (size < 0) mutableListOf() else ArrayList(size))
            out.add(rlpList)
            RLPOutput(rlpList, false)
        }
    }

    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        return true
    }

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
            is BigInteger -> out.add(value.toRLP())
            else -> throw SerializationException("unsupported type ${value.javaClass}")
        }
    }
}
