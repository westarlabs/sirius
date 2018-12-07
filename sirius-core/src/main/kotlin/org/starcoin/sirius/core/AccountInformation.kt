package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin
import java.util.*

class AccountInformation : ProtobufCodec<Starcoin.ProtoAccountInfo> {

    // just keep hash of address
    var address: Hash? = null
        private set
    var allotment: Long = 0
        private set
    var update: Update? = null

    constructor() {}

    constructor(address: BlockAddress, allotment: Long, update: Update?) {
        this.address = Hash.of(address.toBytes())
        this.allotment = allotment
        this.update = update
    }

    constructor(address: Hash, allotment: Long, update: Update?) {
        this.address = address
        this.allotment = allotment
        this.update = update
    }

    override fun marshalProto(): Starcoin.ProtoAccountInfo {
        val builder = Starcoin.ProtoAccountInfo.newBuilder()
            .setAddress(this.address!!.toProto())
            .setAllotment(this.allotment)
        if (this.update != null) {
            builder.update = this.update!!.toProto()
        }
        return builder.build()
    }

    override fun unmarshalProto(proto: Starcoin.ProtoAccountInfo) {
        this.address = Hash.wrap(proto.address)
        this.allotment = proto.allotment
        this.update = if (proto.hasUpdate()) Update(proto.update) else null
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is AccountInformation) {
            return false
        }
        val that = o as AccountInformation?
        return allotment == that!!.allotment &&
                address == that.address &&
                update == that.update
    }

    override fun hashCode(): Int {
        return Objects.hash(address, allotment, update)
    }

    companion object {

        val EMPTY_ACCOUNT = AccountInformation(BlockAddress.DEFAULT_ADDRESS, 0, null)

        fun generateAccountInformation(proto: Starcoin.ProtoAccountInfo?): AccountInformation? {
            if (proto == null) {
                return null
            }
            val accountInformation = AccountInformation()
            accountInformation.unmarshalProto(proto)
            return accountInformation
        }
    }
}