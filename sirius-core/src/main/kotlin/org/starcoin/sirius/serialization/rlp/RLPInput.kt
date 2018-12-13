package org.starcoin.sirius.serialization.rlp

import kotlinx.serialization.ElementValueDecoder
import kotlinx.serialization.internal.EnumDescriptor


class RLPInput(val input: Iterator<RLPType>) : ElementValueDecoder() {

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
}