package org.starcoin.sirius.core

import com.google.protobuf.ByteString
import kotlinx.serialization.*
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.BinaryDecoder
import org.starcoin.sirius.serialization.BinaryEncoder
import org.starcoin.sirius.util.Utils
import java.security.PrivateKey
import java.security.PublicKey

@Serializable
class Signature private constructor(internal val bytes: ByteArray) {

    val size: Int
        get() = bytes.size

    fun verify(data: ByteArray, publicKey: PublicKey): Boolean {
        return CryptoService.verify(data, this, publicKey)
    }

    override fun toString(): String {
        return Utils.HEX.encode(this.bytes)
    }

    fun toBytes() = this.bytes.copyOf()

    fun toByteString(): ByteString = ByteString.copyFrom(this.bytes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Signature) return false

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    fun isZero() = bytes.all { it == 0.toByte() }

    @Serializer(forClass = Signature::class)
    companion object : KSerializer<Signature> {

        override fun deserialize(input: Decoder): Signature {
            return when (input) {
                is BinaryDecoder -> wrap(input.decodeByteArray())
                else -> this.wrap(input.decodeString())
            }
        }

        override fun serialize(output: Encoder, obj: Signature) {
            when (output) {
                is BinaryEncoder -> output.encodeByteArray(obj.bytes)
                else -> output.encodeString(obj.toString())
            }
        }

        val ZERO_SIGN = wrap(ByteArray(4))

        fun wrap(hexString: String): Signature {
            return wrap(Utils.HEX.decode(hexString))
        }

        fun wrap(sign: ByteArray): Signature {
            return Signature(sign)
        }

        fun wrap(byteString: ByteString): Signature {
            return Signature(byteString.toByteArray())
        }

        fun of(data: ByteArray, privateKey: PrivateKey): Signature {
            return CryptoService.sign(data, privateKey)
        }

        fun ofDummyKey(data: ByteArray): Signature {
            return CryptoService.getDummyCryptoKey().sign(data)
        }

    }

}
