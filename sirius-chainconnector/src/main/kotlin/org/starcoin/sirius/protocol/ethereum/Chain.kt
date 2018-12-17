package org.starcoin.sirius.protocol.ethereum

import kotlinx.io.IOException
import org.starcoin.sirius.core.BalanceUpdateChallenge
import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.BlockInfo
import org.starcoin.sirius.core.ChainTransaction
import org.starcoin.sirius.protocol.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.EthTransaction
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.ipc.UnixIpcService
import java.math.BigInteger

const val defaultHttpUrl = "http://127.0.0.1:8545"

class EthereumChain constructor(httpUrl: String = defaultHttpUrl, socketPath: String?) : Chain {

    private val web3jSrv: Web3j? =
        Web3j.build(if (socketPath != null) UnixIpcService(socketPath) else HttpService(httpUrl))

    override fun findTransaction(hash: ByteArray): ChainTransaction? {
        val req = web3jSrv!!.ethGetTransactionByHash(hash.toString()).send()
        if (req.hasError()) throw IOException(req.error.message)
        val tx = req.transaction.orElse(null)
        return ChainTransaction(
            BlockAddress.valueOf(tx.from),
            BlockAddress.valueOf(tx.to),
            0,
            tx.value as Long,
            tx.input,
            ByteArray(0)
        )
    }

    override fun getBlock(height: BigInteger): BlockInfo? {
        val blockReq = web3jSrv!!.ethGetBlockByNumber(
            DefaultBlockParameter.valueOf(height),
            true
        ).send()
        if (blockReq.hasError()) throw IOException(blockReq.error.message)

        // FIXME: Use BigInteger in blockinfo
        val blockInfo = BlockInfo(blockReq.block.number as Int)

        blockReq.block.transactions.map { it ->
            val tx = it as Transaction
            blockInfo.addTransaction(
                ChainTransaction(
                    // Transaction from block address
                    BlockAddress.valueOf(tx.from),
                    // Transaction to block address
                    BlockAddress.valueOf(tx.to),
                    // Transaction timestamp
                    0,  //timestamp
                    // Transaction value
                    tx.value as Long, // value
                    // Transaction data
                    tx.input,
                    // FIXME: No argument in ethereum transaction
                    // Transaction argument
                    ByteArray(0)
                )
            )
        }
        return blockInfo
    }


    override fun watchBlock(onNext: ((b: BlockInfo) -> Unit)) {
        web3jSrv!!.blockFlowable(true).subscribe { block -> onNext(block.block.blockInfo()) }
    }

    override fun watchTransaction(onNext: ((t: ChainTransaction) -> Unit)) {
        web3jSrv!!.transactionFlowable().subscribe { tx -> onNext(tx.chainTransaction()) }
    }

    override fun getBalance(address: ByteArray): BigInteger {
        val req = web3jSrv!!.ethGetBalance(address.toString(), DefaultBlockParameter.valueOf("lastst")).send()
        if (req.hasError()) throw IOException(req.error.message)
        return req.balance
    }

    fun Transaction.chainTransaction(): ChainTransaction {
        return ChainTransaction(
            // Transaction from block address
            BlockAddress.valueOf(this.from),
            // Transaction to block address
            BlockAddress.valueOf(this.to),
            // Transaction timestamp
            0,  //timestamp
            // Transaction value
            this.value as Long, // value
            // Transaction data
            this.input,
            // FIXME: No argument in ethereum transaction
            // Transaction argument
            ByteArray(0)
        )
    }

    fun EthBlock.Block.blockInfo(): BlockInfo {
        val blockInfo = BlockInfo(this.number as Int)
        this.transactions.map { it ->
            val tx = it as Transaction
            blockInfo.addTransaction(
                ChainTransaction(
                    // Transaction from block address
                    BlockAddress.valueOf(tx.from),
                    // Transaction to block address
                    BlockAddress.valueOf(tx.to),
                    // Transaction timestamp
                    0,  //timestamp
                    // Transaction value
                    tx.value as Long, // value
                    // Transaction data
                    tx.input,
                    // FIXME: No argument in ethereum transaction
                    // Transaction argument
                    ByteArray(0)
                )
            )
        }
        return blockInfo
    }
}
