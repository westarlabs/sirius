package org.starcoin.sirius.util

import org.apache.commons.lang3.RandomUtils
import org.ethereum.crypto.ECKey
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPair
import java.security.Security

class KeyPairUtilTest {
    @Test
    @Throws(Exception::class)
    fun testAddress() {
        val keyPair = KeyPairUtil.generateKeyPair()
        val pk = KeyPairUtil.encodePublicKey(keyPair.public, false)
        assertEquals(pk.size.toLong(), 65)

        val pk2 = KeyPairUtil.encodePublicKey(keyPair.public, true)
        assertEquals(pk2.size.toLong(), 33)
    }

    @Test
    fun testRecovery() {
        val keyPair: KeyPair
        try {
            keyPair = KeyPairUtil.generateKeyPair()
            val pks = KeyPairUtil.encodePublicKey(keyPair.public, true)
            val pks2 = keyPair.private.encoded

            val pubKey = KeyPairUtil.recoverPublicKey(pks)
            val privateKey = KeyPairUtil.recoverPrivateKey(pks2)

            val src = "123456".toByteArray()
            val data1 = KeyPairUtil.signData(src, privateKey)

            val r1 = KeyPairUtil.verifySig(src, pubKey, data1)

            assertTrue(r1)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Test
    @Throws(Exception::class)
    fun testReadKeystore() {
        Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
    }

    @Test
    @Throws(Exception::class)
    fun testKeystore() {
        val dir = Files.createTempDirectory(
            "starcoin",
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx"))
        )
            .toFile()
        genKeystore(dir)
        FileUtil.deleteDir(dir)
    }

    @Test
    fun testSignature() {

        val kp = KeyPairUtil.generateKeyPair()
        for (i in 0..9) {
            val count = RandomUtils.nextInt(32, 128)
            val data = RandomUtils.nextBytes(count)
            val sign = KeyPairUtil.signData(data, kp.private)
            assertTrue(KeyPairUtil.verifySig(data, kp.public, sign))
        }
    }

    @Test
    fun testPrivateKeyEncodeAndRecover() {
        val kp = KeyPairUtil.generateKeyPair()
        val bytes = KeyPairUtil.encodePrivateKey(kp.private)
        val privateKey = KeyPairUtil.recoverPrivateKey(bytes)
        Assert.assertArrayEquals(bytes, KeyPairUtil.encodePrivateKey(privateKey))
    }

    @Test
    fun testfromECKey(){
        var eckey = ECKey()

        var keyPair=KeyPairUtil.fromECKey(eckey)

        println(keyPair.private.javaClass)
        println((keyPair.private as BCECPrivateKey).d)
        var ecKey2=KeyPairUtil.getECKey(keyPair)

        assertEquals(eckey,ecKey2)
    }

    companion object {

        @Throws(Exception::class)
        fun genKeystore(dir: File) {
            val privFile = File(dir, "test.pem")
            val pubFile = File(dir, "test.pub")

            val keyPair = KeyPairUtil.generateKeyPair()
            KeyPairUtil.savePemPrivateKey(keyPair.private, privFile)
            KeyPairUtil.savePemPublicKey(keyPair.public, pubFile)

            val privKey = KeyPairUtil.loadPemPrivateKey(privFile)
            val pubKey = KeyPairUtil.loadPemPublicKey(pubFile)

            assertEquals(privKey.getAlgorithm(), "ECDSA")

            assertArrayEquals(keyPair.private.encoded, privKey.getEncoded())
            assertArrayEquals(keyPair.public.encoded, pubKey.getEncoded())
        }
    }
}
