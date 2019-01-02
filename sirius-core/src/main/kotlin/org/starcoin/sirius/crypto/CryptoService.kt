package org.starcoin.sirius.crypto

import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.fallback.DefaultCryptoKey
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*

interface CryptoService {

    val dummyCryptoKey: CryptoKey

    val emptyDataHash: Hash

    val emptyListHash: Hash

    fun generateCryptoKey(): CryptoKey

    fun loadCryptoKey(bytes: ByteArray): CryptoKey

    fun loadCryptoKey(privateKey: PrivateKey): CryptoKey

    fun loadPublicKey(bytes: ByteArray): PublicKey

    fun loadPrivateKey(bytes: ByteArray): PrivateKey

    fun encodePublicKey(publicKey: PublicKey): ByteArray

    fun encodePrivateKey(privateKey: PrivateKey): ByteArray

    fun generateAddress(publicKey: PublicKey): Address

    fun sign(data: ByteArray, key: CryptoKey) = key.sign(data)

    fun sign(data: Hash, key: CryptoKey) = key.sign(data)

    fun sign(data: SiriusObject, key: CryptoKey) = key.sign(data)

    fun verify(data: ByteArray, sign: Signature, key: CryptoKey) = key.verify(data, sign)

    fun verify(data: Hash, sign: Signature, key: CryptoKey) = key.verify(data, sign)

    fun verify(data: SiriusObject, sign: Signature, key: CryptoKey) = key.verify(data, sign)

    fun verify(data: ByteArray, sign: Signature, publicKey: PublicKey): Boolean

    fun verify(data: Hash, sign: Signature, publicKey: PublicKey): Boolean

    fun verify(data: SiriusObject, sign: Signature, publicKey: PublicKey): Boolean

    fun hash(bytes: ByteArray): Hash

    fun <T : SiriusObject> hash(obj: T): Hash

    companion object : CryptoService {
        val instance: CryptoService by lazy {
            val loaders = ServiceLoader
                .load(CryptoServiceProvider::class.java).iterator()
            if (loaders.hasNext()) {
                loaders.next().createService()
            } else {
                //if can not find, use fallback
                DefaultCryptoKey
            }
        }

        override val dummyCryptoKey get() = instance.dummyCryptoKey

        override fun generateCryptoKey() = instance.generateCryptoKey()

        override fun loadCryptoKey(bytes: ByteArray) = instance.loadCryptoKey(bytes)

        override fun loadCryptoKey(privateKey: PrivateKey) = instance.loadCryptoKey(privateKey)

        override fun loadPublicKey(bytes: ByteArray) = instance.loadPublicKey(bytes)

        override fun loadPrivateKey(bytes: ByteArray): PrivateKey = instance.loadPrivateKey(bytes)

        override fun encodePublicKey(publicKey: PublicKey) = instance.encodePublicKey(publicKey)

        override fun encodePrivateKey(privateKey: PrivateKey) = instance.encodePrivateKey(privateKey)

        override fun generateAddress(publicKey: PublicKey) = instance.generateAddress(publicKey)

        override fun verify(data: ByteArray, sign: Signature, publicKey: PublicKey) =
            instance.verify(data, sign, publicKey)

        override fun verify(data: Hash, sign: Signature, publicKey: PublicKey) = instance.verify(data, sign, publicKey)

        override fun verify(data: SiriusObject, sign: Signature, publicKey: PublicKey) =
            instance.verify(data, sign, publicKey)

        override fun hash(bytes: ByteArray) = instance.hash(bytes)

        override fun <T : SiriusObject> hash(obj: T) = instance.hash(obj)

        override val emptyDataHash get() = instance.emptyDataHash

        override val emptyListHash get() = instance.emptyListHash
    }
}
