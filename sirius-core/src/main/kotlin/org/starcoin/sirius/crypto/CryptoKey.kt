package org.starcoin.sirius.crypto

import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import java.security.KeyPair

interface CryptoKey {

    val keyPair: KeyPair

    val address: Address

    fun sign(data: ByteArray): Signature

    fun sign(data: Hash): Signature

    fun sign(data: SiriusObject): Signature

    fun verify(data: ByteArray, sign: Signature): Boolean

    fun verify(data: Hash, sign: Signature): Boolean

    fun verify(data: SiriusObject, sign: Signature): Boolean

    fun toBytes(): ByteArray

}
