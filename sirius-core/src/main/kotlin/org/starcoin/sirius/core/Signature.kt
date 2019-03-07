package org.starcoin.sirius.core

import com.google.protobuf.ByteString
import kotlinx.serialization.*
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.serialization.BinaryDecoder
import org.starcoin.sirius.serialization.BinaryEncoder
import java.security.PublicKey

@Serializable
class Signature private constructor(private val bytes: ByteArray) {

    val size: Int
        get() = bytes.size

    fun verify(data: ByteArray, publicKey: PublicKey): Boolean {
        return CryptoService.verify(data, this, publicKey)
    }

    fun verify(data: SiriusObject, publicKey: PublicKey): Boolean {
        return CryptoService.verify(data, this, publicKey)
    }

    fun verify(data: ByteArray, key: CryptoKey) = key.verify(data, this)

    fun verify(data: SiriusObject, key: CryptoKey) = key.verify(data, this)

    override fun toString(): String {
        return this.bytes.toHEXString()
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

    fun isZero() = bytes.contentEquals(ZERO_SIGN.bytes)

    @Serializer(forClass = Signature::class)
    companion object : KSerializer<Signature> {

        override fun deserialize(decoder: Decoder): Signature {
            return when (decoder) {
                is BinaryDecoder -> wrap(decoder.decodeByteArray())
                else -> this.wrap(decoder.decodeString())
            }
        }

        override fun serialize(encoder: Encoder, obj: Signature) {
            when (encoder) {
                is BinaryEncoder -> encoder.encodeByteArray(obj.bytes)
                else -> encoder.encodeString(obj.toString())
            }
        }

        val ZERO_SIGN = ofDummyKey(Hash.EMPTY_DADA_HASH.toBytes())

        fun wrap(hexString: String): Signature {
            return wrap(hexString.hexToByteArray())
        }

        fun wrap(sign: ByteArray): Signature {
            return Signature(sign)
        }

        fun wrap(byteString: ByteString): Signature {
            return Signature(byteString.toByteArray())
        }

        fun of(data: ByteArray, key: CryptoKey) = key.sign(data)

        fun of(data: SiriusObject, key: CryptoKey) = key.sign(data)

        fun of(data: Hash, key: CryptoKey) = key.sign(data)

        @JvmStatic
        fun ofDummyKey(data: ByteArray): Signature {
            return CryptoService.dummyCryptoKey.sign(data)
        }

        fun random(): Signature {
            return CryptoService.dummyCryptoKey.sign(Hash.random())
        }
    }

}
