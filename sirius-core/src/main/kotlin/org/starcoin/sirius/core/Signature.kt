package org.starcoin.sirius.core

import com.google.protobuf.ByteString
import org.starcoin.proto.Starcoin.ProtoSignature
import org.starcoin.sirius.util.KeyPairUtil
import org.starcoin.sirius.util.Utils

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Arrays

class Signature private constructor(private val sign: ByteArray) {


    fun verify(publicKey: PublicKey, data: ByteArray): Boolean {
        return KeyPairUtil.verifySig(data, publicKey, this.sign)
    }

    fun marshalSize(): Int {
        return 1 + sign.size
    }

    fun toProto(): ProtoSignature {
        return ProtoSignature.newBuilder().setSign(ByteString.copyFrom(sign)).build()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is Signature) {
            return false
        }
        val signature = o as Signature?
        return Arrays.equals(sign, signature!!.sign)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(sign)
    }

    override fun toString(): String {
        return Utils.HEX.encode(this.sign)
    }

    /**
     * Write sign bytes to out
     */
    @Throws(IOException::class)
    fun writeTo(out: OutputStream) {
        out.write(this.sign.size)
        out.write(this.sign)
    }

    companion object {

        val COINBASE_SIGNATURE = Signature.wrap(byteArrayOf(0))

        fun wrap(sign: ByteArray): Signature {
            return Signature(sign)
        }

        fun wrap(protoSignature: ProtoSignature): Signature {
            return Signature(protoSignature.sign.toByteArray())
        }

        fun of(privateKey: PrivateKey, data: ByteArray): Signature {
            return Signature(KeyPairUtil.signData(data, privateKey))
        }

        /**
         * Read bytes and create Signature object.
         */
        @Throws(IOException::class)
        fun readFrom(`in`: InputStream): Signature {
            val len = `in`.read()
            val bytes = ByteArray(len)
            val rLen = `in`.read(bytes)
            if (len != rLen) {
                throw EOFException("unexpected enf of stream to parse Signature")
            }
            return Signature.wrap(bytes)
        }
    }


}
