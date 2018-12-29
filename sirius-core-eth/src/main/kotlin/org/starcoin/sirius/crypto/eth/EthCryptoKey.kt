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
import org.starcoin.sirius.util.Utils
import java.security.KeyPair
import java.security.PrivateKey

class EthCryptoKey internal constructor(val ecKey: ECKey) : CryptoKey {

    internal constructor() : this(ECKey())

    internal constructor(privateKey: PrivateKey) : this(privateKey.encoded)

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EthCryptoKey) return false

        if (!this.toBytes().contentEquals(other.toBytes())) return false
        return true
    }

    override fun hashCode(): Int {
        return this.toBytes().contentHashCode()
    }

    override fun toString(): String {
        return Utils.HEX.encode(this.toBytes())
    }
}
