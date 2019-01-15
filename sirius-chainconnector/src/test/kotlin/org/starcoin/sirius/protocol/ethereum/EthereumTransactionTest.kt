package org.starcoin.sirius.protocol.ethereum

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.HubRoot
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.CommitFunction
import org.starcoin.sirius.protocol.EthereumTransaction
import java.math.BigInteger

class EthereumTransactionTest {

    @Test
    fun testTransactionContract() {
        val hubRoot = HubRoot.mock()
        val key = CryptoService.generateCryptoKey()
        val tx = EthereumTransaction(
            key.address,
            0,
            BigInteger.valueOf(2000),
            BigInteger.valueOf(2000),
            CommitFunction.encode(hubRoot)
        )
        Assert.assertTrue(tx.isContractCall)
        Assert.assertTrue(tx.contractFunction is CommitFunction)
        val hubRoot1 = tx.contractFunction?.decode(tx.data)
        Assert.assertEquals(hubRoot, hubRoot1)
    }
}
