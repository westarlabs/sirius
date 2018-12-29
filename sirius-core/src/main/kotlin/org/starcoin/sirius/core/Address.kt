package org.starcoin.sirius.core


import com.google.common.base.Preconditions
import com.google.protobuf.ByteString
import kotlinx.serialization.*
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.BinaryDecoder
import org.starcoin.sirius.serialization.BinaryEncoder
import org.starcoin.sirius.util.MockUtils
import org.starcoin.sirius.util.Utils
import java.security.PublicKey

@Serializable
class Address private constructor(private val bytes: ByteArray) : CachedHashable() {

    init {
        Preconditions.checkArgument(bytes.size == LENGTH, "expect address length:$LENGTH")
    }

    fun toBytes() = bytes.copyOf()

    fun toByteString(): ByteString {
        return ByteString.copyFrom(this.bytes)
    }

    override fun toString(): String {
        return Utils.HEX.encode(this.bytes)
    }

    override fun hashData(): ByteArray {
        return this.bytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Address) return false

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    @Serializer(forClass = Address::class)
    companion object : KSerializer<Address> {

        val LENGTH = 20

        val DUMMY_ADDRESS: Address by lazy { CryptoService.getDummyCryptoKey().address }

        @Deprecated("use DUMMY_ADDRESS")
        val DEFAULT_ADDRESS = DUMMY_ADDRESS

        override fun deserialize(input: Decoder): Address {
            return when (input) {
                is BinaryDecoder -> wrap(input.decodeByteArray())
                else -> wrap(input.decodeString())
            }
        }

        override fun serialize(output: Encoder, obj: Address) {
            when (output) {
                is BinaryEncoder -> output.encodeByteArray(obj.bytes)
                else -> output.encodeString(obj.toString())
            }
        }

        fun wrap(addressHex: String): Address {
            Preconditions.checkNotNull(addressHex, "addressHex")
            return Address(Utils.HEX.decode(addressHex))
        }

        fun wrap(address: ByteArray): Address {
            return Address(address)
        }

        fun wrap(address: ByteString): Address {
            Preconditions.checkNotNull(address, "address")
            Preconditions.checkArgument(!address.isEmpty, "address")
            return Address(address.toByteArray())
        }

        fun random(): Address {
            return Address(MockUtils.nextBytes(LENGTH))
        }

        fun getAddress(publicKey: PublicKey): Address {
            return CryptoService.generateAddress(publicKey)
        }
    }
}
