package org.starcoin.sirius.serialization

import kotlinx.serialization.Encoder

interface BinaryEncoder :Encoder{

    fun encodeByteArray(byteArray: ByteArray)
}
