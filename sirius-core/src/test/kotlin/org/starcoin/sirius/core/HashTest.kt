package org.starcoin.sirius.core

import com.google.protobuf.InvalidProtocolBufferException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.io.ByteBufferInputStream
import org.starcoin.sirius.io.ByteBufferOutputStream
import org.starcoin.proto.Starcoin.ProtoChainHash

class HashTest {

    @Test
    fun testChainHash() {
        val bytes = RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(1, 1000)).toByteArray()
        val chainHash = Hash.of(bytes)
        val chainHash1 = Hash.of(ByteBuffer.allocate(bytes.size).put(bytes))

        Assert.assertEquals(chainHash, chainHash1)
        Assert.assertEquals(chainHash.toString(), chainHash1.toString())

        val hash = ByteArray(Hash.LENGTH)
        val hash1 = ByteArray(Hash.LENGTH)
        val hash2 = ByteArray(Hash.LENGTH)

        chainHash.getBytes().get(hash)
        chainHash1.getBytes().get(hash1)
        // repeat read
        chainHash1.getBytes().get(hash2)

        Assert.assertArrayEquals(hash, hash1)
        Assert.assertArrayEquals(hash1, hash2)

        Assert.assertEquals(chainHash.getBytes().remaining().toLong(), Hash.LENGTH.toLong())

        try {
            chainHash.getBytes().putInt(0)
            Assert.fail()
        } catch (e: ReadOnlyBufferException) {
        }

    }

    @Test
    @Throws(IOException::class)
    fun testChainHashReadWrite() {
        val bytes = RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(1, 1000)).toByteArray()
        val bytes1 = RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(1, 1000)).toByteArray()
        val chainHash = Hash.of(bytes)
        val chainHash1 = Hash.of(bytes1)

        val buf = ByteBuffer.allocate(Hash.LENGTH * 2)
        val out = ByteBufferOutputStream(buf)

        chainHash.writeTo(out)
        chainHash1.writeTo(out)
        buf.flip()

        val `in` = ByteBufferInputStream(buf)
        val chainHashN = Hash.readFrom(`in`)
        val chainHashN1 = Hash.readFrom(`in`)

        Assert.assertEquals(chainHash, chainHashN)
        Assert.assertEquals(chainHash1, chainHashN1)
    }

    @Test
    fun testHash() {
        val bytes = RandomUtils.nextBytes(RandomUtils.nextInt(1, 1024))
        val buf = ByteBuffer.allocate(bytes.size)
        buf.put(bytes)
        buf.flip()

        Assert.assertEquals(bytes.size.toLong(), buf.remaining().toLong())

        val hash = Hash.hash(bytes)
        val hash2 = Hash.hash(buf)
        Assert.assertArrayEquals(hash, hash2)
    }

    @Test
    fun testChecksum() {
        val bytes = RandomUtils.nextBytes(RandomUtils.nextInt(1, 1024))
        val buf = ByteBuffer.allocate(bytes.size)
        buf.put(bytes)
        buf.flip()

        val checksum = Hash.checksum(bytes)
        Assert.assertEquals(4, checksum.size.toLong())

        val checksum2 = Hash.checksum(buf)
        Assert.assertArrayEquals(checksum, checksum2)
    }

    @Test
    fun testEquals() {
        val hash = Hash.random()
        val hash1 = Hash.wrap(hash.toString())
        Assert.assertEquals(hash, hash1)
    }


    @Test
    @Throws(InvalidProtocolBufferException::class)
    fun testProto() {
        val hash = Hash.random()
        val protoChainHash = hash.toProto()
        val hash1 = Hash.wrap(ProtoChainHash.parseFrom(protoChainHash.toByteString()))
        val hash2 = Hash.wrap(hash.toProto())
        Assert.assertEquals(hash, hash1)
        Assert.assertEquals(hash, hash2)
    }

    @Test
    fun testByteBuffer() {
        val rand = RandomUtils.nextInt(10, 1024)
        val buffer = ByteBuffer.allocate(rand).put(RandomUtils.nextBytes(rand))
        val hash = Hash.of(buffer)

        val hash1 = Hash.wrap(hash.toProto())
        Assert.assertEquals(hash, hash1)
    }

    @Test
    @Throws(InvalidProtocolBufferException::class)
    fun testToBytes() {
        val hash = Hash.random()
        val buffer = hash.getBytes()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        Assert.assertEquals(0, buffer.remaining().toLong())

        val protoChainHash = hash.toProto()
        val hash1 = Hash.wrap(ProtoChainHash.parseFrom(protoChainHash.toByteString()))
        Assert.assertEquals(hash, hash1)
    }

    @Test
    fun testCombine() {
        val hash = Hash.random()
        val hash1 = Hash.random()
        val hash2 = Hash.combine(hash, hash1)
        Assert.assertNotNull(hash2)

        Assert.assertEquals(Hash.LENGTH.toLong(), hash.getBytes().remaining().toLong())
        Assert.assertEquals(Hash.LENGTH.toLong(), hash1.getBytes().remaining().toLong())
        Assert.assertEquals(Hash.LENGTH.toLong(), hash2.getBytes().remaining().toLong())
    }
}
