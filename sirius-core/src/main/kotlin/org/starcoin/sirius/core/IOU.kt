package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@Serializable
@ProtobufSchema(Starcoin.IOU::class)
data class IOU(@SerialId(1) val transaction: OffchainTransaction, @SerialId(2) val update: Update) :
    SiriusObject() {

    companion object : SiriusObjectCompanion<IOU, Starcoin.IOU>(IOU::class) {
        override fun mock(): IOU {
            return IOU(OffchainTransaction.mock(), Update.mock())
        }

        override fun parseFromProtoMessage(proto: Starcoin.IOU): IOU {
            return IOU(
                OffchainTransaction.parseFromProtoMessage(proto.transaction),
                Update.parseFromProtoMessage(proto.update)
            )
        }

        override fun toProtoMessage(obj: IOU): Starcoin.IOU {
            return Starcoin.IOU.newBuilder().setTransaction(OffchainTransaction.toProtoMessage(obj.transaction))
                .setUpdate(Update.toProtoMessage(obj.update)).build()
        }
    }
}
