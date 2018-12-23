package org.starcoin.sirius.serialization

import kotlinx.serialization.ElementValueDecoder

abstract class BinaryElementValueDecoder : ElementValueDecoder(), BinaryDecoder {

    override fun decodeByteArray() = this.decodeValue() as ByteArray

}
