package org.starcoin.sirius.serialization

import org.starcoin.sirius.lang.toBigInteger
import java.math.BigInteger

interface Codec<T> {

    fun encode(value: T): ByteArray

    fun decode(bytes: ByteArray): T
}

object StringCodec : Codec<String> {
    override fun encode(value: String): ByteArray {
        return value.toByteArray()
    }

    override fun decode(bytes: ByteArray): String {
        return String(bytes)
    }

}

object LongCodec : Codec<Long> {
    override fun encode(value: Long): ByteArray {
        return value.toBigInteger().toByteArray()
    }

    override fun decode(bytes: ByteArray): Long {
        return bytes.toBigInteger().longValueExact()
    }
}

object BigIntegerCodec : Codec<BigInteger> {
    override fun encode(value: BigInteger): ByteArray {
        return value.toByteArray()
    }

    override fun decode(bytes: ByteArray): BigInteger {
        return bytes.toBigInteger()
    }
}