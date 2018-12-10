package org.starcoin.sirius.util

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class EncryptUtilTest {

    @Test
    fun testEncryptAndDecrypt() {
        val keyPair = KeyPairUtil.generateKeyPair()
        val publicKey = keyPair.public
        val privateKey = keyPair.private

        val data = "hello".toByteArray()
        val t1 = System.currentTimeMillis()
        val edata = EncryptUtil.encrypt(publicKey, data)
        val t2 = System.currentTimeMillis()
        println("encrypt length" + edata.size)
        val odata = EncryptUtil.decrypt(privateKey, edata)
        val t3 = System.currentTimeMillis()
        println("decrypt length: " + odata.size)
        println(String(odata))
        assertArrayEquals(data, odata)
        val e1 = t2 - t1
        val e2 = t3 - t2
        println("time1: $e1")
        println("time2: $e2")
    }
}