package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.ElementValueDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.internal.EnumDescriptor
import org.starcoin.sirius.serialization.BinaryElementValueDecoder


class RLPInput internal constructor(private val input: Iterator<RLPType>, private val begin: Boolean) :
    BinaryElementValueDecoder() {

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return if (begin) {
            this
        } else {
            val rlpList = input.next() as RLPList
            return RLPInput(rlpList.iterator(), false)
        }
    }

    fun nextElement(): RLPElement {
        return input.next() as RLPElement
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
}