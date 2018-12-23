package org.starcoin.sirius.core

import com.google.protobuf.Any
import com.google.protobuf.GeneratedMessageV3
import org.starcoin.proto.Starcoin
import java.util.*

class HubEvent<D : ProtobufCodec<*>> : ProtobufCodec<Starcoin.ProtoHubEvent> {

    var type: HubEventType? = null
        private set
    var address: BlockAddress? = null
        private set
    var payload: D? = null
        private set

    val isPublicEvent: Boolean
        get() = this.address == null

    constructor() {}

    constructor(proto: Starcoin.ProtoHubEvent) {
        this.unmarshalProto(proto)
    }

    constructor(type: HubEventType, address: BlockAddress, payload: D) {
        this.type = type
        this.address = address
        this.payload = payload
    }

    constructor(type: HubEventType, payload: D) {
        this.type = type
        this.payload = payload
    }

    override fun marshalProto(): Starcoin.ProtoHubEvent {
        val builder = Starcoin.ProtoHubEvent.newBuilder().setType(this.type!!.toProto())
        if (this.address != null) {
            builder.address = address!!.toByteString()
        }
        if (this.payload != null) {
            builder.payload = Any.pack<GeneratedMessageV3>(payload!!.toProto())
        }
        return builder.build()
    }

    override fun unmarshalProto(proto: Starcoin.ProtoHubEvent) {
        this.type = HubEventType.valueOf(proto.type.number)
        this.address = if (proto.address.isEmpty) null else BlockAddress.valueOf(proto.address)
        this.payload = if (proto.hasPayload()) this.type!!.parsePayload(proto.payload) else null
    }

    override fun equals(o: kotlin.Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is HubEvent<*>) {
            return false
        }
        val hubEvent = o as HubEvent<*>?
        return (type == hubEvent!!.type
                && address == hubEvent.address
                && payload == hubEvent.payload)
    }

    override fun hashCode(): Int {
        return Objects.hash(type, address, payload)
    }

    override fun toString(): String {
        // TODO use toJson.
        return (this.type!!.name
                + " "
                + (if (this.address == null) "" else this.address!!.toString())
                + " "
                + this.payload!!.toJson())
    }
}
