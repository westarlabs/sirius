package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin.DepositRequest
import java.util.*

class Deposit(address: BlockAddress, amount: Long) : ProtobufCodec<DepositRequest> {

    var address: BlockAddress = address
        private set
    var amount: Long = amount
        private set

    override fun marshalProto(): DepositRequest {
        val builder = DepositRequest.newBuilder()
        builder.address = this.address.toByteString()
        builder.amount = this.amount
        return builder.build()
    }

    override fun unmarshalProto(proto: DepositRequest) {
        this.address = BlockAddress.wrap(proto.address)
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
