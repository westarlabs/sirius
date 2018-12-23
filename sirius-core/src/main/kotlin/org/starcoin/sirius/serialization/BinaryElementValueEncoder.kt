package org.starcoin.sirius.serialization

import kotlinx.serialization.ElementValueEncoder

abstract class BinaryElementValueEncoder : ElementValueEncoder(), BinaryEncoder {

    override fun encodeByteArray(byteArray: ByteArray) = encodeValue(byteArray)

}
