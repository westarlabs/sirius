package org.starcoin.sirius.core

import com.google.protobuf.ByteString
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.util.KeyPairUtil

import java.security.PublicKey
import java.util.Objects
import java.util.logging.Logger

class BalanceUpdateChallenge : ProtobufCodec<Starcoin.ProtoBalanceUpdateChallenge> {

    var proof: BalanceUpdateProof? = null

    var publicKey: PublicKey? = null

    private var status: ChallengeStatus? = null

    private val logger = Logger.getLogger(BalanceUpdateChallenge::class.java.name)

    val isClosed: Boolean
        get() = synchronized(this) {
            return this.status != null && this.status == ChallengeStatus.CLOSE
        }

    constructor() {}

    constructor(proto: Starcoin.ProtoBalanceUpdateChallenge) {
        this.unmarshalProto(proto)
    }

    constructor(update: Update, provePath: AugmentedMerklePath, publicKey: PublicKey) {
        val proof = BalanceUpdateProof(update, provePath)
        this.proof = proof
        this.publicKey = publicKey
    }

    constructor(proof: BalanceUpdateProof, publicKey: PublicKey) {
        this.proof = proof
        this.publicKey = publicKey
    }

    fun closeChallenge(): Boolean {
        synchronized(this) {
            if (this.status != null && this.status == ChallengeStatus.OPEN) {
                this.status = ChallengeStatus.CLOSE
                logger.info("closeChallenge succ")
                return true
            }

            logger.warning(
                "closeChallenge err status : " + if (this.status == null) "null" else this.status
            )
            return false
        }
    }

    fun openChallenge(): Boolean {
        synchronized(this) {
            if (this.status == null) {
                this.status = ChallengeStatus.OPEN
                logger.info("openChallenge succ")
                return true
            }

            logger.warning(
                "openChallenge err status : " + if (this.status == null) "null" else this.status
            )
            return false
        }
    }

    override fun marshalProto(): Starcoin.ProtoBalanceUpdateChallenge {
        val builder = Starcoin.ProtoBalanceUpdateChallenge.newBuilder()
        if (this.proof != null) builder.proof = this.proof!!.marshalProto()
        if (this.publicKey != null) {
            builder.publicKey = ByteString.copyFrom(KeyPairUtil.encodePublicKey(this.publicKey!!))
        }
        return builder.build()
    }

    override fun unmarshalProto(proto: Starcoin.ProtoBalanceUpdateChallenge) {
        if (proto.hasProof()) {
            val proof = BalanceUpdateProof()
            proof.unmarshalProto(proto.proof)
            this.proof = proof
        }

        if (!proto.publicKey.isEmpty) {
            this.publicKey = KeyPairUtil.recoverPublicKey(proto.publicKey.toByteArray())
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is BalanceUpdateChallenge) {
            return false
        }
        val challenge = o as BalanceUpdateChallenge?
        return this.proof == challenge!!.proof && this.publicKey == challenge.publicKey
    }

    override fun hashCode(): Int {
        return Objects.hash(this.proof, this.publicKey)
    }

    override fun toString(): String {
        return this.toJson()
    }

    companion object {

        fun generateBalanceUpdateChallenge(
            proto: Starcoin.ProtoBalanceUpdateChallenge
        ): BalanceUpdateChallenge {
            val challenge = BalanceUpdateChallenge()
            challenge.unmarshalProto(proto)
            return challenge
        }
    }
}
