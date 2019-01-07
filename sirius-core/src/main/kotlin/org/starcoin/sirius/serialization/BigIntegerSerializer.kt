package org.starcoin.sirius.serialization

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import org.starcoin.sirius.util.Utils
import java.math.BigInteger

@Serializer(forClass = BigInteger::class)
class BigIntegerSerializer : KSerializer<BigInteger> {
    override fun deserialize(input: Decoder): BigInteger {
        return when (input) {
            is BinaryDecoder -> input.decodeBigInteger()
            else -> BigInteger(
                Utils.HEX.decode(
                    input.decodeString()
                )
            )
        }
    }

    override fun serialize(output: Encoder, obj: BigInteger) {
        when (output) {
            is BinaryEncoder -> output.encodeBigInteger(obj)
            else -> output.encodeString(
                Utils.HEX.encode(
                    obj.toByteArray()
                )
            )
        }
    }
}
