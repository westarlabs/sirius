package org.starcoin.sirius.crypto

import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.Signature
import java.security.KeyPair

interface CryptoKey {
    fun getKeyPair(): KeyPair

    fun sign(bytes: ByteArray): Signature

    fun getAddress(): BlockAddress

    fun toBytes(): ByteArray
}
