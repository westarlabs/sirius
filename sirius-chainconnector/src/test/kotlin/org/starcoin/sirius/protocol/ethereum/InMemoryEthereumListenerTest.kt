package org.starcoin.sirius.protocol.ethereum

import org.ethereum.core.Transaction
import org.ethereum.crypto.ECKey
import org.ethereum.util.ByteUtil
import org.ethereum.util.blockchain.EtherUtil
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.eth.EthCryptoKey
import org.starcoin.sirius.protocol.EthereumTransaction
import java.math.BigInteger

class InMemoryEthereumListenerTest {

    @Test
    fun testNewTransaction() {
        var chain = InMemoryChain(true)

        var alice = CryptoService.generateCryptoKey()
        var bob = CryptoService.generateCryptoKey()

        chain.sb.withAccountBalance(bob.address.toBytes(), EtherUtil.convert(100, EtherUtil.Unit.ETHER))
        chain.sb.withAccountBalance(alice.address.toBytes(), EtherUtil.convert(199, EtherUtil.Unit.ETHER))

        var transaction = EthereumTransaction(
            bob.address,
            0,
            0, 0,
            1,
            null
        )

        chain.sb.sender=(alice as EthCryptoKey).ecKey
        transaction.ethTx.sign((alice as EthCryptoKey).ecKey)
        //chain.newTransaction(alice, transaction)

        println(transaction.ethTx.key)

        var tx=chain.sb.createTransaction(0,(bob as EthCryptoKey).ecKey.address, 1, null)
        println(tx.key)

        chain.sb.createBlock()

    }
}
