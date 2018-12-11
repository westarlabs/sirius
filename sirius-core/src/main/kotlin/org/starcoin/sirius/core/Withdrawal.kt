package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin.InitiateWithdrawalRequest

import java.util.Objects

class Withdrawal : ProtobufCodec<InitiateWithdrawalRequest> {

    var address: BlockAddress? = null
        private set
    var path: AugmentedMerklePath? = null
        private set
    var amount: Long = 0
        private set

    constructor(proto: InitiateWithdrawalRequest) {
        this.unmarshalProto(proto)
    }

    constructor(address: BlockAddress, path: AugmentedMerklePath, amount: Long) {
        this.address = address
        this.path = path
        this.amount = amount
    }

    override fun marshalProto(): InitiateWithdrawalRequest {
        return InitiateWithdrawalRequest.newBuilder()
            .setPath(path!!.toProto())
            .setAddress(this.address!!.toByteString())
            .setAmount(amount)
            .build()
    }

    override fun unmarshalProto(proto: InitiateWithdrawalRequest) {
        this.address = BlockAddress.valueOf(proto.address)
        this.path = AugmentedMerklePath(proto.path)
        this.amount = proto.amount
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is Withdrawal) {
            return false
        }
        val that = o as Withdrawal?
        return (amount == that!!.amount
                && address == that.address
                && path == that.path)
    }

    override fun hashCode(): Int {
        return Objects.hash(address, path, amount)
    }

    override fun toString(): String {
        return this.toJson()
    }
}
