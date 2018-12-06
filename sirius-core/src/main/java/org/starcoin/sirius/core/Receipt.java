package org.starcoin.sirius.core;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import org.starcoin.core.ChainDecoder;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.io.BlockInputStream;
import org.starcoin.proto.Starcoin.ProtoReceipt;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class Receipt implements ProtobufCodec<ProtoReceipt>, ChainDecoder {

    private boolean success;
    private String error;
    private byte[] data;

    public Receipt() {
        this.success = true;
    }

    public Receipt(boolean success) {
        this.success = success;
    }

    public Receipt(String error) {
        this.success = false;
        this.error = error;
    }

    public void readFrom(BlockInputStream in) throws IOException {
        this.success = in.readBoolean();
    }

    public Receipt(boolean success, byte[] data) {
        this.success = success;
        this.data = data;
    }

    public Receipt(boolean success, GeneratedMessageV3 data) {
        this(success, data.toByteArray());
    }

    public Receipt(ProtoReceipt proto) {
        this.unmarshalProto(proto);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    public byte[] getData() {
        return data;
    }

    public <T extends GeneratedMessageV3> T getData(Class<T> clazz) {
        if (this.data == null) {
            return null;
        }
        try {
            return (T) clazz.getMethod("parseFrom", byte[].class).invoke(null, this.data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProtoReceipt marshalProto() {
        ProtoReceipt.Builder builder = ProtoReceipt.newBuilder().setSuccess(success);
        if (this.error != null) {
            builder.setError(this.error);
        }
        if (this.data != null) {
            builder.setData(ByteString.copyFrom(data));
        }
        return builder.build();
    }

    @Override
    public void unmarshalProto(ProtoReceipt proto) {
        this.success = proto.getSuccess();
        // Protobuf's string default value is empty string.
        this.error = proto.getError().isEmpty() ? null : proto.getError();
        this.data = proto.getData().isEmpty() ? null : proto.getData().toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Receipt)) {
            return false;
        }
        Receipt receipt = (Receipt) o;
        return success == receipt.success
                && Objects.equals(error, receipt.error)
                && Arrays.equals(data, receipt.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(success, error);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
