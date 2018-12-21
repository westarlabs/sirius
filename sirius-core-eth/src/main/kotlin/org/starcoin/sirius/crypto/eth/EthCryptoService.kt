package org.starcoin.sirius.crypto.eth

import org.ethereum.crypto.ECKey
import org.ethereum.crypto.jce.SpongyCastleProvider
import org.ethereum.util.RLP
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.rlp.*
import java.security.*

object EthCryptoService : CryptoService {

    val EMPTY_BYTE_ARRAY = ByteArray(0)
    var EMPTY_DATA_HASH: Hash
    var EMPTY_LIST_HASH: Hash

    private var CRYPTO_PROVIDER: Provider

    private var HASH_256_ALGORITHM_NAME: String
    private var HASH_512_ALGORITHM_NAME: String

    init {
        Security.addProvider(SpongyCastleProvider.getInstance())
        CRYPTO_PROVIDER = Security.getProvider("SC")
        HASH_256_ALGORITHM_NAME = "ETH-KECCAK-256"
        HASH_512_ALGORITHM_NAME = "ETH-KECCAK-512"
        EMPTY_DATA_HASH = hash(EMPTY_BYTE_ARRAY)
        EMPTY_LIST_HASH = hash(RLP.encodeList())
    }


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
        return Hash.wrap(sha3(bytes))
    }

    override fun <T : SiriusObject> hash(obj: T): Hash {
        return hash(obj.toRLP())
    }

    fun sha3(input: ByteArray): ByteArray {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER)
            digest.update(input)
            return digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }

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
