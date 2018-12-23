package org.starcoin.sirius.protocol.ethereum

import org.ethereum.crypto.ECKey
import org.ethereum.util.blockchain.EtherUtil
import org.junit.Test
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.util.KeyPairUtil

class InMemoryEthereumListenerTest {

    @Test
    fun testNewTransaction(){
        var chain= InMemoryChain(true)

        var alice=ECKey()
        var bob = ECKey()

        chain.sb.withAccountBalance(bob.getAddress(), EtherUtil.convert(100, EtherUtil.Unit.ETHER))
        chain.sb.withAccountBalance(alice.getAddress(), EtherUtil.convert(199, EtherUtil.Unit.ETHER))

        var transaction = EthereumTransaction(
            Address.wrap(alice.address),
            Address.wrap(bob.address),
            0,
            0,0,
            1,
            null
        )

        var keyPair = KeyPairUtil.fromECKey(alice)
        chain.newTransaction(keyPair,transaction)

        chain.sb.createBlock()
    }


}
