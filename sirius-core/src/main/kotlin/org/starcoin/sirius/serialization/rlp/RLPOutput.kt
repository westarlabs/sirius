package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import org.starcoin.sirius.serialization.BinaryElementValueEncoder


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
        out.add(value.toRLP())
    }
}
