package org.starcoin.sirius.serialization

import kotlinx.serialization.Encoder
import java.math.BigInteger

interface BinaryEncoder :Encoder{

    fun encodeByteArray(byteArray: ByteArray)

    fun encodeBigInteger(bigInteger: BigInteger) = encodeByteArray(bigInteger.toByteArray())
}
