package org.starcoin.sirius.crypto.eth

import org.ethereum.crypto.ECKey
import org.ethereum.crypto.jce.ECKeyFactory
import org.ethereum.crypto.jce.SpongyCastleProvider
import org.spongycastle.jce.spec.ECPrivateKeySpec
import org.spongycastle.jce.spec.ECPublicKeySpec
import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.crypto.CryptoKey
import java.security.KeyPair

class EthCryptoKey internal constructor(private val ecKey: ECKey) : CryptoKey {

    internal constructor() : this(ECKey())

    internal constructor(bytes: ByteArray) : this(ECKey.fromPrivate(bytes))

    override fun getKeyPair(): KeyPair {
        var privateKey = ECKeyFactory.getInstance(SpongyCastleProvider.getInstance())
            .generatePrivate(ECPrivateKeySpec(ecKey.privKey!!, ECKey.CURVE_SPEC));
        var publicKey = ECKeyFactory.getInstance(SpongyCastleProvider.getInstance()).generatePublic(
            ECPublicKeySpec(
                ecKey.pubKeyPoint,
                ECKey.CURVE_SPEC
            )
        )
        return KeyPair(publicKey, privateKey)
    }

    override fun sign(bytes: ByteArray) = ecKey.sign(bytes).toSignature()

    override fun sign(data: Hash) = sign(data.toBytes())

    override fun verify(data: ByteArray, sign: Signature): Boolean {
        return this.ecKey.verify(data, sign.toECDSASignature())
    }

    override fun getAddress() = BlockAddress.valueOf(ecKey.address)

    override fun toBytes(): ByteArray {
        return ecKey.privKeyBytes!!
    }
}
