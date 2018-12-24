package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin.ProtoIOU
import org.starcoin.sirius.serialization.ProtobufSchema

@Serializable
@ProtobufSchema(ProtoIOU::class)
data class IOU(@SerialId(1) val transaction: OffchainTransaction, @SerialId(2) val update: Update) :
    SiriusObject() {

    companion object : SiriusObjectCompanion<IOU, ProtoIOU>(IOU::class) {
        override fun mock(): IOU {
            return IOU(OffchainTransaction.mock(), Update.mock())
        }

        override fun parseFromProtoMessage(proto: ProtoIOU): IOU {
            return IOU(
                OffchainTransaction.parseFromProtoMessage(proto.transaction),
                Update.parseFromProtoMessage(proto.update)
            )
        }

        override fun toProtoMessage(obj: IOU): ProtoIOU {
            return ProtoIOU.newBuilder().setTransaction(OffchainTransaction.toProtoMessage(obj.transaction))
                .setUpdate(Update.toProtoMessage(obj.update)).build()
        }
    }
}
