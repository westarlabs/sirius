package org.starcoin.sirius.serialization

import com.google.protobuf.ByteString
import kotlinx.serialization.*
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString

@Serializable
class ByteArrayWrapper(val bytes: ByteArray) {

    val size: Int
        get() = bytes.size

    override fun toString(): String {
        return bytes.toHEXString()
    }

    fun toBytes() = this.bytes

    fun toByteString() = ByteString.copyFrom(this.bytes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByteArrayWrapper) return false

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    @Serializer(forClass = ByteArrayWrapper::class)
    companion object : KSerializer<ByteArrayWrapper> {

        fun wrap(bytes: ByteArray): ByteArrayWrapper {
            return ByteArrayWrapper(bytes)
        }

        fun wrap(hexString: String): ByteArrayWrapper {
            return ByteArrayWrapper(hexString.hexToByteArray())
        }

        override fun deserialize(input: Decoder): ByteArrayWrapper {
            return when (input) {
                is BinaryDecoder -> ByteArrayWrapper.wrap(input.decodeByteArray())
                else -> ByteArrayWrapper.wrap(input.decodeString())
            }
        }

        override fun serialize(output: Encoder, obj: ByteArrayWrapper) {
            when (output) {
                is BinaryEncoder -> output.encodeByteArray(obj.bytes)
                else -> output.encodeString(obj.toString())
            }
        }
    }
}

