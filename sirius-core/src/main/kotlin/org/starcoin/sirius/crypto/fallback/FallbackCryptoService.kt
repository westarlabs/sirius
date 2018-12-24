package org.starcoin.sirius.crypto.fallback

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.util.HashUtil
import org.starcoin.sirius.util.KeyPairUtil
import java.security.*

/**
 * just for can not find CryptServiceProvider
 */
object FallbackCryptoService : CryptoService {

    val EMPTY_BYTE_ARRAY = ByteArray(0)
    val EMPTY_HASH = hash(EMPTY_BYTE_ARRAY)

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

    override fun getAddress(publicKey: PublicKey): Address {
        return Address.wrap(HashUtil.hash160(HashUtil.sha256(KeyPairUtil.encodePublicKey(publicKey, true))))
    }

    override fun sign(data: ByteArray, privateKey: PrivateKey): Signature {
        return Signature.wrap(KeyPairUtil.signData(data, privateKey))
    }

    override fun sign(data: Hash, privateKey: PrivateKey): Signature {
        return this.sign(data.bytes, privateKey)
    }

    override fun sign(data: SiriusObject, privateKey: PrivateKey): Signature {
        return this.sign(data.toProtobuf(), privateKey)
    }

    override fun verify(
        data: ByteArray,
        sign: Signature,
        publicKey: PublicKey
    ): Boolean {
        return KeyPairUtil.verifySig(data, publicKey, sign.toBytes())
    }

    override fun verify(data: Hash, sign: Signature, publicKey: PublicKey): Boolean {
        return this.verify(data.bytes, sign, publicKey)
    }

    override fun verify(data: SiriusObject, sign: Signature, publicKey: PublicKey): Boolean {
        return this.verify(data.toProtobuf(), sign, publicKey)
    }

    override fun hash(bytes: ByteArray): Hash {
        return Hash.wrap(doHash(bytes))
    }

    override fun <T : SiriusObject> hash(obj: T): Hash {
        return hash(obj.toProtobuf())
    }

    override fun getEmptyDataHash(): Hash {
        return EMPTY_HASH
    }

    override fun getEmptyListHash(): Hash {
        return EMPTY_HASH
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
