package org.starcoin.sirius.core

import com.google.common.base.Preconditions
import com.google.common.base.Preconditions.checkArgument
import com.google.common.io.ByteStreams
import com.google.common.primitives.Ints
import com.google.protobuf.ByteString
import com.google.protobuf.UnsafeByteOperations
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.util.HashUtil
import org.starcoin.sirius.util.Utils
import java.io.*
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.experimental.and


/**
 * A ChainHash just wraps a byte[] so that equals and hashcode work correctly, allowing it to be
 * used as keys in a map. It also checks that the length is correct and provides a bit more type
 * safety. It is a final unmodifiable object.
 */
class Hash private constructor(private val bytes: ByteBuffer) : Serializable, Comparable<Hash> {

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is Hash) {
            return false
        }
        val chainHash = o as Hash?
        // TODO ensure
        return this.compareTo(chainHash!!) == 0
    }

    /**
     * Returns the last four bytes of the wrapped hash. This should be unique enough to be a suitable
     * hash code even for blocks, where the goal is to try and get the first bytes to be zeros (i.e.
     * the value as a big integer lower than the target value).
     */
    override fun hashCode(): Int {
        // Use the last 4 bytes, not the first 4 which are often zeros.
        return Ints.fromBytes(
            bytes.get(LENGTH - 4), bytes.get(LENGTH - 3), bytes.get(LENGTH - 2), bytes.get(LENGTH - 1)
        )
    }

    override fun toString(): String {
        return Utils.HEX.encode(bytes.array())
    }

    fun toMD5Hex(): String {
        return HashUtil.md5Hex(bytes.array())
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     */
    fun toBigInteger(): BigInteger {
        return BigInteger(1, bytes.array())
    }

    fun getBytes() = bytes.asReadOnlyBuffer()
    fun toBytes() = this.getBytes()

    fun toByteString() = ByteString.copyFrom(this.getBytes())

    override fun compareTo(other: Hash): Int {
        for (i in LENGTH - 1 downTo 0) {
            val thisByte = this.bytes.get(i) and 0xff.toByte()
            val otherByte = other.bytes.get(i) and 0xff.toByte()
            if (thisByte > otherByte) {
                return 1
            }
            if (thisByte < otherByte) {
                return -1
            }
        }
        return 0
    }

    /**
     * Write hash bytes to out
     */
    @Throws(IOException::class)
    fun writeTo(out: OutputStream) {
        out.write(this.bytes.array())
    }

    companion object {

        val LENGTH = 32 // bytes
        val CHECKSUM_LENGTH = 4 // bytes

        val ZERO_HASH = wrap(ByteArray(LENGTH))

        /**
         * Creates a new instance that wraps the given hash value. Keep private for unmodifiable object.
         *
         * @param rawHashBytes the raw hash bytes to readFrom
         * @return a new instance
         * @throws IllegalArgumentException if the given array length is not exactly 32
         */
        fun wrap(rawHashBytes: ByteArray): Hash {
            checkArgument(rawHashBytes.size == LENGTH, "unexpected hash length:" + rawHashBytes.size)
            val bytes = ByteBuffer.allocate(rawHashBytes.size).put(rawHashBytes)
            bytes.flip()
            return Hash(bytes)
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
            return wrap(Utils.HEX.decode(hexString))
        }

        fun wrap(byteString: ByteString): Hash {
            return wrap(byteString.toByteArray())
        }

        /**
         * combine tow hash to one.
         */
        fun combine(left: Hash?, right: Hash?): Hash {
            Preconditions.checkArgument(left != null || right != null, "left and right both null.")
            val leftLength = left?.bytes?.capacity() ?: 0
            val rightLength = right?.bytes?.capacity() ?: 0
            val bb = ByteBuffer.allocate(leftLength + rightLength)
            // ByteBuffer.put(buf) affect the argument buf's position, so use asReadOnlyBuffer
            if (left != null) {
                bb.put(left.bytes.asReadOnlyBuffer())
            }
            if (right != null) {
                bb.put(right.bytes.asReadOnlyBuffer())
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
            return wrap(hash(contents))
        }

        fun of(buffer: ByteBuffer): Hash {
            if (buffer.hasArray()) {
                return wrap(hash(buffer.array()))
            } else {
                buffer.mark()
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                buffer.reset()
                return wrap(hash(bytes))
            }
        }

        fun of(obj: SiriusObject): Hash {
            TODO()
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

        /**
         * Returns a new SHA-256 MessageDigest instance.
         *
         *
         * This is a convenience method which wraps the checked exception that can never occur with a
         * RuntimeException.
         *
         * @return a new SHA-256 MessageDigest instance
         */
        fun newDigest(): MessageDigest {
            try {
                return MessageDigest.getInstance("SHA-256")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e) // Can't happen.
            }

        }

        /**
         * Calculates the SHA-256 hash of the given byte range.
         *
         * @param input  the array containing the bytes to hash
         * @param offset the offset within the array of the bytes to hash
         * @param length the number of bytes to hash
         * @return the hash (in big-endian order)
         */
        @JvmOverloads
        fun hash(input: ByteArray, offset: Int = 0, length: Int = input.size): ByteArray {
            val digest = newDigest()
            digest.update(input, offset, length)
            return digest.digest()
        }

        fun hash(input: ByteBuffer): ByteArray {
            val digest = newDigest()
            digest.update(input)
            return digest.digest()
        }

        /**
         * @see .checksum
         */
        fun checksum(input: ByteBuffer): ByteArray {
            return Arrays.copyOfRange(hash(input), 0, 4)
        }

        /**
         * Calculates checksum, SHA-256 hash of the given bytes and return first 4 byte.
         */
        fun checksum(input: ByteArray): ByteArray {
            return Arrays.copyOfRange(hash(input), 0, CHECKSUM_LENGTH)
        }

        /**
         * Read bytes and create ChainHash object.
         */
        @Throws(IOException::class)
        fun readFrom(`in`: InputStream): Hash {
            val bytes = ByteArray(Hash.LENGTH)
            val len = `in`.read(bytes)
            if (len != LENGTH) {
                throw EOFException("unexpected enf of stream to parse Sha256Hash")
            }
            return Hash.wrap(bytes)
        }

        fun random(): Hash {
            return Hash.of(RandomUtils.nextBytes(100))
        }
    }
}
