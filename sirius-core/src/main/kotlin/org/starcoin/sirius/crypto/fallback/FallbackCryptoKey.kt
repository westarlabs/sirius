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
import org.starcoin.sirius.util.Utils
import java.math.BigInteger
import java.security.*

class FallbackCryptoKey(override val keyPair: KeyPair) : CryptoKey {

    constructor() : this(generateKeyPair())

    internal constructor(privateKey: PrivateKey) : this(
        KeyPair(
            generatePublicKeyFromPrivateKey(privateKey),
            privateKey
        )
    )

    internal constructor(bytes: ByteArray) : this(loadPrivateKey(bytes))

    override val address: Address by lazy { getAddress(this.keyPair.public) }

    override fun verify(data: ByteArray, sign: Signature): Boolean {
        return verifySig(data, keyPair.public, sign.toBytes())
    }

    override fun verify(data: Hash, sign: Signature): Boolean {
        return this.verify(data.bytes, sign)
    }

    override fun verify(data: SiriusObject, sign: Signature): Boolean {
        return this.verify(data.toProtobuf(), sign)
    }

    override fun sign(data: Hash): Signature = this.sign(data.toBytes())

    override fun sign(bytes: ByteArray): Signature {
        return Signature.wrap(signData(bytes, this.keyPair.private))
    }

    override fun sign(data: SiriusObject): Signature {
        return this.sign(data.hash())
    }


    override fun toBytes(): ByteArray {
        return encodePrivateKey(this.keyPair.private)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FallbackCryptoKey) return false

        if (!this.toBytes().contentEquals(other.toBytes())) return false

        return true
    }

    override fun hashCode(): Int {
        return this.toBytes().contentHashCode()
    }

    override fun toString(): String {
        return Utils.HEX.encode(this.toBytes())
    }

    companion object : CryptoService {
        val ALGORITHM = "EC"
        val CURVE: ECDomainParameters
        val CURVE_SPEC: ECParameterSpec
        val PROVIDER: BouncyCastleProvider

        val EMPTY_BYTE_ARRAY = ByteArray(0)
        val EMPTY_HASH: Hash
        val DUMMY_KEY: FallbackCryptoKey

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
            DUMMY_KEY = FallbackCryptoKey(FallbackCryptoKey.generatePrivateKeyFromBigInteger(BigInteger.ONE))
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

        override fun loadPublicKey(encoded: ByteArray): PublicKey {
            return generatePublicKeyFromPoint(CURVE.curve.decodePoint(encoded))
        }

        override fun encodePublicKey(publicKey: PublicKey): ByteArray {
            return (publicKey as BCECPublicKey).q.getEncoded(true)
        }

        override fun loadPrivateKey(encoded: ByteArray): PrivateKey {
            return generatePrivateKeyFromBigInteger(BigInteger(1, encoded))
        }

        override fun encodePrivateKey(privateKey: PrivateKey): ByteArray {
            //TODO check type
            return ByteUtil.bigIntegerToBytes((privateKey as BCECPrivateKey).d, 32)
        }

        override fun getDummyCryptoKey(): CryptoKey {
            return DUMMY_KEY
        }

        override fun generateCryptoKey(): CryptoKey {
            return FallbackCryptoKey()
        }

        override fun loadCryptoKey(bytes: ByteArray): CryptoKey {
            return FallbackCryptoKey(bytes)
        }

        override fun getAddress(publicKey: PublicKey): Address {
            return Address.wrap(HashUtil.hash160(HashUtil.sha256(encodePublicKey(publicKey))))
        }

        override fun sign(data: ByteArray, privateKey: PrivateKey): Signature {
            return Signature.wrap(signData(data, privateKey))
        }

        override fun sign(data: Hash, privateKey: PrivateKey): Signature {
            return this.sign(data.bytes, privateKey)
        }

        override fun sign(data: SiriusObject, privateKey: PrivateKey): Signature {
            return this.sign(data.toProtobuf(), privateKey)
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
            return this.verify(data.toProtobuf(), sign, publicKey)
        }

        override fun hash(bytes: ByteArray): Hash {
            return Hash.wrap(doHash(bytes))
        }

        override fun <T : SiriusObject> hash(obj: T): Hash {
            return hash(obj.toProtobuf())
        }

        override fun getEmptyDataHash(): Hash {
            return EMPTY_HASH
        }

        override fun getEmptyListHash(): Hash {
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
