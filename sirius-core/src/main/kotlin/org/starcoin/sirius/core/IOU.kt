package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin.ProtoIOU

class IOU : ProtobufCodec<ProtoIOU> {

    var transaction: OffchainTransaction? = null

    var update: Update? = null

    constructor() {}

    constructor(transaction: OffchainTransaction, update: Update) {
        this.transaction = transaction
        this.update = update
    }

    constructor(protoIOU: ProtoIOU) {
        this.unmarshalProto(protoIOU)
    }

    override fun marshalProto(): ProtoIOU {
        return ProtoIOU.newBuilder().setTransaction(this.transaction!!.marshalProto())
            .setUpdate(this.update!!.marshalProto()).build()
    }

    override fun unmarshalProto(proto: ProtoIOU) {
        this.transaction = OffchainTransaction(proto.transaction)
        this.update = Update(proto.update)
    }
}
