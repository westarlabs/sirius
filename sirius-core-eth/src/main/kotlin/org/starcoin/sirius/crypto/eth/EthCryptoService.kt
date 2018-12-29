package org.starcoin.sirius.crypto.eth

import org.ethereum.crypto.ECKey
import org.ethereum.crypto.jce.ECKeyFactory
import org.ethereum.crypto.jce.SpongyCastleProvider
import org.ethereum.util.RLP
import org.spongycastle.jce.spec.ECPrivateKeySpec
import org.spongycastle.jce.spec.ECPublicKeySpec
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.rlp.*
import java.math.BigInteger
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

    override fun loadPublicKey(bytes: ByteArray): PublicKey {
        return ECKeyFactory.getInstance(SpongyCastleProvider.getInstance()).generatePublic(
            ECPublicKeySpec(
                ECKey.fromPublicOnly(bytes).pubKeyPoint,
                ECKey.CURVE_SPEC
            )
        )
    }

    override fun loadPrivateKey(bytes: ByteArray): PrivateKey {
        return ECKeyFactory.getInstance(SpongyCastleProvider.getInstance())
            .generatePrivate(ECPrivateKeySpec(BigInteger(1, bytes), ECKey.CURVE_SPEC))
    }

    override fun encodePublicKey(publicKey: PublicKey): ByteArray {
        val pk = publicKey as org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
        return pk.q.getEncoded(true)
    }

    override fun encodePrivateKey(privateKey: PrivateKey): ByteArray {
        val pk = privateKey as org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
        return pk.d.toByteArray()
    }

    override fun sign(data: ByteArray, privateKey: PrivateKey): Signature {
        return EthCryptoKey(privateKey).sign(data)
    }

    override fun sign(data: Hash, privateKey: PrivateKey): Signature {
        return this.sign(data.toBytes(), privateKey)
    }

    override fun sign(data: SiriusObject, privateKey: PrivateKey): Signature {
        return this.sign(data.hash(), privateKey)
    }

    override fun verify(
        data: ByteArray,
        sign: Signature,
        publicKey: PublicKey
    ): Boolean {
        return ECKey.verify(data, sign.toECDSASignature(), publicKey.encoded)
    }

    override fun verify(data: Hash, sign: Signature, publicKey: PublicKey): Boolean {
        return this.verify(data.toBytes(), sign, publicKey)
    }

    override fun verify(data: SiriusObject, sign: Signature, publicKey: PublicKey): Boolean {
        return this.verify(data.hash(), sign, publicKey)
    }

    override fun hash(bytes: ByteArray): Hash {
        return Hash.wrap(sha3(bytes))
    }

    override fun <T : SiriusObject> hash(obj: T): Hash {
        return hash(obj.toRLP())
    }

    override fun generateAddress(publicKey: PublicKey): Address {
        return Address.wrap(ECKey.computeAddress(publicKey.encoded))
    }

    override fun getEmptyDataHash(): Hash {
        return EMPTY_DATA_HASH
    }

    override fun getEmptyListHash(): Hash {
        return EMPTY_LIST_HASH
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
