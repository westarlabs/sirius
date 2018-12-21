package org.starcoin.sirius.crypto.fallback

import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.util.HashUtil
import org.starcoin.sirius.util.KeyPairUtil
import java.security.KeyPair

internal class FallbackCryptoKey(private val keyPairArg: KeyPair) : CryptoKey {

    internal constructor(bytes: ByteArray) : this(KeyPairUtil.decodeKeyPair(bytes))

    override fun getKeyPair(): KeyPair {
        return this.keyPairArg
    }

    override fun sign(bytes: ByteArray): Signature {
        try {
            val signer = java.security.Signature.getInstance("SHA256withECDSA")
            signer.initSign(this.keyPairArg.private)
            signer.update(bytes)
            return Signature.wrap(signer.sign())
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    override fun getAddress(): BlockAddress {
        return BlockAddress.valueOf(
            HashUtil.hash160(
                HashUtil.sha256(
                    KeyPairUtil.encodePublicKey(
                        this.keyPairArg.public,
                        true
                    )
                )
            )
        )
    }

    override fun toBytes(): ByteArray {
        return KeyPairUtil.encodeKeyPair(this.keyPairArg)
    }
}
