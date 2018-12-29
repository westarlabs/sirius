package org.starcoin.sirius.crypto

import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.fallback.FallbackCryptoKey
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*

interface CryptoService {

    fun getDummyCryptoKey(): CryptoKey

    fun generateCryptoKey(): CryptoKey

    fun loadCryptoKey(bytes: ByteArray): CryptoKey

    fun loadPublicKey(bytes: ByteArray): PublicKey

    fun loadPrivateKey(bytes: ByteArray): PrivateKey

    fun encodePublicKey(publicKey: PublicKey): ByteArray

    fun encodePrivateKey(privateKey: PrivateKey): ByteArray

    fun getAddress(publicKey: PublicKey): Address

    fun sign(data: ByteArray, privateKey: PrivateKey): Signature

    fun sign(data: Hash, privateKey: PrivateKey): Signature

    fun sign(data: SiriusObject, privateKey: PrivateKey): Signature

    fun verify(data: ByteArray, sign: Signature, publicKey: PublicKey): Boolean

    fun verify(data: Hash, sign: Signature, publicKey: PublicKey): Boolean

    fun verify(data: SiriusObject, sign: Signature, publicKey: PublicKey): Boolean

    fun hash(bytes: ByteArray): Hash

    fun <T : SiriusObject> hash(obj: T): Hash

    fun getEmptyDataHash(): Hash

    fun getEmptyListHash(): Hash

    companion object : CryptoService {
        val instance: CryptoService by lazy {
            val loaders = ServiceLoader
                .load(CryptoServiceProvider::class.java).iterator()
            if (loaders.hasNext()) {
                loaders.next().createService()
            } else {
                //if can not find, use fallback
                FallbackCryptoKey
            }
        }

        override fun getDummyCryptoKey() = instance.getDummyCryptoKey()

        override fun generateCryptoKey() = instance.generateCryptoKey()

        override fun loadCryptoKey(bytes: ByteArray) = instance.loadCryptoKey(bytes)

        override fun loadPublicKey(bytes: ByteArray) = instance.loadPublicKey(bytes)

        override fun loadPrivateKey(bytes: ByteArray): PrivateKey = instance.loadPrivateKey(bytes)

        override fun encodePublicKey(publicKey: PublicKey) = instance.encodePublicKey(publicKey)

        override fun encodePrivateKey(privateKey: PrivateKey) = instance.encodePrivateKey(privateKey)

        override fun getAddress(publicKey: PublicKey) = instance.getAddress(publicKey)

        override fun sign(data: ByteArray, privateKey: PrivateKey) = instance.sign(data, privateKey)

        override fun sign(data: Hash, privateKey: PrivateKey) = instance.sign(data, privateKey)

        override fun sign(data: SiriusObject, privateKey: PrivateKey) = instance.sign(data, privateKey)

        override fun verify(data: ByteArray, sign: Signature, publicKey: PublicKey) =
            instance.verify(data, sign, publicKey)

        override fun verify(data: Hash, sign: Signature, publicKey: PublicKey) = instance.verify(data, sign, publicKey)

        override fun verify(data: SiriusObject, sign: Signature, publicKey: PublicKey) =
            instance.verify(data, sign, publicKey)

        override fun hash(bytes: ByteArray) = instance.hash(bytes)

        override fun <T : SiriusObject> hash(obj: T) = instance.hash(obj)

        override fun getEmptyDataHash() = instance.getEmptyDataHash()

        override fun getEmptyListHash() = instance.getEmptyListHash()
    }
}
