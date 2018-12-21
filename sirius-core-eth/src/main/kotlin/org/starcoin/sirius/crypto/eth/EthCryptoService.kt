package org.starcoin.sirius.crypto.eth

import org.ethereum.crypto.ECKey
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService

object EthCryptoService : CryptoService {

    override fun getDummyCryptoKey(): CryptoKey {
        return EthCryptoKey(ECKey.DUMMY)
    }

    override fun loadCryptoKey(bytes: ByteArray): CryptoKey {
        return EthCryptoKey(bytes)
    }

    override fun generateCryptoKey(): CryptoKey {
        return EthCryptoKey()
    }

    override fun hash(bytes: ByteArray): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : SiriusObject> hash(obj: T): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
