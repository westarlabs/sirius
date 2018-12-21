package org.starcoin.sirius.crypto.eth

import org.ethereum.crypto.ECKey
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.rlp.*
import java.security.PublicKey

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

    override fun verify(
        data: ByteArray,
        sign: Signature,
        publicKey: PublicKey
    ): Boolean {
        return ECKey.verify(data, sign.toECDSASignature(), publicKey.encoded)
    }

    override fun hash(bytes: ByteArray): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : SiriusObject> hash(obj: T): Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun Signature.toECDSASignature(): ECKey.ECDSASignature {
    val list = this.toBytes().decodeRLP() as RLPList
    val v = (list[0] as RLPElement).toByteFromRLP()
    val r = (list[1] as RLPElement).toBigIntegerFromRLP()
    val s = (list[2] as RLPElement).toBigIntegerFromRLP()
    return ECKey.ECDSASignature.fromComponents(r.toByteArray(), s.toByteArray(), v)
}

fun ECKey.ECDSASignature.toSignature(): Signature {
    val list = RLPList()
    list.add(this.v.toRLP())
    list.add(this.r.toRLP())
    list.add(this.s.toRLP())
    return Signature.wrap(list.encode())
}
