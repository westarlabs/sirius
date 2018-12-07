package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin.DepositRequest

import java.util.Objects

class Deposit : ProtobufCodec<DepositRequest> {

    var address: BlockAddress? = null
        private set
    var amount: Long = 0
        private set

    constructor() {}

    constructor(proto: DepositRequest) {
        this.unmarshalProto(proto)
    }

    constructor(address: BlockAddress, amount: Long) {
        this.address = address
        this.amount = amount
    }

    override fun marshalProto(): DepositRequest {
        val builder = DepositRequest.newBuilder()
        if (this.address != null) builder.address = this.address!!.toProto()
        builder.amount = this.amount
        return builder.build()
    }

    override fun unmarshalProto(proto: DepositRequest) {
        this.address = if (proto.hasAddress()) BlockAddress.valueOf(proto.address) else null
        this.amount = proto.amount
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is Deposit) {
            return false
        }
        val deposit = o as Deposit?
        return this.amount == deposit!!.amount && this.address == deposit.address
    }

    override fun hashCode(): Int {
        return Objects.hash(this.address, this.amount)
    }
}
