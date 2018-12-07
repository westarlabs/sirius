package org.starcoin.sirius.core

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import org.starcoin.proto.Starcoin.ProtoReceipt

import java.io.IOException
import java.util.Arrays
import java.util.Objects

class Receipt : ProtobufCodec<ProtoReceipt> {

    var isSuccess: Boolean = false
        private set
    var error: String? = null
        private set
    var data: ByteArray? = null
        private set

    constructor() {
        this.isSuccess = true
    }

    constructor(success: Boolean) {
        this.isSuccess = success
    }

    constructor(error: String) {
        this.isSuccess = false
        this.error = error
    }

    constructor(success: Boolean, data: ByteArray) {
        this.isSuccess = success
        this.data = data
    }

    constructor(success: Boolean, data: GeneratedMessageV3) : this(success, data.toByteArray()) {}

    constructor(proto: ProtoReceipt) {
        this.unmarshalProto(proto)
    }

    fun <T : GeneratedMessageV3> getData(clazz: Class<T>): T? {
        if (this.data == null) {
            return null
        }
        try {
            return clazz.getMethod("parseFrom", ByteArray::class.java).invoke(null, this.data) as T
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    override fun marshalProto(): ProtoReceipt {
        val builder = ProtoReceipt.newBuilder().setSuccess(isSuccess)
        if (this.error != null) {
            builder.error = this.error
        }
        if (this.data != null) {
            builder.data = ByteString.copyFrom(data!!)
        }
        return builder.build()
    }

    override fun unmarshalProto(proto: ProtoReceipt) {
        this.isSuccess = proto.success
        // Protobuf's string default value is empty string.
        this.error = if (proto.error.isEmpty()) null else proto.error
        this.data = if (proto.data.isEmpty) null else proto.data.toByteArray()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is Receipt) {
            return false
        }
        val receipt = o as Receipt?
        return (isSuccess == receipt!!.isSuccess
                && error == receipt.error
                && Arrays.equals(data, receipt.data))
    }

    override fun hashCode(): Int {
        var result = Objects.hash(isSuccess, error)
        result = 31 * result + Arrays.hashCode(data)
        return result
    }
}
