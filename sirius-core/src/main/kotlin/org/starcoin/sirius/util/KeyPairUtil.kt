package org.starcoin.sirius.util

import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter

import java.io.*
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec

object KeyPairUtil {

    //just for junit test
    val TEST_KEYPAIR: KeyPair
        get() = generateKeyPair()

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun generateKeyPair(): KeyPair {
        try {
            val keyGen = KeyPairGenerator.getInstance("ECDSA", "BC")
            val ecSpec = ECGenParameterSpec("secp256k1")
            keyGen.initialize(ecSpec, SecureRandom())
            return keyGen.generateKeyPair()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw RuntimeException(e)
        }

    }

    fun signData(data: ByteArray, key: PrivateKey): ByteArray {
        try {
            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initSign(key)
            signer.update(data)
            return signer.sign()
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }

    }

    fun verifySig(data: ByteArray, key: PublicKey, sig: ByteArray): Boolean {
        try {
            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initVerify(key)
            signer.update(data)
            return signer.verify(sig)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return false
    }

    fun recoverPublicKey(encoded: ByteArray): PublicKey {
        try {
            val params = ECNamedCurveTable.getParameterSpec("secp256k1")
            val fact = KeyFactory.getInstance("ECDSA", "BC")
            val curve = params.curve
            val ellipticCurve = EC5Util.convertCurve(curve, params.seed)
            val point = ECPointUtil.decodePoint(ellipticCurve, encoded)
            val params2 = EC5Util.convertSpec(ellipticCurve, params)
            val keySpec = java.security.spec.ECPublicKeySpec(point, params2)
            return fact.generatePublic(keySpec) as ECPublicKey
        } catch (ex: Exception) {
            throw java.lang.RuntimeException(ex)
        }
    }

    fun recoverPrivateKey(bytes: ByteArray): PrivateKey {
        val keyGen: KeyFactory
        try {
            keyGen = KeyFactory.getInstance("ECDSA")
            return keyGen.generatePrivate(PKCS8EncodedKeySpec(bytes))
        } catch (e: NoSuchAlgorithmException) {
            throw java.lang.RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            throw java.lang.RuntimeException(e)
        }
    }

    fun encodePrivateKey(privateKey: PrivateKey): ByteArray {
        return privateKey.encoded
    }

    fun encodePublicKey(pubkey: PublicKey, compress: Boolean = true): ByteArray {
        val pk = pubkey as org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
        return pk.q.getEncoded(compress)
    }

    // Ref 1: https://stackoverflow.com/questions/11410770/load-rsa-public-key-from-file
    // Ref 2: https://www.txedo.com/blog/java-generate-rsa-keys-write-pem-file/
    fun savePemPrivateKey(privateKey: PrivateKey, filename: String) {
        savePemPrivateKey(privateKey, File(filename))
    }

    fun savePemPrivateKey(privateKey: PrivateKey, file: File) {
        val stringWriter = StringWriter()
        val pemWriter = PemWriter(stringWriter)

        try {
            // PemObjectGenerator x = null;
            val x = PemObject("EC PRIVATE KEY", privateKey.encoded)
            pemWriter.writeObject(x)
            pemWriter.flush()
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        } finally {
            try {
                pemWriter.close()
            } catch (ex2: Exception) {
            }

        }

        FileUtil.writeFile(file, stringWriter.toString().toByteArray())
    }

    fun savePemPublicKey(publicKey: PublicKey, filename: String) {
        savePemPublicKey(publicKey, File(filename))
    }

    fun savePemPublicKey(publicKey: PublicKey, file: File) {
        val stringWriter = StringWriter()
        val pemWriter = PemWriter(stringWriter)
        val data1 = encodePublicKey(publicKey, false)

        try {
            // PemObjectGenerator x = null;
            val x = PemObject("EC PUBLIC KEY", data1)
            pemWriter.writeObject(x)
            pemWriter.flush()
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        } finally {
            try {
                pemWriter.close()
            } catch (ex2: Exception) {
            }

        }

        FileUtil.writeFile(file, stringWriter.toString().toByteArray())
    }

    /**
     * Reads a Private Key from a pem base64 encoded file.
     *
     * @param file name of the file to read.
     * @return returns the privatekey which is read from the file;
     * @throws Exception
     */
    fun loadPemPrivateKey(file: File): PrivateKey {
        try {
            val keyBytes = FileUtil.readFile(file)
            val rdr = StringReader(String(keyBytes))
            val pr = PemReader(rdr)
            val po = pr.readPemObject()
            val privateKey = KeyPairUtil.recoverPrivateKey(po.content)
            pr.close()
            return privateKey
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    fun loadPemPublicKey(file: File): PublicKey {
        try {
            val keyBytes = FileUtil.readFile(file)
            val rdr = StringReader(String(keyBytes))
            val pr = PemReader(rdr)
            val po = pr.readPemObject()
            val publicKey = recoverPublicKey(po.content)
            pr.close()
            return publicKey
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

    }
}
