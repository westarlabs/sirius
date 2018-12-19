package org.starcoin.sirius.protocol


import org.ethereum.core.Transaction
import org.spongycastle.util.BigIntegers
import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.core.ChainTransaction
import kotlin.properties.Delegates


class EthereumTransaction: ChainTransaction {

    private var ethTransaction : Transaction by Delegates.notNull()

    constructor(
        from: BlockAddress,
        to: BlockAddress,
        nonce: Long,
        gasPrice: Long,
        gasLimit: Long,
        amount: Long,
        data:ByteArray
    ) :super(from, to, amount) {
        ethTransaction = Transaction(
            BigIntegers.asUnsignedByteArray(nonce.toBigInteger()),
            BigIntegers.asUnsignedByteArray(gasPrice.toBigInteger()),
            BigIntegers.asUnsignedByteArray(gasLimit.toBigInteger()),
            to.address,
            BigIntegers.asUnsignedByteArray(amount.toBigInteger()),
            data
        )
    }

    constructor(transaction: Transaction){
        ethTransaction = transaction
    }

}