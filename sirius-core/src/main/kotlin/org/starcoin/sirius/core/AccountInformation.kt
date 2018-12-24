package org.starcoin.sirius.core

import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

//TODO rename to AccountInfo
@Serializable
@ProtobufSchema(Starcoin.ProtoAccountInfo::class)
data class AccountInformation(val addressHash: Hash, var update: Update, var allotment: Long = 0) :
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
            return super.mock()
        }

        override fun parseFromProtoMessage(protoMessage: Starcoin.ProtoAccountInfo): AccountInformation {
            return AccountInformation(
                Hash.wrap(protoMessage.addressHash),
                Update.parseFromProtoMessage(protoMessage.update),
                protoMessage.allotment
            )
        }

        override fun toProtoMessage(obj: AccountInformation): Starcoin.ProtoAccountInfo {
            return super.toProtoMessage(obj)
        }
    }
}
