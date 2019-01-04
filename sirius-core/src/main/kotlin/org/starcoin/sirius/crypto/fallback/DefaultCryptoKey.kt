package org.starcoin.sirius.crypto.fallback

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Signature
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.util.ByteUtil
import org.starcoin.sirius.util.HashUtil
import java.math.BigInteger
import java.security.*

class DefaultCryptoKey(override val keyPair: KeyPair) : CryptoKey() {

    constructor() : this(generateKeyPair())

    internal constructor(privateKey: PrivateKey) : this(
        KeyPair(
            generatePublicKeyFromPrivateKey(privateKey),
            privateKey
        )
    )

    internal constructor(bytes: ByteArray) : this(loadPrivateKey(bytes))

    override val address: Address by lazy { generateAddress(this.keyPair.public) }

    override fun verify(data: ByteArray, sign: Signature): Boolean {
        return verifySig(data, keyPair.public, sign.toBytes())
    }

    override fun verify(data: Hash, sign: Signature): Boolean {
        return this.verify(data.bytes, sign)
    }

    override fun verify(data: SiriusObject, sign: Signature): Boolean {
        return this.verify(data.hash(), sign)
    }

    override fun sign(data: Hash): Signature = this.sign(data.toBytes())

    override fun sign(data: ByteArray): Signature {
        return Signature.wrap(signData(data, this.keyPair.private))
    }

    override fun sign(data: SiriusObject): Signature {
        return this.sign(data.hash())
    }


    override fun toBytes(): ByteArray {
        return encodePrivateKey(this.keyPair.private)
    }

    companion object : CryptoService {
        private const val ALGORITHM = "EC"
        private val CURVE: ECDomainParameters
        private val CURVE_SPEC: ECParameterSpec
        private val PROVIDER: BouncyCastleProvider

        private val EMPTY_BYTE_ARRAY = ByteArray(0)
        private val EMPTY_HASH: Hash
        private val DUMMY_KEY: DefaultCryptoKey

        private val keyFactory: KeyFactory

        init {
            val params = SECNamedCurves.getByName("secp256k1")
            CURVE = ECDomainParameters(params.curve, params.g, params.n, params.h)
            CURVE_SPEC = ECParameterSpec(
                params.curve,
                params.g,
                params.n,
                params.h
            )
            PROVIDER = BouncyCastleProvider()
            keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER)
            DUMMY_KEY = DefaultCryptoKey(DefaultCryptoKey.generatePrivateKeyFromBigInteger(BigInteger.ONE))
            EMPTY_HASH = hash(EMPTY_BYTE_ARRAY)
        }

        fun generateKeyPair(): KeyPair {
            return generateKeyPair(SecureRandom())
        }

        fun generateKeyPair(secureRandom: SecureRandom): KeyPair {
            val keyGen = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER)
            keyGen.initialize(CURVE_SPEC, secureRandom)
            return keyGen.generateKeyPair()
        }

        fun generatePrivateKeyFromBigInteger(privateKey: BigInteger): PrivateKey {
            return keyFactory.generatePrivate(
                ECPrivateKeySpec(
                    privateKey,
                    CURVE_SPEC
                )
            )
        }

//        fun generatePublicKeyFromPoint(point:ECPoint):PublicKey{
//            return Companion.generatePublicKeyFromPoint(org.bouncycastle.math.ec.ECPoint(CURVE, ECFieldElement()))
//        }

        private fun generatePublicKeyFromPoint(point: org.bouncycastle.math.ec.ECPoint): PublicKey {
            return keyFactory.generatePublic(
                ECPublicKeySpec(
                    point,
                    CURVE_SPEC
                )
            )
        }

        fun generatePublicKeyFromPrivateKey(privateKey: PrivateKey): PublicKey {
            val point = CURVE.g.multiply(BigInteger(1, encodePrivateKey(privateKey)))
            return generatePublicKeyFromPoint(point)
        }

        override fun loadPublicKey(bytes: ByteArray): PublicKey {
            return generatePublicKeyFromPoint(CURVE.curve.decodePoint(bytes))
        }

        override fun encodePublicKey(publicKey: PublicKey): ByteArray {
            return (publicKey as BCECPublicKey).q.getEncoded(true)
        }

        override fun loadPrivateKey(bytes: ByteArray): PrivateKey {
            return generatePrivateKeyFromBigInteger(BigInteger(1, bytes))
        }

        override fun encodePrivateKey(privateKey: PrivateKey): ByteArray {
            //TODO check type
            return ByteUtil.bigIntegerToBytes((privateKey as BCECPrivateKey).d, 32)
        }

        override val dummyCryptoKey: CryptoKey
            get() {
                return DUMMY_KEY
            }

        override fun generateCryptoKey(): CryptoKey {
            return DefaultCryptoKey()
        }

        override fun loadCryptoKey(bytes: ByteArray): CryptoKey {
            return DefaultCryptoKey(bytes)
        }

        override fun loadCryptoKey(privateKey: PrivateKey): CryptoKey {
            return DefaultCryptoKey(privateKey)
        }

        override fun generateAddress(publicKey: PublicKey): Address {
            return Address.wrap(HashUtil.hash160(HashUtil.sha256(encodePublicKey(publicKey))))
        }

        override fun verify(
            data: ByteArray,
            sign: Signature,
            publicKey: PublicKey
        ): Boolean {
            return verifySig(data, publicKey, sign.toBytes())
        }

        override fun verify(data: Hash, sign: Signature, publicKey: PublicKey): Boolean {
            return this.verify(data.bytes, sign, publicKey)
        }

        override fun verify(data: SiriusObject, sign: Signature, publicKey: PublicKey): Boolean {
            return this.verify(data.hash(), sign, publicKey)
        }

        override fun hash(bytes: ByteArray): Hash {
            return Hash.wrap(doHash(bytes))
        }

        override fun <T : SiriusObject> hash(obj: T): Hash {
            return hash(obj.toProtobuf())
        }

        override val emptyDataHash: Hash
            get() {
                return EMPTY_HASH
            }

        override val emptyListHash: Hash
            get() {
                return EMPTY_HASH
            }

        /**
         * Returns a new SHA-256 MessageDigest instance.
         *
         *
         * This is a convenience method which wraps the checked exception that can never occur with a
         * RuntimeException.
         *
         * @return a new SHA-256 MessageDigest instance
         */
        private fun newDigest(): MessageDigest {
            try {
                return MessageDigest.getInstance("SHA-256")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e) // Can't happen.
            }

        }

        private fun doHash(input: ByteArray): ByteArray {
            val digest = newDigest()
            digest.update(input)
            return digest.digest()
        }

        fun verifySig(data: ByteArray, key: PublicKey, sig: ByteArray): Boolean {
            val signer = java.security.Signature.getInstance("SHA256withECDSA")
            signer.initVerify(key)
            signer.update(data)
            return signer.verify(sig)
        }

        fun signData(data: ByteArray, key: PrivateKey): ByteArray {
            val signer = java.security.Signature.getInstance("SHA256withECDSA")
            signer.initSign(key)
            signer.update(data)
            return signer.sign()
        }
    }
}
