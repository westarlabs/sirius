package org.starcoin.sirius.serialization

import kotlinx.serialization.ElementValueEncoder
import java.math.BigInteger

abstract class BinaryElementValueEncoder : ElementValueEncoder(), BinaryEncoder {

    override fun encodeByteArray(byteArray: ByteArray) = encodeValue(byteArray)

    override fun encodeBigInteger(bigInteger: BigInteger) = encodeValue(bigInteger)
}
