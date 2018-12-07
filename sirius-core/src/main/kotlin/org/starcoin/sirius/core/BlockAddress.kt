package org.starcoin.sirius.core


import com.google.common.base.Preconditions
import com.google.protobuf.ByteString
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin.ProtoBlockAddress
import org.starcoin.sirius.util.HashUtil
import org.starcoin.sirius.util.KeyPairUtil
import org.starcoin.sirius.util.Utils

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.security.PublicKey
import java.util.Arrays

/**
 * BlockMsg Address:
 *
 *
 * RIPEMD160(sha256(public key))
 *
 *
 * pubkey is 65 bytes ecdsa public key result is 20 bytes hash
 *
 * @author Tim
 */
class BlockAddress private constructor(val address: ByteArray) : Protobufable<ProtoBlockAddress>, Hashable {

    init {
        Preconditions.checkArgument(address.size == LENGTH, "expect address length:$LENGTH")
    }

    fun toBytes(): ByteArray {
        // not changeable
        return address.clone()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as BlockAddress
        return Arrays.equals(address, that.address)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(address)
    }

    override fun toString(): String {
        return Utils.HEX.encode(this.address)
    }

    override fun toProto(): ProtoBlockAddress {
        return ProtoBlockAddress.newBuilder().setAddress(ByteString.copyFrom(address)).build()
    }

    override fun hash(): Hash {
        return Hash.of(this.address)
    }

    companion object {

        val LENGTH = 20
        val DEFAULT_ADDRESS = BlockAddress(
            byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
        )

        @Throws(IOException::class)
        fun readFrom(`in`: InputStream): BlockAddress {
            val ad = ByteArray(LENGTH)
            val len = `in`.read(ad)
            Preconditions.checkArgument(
                len == LENGTH, EOFException(BlockAddress::class.java.name + " expect more data")
            )
            return BlockAddress(ad)
        }

        fun valueOf(address: ProtoBlockAddress): BlockAddress {
            return BlockAddress(address.address.toByteArray())
        }

        fun valueOf(addressHex: String): BlockAddress {
            Preconditions.checkNotNull(addressHex, "addressHex")
            return BlockAddress(Utils.HEX.decode(addressHex))
        }

        fun random(): BlockAddress {
            return BlockAddress(RandomUtils.nextBytes(LENGTH))
        }

        fun genBlockAddressFromPublicKey(publicKey: PublicKey): BlockAddress {
            return BlockAddress(HashUtil.hash160(HashUtil.sha256(KeyPairUtil.encodePublicKey(publicKey, true))))
        }
    }
}
