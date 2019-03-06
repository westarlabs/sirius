package org.starcoin.sirius.core

import com.google.common.io.ByteStreams
import com.google.common.primitives.Ints
import com.google.protobuf.ByteString
import kotlinx.serialization.*
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.serialization.BinaryDecoder
import org.starcoin.sirius.serialization.BinaryEncoder
import org.starcoin.sirius.serialization.Codec
import org.starcoin.sirius.util.HashUtil
import org.starcoin.sirius.util.MockUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and


/**
 * A ChainHash just wraps a byte[] so that equals and hashcode work correctly, allowing it to be
 * used as keys in a map. It also checks that the length is correct and provides a bit more type
 * safety. It is a final unmodifiable object.
 */
@Serializable
class Hash private constructor(internal val bytes: ByteArray) : Comparable<Hash>, Hashable, ToByteArray {

    val size: Int
        get() = bytes.size

    /**
     * Returns the last four bytes of the wrapped hash. This should be unique enough to be a suitable
     * hash code even for blocks, where the goal is to try and get the first bytes to be zeros (i.e.
     * the value as a big integer lower than the target value).
     */
    override fun hashCode(): Int {
        // Use the last 4 bytes, not the first 4 which are often zeros.
        return Ints.fromBytes(
            bytes[LENGTH - 4], bytes[LENGTH - 3], bytes[LENGTH - 2], bytes[LENGTH - 1]
        )
    }

    override fun toString(): String {
        return this.bytes.toHEXString()
    }

    fun toMD5Hex(): String {
        return HashUtil.md5Hex(bytes)
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     */
    fun toBigInteger(): BigInteger {
        return BigInteger(1, bytes)
    }

    override fun toBytes() = bytes.copyOf()

    fun toByteString() = ByteString.copyFrom(this.bytes)

    override fun compareTo(other: Hash): Int {
        for (i in LENGTH - 1 downTo 0) {
            val thisByte = this.bytes[i] and 0xff.toByte()
            val otherByte = other.bytes[i] and 0xff.toByte()
            if (thisByte > otherByte) {
                return 1
            }
            if (thisByte < otherByte) {
                return -1
            }
        }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Hash) return false

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hash(): Hash {
        return this
    }

    @Serializer(forClass = Hash::class)
    companion object : KSerializer<Hash>, Codec<Hash> {

        val LENGTH = 32 // bytes
        val CHECKSUM_LENGTH = 4 // bytes

        val ZERO_HASH = wrap(ByteArray(LENGTH))
        val EMPTY_LIST_HASH by lazy { CryptoService.emptyListHash }
        val EMPTY_DADA_HASH by lazy { CryptoService.emptyDataHash }

        override fun deserialize(input: Decoder): Hash {
            return when (input) {
                is BinaryDecoder -> Hash.wrap(input.decodeByteArray())
                else -> Hash.wrap(input.decodeString())
            }
        }

        override fun serialize(output: Encoder, obj: Hash) {
            when (output) {
                is BinaryEncoder -> output.encodeByteArray(obj.bytes)
                else -> output.encodeString(obj.toString())
            }
        }

        /**
         * Creates a new instance that wraps the given hash value. Keep private for unmodifiable object.
         *
         * @param rawHashBytes the raw hash bytes to readFrom
         * @return a new instance
         * @throws IllegalArgumentException if the given array length is not exactly 32
         */
        fun wrap(rawHashBytes: ByteArray): Hash {
            require(rawHashBytes.size == LENGTH) { "unexpected hash length:${rawHashBytes.size}" }
            return Hash(rawHashBytes)
        }

        /**
         * Creates a new instance that wraps the given hash value (represented as a hex string).
         *
         * @param hexString a hash value represented as a hex string
         * @return a new instance
         * @throws IllegalArgumentException if the given string is not a valid hex string, or if it does
         * not represent exactly 32 bytes
         */
        fun wrap(hexString: String): Hash {
            return wrap(hexString.hexToByteArray())
        }

        fun wrap(byteString: ByteString): Hash {
            return wrap(byteString.toByteArray())
        }

        /**
         * combine tow hash to one.
         */
        fun combine(left: Hash?, right: Hash?): Hash {
            require(left != null || right != null) { "left and right both null." }
            val leftLength = left?.bytes?.size ?: 0
            val rightLength = right?.bytes?.size ?: 0
            val bb = ByteArray(leftLength + rightLength)
            // ByteBuffer.put(buf) affect the argument buf's position, so use asReadOnlyBuffer
            if (left != null) {
                System.arraycopy(left.bytes, 0, bb, 0, left.bytes.size)
            }
            if (right != null) {
                System.arraycopy(right.bytes, 0, bb, left?.bytes?.size ?: 0, right.bytes.size)
            }
            return of(bb)
        }

        /**
         * Creates a new instance containing the calculated (one-time) hash of the given bytes.
         *
         * @param contents the bytes on which the hash value is calculated
         * @return a new instance containing the calculated (one-time) hash
         */
        fun of(contents: ByteArray): Hash {
            return hash(contents)
        }

        fun of(buffer: ByteBuffer): Hash {
            if (buffer.hasArray()) {
                return hash(buffer.array())
            } else {
                buffer.mark()
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                buffer.reset()
                return hash(bytes)
            }
        }

        fun of(obj: SiriusObject): Hash {
            return CryptoService.hash(obj)
        }

        /**
         * Creates a new instance containing the calculated (one-time) hash of the given file's contents.
         *
         *
         * The file contents are read fully into memory, so this method should only be used with small
         * files.
         *
         * @param file the file on which the hash value is calculated
         * @return a new instance containing the calculated (one-time) hash
         * @throws IOException if an error occurs while reading the file
         */
        @Throws(IOException::class)
        fun of(file: File): Hash {
            val `in` = FileInputStream(file)
            try {
                return of(ByteStreams.toByteArray(`in`))
            } finally {
                `in`.close()
            }
        }

        private fun hash(input: ByteArray): Hash {
            return CryptoService.hash(input)
        }

        /**
         * @see .checksum
         */
        fun checksum(input: ByteBuffer): ByteArray {
            return Arrays.copyOfRange(of(input).bytes, 0, 4)
        }

        /**
         * Calculates checksum, SHA-256 hash of the given bytes and return first 4 byte.
         */
        fun checksum(input: ByteArray): ByteArray {
            return Arrays.copyOfRange(hash(input).bytes, 0, CHECKSUM_LENGTH)
        }

        fun random(): Hash {
            return Hash.of(MockUtils.nextBytes(LENGTH))
        }

        override fun encode(value: Hash): ByteArray {
            return value.bytes
        }

        override fun decode(bytes: ByteArray): Hash {
            return Hash.wrap(bytes)
        }
    }
}

fun ByteArray.toHash() = Hash.wrap(this)
fun String.toHash() = Hash.wrap(this)
