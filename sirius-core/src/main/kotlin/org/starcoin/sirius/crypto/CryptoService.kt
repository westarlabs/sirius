package org.starcoin.sirius.crypto

import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.fallback.FallbackCryptoService
import java.util.*

interface CryptoService {

    fun getDummyCryptoKey(): CryptoKey

    fun generateCryptoKey(): CryptoKey

    fun loadCryptoKey(bytes: ByteArray): CryptoKey

    fun hash(bytes: ByteArray): Hash

    fun <T : SiriusObject> hash(obj: T): Hash

    companion object {
        val INSTANCE:CryptoService by lazy {
            val loaders = ServiceLoader
                .load(CryptoServiceProvider::class.java).iterator()
            if(loaders.hasNext()){
                loaders.next().createService()
            }else{
                //if can not find
                FallbackCryptoService
            }

        }
    }
}
