package org.starcoin.sirius.core;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import org.starcoin.core.BlockAddress;
import org.starcoin.core.ProtobufCodec;
import org.starcoin.proto.Starcoin.ProtoHubAccount;
import org.starcoin.util.KeyPairUtil;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HubAccount implements ProtobufCodec<ProtoHubAccount> {

    private BlockAddress address;
    private long allotment;
    private Update update;
    private long deposit;
    private long withdraw;
    private List<OffchainTransaction> transactions;
    private PublicKey publicKey;

    public HubAccount() {
    }

    public HubAccount(ProtoHubAccount hubAccount) {
        this.unmarshalProto(hubAccount);
    }

    public HubAccount(BlockAddress address, long allotment, Update update, PublicKey publicKey) {
        this.address = address;
        this.allotment = allotment;
        this.update = update;
        this.transactions = new ArrayList<>();
        this.publicKey = publicKey;
    }

    public void appendTransaction(OffchainTransaction tx, Update update) {
        this.checkUpdate(tx, update);
        this.transactions.add(tx);
        this.update = update;
    }

    public BlockAddress getAddress() {
        return address;
    }

    public long getAllotment() {
        return allotment;
    }

    public Update getUpdate() {
        return update;
    }

    public List<OffchainTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void addDeposit(long amount) {
        this.deposit = this.deposit + amount;
    }

    public boolean addWithdraw(long amount) {
        if (this.getBalance() - amount >= 0) {
            this.withdraw = this.withdraw + amount;
            return true;
        }
        return false;
    }

    public long getBalance() {
        return this.allotment
                + deposit
                + update.getReceiveAmount()
                - this.withdraw
                - update.getSendAmount();
    }

    public long getDeposit() {
        return deposit;
    }

    public long getWithdraw() {
        return withdraw;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private void checkUpdate(OffchainTransaction newTx, Update newUpdate) {
        List<OffchainTransaction> sendTxs = new ArrayList<>(this.transactions);
        sendTxs.add(newTx);
        Update prepareUpdate =
                new Update(newUpdate.getEon(), newUpdate.getVersion(), this.address, sendTxs);

        Preconditions.checkArgument(
                newUpdate.getRoot().equals(prepareUpdate.getRoot()),
                "check " + this.getAddress() + " update root hash fail, expect:" + prepareUpdate.getRoot()
                        .toMD5Hex() + ", but get " + newUpdate.getRoot().toMD5Hex());

        Preconditions.checkArgument(
                newUpdate.getSendAmount() == prepareUpdate.getSendAmount(), "sendAmount");
        Preconditions.checkArgument(
                newUpdate.getReceiveAmount() == prepareUpdate.getReceiveAmount(),
                String.format(
                        "expect receiveAmount %s, but get %s",
                        prepareUpdate.getReceiveAmount(), newUpdate.getReceiveAmount()));

        Preconditions.checkState(this.update != null, "previousUpdate");
        Preconditions.checkArgument(newUpdate.getVersion() > update.getVersion());
        Preconditions.checkArgument(checkBalance(), "has not enough balance.");
    }

    private boolean checkBalance() {
        return this.getBalance() >= 0;
    }

    public AccountInformation toNewAccountInformation() {
        long allotment = this.calculateNewAllotment();
        AccountInformation accountInformation = new AccountInformation(this.address, allotment, update);
        return accountInformation;
    }

    private long calculateNewAllotment() {
        long allotment = this.getBalance();
        assert allotment >= 0;
        return allotment;
    }

    public HubAccount toNextEon(int eon) {
        long allotment = this.calculateNewAllotment();
        HubAccount account =
                new HubAccount(this.address, allotment, new Update(eon, 0, 0, 0, null), publicKey);
        return account;
    }

    @Override
    public ProtoHubAccount marshalProto() {
        return ProtoHubAccount.newBuilder()
                .setAddress(this.address.toProto())
                .setUpdate(this.update.toProto())
                .setAllotment(this.allotment)
                .setDeposit(this.deposit)
                .setWithdraw(this.withdraw)
                .setPublicKey(ByteString.copyFrom(KeyPairUtil.encodePublicKey(this.publicKey)))
                .build();
    }

    @Override
    public void unmarshalProto(ProtoHubAccount proto) {
        this.address = BlockAddress.valueOf(proto.getAddress());
        this.update = new Update(proto.getUpdate());
        this.allotment = proto.getAllotment();
        this.deposit = proto.getDeposit();
        this.withdraw = proto.getWithdraw();
        this.publicKey = KeyPairUtil.recoverPublicKey(proto.getPublicKey().toByteArray());
    }

    @Override
    public String toString() {
        return this.toJson();
    }
}
