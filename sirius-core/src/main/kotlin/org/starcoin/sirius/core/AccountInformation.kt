package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

//TODO rename to AccountInfo
@Serializable
@ProtobufSchema(Starcoin.ProtoAccountInfo::class)
data class AccountInformation(@SerialId(1) val addressHash: Hash, @SerialId(2) var update: Update, @SerialId(3) var allotment: Long = 0) :
    SiriusObject() {

    constructor(
        address: Address,
        update: Update,
        allotment: Long
    ) : this(address.hash(), update, allotment)

    companion object : SiriusObjectCompanion<AccountInformation, Starcoin.ProtoAccountInfo>(AccountInformation::class) {
        //TODO ensure empty account
        val EMPTY_ACCOUNT = AccountInformation(Address.DUMMY_ADDRESS, Update(UpdateData()), 0)

        @Deprecated("Please use parseFromProtoMessage")
        fun generateAccountInformation(proto: Starcoin.ProtoAccountInfo): AccountInformation? {
            return parseFromProtoMessage(proto)
        }

        override fun mock(): AccountInformation {
            return AccountInformation(Hash.random(), Update.mock(), RandomUtils.nextLong())
        }

        override fun parseFromProtoMessage(protoMessage: Starcoin.ProtoAccountInfo): AccountInformation {
            return AccountInformation(
                Hash.wrap(protoMessage.addressHash),
                Update.parseFromProtoMessage(protoMessage.update),
                protoMessage.allotment
            )
        }

        override fun toProtoMessage(obj: AccountInformation): Starcoin.ProtoAccountInfo {
            return Starcoin.ProtoAccountInfo.newBuilder().setAddressHash(obj.addressHash.toByteString())
                .setUpdate(Update.toProtoMessage(obj.update)).setAllotment(obj.allotment).build()
        }
    }
}
