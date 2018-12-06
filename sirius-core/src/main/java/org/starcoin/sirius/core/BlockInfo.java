package org.starcoin.sirius.core;

import org.starcoin.core.BlockAddress;
import org.starcoin.core.Hash;
import org.starcoin.core.Hashable;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin.ProtoBlockInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BlockInfo implements ProtobufCodec<ProtoBlockInfo>, Hashable {

    private int height;
    private Hash hash;
    private List<ChainTransaction> transactions;

    public BlockInfo() {
    }

    public BlockInfo(int height) {
        this.height = height;
        // TODO calculate hash from data.
        this.hash = Hash.random();
        this.transactions = new ArrayList<>();
    }

    public BlockInfo(ProtoBlockInfo blockInfo) {
        this.unmarshalProto(blockInfo);
    }

    public int getHeight() {
        return height;
    }

    public Hash getHash() {
        return hash;
    }

    @Override
    public Hash hash() {
        return this.hash;
    }

    public void addTransaction(ChainTransaction tx) {
        this.transactions.add(tx);
    }

    public List<ChainTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    @Override
    public ProtoBlockInfo marshalProto() {
        return ProtoBlockInfo.newBuilder()
                .setHeight(this.height)
                .addAllTransactions(
                        this.transactions
                                .stream()
                                .map(ChainTransaction::marshalProto)
                                .collect(Collectors.toList()))
                .build();
    }

    @Override
    public void unmarshalProto(ProtoBlockInfo proto) {
        this.height = proto.getHeight();
        this.transactions =
                proto
                        .getTransactionsList()
                        .stream()
                        .map(ChainTransaction::new)
                        .collect(Collectors.toList());
    }

    public List<ChainTransaction> filterTxByTo(BlockAddress to) {
        return this.transactions
                .stream()
                .filter(tx -> tx.isSuccess() && tx.getTo().equals(to))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlockInfo)) {
            return false;
        }
        BlockInfo blockInfo = (BlockInfo) o;
        return height == blockInfo.height && Objects.equals(transactions, blockInfo.transactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, transactions);
    }

    public String toString() {
        return this.toJson();
    }
}
