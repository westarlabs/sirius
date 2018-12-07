package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin

import java.util.Objects

class BalanceUpdateProof : ProtobufCodec<Starcoin.ProtoBalanceUpdateProof> {

    var update: Update? = null

    var provePath: AugmentedMerklePath? = null

    constructor() {}

    constructor(proto: Starcoin.ProtoBalanceUpdateProof) {
        this.unmarshalProto(proto)
    }

    constructor(update: Update, provePath: AugmentedMerklePath) {
        this.update = update
        this.provePath = provePath
    }

    override fun marshalProto(): Starcoin.ProtoBalanceUpdateProof {
        val builder = Starcoin.ProtoBalanceUpdateProof.newBuilder()
        if (this.update != null) builder.update = this.update!!.marshalProto()
        if (this.provePath != null) builder.path = this.provePath!!.marshalProto()

        return builder.build()
    }

    override fun unmarshalProto(proto: Starcoin.ProtoBalanceUpdateProof) {
        if (proto.hasUpdate()) {
            val update = Update()
            update.unmarshalProto(proto.update)
            this.update = update
        }

        if (proto.hasPath()) {
            val merklePath = AugmentedMerklePath(proto.path)

            this.provePath = merklePath
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is BalanceUpdateProof) {
            return false
        }
        val proof = o as BalanceUpdateProof?
        return this.update == proof!!.update && this.provePath == proof.provePath
    }

    override fun hashCode(): Int {
        return Objects.hash(this.update, this.provePath)
    }

    override fun toString(): String {
        return this.toJson()
    }

    companion object {

        fun generateBalanceUpdateProof(
            proto: Starcoin.ProtoBalanceUpdateProof
        ): BalanceUpdateProof {
            val proof = BalanceUpdateProof()
            proof.unmarshalProto(proto)
            return proof
        }
    }
}
