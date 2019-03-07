package org.starcoin.sirius.util

import com.google.common.hash.Hashing
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.starcoin.sirius.lang.toHEXString
import java.security.MessageDigest
import java.security.Security

object HashUtil {
    @Suppress("DEPRECATION")
    private val md5Function = Hashing.md5()!!
    private val sha256Function = Hashing.sha256()!!

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun sha256(src: ByteArray): ByteArray {
        return sha256Function.hashBytes(src).asBytes()
    }

    fun hash160(src: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("RIPEMD160")
        return digest.digest(src)
    }

    fun md5Hex(src: ByteArray): String {
        return md5(src).toHEXString()
    }

    fun md5(src: ByteArray): ByteArray {
        return md5Function.hashBytes(src).asBytes()
    }
}
