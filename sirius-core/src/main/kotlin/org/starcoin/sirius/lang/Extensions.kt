package org.starcoin.sirius.lang

import org.starcoin.sirius.util.Utils
import java.math.BigInteger
import java.util.*

fun ByteArray.toULong() = BigInteger(1, this).toLong()

fun ByteArray.toHEXString() = Utils.HEX.encode(this)

fun ByteArray.toBigInteger(offset: Int, length: Int) = BigInteger(1, Arrays.copyOfRange(this, offset, offset + length))
fun ByteArray.toBigInteger() = BigInteger(1, this)
