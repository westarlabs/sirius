package org.starcoin.sirius.crypto.eth

import org.ethereum.crypto.ECKey
import org.ethereum.crypto.jce.ECKeyFactory
import org.ethereum.crypto.jce.SpongyCastleProvider
import org.spongycastle.jce.spec.ECPrivateKeySpec
import org.spongycastle.jce.spec.ECPublicKeySpec
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.CryptoKey
import java.security.KeyPair
import java.security.PrivateKey

class EthCryptoKey (val ecKey: ECKey) : CryptoKey() {

    internal constructor() : this(ECKey())

    internal constructor(privateKey: PrivateKey) : this(EthCryptoService.encodePrivateKey(privateKey))

    internal constructor(bytes: ByteArray) : this(ECKey.fromPrivate(bytes))

    override val keyPair: KeyPair by lazy {
        var privateKey = ECKeyFactory.getInstance(SpongyCastleProvider.getInstance())
            .generatePrivate(ECPrivateKeySpec(ecKey.privKey!!, ECKey.CURVE_SPEC))
        var publicKey = ECKeyFactory.getInstance(SpongyCastleProvider.getInstance()).generatePublic(
            ECPublicKeySpec(
                ecKey.pubKeyPoint,
                ECKey.CURVE_SPEC
            )
        )
        KeyPair(publicKey, privateKey)
    }

    override fun sign(data: ByteArray) = ecKey.sign(
        when {
            data.size > Hash.LENGTH -> EthCryptoService.sha3(data)
            else -> data
        }
    ).toSignature()

    override fun sign(data: Hash) = sign(data.toBytes())

    override fun sign(data: SiriusObject): Signature {
        return sign(data.hash())
    }

    override fun verify(data: ByteArray, sign: Signature): Boolean {
        return this.ecKey.verify(data, sign.toECDSASignature())
    }

    override fun verify(data: Hash, sign: Signature): Boolean {
        return this.verify(data.toBytes(), sign)
    }

    override fun verify(data: SiriusObject, sign: Signature): Boolean {
        return this.verify(data.hash(), sign)
    }

    override val address get() = Address.wrap(ecKey.address)

    override fun toBytes(): ByteArray {
        return ecKey.privKeyBytes!!
    }
}
