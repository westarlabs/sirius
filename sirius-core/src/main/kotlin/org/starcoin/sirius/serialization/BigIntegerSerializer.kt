package org.starcoin.sirius.serialization

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import java.math.BigInteger

@Serializer(forClass = BigInteger::class)
class BigIntegerSerializer : KSerializer<BigInteger> {
    override fun deserialize(decoder: Decoder): BigInteger {
        return when (decoder) {
            is BinaryDecoder -> decoder.decodeBigInteger()
            else -> BigInteger(
                decoder.decodeString().hexToByteArray()
            )
        }
    }

    override fun serialize(encoder: Encoder, obj: BigInteger) {
        when (encoder) {
            is BinaryEncoder -> encoder.encodeBigInteger(obj)
            else -> encoder.encodeString(
                obj.toByteArray().toHEXString()
            )
        }
    }
}
