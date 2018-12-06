package org.starcoin.sirius.core;

import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin.ProtoIOU;

public class IOU implements ProtobufCodec<ProtoIOU> {

    private OffchainTransaction transaction;

    private Update update;

    public IOU() {
    }

    public IOU(OffchainTransaction transaction, Update update) {
        this.transaction = transaction;
        this.update = update;
    }

    public IOU(ProtoIOU protoIOU) {
        this.unmarshalProto(protoIOU);
    }

    public OffchainTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(OffchainTransaction transaction) {
        this.transaction = transaction;
    }

    public Update getUpdate() {
        return update;
    }

    public void setUpdate(Update update) {
        this.update = update;
    }

    @Override
    public ProtoIOU marshalProto() {
        return ProtoIOU.newBuilder().setTransaction(this.transaction.marshalProto())
                .setUpdate(this.update.marshalProto()).build();
    }

    @Override
    public void unmarshalProto(ProtoIOU proto) {
        this.transaction = new OffchainTransaction(proto.getTransaction());
        this.update = new Update(proto.getUpdate());
    }
}
