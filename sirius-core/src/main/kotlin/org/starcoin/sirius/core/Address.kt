package org.starcoin.sirius.core


import com.google.protobuf.ByteString
import kotlinx.serialization.*
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.serialization.BinaryDecoder
import org.starcoin.sirius.serialization.BinaryEncoder
import org.starcoin.sirius.serialization.Codec
import org.starcoin.sirius.util.MockUtils
import java.security.PublicKey

@Serializable
class Address private constructor(internal val bytes: ByteArray) : CachedHashable(), ToByteArray {

    init {
        require(bytes.size == LENGTH) { "expect address length:$LENGTH" }
    }

    override fun toBytes() = bytes.copyOf()

    fun toByteString(): ByteString {
        return ByteString.copyFrom(this.bytes)
    }

    override fun toString(): String {
        return this.bytes.toHEXString()
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

    fun toProto(): Starcoin.ProtoBlockAddress {
        return Starcoin.ProtoBlockAddress.newBuilder().setAddress(ByteString.copyFrom(bytes)).build()
    }

    @Serializer(forClass = Address::class)
    companion object : KSerializer<Address>, Codec<Address> {

        val LENGTH = 20

        val DUMMY_ADDRESS: Address by lazy { CryptoService.dummyCryptoKey.address }

        val ZERO_ADDRESS = Address.wrap(ByteArray(LENGTH))

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
            return Address(addressHex.hexToByteArray())
        }

        fun wrap(address: ByteArray): Address {
            return Address(address)
        }

        fun wrap(address: ByteString): Address {
            require(!address.isEmpty)
            return Address(address.toByteArray())
        }

        fun random(): Address {
            return Address(MockUtils.nextBytes(LENGTH))
        }

        fun getAddress(publicKey: PublicKey): Address {
            return CryptoService.generateAddress(publicKey)
        }

        override fun decode(bytes: ByteArray): Address {
            return Address.wrap(bytes)
        }

        override fun encode(value: Address): ByteArray {
            return value.bytes
        }
    }
}

fun ByteArray.toAddress() = Address.wrap(this)
fun String.toAddress() = Address.wrap(this.let {
    if (this.startsWith("0x")) this.substring(2) else this
})
