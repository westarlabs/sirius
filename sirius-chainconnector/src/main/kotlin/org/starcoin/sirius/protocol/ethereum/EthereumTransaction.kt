package org.starcoin.sirius.protocol


import org.ethereum.core.Transaction
import org.ethereum.db.ByteArrayWrapper
import org.starcoin.sirius.core.*
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toULong
import org.starcoin.sirius.lang.toUnsignedBigInteger
import org.starcoin.sirius.protocol.ethereum.functionMap
import java.math.BigInteger

class EthereumTransaction(val tx: Transaction) : ChainTransaction() {
    override val from: Address?
        get() = tx.sender?.toAddress()

    val nonce: Long
        get() = tx.nonce.toULong()
    val gasPrice: BigInteger
        get() = tx.gasPrice.toUnsignedBigInteger()
    val gasLimit: BigInteger
        get() = tx.gasLimit.toUnsignedBigInteger()
    val data: ByteArray?
        get() = tx.data

    override val amount: BigInteger
        get() = tx.value.toUnsignedBigInteger()

    override val to: Address?
        get() = tx.receiveAddress?.toAddress()

    override val isContractCall: Boolean
        get() = this.data?.let { it.size > 4 && this.to != null } ?: false

    override val contractFunction: ContractFunction<out SiriusObject>?
        get() = if (isContractCall) ContractFunction.functionMap[ByteArrayWrapper(
            this.data!!.copyOfRange(
                0,
                4
            )
        )] else null


    constructor(web3Tx: org.web3j.protocol.core.methods.response.Transaction) : this(
        Transaction(
            web3Tx.nonce.toByteArray(),
            web3Tx.gasPrice.toByteArray(),
            web3Tx.gas.toByteArray(),
            web3Tx.to?.toAddress()?.toBytes(),
            web3Tx.value.toByteArray(),
            web3Tx.input.hexToByteArray()
        )
    )

    constructor(
        toAddress: Address,
        nonce: Long,
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        value: BigInteger

    ) : this(
        Transaction(
            nonce.toBigInteger().toByteArray(),
            gasPrice.toByteArray(),
            gasLimit.toByteArray(),
            toAddress.toBytes(),
            value.toByteArray(),
            null
        )
    )

    constructor(
        toAddress: Address,
        nonce: Long,
        gasPrice: Long,
        gasLimit: Long,
        value: Long

    ) : this(
        Transaction(
            nonce.toBigInteger().toByteArray(),
            gasPrice.toBigInteger().toByteArray(),
            gasLimit.toBigInteger().toByteArray(),
            toAddress.toBytes(),
            value.toBigInteger().toByteArray(),
            null
        )
    )

    constructor(
        contractAddress: Address,
        nonce: Long,
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        data: ByteArray
    ) : this(
        Transaction(
            nonce.toBigInteger().toByteArray(),
            gasPrice.toByteArray(),
            gasLimit.toByteArray(),
            contractAddress.toBytes(),
            BigInteger.ZERO.toByteArray(),
            data
        )
    )

    constructor(
        nonce: Long,
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        data: ByteArray
    ) : this(
        Transaction(
            nonce.toBigInteger().toByteArray(),
            gasPrice.toByteArray(),
            gasLimit.toByteArray(),
            null,
            BigInteger.ZERO.toByteArray(),
            data
        )
    )

    override fun txHash(): Hash {
        return Hash.wrap(this.tx.hash)
    }

}
