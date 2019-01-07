package org.starcoin.sirius.serialization

import kotlinx.serialization.ElementValueDecoder
import java.math.BigInteger

abstract class BinaryElementValueDecoder : ElementValueDecoder(), BinaryDecoder {

    override fun decodeByteArray() = this.decodeValue() as ByteArray

    override fun decodeBigInteger() = this.decodeValue() as BigInteger
}
