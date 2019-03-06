package org.starcoin.sirius.util

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import javax.crypto.Cipher

/**
 * Download Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files see
 * https://stackoverflow.com/questions/6481627/java-security-illegal-key-size-or-default-parameters
 */
object EncryptUtil {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun encrypt(publicKey: PublicKey, data: ByteArray): ByteArray {
        // 参阅:
        // 1. http://www.flexiprovider.de/examples/ExampleECIES.html
        // 2. https://crypto.stackexchange.com/questions/12823/ecdsa-vs-ecies-vs-ecdh
        // 3. https://crypto.stackexchange.com/questions/24516/ecdsa-for-encryption

        val cipher = Cipher.getInstance("ECIES", "BC")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    fun decrypt(privateKey: PrivateKey, encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("ECIES", "BC")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        return cipher.doFinal(encrypted)

    }
}
