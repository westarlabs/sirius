package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.internal.EnumDescriptor
import org.starcoin.sirius.serialization.BinaryElementValueDecoder
import java.math.BigInteger


class RLPInput internal constructor(private val input: RLPList, private var begin: Boolean) :
    BinaryElementValueDecoder() {

    val iterator = input.listIterator()

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return if (begin) {
            begin = false
            this
        } else {
            val rlpList = iterator.next() as RLPList
            return RLPInput(rlpList, false)
        }
    }

    private fun nextElement(): RLPElement {
        return iterator.next() as RLPElement
    }

    override fun decodeBoolean(): Boolean {
        return nextElement().toBooleanFromRLP()
    }

    override fun decodeByte(): Byte {
        return nextElement().toByteFromRLP()
    }

    override fun decodeChar(): Char {
        return nextElement().toCharFromRLP()
    }

    override fun decodeDouble(): Double {
        return nextElement().toDoubleFromRLP()
    }

    override fun decodeEnum(enumDescription: EnumDescriptor): Int {
        return nextElement().toIntFromRLP()
    }

    override fun decodeFloat(): Float {
        return nextElement().toFloatFromRLP()
    }

    override fun decodeInt(): Int {
        return nextElement().toIntFromRLP()
    }

    override fun decodeLong(): Long {
        return nextElement().toLongFromRLP()
    }

    override fun decodeShort(): Short {
        return nextElement().toShortFromRLP()
    }

    override fun decodeString(): String {
        return nextElement().toStringFromRLP()
    }

    override fun decodeUnit() {
    }

    override fun decodeByteArray(): ByteArray {
        return nextElement().bytes
    }

    override fun decodeBigInteger(): BigInteger {
        return nextElement().toBigIntegerFromRLP()
    }

    override fun decodeCollectionSize(desc: SerialDescriptor): Int {
        return input.size
    }
}
