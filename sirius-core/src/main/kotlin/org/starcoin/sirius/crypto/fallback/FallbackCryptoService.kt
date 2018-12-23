package org.starcoin.sirius.crypto.fallback

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.util.HashUtil
import org.starcoin.sirius.util.KeyPairUtil
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Security

/**
 * just for can not find CryptServiceProvider
 */
object FallbackCryptoService : CryptoService {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    override fun getDummyCryptoKey(): CryptoKey {
        return FallbackCryptoKey(KeyPairUtil.TEST_KEYPAIR)
    }

    override fun generateCryptoKey(): CryptoKey {
        return FallbackCryptoKey(KeyPairUtil.generateKeyPair())
    }

    override fun loadCryptoKey(bytes: ByteArray): CryptoKey {
        return FallbackCryptoKey(KeyPairUtil.decodeKeyPair(bytes))
    }

    override fun getAddress(publicKey: PublicKey): BlockAddress {
        return BlockAddress.wrap(HashUtil.hash160(HashUtil.sha256(KeyPairUtil.encodePublicKey(publicKey, true))))
    }

    override fun verify(
        data: ByteArray,
        sign: Signature,
        publicKey: PublicKey
    ): Boolean {
        return KeyPairUtil.verifySig(data, publicKey, sign.toBytes())
    }

    override fun hash(bytes: ByteArray): Hash {
        return Hash.wrap(doHash(bytes))
    }

    override fun <T : SiriusObject> hash(obj: T): Hash {
        //TODO use rlp binary?
        return hash(obj.toRLP())
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
    private fun newDigest(): MessageDigest {
        try {
            return MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e) // Can't happen.
        }

    }

    private fun doHash(input: ByteArray): ByteArray {
        val digest = newDigest()
        digest.update(input)
        return digest.digest()
    }

}
