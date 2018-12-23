package org.starcoin.sirius.serialization

import kotlinx.serialization.*
import org.starcoin.sirius.util.Utils

@Serializable
data class ByteArrayWrapper(val byteArray: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByteArrayWrapper) return false

        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        return byteArray.contentHashCode()
    }

    @Serializer(forClass = ByteArrayWrapper::class)
    companion object : KSerializer<ByteArrayWrapper> {

        override fun deserialize(input: Decoder): ByteArrayWrapper {
            return when (input) {
                is BinaryDecoder -> ByteArrayWrapper(
                    input.decodeByteArray()
                )
                else -> ByteArrayWrapper(
                    Utils.HEX.decode(
                        input.decodeString()
                    )
                )
            }
        }

        override fun serialize(output: Encoder, obj: ByteArrayWrapper) {
            when (output) {
                is BinaryEncoder -> output.encodeByteArray(obj.byteArray)
                else -> output.encodeString(Utils.HEX.encode(obj.byteArray))
            }
        }

        fun valueOf(bytes: ByteArray): ByteArrayWrapper {
            return ByteArrayWrapper(bytes)
        }
    }
}
