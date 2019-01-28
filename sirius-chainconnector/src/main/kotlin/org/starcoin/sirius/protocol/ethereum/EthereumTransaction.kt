package org.starcoin.sirius.protocol


import io.netty.buffer.ByteBuf
import org.ethereum.core.Transaction
import org.starcoin.sirius.core.*
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.lang.toULong
import org.starcoin.sirius.lang.toUnsignedBigInteger
import org.starcoin.sirius.serialization.rlp.toByteArray
import org.starcoin.sirius.serialization.toByteArrayRemoveLeadZero
import java.math.BigInteger

class EthereumTransaction(private val tx: Transaction) : ChainTransaction() {
    override val from: Address?
        get() = tx.sender?.toAddress()

    val nonce: Long
        get() = tx.nonce.toULong()
    val gasPrice: BigInteger
        get() = tx.gasPrice.toUnsignedBigInteger()
    val gasLimit: BigInteger
        get() = tx.gasLimit.toUnsignedBigInteger()
    override val data: ByteArray?
        get() = tx.data

    override val amount: BigInteger
        get() = tx.value.toUnsignedBigInteger()

    override val to: Address?
        get() = if (tx.receiveAddress != null && tx.receiveAddress.isNotEmpty()) tx.receiveAddress.toAddress() else null

    val contractAddress: Address?
        get() = tx.contractAddress?.toAddress()

    override val isContractCall: Boolean
        get() = this.data?.let { it.size > 4 && this.to != null } ?: false

    override val contractFunction: ContractFunction<out SiriusObject>?
        get() = if (isContractCall) ContractFunction.functions[FunctionSignature(
            this.data!!.copyOfRange(
                0,
                4
            )
        )] else null

    init {
        this.verify()
    }

    fun verify() {
        this.tx.verify()
    }

    constructor(web3Tx: org.web3j.protocol.core.methods.response.Transaction) : this(
        Transaction(
            web3Tx.nonce.toByteArray(),
            web3Tx.gasPrice.toByteArray(),
            web3Tx.gas.toByteArray(),
            web3Tx.to?.toAddress()?.toBytes(),
            web3Tx.value.toByteArray(),
            web3Tx.input.hexToByteArray(),
            web3Tx.r.hexToByteArray(),
            web3Tx.s.hexToByteArray(),
            web3Tx.v.toByte()
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
            gasLimit.toByteArrayRemoveLeadZero(),
            toAddress.toBytes(),
            value.toByteArrayRemoveLeadZero(),
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
            gasLimit.toBigInteger().toByteArrayRemoveLeadZero(),
            toAddress.toBytes(),
            value.toBigInteger().toByteArrayRemoveLeadZero(),
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
            gasLimit.toByteArrayRemoveLeadZero(),
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
            gasLimit.toByteArrayRemoveLeadZero(),
            null,
            BigInteger.ZERO.toByteArray(),
            data
        )
    )

    fun sign(key: EthCryptoKey) {
        this.tx.sign(key.ecKey)
        //tx hash will change after sign, so reset hash cache.
        this.resetHash()
    }

    override fun txHash(): Hash {
        return Hash.wrap(this.tx.hash)
    }

    override fun toString(): String {
        return tx.toString()
    }

    fun toHEXString(): String {
        return tx.encoded.toHEXString()
    }

    fun toEthTransaction(): Transaction {
        return tx
    }

}
