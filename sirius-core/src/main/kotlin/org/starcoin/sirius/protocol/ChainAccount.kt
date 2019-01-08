package org.starcoin.sirius.protocol

import org.starcoin.sirius.crypto.CryptoKey

abstract class ChainAccount {

    abstract val key: CryptoKey
    val address by lazy { key.address }

}
