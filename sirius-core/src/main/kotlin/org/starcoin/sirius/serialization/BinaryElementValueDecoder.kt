package org.starcoin.sirius.serialization

import kotlinx.serialization.ElementValueDecoder

abstract class BinaryElementValueDecoder : ElementValueDecoder() {

    open fun decodeByteArray() = this.decodeValue() as ByteArray

}