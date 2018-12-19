package org.starcoin.sirius.serialization

import kotlinx.serialization.ElementValueEncoder

abstract class BinaryElementValueEncoder : ElementValueEncoder() {

    open fun encodeByteArray(byteArray: ByteArray) = encodeValue(byteArray)

}