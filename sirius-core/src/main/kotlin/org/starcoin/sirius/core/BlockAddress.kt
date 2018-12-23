package org.starcoin.sirius.core


import com.google.common.base.Preconditions
import com.google.protobuf.ByteString
import kotlinx.serialization.*
import org.apache.commons.lang3.RandomUtils
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.BinaryDecoder
import org.starcoin.sirius.serialization.BinaryEncoder
import org.starcoin.sirius.util.Utils
import java.security.PublicKey

@Serializable
class BlockAddress private constructor(private val bytes: ByteArray) : CachedHash() {

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
        if (other !is BlockAddress) return false

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    @Serializer(forClass = BlockAddress::class)
    companion object : KSerializer<BlockAddress> {

        val LENGTH = 20

        val DUMMY_ADDRESS:BlockAddress by lazy { CryptoService.getDummyCryptoKey().getAddress() }

        @Deprecated("use DUMMY_ADDRESS")
        val DEFAULT_ADDRESS = DUMMY_ADDRESS

        override fun deserialize(input: Decoder): BlockAddress {
            return when (input) {
                is BinaryDecoder -> wrap(input.decodeByteArray())
                else -> wrap(input.decodeString())
            }
        }

        override fun serialize(output: Encoder, obj: BlockAddress) {
            when (output) {
                is BinaryEncoder -> output.encodeByteArray(obj.bytes)
                else -> output.encodeString(obj.toString())
            }
        }

        fun wrap(addressHex: String): BlockAddress {
            Preconditions.checkNotNull(addressHex, "addressHex")
            return BlockAddress(Utils.HEX.decode(addressHex))
        }

        fun wrap(address: ByteArray): BlockAddress {
            return BlockAddress(address)
        }

        fun wrap(address: ByteString): BlockAddress {
            Preconditions.checkNotNull(address, "address")
            Preconditions.checkArgument(!address.isEmpty, "address")
            return BlockAddress(address.toByteArray())
        }

        fun random(): BlockAddress {
            return BlockAddress(RandomUtils.nextBytes(LENGTH))
        }

        fun getAddress(publicKey: PublicKey): BlockAddress {
            return CryptoService.getAddress(publicKey)
        }
    }
}
