package org.starcoin.sirius.serialization.rlp

import java.math.BigInteger
import java.math.BigInteger.ZERO

/**
 * original from https://github.com/walleth/kethereum, add more type support
RLP as of Appendix B. Recursive Length Prefix at https://github.com/ethereum/yellowpaper
 */

fun Int.toByteArray() = ByteArray(4) { i ->
    shr(8 * (3 - i)).toByte()
}

fun Int.toMinimalByteArray() = toByteArray().let {
    it.copyOfRange(it.minimalStart(), 4)
}

private fun ByteArray.minimalStart() = indexOfFirst { it != 0.toByte() }.let { if (it == -1) 3 else it }
fun ByteArray.removeLeadingZero() = if (first() == 0.toByte()) copyOfRange(1, size) else this


// to RLP

fun String.toRLP() = RLPElement(toByteArray())

fun Int.toRLP() = RLPElement(toMinimalByteArray())
fun BigInteger.toRLP() = RLPElement(toByteArray().removeLeadingZero())
fun ByteArray.toRLP() = RLPElement(this)
fun Byte.toRLP() = RLPElement(kotlin.ByteArray(1) { this })

fun Boolean.toRLP() = RLPElement(ByteArray(1) { if (this) 1.toByte() else 0.toByte() })
fun Long.toRLP() = this.toBigInteger().toRLP()
fun Float.toRLP() = this.toBigDecimal().toBigInteger().toRLP()
fun Double.toRLP() =
    this.toBigDecimal().toBigInteger().toRLP()

fun Short.toRLP() = this.toInt().toRLP()
fun Char.toRLP() = this.toShort().toRLP()

// from RLP

fun RLPElement.toIntFromRLP() = bytes
    .mapIndexed { index, byte -> byte.toInt().shl((bytes.size - 1 - index) * 8) }
    .reduce { acc, i -> acc + i }

fun RLPElement.toBigIntegerFromRLP(): BigInteger = if (bytes.isEmpty()) ZERO else BigInteger(bytes)
fun RLPElement.toUnsignedBigIntegerFromRLP(): BigInteger = if (bytes.isEmpty()) ZERO else BigInteger(1, bytes)
fun RLPElement.toByteFromRLP(): Byte {
    if (bytes.size != 1) {
        throw IllegalArgumentException("trying to convert RLP with != 1 byte to Byte")
    }
    return bytes.first()
}

fun RLPElement.toStringFromRLP() = String(bytes)

fun RLPElement.toBooleanFromRLP(): Boolean = this.toByteFromRLP() == 1.toByte()
fun RLPElement.toShortFromRLP(): Short = this.toIntFromRLP().toShort()
fun RLPElement.toCharFromRLP(): Char = this.toShortFromRLP().toChar()
fun RLPElement.toLongFromRLP(): Long = this.toBigIntegerFromRLP().toLong()
fun RLPElement.toFloatFromRLP(): Float = this.toBigIntegerFromRLP().toFloat()
fun RLPElement.toDoubleFromRLP(): Double = this.toBigIntegerFromRLP().toDouble()