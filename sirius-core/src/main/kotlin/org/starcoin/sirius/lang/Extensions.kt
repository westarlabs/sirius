package org.starcoin.sirius.lang

import com.google.common.io.BaseEncoding
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.Charset
import java.util.*

private val HEX = BaseEncoding.base16().lowerCase()

fun ByteArray.toULong() = BigInteger(1, this).toLong()

fun ByteArray.toHEXString() = "0x" + this.toNoPrefixHEXString()
fun ByteArray.toNoPrefixHEXString() = HEX.encode(this)

fun ByteArray.toBigInteger(offset: Int, length: Int) = BigInteger(1, Arrays.copyOfRange(this, offset, offset + length))
fun ByteArray.toBigInteger() = BigInteger(this)
fun ByteArray.toUnsignedBigInteger() = BigInteger(1, this)
fun ByteArray.isZeroBytes() = this.all { it == 0.toByte() }

fun String.hexToByteArray(): ByteArray {
    val cleanInput = (if (startsWith("0x")) substring(2) else this).let {
        if (it.length % 2 != 0) "0$it" else it
    }
    return ByteArray(cleanInput.length / 2).apply {
        var i = 0
        while (i < cleanInput.length) {
            this[i / 2] = ((cleanInput[i].getNibbleValue() shl 4) + cleanInput[i + 1].getNibbleValue()).toByte()
            i += 2
        }
    }
}

private fun Char.getNibbleValue() = Character.digit(this, 16).also {
    if (it == -1) throw IllegalArgumentException("Not a valid hex char: $this")
}

data class ClassPathResource(val path: String) {
    fun readAsStream(): InputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
    fun readAsText(): String = this.readAsStream().use { it.readText() }
}

fun String.toClassPathResource() = ClassPathResource(this)

fun InputStream.readText() = this.readBytes().toString(Charset.defaultCharset())

fun Number.toBigInteger(): BigInteger = when (this) {
    is BigInteger -> this
    is BigDecimal -> this.toBigInteger()
    else -> BigInteger.valueOf(this.toLong())
}
