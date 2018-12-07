package org.starcoin.sirius.util

import org.bouncycastle.jce.provider.BouncyCastleProvider

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import java.security.*

/**
 * Download Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files see
 * https://stackoverflow.com/questions/6481627/java-security-illegal-key-size-or-default-parameters
 */
object EncryptUtil {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun encrypt(publicKey: PublicKey, data: ByteArray): ByteArray {
        try {
            // 参阅:
            // 1. http://www.flexiprovider.de/examples/ExampleECIES.html
            // 2. https://crypto.stackexchange.com/questions/12823/ecdsa-vs-ecies-vs-ecdh
            // 3. https://crypto.stackexchange.com/questions/24516/ecdsa-for-encryption

            val cipher = Cipher.getInstance("ECIES", "BC")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            return cipher.doFinal(data)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException(e)
        } catch (e: BadPaddingException) {
            throw RuntimeException(e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException(e)
        } catch (e: IllegalBlockSizeException) {
            throw RuntimeException(e)
        }

    }

    fun decrypt(privateKey: PrivateKey, encrypted: ByteArray): ByteArray {
        try {
            val cipher = Cipher.getInstance("ECIES", "BC")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)

            return cipher.doFinal(encrypted)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException(e)
        } catch (e: BadPaddingException) {
            throw RuntimeException(e)
        } catch (e: IllegalBlockSizeException) {
            throw RuntimeException(e)
        }

    }
}
