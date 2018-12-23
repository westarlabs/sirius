package org.starcoin.sirius.serialization

import kotlinx.serialization.Decoder


interface BinaryDecoder : Decoder {
    fun decodeByteArray(): ByteArray
}
