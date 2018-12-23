package org.starcoin.sirius.crypto

import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.fallback.FallbackCryptoService
import java.security.PublicKey
import java.util.*

interface CryptoService {

    fun getDummyCryptoKey(): CryptoKey

    fun generateCryptoKey(): CryptoKey

    fun loadCryptoKey(bytes: ByteArray): CryptoKey

    fun getAddress(publicKey: PublicKey): Address

    fun verify(data: ByteArray, sign: Signature, publicKey: PublicKey): Boolean

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
                FallbackCryptoService
            }
        }

        override fun getDummyCryptoKey() = instance.getDummyCryptoKey()

        override fun generateCryptoKey() = instance.generateCryptoKey()

        override fun loadCryptoKey(bytes: ByteArray) = instance.loadCryptoKey(bytes)

        override fun getAddress(publicKey: PublicKey) = instance.getAddress(publicKey)

        override fun verify(data: ByteArray, sign: Signature, publicKey: PublicKey) =
            instance.verify(data, sign, publicKey)

        override fun hash(bytes: ByteArray) = instance.hash(bytes)

        override fun <T : SiriusObject> hash(obj: T) = instance.hash(obj)
    }
}
