package org.starcoin.sirius.crypto

import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import java.security.KeyPair

interface CryptoKey {
    fun getKeyPair(): KeyPair

    fun sign(data: ByteArray): Signature

    fun sign(data: Hash): Signature

    fun verify(data: ByteArray, sign: Signature): Boolean

    fun getAddress(): BlockAddress

    fun toBytes(): ByteArray
}
