package org.starcoin.sirius.util

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.Security
import kotlin.experimental.and

object HashUtil {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(value.ushr(24).toByte(), value.ushr(16).toByte(), value.ushr(8).toByte(), value.toByte())
    }

    fun longToByteArray(value: Long): ByteArray {
        return byteArrayOf(
            value.ushr(56).toByte(),
            value.ushr(48).toByte(),
            value.ushr(40).toByte(),
            value.ushr(32).toByte(),
            value.ushr(24).toByte(),
            value.ushr(16).toByte(),
            value.ushr(8).toByte(),
            value.toByte()
        )
    }

    fun sha256(src: Int): ByteArray? {
        return sha256(intToByteArray(src))
    }

    fun sha256(src: ByteArray): ByteArray {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(src)
        } catch (e: NoSuchAlgorithmException) {
            throw java.lang.RuntimeException(e)
        }
    }

    fun hash160(src: ByteArray): ByteArray {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance("RIPEMD160")
            return digest.digest(src)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }

    }

    fun md5Hex(src: ByteArray): String {
        return bytesToHex(md5(src)!!)
    }

    fun md5(src: ByteArray): ByteArray? {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance("MD5")
            return digest.digest(src)
        } catch (e: NoSuchAlgorithmException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        return null
    }

    fun bytesToHex(bytes: ByteArray): String {
        val result = StringBuilder()
        for (b in bytes) result.append(Integer.toString((b and 0xff.toByte()) + 0x100, 16).substring(1))
        return result.toString()
    }
}
