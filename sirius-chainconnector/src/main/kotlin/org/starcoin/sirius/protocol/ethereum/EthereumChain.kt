package org.starcoin.sirius.protocol.ethereum

import kotlinx.io.IOException
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.Hash
import org.starcoin.sirius.core.Receipt
import org.starcoin.sirius.crypto.CryptoKey
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.protocol.*
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.ipc.UnixIpcService
import org.web3j.utils.Numeric
import java.math.BigInteger

const val defaultHttpUrl = "http://127.0.0.1:8545"

class EthereumChain constructor(httpUrl: String = defaultHttpUrl, socketPath: String? = null) :
    Chain<EthereumTransaction, EthereumBlock, HubContract> {
    override fun getTransactionReceipts(txHashs: List<Hash>): List<Receipt> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun newTransaction(key: CryptoKey, transaction: EthereumTransaction) {
        val rawtx = RawTransaction.createTransaction(
            BigInteger.valueOf(transaction.nonce),
            BigInteger.valueOf(transaction.gasPrice),
            BigInteger.valueOf(transaction.gasLimit),
            transaction.to.toString(),
            BigInteger.valueOf(transaction.amount),
            transaction.data.toString()
        )
        val credentials = Credentials.create((key as EthCryptoKey).ecKey.privKey.toString())
        val hexTx = Numeric.toHexString(TransactionEncoder.signMessage(rawtx, credentials))
        web3jSrv!!.ethSendRawTransaction(hexTx).sendAsync().get()
    }

    private val web3jSrv: Web3j? =
        Web3j.build(if (socketPath != null) UnixIpcService(socketPath) else HttpService(httpUrl))


    override fun findTransaction(hash: Hash): EthereumTransaction? {
        val req = web3jSrv!!.ethGetTransactionByHash(hash.toString()).send()
        if (req.hasError()) throw IOException(req.error.message)
        val tx = req.transaction.orElse(null)
        return tx.chainTransaction()
    }

    override fun getBlock(height: BigInteger): EthereumBlock? {
        val blockReq = web3jSrv!!.ethGetBlockByNumber(
            if (height == BigInteger.valueOf(-1)) DefaultBlockParameterName.LATEST else
                DefaultBlockParameter.valueOf(height),
            true
        ).send()
        if (blockReq.hasError()) throw IOException(blockReq.error.message)

        // FIXME: Use BigInteger in blockinfo
        val blockInfo = EthereumBlock(blockReq.block)
        blockReq.block.transactions.map { it ->
            val tx = it as Transaction
            blockInfo.addTransaction(tx.chainTransaction())
        }
        return blockInfo
    }


    override fun watchBlock(filter: (FilterArguments) -> Boolean, onNext: (block: EthereumBlock) -> Unit) {
        web3jSrv!!.blockFlowable(true).subscribe { block -> onNext(block.block.blockInfo()) }
    }

    override fun watchTransactions(
        filter: (FilterArguments) -> Boolean,
        onNext: (txResult: TransactionResult<EthereumTransaction>) -> Unit
    ) {
        TODO()
    }

    override fun getBalance(address: Address): BigInteger {
        val req = web3jSrv!!.ethGetBalance(address.toString(), DefaultBlockParameterName.LATEST).send()
        if (req.hasError()) throw IOException(req.error.message)
        return req.balance
    }

    fun Transaction.chainTransaction(): EthereumTransaction {
        return EthereumTransaction(
            Address.wrap(this.to),
            System.currentTimeMillis(),  //timestamp
            this.gasPrice.longValueExact(),
            0,
            this.value as Long,
            this.input.toByteArray()
        )
    }

    fun EthBlock.Block.blockInfo(): EthereumBlock {
        return EthereumBlock(this)
    }


    override fun getContract(parameter: QueryContractParameter): HubContract {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
