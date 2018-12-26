package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin.ProtoBlockInfo
import org.starcoin.sirius.util.MockUtils
import java.util.*
import java.util.stream.Collectors

class BlockInfo : ProtobufCodec<ProtoBlockInfo>, CachedHashable {

    var height: Int = 0
        private set
    private var transactions: MutableList<ChainTransaction>? = null

    constructor() {}

    constructor(height: Int) {
        this.height = height
        this.transactions = ArrayList()
    }

    constructor(blockInfo: ProtoBlockInfo) {
        this.unmarshalProto(blockInfo)
    }

    override fun hashData(): ByteArray {
        //TODO
        return MockUtils.nextBytes(100)
    }

    fun addTransaction(tx: ChainTransaction) {
        this.transactions!!.add(tx)
    }

    fun getTransactions(): List<ChainTransaction> {
        return Collections.unmodifiableList(transactions!!)
    }

    override fun marshalProto(): ProtoBlockInfo {
        return ProtoBlockInfo.newBuilder()
            .setHeight(this.height)
            .addAllTransactions(
                this.transactions!!
                    .stream()
                    .map { it.marshalProto() }
                    .collect(Collectors.toList())
            )
            .build()
    }

    override fun unmarshalProto(proto: ProtoBlockInfo) {
        this.height = proto.height
        this.transactions = proto
            .transactionsList
            .stream()
            .map { ChainTransaction(it) }
            .collect(Collectors.toList())
    }

    fun filterTxByTo(to: Address): List<ChainTransaction> {
        return this.transactions!!
            .stream()
            .filter { tx -> tx.isSuccess && tx.to == to }
            .collect(Collectors.toList())
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is BlockInfo) {
            return false
        }
        val blockInfo = o as BlockInfo?
        return height == blockInfo!!.height && transactions == blockInfo.transactions
    }

    override fun hashCode(): Int {
        return Objects.hash(height, transactions)
    }

    override fun toString(): String {
        return this.toJson()
    }
}
