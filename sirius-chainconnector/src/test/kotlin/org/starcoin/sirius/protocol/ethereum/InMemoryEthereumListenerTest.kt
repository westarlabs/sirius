package org.starcoin.sirius.protocol.ethereum

import org.ethereum.crypto.ECKey
import org.ethereum.util.blockchain.EtherUtil
import org.junit.Test
import org.starcoin.sirius.core.BlockAddress
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.util.KeyPairUtil
import java.security.KeyPair

class InMemoryEthereumListenerTest {

    @Test
    fun testNewTransaction(){
        var chain= InMemoryChain(true)

        var alice=ECKey()
        var bob = ECKey()

        chain.sb.withAccountBalance(bob.getAddress(), EtherUtil.convert(100, EtherUtil.Unit.ETHER))
        chain.sb.withAccountBalance(alice.getAddress(), EtherUtil.convert(199, EtherUtil.Unit.ETHER))

        var transaction = EthereumTransaction(
            BlockAddress.valueOf(alice.address),
            BlockAddress.valueOf(bob.address),
            0,
            0,0,
            1,
            null
        )

        var keyPair = KeyPairUtil.fromECKey(alice)
        chain.newTransaction(keyPair,transaction)
    }
}