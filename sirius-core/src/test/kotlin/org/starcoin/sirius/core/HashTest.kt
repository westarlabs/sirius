package org.starcoin.sirius.core

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer

class HashTest {

    @Test
    fun testChainHash() {
        val bytes = RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(1, 1000)).toByteArray()
        val chainHash = Hash.of(bytes)
        val chainHash1 = Hash.of(ByteBuffer.allocate(bytes.size).put(bytes))

        Assert.assertEquals(chainHash, chainHash1)
        Assert.assertEquals(chainHash.toString(), chainHash1.toString())

        val hash = chainHash.toBytes()
        val hash1 = chainHash1.toBytes()
        // repeat read
        val hash2 = chainHash1.toBytes()


        Assert.assertArrayEquals(hash, hash1)
        Assert.assertArrayEquals(hash1, hash2)

        Assert.assertEquals(chainHash.size, Hash.LENGTH)

        //not change
        chainHash.toBytes().fill(0)
        Assert.assertEquals(Hash.of(bytes), chainHash)
    }

    @Test
    fun testHash() {
        val bytes = RandomUtils.nextBytes(RandomUtils.nextInt(1, 1024))
        val buf = ByteBuffer.allocate(bytes.size)
        buf.put(bytes)
        buf.flip()

        Assert.assertEquals(bytes.size.toLong(), buf.remaining().toLong())

        val hash = Hash.of(bytes)
        val hash2 = Hash.of(buf)
        Assert.assertEquals(hash, hash2)
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
    fun testProto() {
        val hash = Hash.random()
        val protoChainHash = hash.toByteString()
        val hash1 = Hash.wrap(protoChainHash)
        Assert.assertEquals(hash, hash1)
    }

    @Test
    fun testByteBuffer() {
        val rand = RandomUtils.nextInt(10, 1024)
        val buffer = ByteBuffer.allocate(rand).put(RandomUtils.nextBytes(rand))
        val hash = Hash.of(buffer)

        val hash1 = Hash.wrap(hash.toByteString())
        Assert.assertEquals(hash, hash1)
    }

    @Test
    fun testCombine() {
        val hash = Hash.random()
        val hash1 = Hash.random()
        val hash2 = Hash.combine(hash, hash1)
        Assert.assertNotNull(hash2)

        val hash3 = Hash.combine(hash, null)
        val hash4 = Hash.combine(null, hash1)

        Assert.assertEquals(Hash.LENGTH, hash.size)
        Assert.assertEquals(Hash.LENGTH, hash1.size)
        Assert.assertEquals(Hash.LENGTH, hash2.size)
        Assert.assertEquals(Hash.LENGTH, hash3.size)
        Assert.assertEquals(Hash.LENGTH, hash4.size)
    }

}
