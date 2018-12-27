package org.starcoin.sirius.protocol.ethereum

import org.ethereum.util.blockchain.EtherUtil
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.EthereumTransaction

class InMemoryEthereumListenerTest {

    @Test
    fun testNewTransaction() {
        var chain = InMemoryChain(true)

        var alice = CryptoService.generateCryptoKey()
        var bob = CryptoService.generateCryptoKey()

        chain.sb.withAccountBalance(bob.getAddress().toBytes(), EtherUtil.convert(100, EtherUtil.Unit.ETHER))
        chain.sb.withAccountBalance(alice.getAddress().toBytes(), EtherUtil.convert(199, EtherUtil.Unit.ETHER))

        var transaction = EthereumTransaction(
            bob.getAddress(),
            0,
            0, 0,
            1,
            null
        )

        chain.newTransaction(alice, transaction)

        chain.sb.createBlock()
    }


}
