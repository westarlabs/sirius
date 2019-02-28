package org.starcoin.sirius.protocol.ethereum

import org.ethereum.core.CallTransaction
import org.ethereum.core.TransactionReceipt
import org.ethereum.solidity.SolidityType
import org.ethereum.vm.DataWord
import org.ethereum.vm.LogInfo
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.core.toHash
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toBigInteger
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger

data class EthereumReceipt(
    override val transactionHash: Hash, val txStatus: Boolean,
    val gasUsed: BigInteger, val cumulativeGasUsed: BigInteger,
    val logBloom: String, override val logs: List<LogInfo>?
) : Receipt() {

    override val status: Boolean
        //TODO parse log return Event
        get() = txStatus && (returnEvent?.let { it } ?: true)

    override val recoveryMode: Boolean
        get() = logs?.any { recoveryModeEventSignature.contentEquals(it.topics[0].data) } ?: false

    val returnEvent: Boolean?
        get() = logs?.find { returnEventSignature.contentEquals(it.topics[0].data) }?.let { boolType.decode(it.data) as Boolean }

    constructor(receipt: TransactionReceipt) : this(
        receipt.transaction.hash.toHash(),
        receipt.isTxStatusOK && receipt.isSuccessful,
        if (receipt.gasUsed.isEmpty()) BigInteger.ZERO else receipt.gasUsed.toBigInteger(),
        if (receipt.gasUsed.isEmpty()) BigInteger.ZERO else receipt.cumulativeGas.toBigInteger(),
        receipt.bloomFilter.data.toHEXString(), receipt.logInfoList
    )

    constructor(receipt: org.web3j.protocol.core.methods.response.TransactionReceipt) : this(
        receipt.transactionHash.toHash(),
        receipt.isStatusOK,
        receipt.gasUsed,
        receipt.cumulativeGasUsed,
        receipt.logsBloom,
        receipt.logs.map {
            LogInfo(
                it.address.hexToByteArray(),
                it.topics.map { topic -> DataWord.of(topic.hexToByteArray()) },
                it.data.hexToByteArray()
            )
        }
    )

    companion object : WithLogging() {
        //TODO handle event
        private val returnEventFunction = CallTransaction.Function.fromSignature("ReturnEvent", "bool")
        private val returnEventSignature = returnEventFunction.encodeSignatureLong()
        private val boolType = SolidityType.BoolType()

        private val recoveryModeEventFunction = CallTransaction.Function.fromSignature("RecoveryModeEvent")
        private val recoveryModeEventSignature = recoveryModeEventFunction.encodeSignatureLong()
    }

}