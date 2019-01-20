package org.starcoin.sirius.crypto

import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.lang.toHEXString
import java.security.KeyPair

abstract class CryptoKey {

    abstract val keyPair: KeyPair

    abstract val address: Address

    abstract fun sign(data: ByteArray): Signature

    abstract fun sign(data: Hash): Signature

    abstract fun sign(data: SiriusObject): Signature

    abstract fun verify(data: ByteArray, sign: Signature): Boolean

    abstract fun verify(data: Hash, sign: Signature): Boolean

    abstract fun verify(data: SiriusObject, sign: Signature): Boolean

    abstract fun toBytes(): ByteArray

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CryptoKey) return false

        if (!this.toBytes().contentEquals(other.toBytes())) return false

        return true
    }

    override fun hashCode(): Int {
        return this.toBytes().contentHashCode()
    }

    override fun toString(): String {
        return this.toBytes().toHEXString()
    }
}
